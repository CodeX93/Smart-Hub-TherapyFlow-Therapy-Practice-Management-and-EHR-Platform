#!/usr/bin/env ruby
# frozen_string_literal: true

require "json"
require "minitest/autorun"
require "open3"
require "tmpdir"
require "yaml"

ROOT = File.expand_path("..", __dir__)
CI_FILE = File.join(ROOT, ".gitlab-ci.yml")
HELPER = File.join(ROOT, "scripts", "verify-production-evidence.sh")

class ProductionCiGatesTest < Minitest::Test
  def setup
    @ci = YAML.safe_load(File.read(CI_FILE), aliases: true)
    @deploy = @ci.fetch("deploy-prod")
    @all_script = @ci.values.grep(Hash).map { |job| job["script"] }.compact.flatten.join("\n")
  end

  def test_production_test_gate_runs_unit_and_integration_tests_without_bypass
    test_job = @ci.fetch("test")
    script = Array(test_job.fetch("script")).join("\n")

    refute @ci.fetch("variables", {}).key?("SKIP_TESTS"), "production CI must not define SKIP_TESTS"
    refute_match(/SKIP_TESTS|skipTests|maven\.test\.skip/, script)
    assert_includes script, "mvn clean verify -B"
    refute test_job["allow_failure"], "the production test job must be blocking"
    junit_reports = Array(test_job.dig("artifacts", "reports", "junit"))
    assert_includes junit_reports, "target/surefire-reports/TEST-*.xml"
    assert_includes junit_reports, "target/failsafe-reports/TEST-*.xml"

    stages = @ci.fetch("stages")
    assert_operator stages.index(test_job.fetch("stage")), :<, stages.index(@ci.fetch("build").fetch("stage"))
    assert_operator stages.index(test_job.fetch("stage")), :<, stages.index(@deploy.fetch("stage"))
  end

  def test_production_gate_jobs_are_blocking_and_deploy_needs_their_artifacts
    required = %w[test dependency-scan secret-scan migration-validation]
    stages = @ci.fetch("stages")
    deploy_stage = stages.index(@deploy.fetch("stage"))
    needed_jobs = Array(@deploy["needs"]).map { |need| need.is_a?(Hash) ? need["job"] : need }

    required.each do |name|
      job = @ci[name]
      refute_nil job, "#{name} job is required"
      next unless job
      assert_operator stages.index(job.fetch("stage")), :<, deploy_stage, "#{name} must precede deploy-prod"
      refute_equal true, job["allow_failure"], "#{name} must be blocking"
      refute_equal "manual", job["when"], "#{name} must not be manual"
      assert_includes needed_jobs, name
    end

    refute_equal true, @deploy["allow_failure"]
    assert_equal %w[main tags], Array(@deploy.fetch("only"))
  end

  def test_required_scan_and_migration_reports_are_declared_as_artifacts
    {
      "dependency-scan" => "release-evidence/dependency-scan.status",
      "secret-scan" => "release-evidence/secret-scan.status",
      "migration-validation" => "release-evidence/migration-status.txt"
    }.each do |job_name, artifact|
      job = @ci[job_name]
      refute_nil job, "#{job_name} job is required"
      next unless job
      paths = Array(job.dig("artifacts", "paths"))
      assert_includes paths, artifact, "#{job_name} must publish #{artifact}"
    end

    assert_includes Array(@ci.fetch("build-docker-prod").dig("artifacts", "paths")),
                    "release-evidence/image-provenance.json"
    deploy_artifacts = Array(@deploy.dig("artifacts", "paths"))
    assert deploy_artifacts.include?("release-evidence/release-metadata.json") ||
           deploy_artifacts.include?("release-evidence/"),
           "deploy-prod must publish release metadata"
  end

  def test_preflight_stops_before_deploy_when_a_report_is_missing_or_security_failed
    assert File.executable?(HELPER), "production evidence preflight helper is missing or not executable"

    Dir.mktmpdir do |directory|
      write_common_evidence(directory)
      File.delete(File.join(directory, "secret-scan.status"))
      assert_deploy_not_reached(directory, "missing report must block deployment")

      File.write(File.join(directory, "secret-scan.status"), "status=failed\n")
      assert_deploy_not_reached(directory, "failed security scan must block deployment")
    end
  end

  def test_release_metadata_records_commit_image_tests_scans_migration_health_and_traffic
    assert File.executable?(HELPER), "production evidence helper is missing or not executable"

    Dir.mktmpdir do |directory|
      write_common_evidence(directory)
      File.write(File.join(directory, "health.status"), "status=passed\nhttp_status=200\n")
      File.write(File.join(directory, "traffic.status"), "status=passed\nactive_weight=100\n")
      env = {
        "RELEASE_EVIDENCE_DIR" => directory,
        "CI_COMMIT_SHA" => "commit-123",
        "IMAGE_REFERENCE" => "registry.example/therapy-flow:prod-123",
        "IMAGE_DIGEST" => "sha256:abc123"
      }

      output, error, status = Open3.capture3(env, "bash", HELPER, "post-deploy", chdir: ROOT)
      assert status.success?, "post-deploy evidence failed: #{output}\n#{error}"

      metadata = JSON.parse(File.read(File.join(directory, "release-metadata.json")))
      assert_equal "commit-123", metadata.dig("commit", "sha")
      assert_equal "registry.example/therapy-flow:prod-123", metadata.dig("image", "reference")
      assert_equal "sha256:abc123", metadata.dig("image", "digest")
      assert_equal "passed", metadata.dig("tests", "status")
      assert_equal "passed", metadata.dig("scans", "dependency")
      assert_equal "passed", metadata.dig("scans", "secrets")
      assert_equal "passed", metadata.dig("migration", "status")
      assert_equal "passed", metadata.dig("health", "status")
      assert_equal "passed", metadata.dig("traffic", "status")
    end
  end

  def test_deploy_script_validates_evidence_before_invoking_azure_deploy
    script = Array(@deploy.fetch("script")).join("\n")
    preflight = script.index("verify-production-evidence.sh pre-deploy")
    deploy = script.index("./scripts/deploy-azure.sh --deploy-only")

    refute_nil preflight, "deploy-prod must run the evidence preflight"
    refute_nil deploy, "deploy-prod must invoke the deploy script"
    assert_operator preflight, :<, deploy, "missing or failed reports must be checked before deployment"
    assert_includes script, "verify-production-evidence.sh post-deploy"
    assert_match(/containerapp show.*traffic|traffic.*containerapp show/m, script)
    assert_match(/actuator\/health/, script)
  end

  private

  def write_common_evidence(directory)
    File.write(File.join(directory, "test-status.txt"), "status=passed\n")
    File.write(File.join(directory, "dependency-scan.status"), "status=passed\n")
    File.write(File.join(directory, "secret-scan.status"), "status=passed\n")
    File.write(File.join(directory, "migration-status.txt"), "status=passed\n")
    File.write(File.join(directory, "image-provenance.json"), <<~JSON)
      {"image":"registry.example/therapy-flow:prod-123","digest":"sha256:abc123"}
    JSON
  end

  def assert_deploy_not_reached(directory, message)
    marker = File.join(directory, "deploy-called")
    env = { "RELEASE_EVIDENCE_DIR" => directory }
    command = "bash #{HELPER} pre-deploy && touch #{marker}"
    _output, _error, status = Open3.capture3(env, "bash", "-e", "-c", command, chdir: ROOT)
    refute status.success?, message
    refute File.exist?(marker), message
  end
end
