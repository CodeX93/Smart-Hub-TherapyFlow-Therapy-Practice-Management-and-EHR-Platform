#!/usr/bin/env python3
"""Run backend regression suites on a run-owned, disposable local PostgreSQL database."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import secrets
import signal
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent.parent
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--suites", default="qa/backend-tenant-regression-suites.txt",
                    help="Repository-relative suite list, or 'all' for backend JUnit (browser harness runs separately)")
args = parser.parse_args()
SUITE_FILE = None if args.suites == "all" else (ROOT / args.suites).resolve()
if SUITE_FILE is not None and (not SUITE_FILE.is_relative_to(ROOT / "qa") or not SUITE_FILE.is_file()):
    parser.error("Suite list must be an existing file in this repository's qa directory")
SUITES = [line.strip() for line in SUITE_FILE.read_text().splitlines()
          if line.strip() and not line.lstrip().startswith("#")] if SUITE_FILE else []
if SUITE_FILE and (not SUITES or any(not name.isidentifier() for name in SUITES)):
    parser.error("Suite lists must contain Java test class names, one per line")
RUN_ID = "tenant-" + time.strftime("%Y%m%d-%H%M%S") + "-" + secrets.token_hex(4)
OUTPUT = ROOT / "qa-results" / RUN_ID


def source_fingerprint():
    digest = hashlib.sha256()
    files = [ROOT / "pom.xml", Path(__file__).resolve()] + ([SUITE_FILE] if SUITE_FILE else [])
    files.extend((ROOT / "src").rglob("*"))
    for path in sorted(set(path for path in files if path.is_file())):
        digest.update(str(path.relative_to(ROOT)).encode())
        digest.update(path.read_bytes())
    return digest.hexdigest()


def main():
    env = {key: os.environ[key] for key in
           ("PATH", "HOME", "TMPDIR", "JAVA_HOME", "DOCKER_HOST", "DOCKER_CONTEXT", "DOCKER_CONFIG")
           if key in os.environ}
    env.update(CI="true", TZ="UTC")

    def command(args, **kwargs):
        return subprocess.check_output(args, cwd=ROOT, env=env, text=True, **kwargs).strip()

    java = str(Path(env["JAVA_HOME"]) / "bin/java") if env.get("JAVA_HOME") else "java"
    version = command([java, "-version"], stderr=subprocess.STDOUT)
    if 'version "17.' not in version:
        raise RuntimeError("Set JAVA_HOME to JDK 17 before running tenant regression tests")
    endpoint = command(["docker", "context", "inspect", "--format", "{{.Endpoints.docker.Host}}"])
    if not endpoint.startswith("unix://"):
        raise RuntimeError("A local Docker daemon is required")

    OUTPUT.mkdir(parents=True)
    print(f"Evidence: {OUTPUT}", flush=True)
    env["POSTGRES_PASSWORD"] = secrets.token_hex(24)
    container = None
    result_code = 1
    started_fingerprint = source_fingerprint()
    try:
        container = command(["docker", "run", "--rm", "-d", "--label", "smarthub.qa.run=" + RUN_ID,
                             "-e", "POSTGRES_DB=therapyflow_test", "-e", "POSTGRES_USER=postgres",
                             "-e", "POSTGRES_PASSWORD", "-p", "127.0.0.1::5432", "postgres:15", "-c", "max_connections=250"])
        for _ in range(60):
            ready = subprocess.run(["docker", "exec", container, "pg_isready", "-U", "postgres",
                                    "-d", "therapyflow_test"], env=env, stdout=subprocess.DEVNULL,
                                   stderr=subprocess.DEVNULL).returncode == 0
            if ready:
                break
            time.sleep(1)
        else:
            raise RuntimeError("Disposable database did not become ready")
        port = command(["docker", "port", container, "5432/tcp"]).split(":")[-1]
        env.update(
            DB_URL=f"jdbc:postgresql://127.0.0.1:{port}/therapyflow_test",
            DB_USERNAME="postgres", DB_PASSWORD=env.pop("POSTGRES_PASSWORD"),
            SPRING_PROFILES_ACTIVE="test", SPRING_DOCKER_COMPOSE_ENABLED="false",
            SPRING_JPA_SHOW_SQL="false", LOGGING_LEVEL_ROOT="ERROR",
            LOGGING_LEVEL_COM_SMART_THERAPY_FLOW="ERROR", LOGGING_LEVEL_ORG_HIBERNATE_SQL="OFF",
            LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_SECURITY="ERROR", LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_WEB="ERROR",
            APP_STORAGE_LOCAL_PATH=str(OUTPUT / "uploads"), APP_REDIS_ENABLED="false", ZOOM_ENABLED="false",
            SPRING_MAIL_HOST="127.0.0.1", SPRING_MAIL_PORT="1", AI_INTEGRATIONS_OPENAI_BASE_URL="http://127.0.0.1:1",
            TENANT_JOBS_MIGRATION_ENABLED="false", TENANT_MIGRATION_RUN_ON_STARTUP="false",
            CLIENTHUB_MIGRATION_ENABLED="false", QA_MANAGED_DATABASE=RUN_ID)
        with (OUTPUT / "tests.log").open("w") as log:
            process = subprocess.Popen(
                [str(ROOT / "mvnw"), "-B", "-q", "-Dspring.profiles.active=test",
                 "-Dqa.build.directory=" + str(OUTPUT / "build"),
                 "-Dtest=" + (",".join(SUITES) if SUITES else "*Test,*Tests,*IT,!QaApplicationHarnessTest"), "test"],
                cwd=ROOT, env=env, stdout=log, stderr=subprocess.STDOUT, start_new_session=True)
            try:
                result_code = process.wait(timeout=1800)
            finally:
                if process.poll() is None:
                    os.killpg(process.pid, signal.SIGTERM)
                    try:
                        process.wait(timeout=5)
                    except subprocess.TimeoutExpired:
                        os.killpg(process.pid, signal.SIGKILL)
                        process.wait()

        reports = []
        for path in sorted((OUTPUT / "build/surefire-reports").glob("TEST-*.xml")):
            suite = ET.parse(path).getroot()
            reports.append({"name": suite.get("name"), **{
                key: int(suite.get(key, "0")) for key in ("tests", "failures", "errors", "skipped")}})
        missing = sorted(set(SUITES) - {report["name"].split(".")[-1] for report in reports})
        totals = {key: sum(report[key] for report in reports) for key in ("tests", "failures", "errors", "skipped")}
        sources_unchanged = started_fingerprint == source_fingerprint()
        (OUTPUT / "summary.json").write_text(json.dumps(
            {"mavenExit": result_code, "suiteSelection": args.suites,
             "excludedHarness": "QaApplicationHarnessTest (launched separately by scripts/test-app.sh)" if not SUITES else None,
             "missingSuites": missing, "totals": totals, "suites": reports,
             "javaVersion": version, "sourceFingerprint": started_fingerprint, "sourcesUnchanged": sources_unchanged}, indent=2) + "\n")
        print(f"Maven exit: {result_code}; totals: {totals}; missing suites: {missing}", flush=True)
        if not reports or not totals["tests"] or not sources_unchanged or missing or totals["failures"] or totals["errors"] or totals["skipped"]:
            result_code = result_code or 1
    finally:
        if container:
            owner = command(["docker", "inspect", "--format", '{{index .Config.Labels "smarthub.qa.run"}}', container])
            if owner != RUN_ID:
                raise RuntimeError("Refusing to remove a database not owned by this run")
            command(["docker", "stop", container])
            (OUTPUT / "cleanup.json").write_text(json.dumps({"runId": RUN_ID, "databaseRemoved": True}) + "\n")
            print("Run-owned database removed.", flush=True)
    return result_code


if __name__ == "__main__":
    sys.exit(main())
