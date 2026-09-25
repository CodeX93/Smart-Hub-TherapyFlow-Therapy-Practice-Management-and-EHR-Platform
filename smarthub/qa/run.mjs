import fs from 'node:fs';
import path from 'node:path';
import net from 'node:net';
import crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';
import { spawn, execFileSync } from 'node:child_process';
import { readJUnit, readPlaywright, buildReport, writeReport } from './lib/report.mjs';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const args = process.argv.slice(2);
const mode = args[0] || 'smoke';
if (args.includes('--help') || mode === 'help') {
  console.log('Usage: ./scripts/test-app.sh [smoke|full|browser|integrations] [--require-complete]\nSet QA_FRONTEND_ROOT for a non-sibling frontend checkout. Requires Java 17, Node, Docker and installed npm dependencies.\nfull runs all existing Maven tests, frontend typecheck/build/lint and the implemented real-backend browser suite.\nbrowser runs only the isolated application/browser path for focused reruns.\nintegrations runs local provider CONTRACT tests; live-provider validation remains unverified.\n--require-complete fails while any inventory scope is unverified or partial.');
  process.exit(0);
}
if (!['smoke','full','browser','integrations'].includes(mode) || args.slice(1).some(a => a !== '--require-complete')) throw new Error('Unknown argument. Use --help.');
const frontend = path.resolve(process.env.QA_FRONTEND_ROOT || path.join(root, '../trappy-flow-frontend'));
const outputRoot = path.join(root, 'qa-results');
fs.mkdirSync(outputRoot, {recursive: true});
const lock = path.join(outputRoot, '.runner-lock');
try { fs.mkdirSync(lock); } catch { throw new Error(`Another QA runner owns ${lock}. If it crashed, inspect its owner.json and remove the stale lock before retrying.`); }
fs.writeFileSync(path.join(lock, 'owner.json'), JSON.stringify({pid: process.pid, startedAt: new Date().toISOString()}));
const id = new Date().toISOString().replace(/[:.]/g, '-') + '-' + crypto.randomBytes(3).toString('hex');
const output = path.join(outputRoot, id);
fs.mkdirSync(output, {recursive: true, mode: 0o700});
const runtime = path.join(output, '.runtime');
fs.mkdirSync(runtime, {mode: 0o700});
const run = {id, mode, startedAt: new Date().toISOString(), backendRevision: '', frontendRevision: '', sourceFingerprint: '', stages: []};
const cleanEnv = {};
// Do not inherit database credentials, provider keys, application URLs or .env values.
for (const key of ['PATH','HOME','TMPDIR','TEMP','TMP','SystemRoot','DOCKER_HOST','DOCKER_CONTEXT','DOCKER_CONFIG'])
  if (process.env[key]) cleanEnv[key] = process.env[key];
Object.assign(cleanEnv, {CI:'true', NO_COLOR:'1', TZ:'UTC', LANG:'en_US.UTF-8', QA_RUN_DIR:output, QA_RUNTIME_DIR:runtime, QA_FRONTEND_ROOT:frontend});
const pause = ms => new Promise(r => setTimeout(r, ms));
let aborted = false, containerId, env = cleanEnv, cases = [], discovery;
const services = [];
const children = new Set();
function kill(child, signal = 'SIGTERM') {
  if (child.exitCode !== null || child.signalCode !== null) return;
  try { process.kill(process.platform === 'win32' ? child.pid : -child.pid, signal); } catch { /* already exited */ }
}
process.on('SIGINT', () => { aborted = true; for (const c of children) kill(c); });
process.on('SIGTERM', () => { aborted = true; for (const c of children) kill(c); });
function start(name, command, argv, cwd = root, extraEnv = {}) {
  const log = path.join(output, `${name}.log`), fd = fs.openSync(log, 'w', 0o600);
  const child = spawn(command, argv, {cwd, env: {...env, ...extraEnv}, detached: process.platform !== 'win32', stdio: ['ignore', fd, fd]});
  fs.closeSync(fd); children.add(child);
  const done = new Promise(resolve => { child.on('error', e => { fs.appendFileSync(log, `\n${e.message}\n`); resolve(127); }); child.on('exit', code => { children.delete(child); resolve(code ?? 130); }); });
  return {child, done, log};
}
function monitorHostCapacity(name) {
  if (name !== 'backend' || process.platform !== 'darwin') return () => {};
  const log = path.join(output, 'host-capacity.log');
  run.hostCapacityLog = log;
  const sample = () => {
    const timestamp = new Date().toISOString();
    try {
      const capacity = command('/usr/bin/pmset', ['-g', 'therm'], {timeout: 3000});
      fs.appendFileSync(log, `${timestamp}\n${capacity}\n\n`);
    } catch {
      fs.appendFileSync(log, `${timestamp}\nCPU capacity diagnostic unavailable\n\n`);
    }
  };
  sample();
  const timer = setInterval(sample, 30000);
  timer.unref();
  return () => { clearInterval(timer); sample(); };
}
async function stage(name, command, argv, {cwd = root, extraEnv = {}, timeout = 900000, junit = []} = {}) {
  if (aborted) throw new Error('Run interrupted.');
  console.log(`[QA] ${name}`);
  const started = Date.now(), p = start(name, command, argv, cwd, extraEnv);
  const stopCapacity = monitorHostCapacity(name);
  let timedOut = false;
  const timer = setTimeout(() => {timedOut = true; kill(p.child); setTimeout(() => kill(p.child, 'SIGKILL'), 3000).unref();}, timeout);
  const exitCode = await p.done; clearTimeout(timer); stopCapacity();
  const collected = junit.flatMap(readJUnit); cases.push(...collected);
  const failed = exitCode !== 0 || timedOut || collected.some(c => c.status === 'failed') || (junit.length && !collected.length);
  const record = {name, status: failed ? 'failed' : 'passed', exitCode, seconds: (Date.now()-started)/1000, log:p.log, timedOut};
  run.stages.push(record);
  console.log(`[QA] ${name}: ${record.status}${collected.length ? ` (${collected.length} test cases)` : ''}`);
  return !failed;
}
const command = (cmd, argv, options = {}) => execFileSync(cmd, argv, {cwd:root, env, encoding:'utf8', stdio:['ignore','pipe','pipe'], ...options}).trim();
async function freePort() {
  const server = net.createServer(); await new Promise((resolve,reject) => server.listen(0,'127.0.0.1',resolve).on('error',reject));
  const port = server.address().port; await new Promise(r=>server.close(r)); return port;
}
async function waitUntil(predicate, ms, processInfo) {
  const until = Date.now()+ms;
  while (Date.now()<until && !aborted) {
    if (await predicate()) return;
    if (processInfo && (processInfo.child.exitCode !== null || processInfo.child.signalCode !== null)) throw new Error(`Service exited; see ${processInfo.log}`);
    await pause(500);
  }
  throw new Error(aborted ? 'Interrupted' : 'Readiness timed out');
}
function fingerprint() {
  const h = crypto.createHash('sha256');
  for (const repo of [root, frontend]) {
    const names = command('git',['ls-files','-z','--cached','--others','--exclude-standard'],{cwd:repo}).split('\0').filter(Boolean).sort();
    for (const name of names) {
      if (!/^(src\/|qa\/|scripts\/|pom.xml$|package(-lock)?\.json$|vite.config|tsconfig)/.test(name)) continue;
      const file=path.join(repo,name); if(fs.existsSync(file)&&fs.statSync(file).isFile()) {h.update(path.basename(repo)+'/'+name+'\0');h.update(fs.readFileSync(file));}
    }
  }
  return h.digest('hex');
}
const smokeTests = ['SessionStatusBillingTriggerTest','BillingGuardSessionEligibilityTest','InvoicePolicyServiceTest','BillingServiceRulesTest','JwtTokenProviderTest','SessionTranscriptControllerContractTest','TaskServiceTest','FormServiceTest','ChecklistServiceTest','NotificationServiceTest','ZoomServiceTest','SessionTranscriptServiceTest'];
const providerTests = ['ZoomServiceTest','ZoomApiServiceTest','StripeServiceTest','StripeServiceWebhookTest','StripeWebhookEventServiceTest','TwilioSmsServiceTest','SparkPostEmailServiceTest','OpenAiClientComplianceTest','SessionTranscriptControllerContractTest'];
try {
  let javaHome = process.env.JAVA_HOME;
  if (process.platform === 'darwin') javaHome = command('/usr/libexec/java_home',['-v','17']);
  if (javaHome) env.JAVA_HOME = javaHome;
  const java = javaHome ? path.join(javaHome,'bin/java') : 'java';
  // java -version writes stderr; inspect it separately without shell interpolation.
  const javaCheck = spawn(java,['-version'],{env,stdio:['ignore','pipe','pipe']});
  let javaText=''; javaCheck.stderr.on('data',b=>javaText+=b); await new Promise(r=>javaCheck.on('close',r));
  if (!/version "17[.\"]/.test(javaText)) throw new Error('Java 17 is required. Set JAVA_HOME to a JDK 17 installation.');
  if (!fs.existsSync(path.join(frontend,'node_modules/vite/bin/vite.js'))) throw new Error('Install frontend dependencies with npm ci in QA_FRONTEND_ROOT first.');
  const host = env.DOCKER_HOST || JSON.parse(command('docker',['context','inspect','--format','{{json .Endpoints.docker.Host}}']));
  if (!host.startsWith('unix://') && !/^tcp:\/\/(127\.0\.0\.1|localhost):/.test(host)) throw new Error('QA requires a local Docker daemon. Remote Docker contexts are not supported.');
  command('docker',['info','--format','{{.ServerVersion}}']);
  run.backendRevision=command('git',['rev-parse','HEAD']);run.frontendRevision=command('git',['rev-parse','HEAD'],{cwd:frontend});
  run.sourceFingerprint=fingerprint();
  run.workingTree={backend:command('git',['status','--short','--untracked-files=no']),frontend:command('git',['status','--short','--untracked-files=no'],{cwd:frontend})};
  run.stages.push({name:'preflight',status:'passed',exitCode:0});
  await stage('report-selfcheck',process.execPath,['--test',path.join(root,'qa/lib/report.test.mjs')]);
  const password=crypto.randomBytes(24).toString('hex');
  const containerName=`smarthub-qa-${id}`;
  containerId=command('docker',['run','--rm','-d','--name',containerName,'--label',`smarthub.qa.run=${id}`,'-e','POSTGRES_DB=therapyflow_test','-e','POSTGRES_USER=postgres','-e','POSTGRES_PASSWORD','-p','127.0.0.1::5432','postgres:15','-c','max_connections=250'],{env:{...env,POSTGRES_PASSWORD:password}});
  await waitUntil(async()=> { try{return command('docker',['exec',containerId,'pg_isready','-U','postgres','-d','therapyflow_test']).includes('accepting');}catch{return false;} },60000);
  const dbPort=command('docker',['port',containerId,'5432/tcp']).split(':').at(-1);
  Object.assign(env,{DB_URL:`jdbc:postgresql://127.0.0.1:${dbPort}/therapyflow_test`,DB_USERNAME:'postgres',DB_PASSWORD:password,
    QA_MANAGED_DATABASE:id,
    SPRING_PROFILES_ACTIVE:'test',SPRING_DOCKER_COMPOSE_ENABLED:'false',SPRING_JPA_SHOW_SQL:'false',
    LOGGING_LEVEL_ROOT:'ERROR',LOGGING_LEVEL_COM_SMART_THERAPY_FLOW:'ERROR',LOGGING_LEVEL_ORG_HIBERNATE_SQL:'OFF',LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_SECURITY:'ERROR',LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_WEB:'ERROR',
    APP_STORAGE_LOCAL_PATH:path.join(runtime,'uploads'),APP_REDIS_ENABLED:'false',ZOOM_ENABLED:'false',
    SPRING_MAIL_HOST:'127.0.0.1',SPRING_MAIL_PORT:'1',AI_INTEGRATIONS_OPENAI_BASE_URL:'http://127.0.0.1:1',
    TENANT_JOBS_MIGRATION_ENABLED:'false',TENANT_MIGRATION_RUN_ON_STARTUP:'false',CLIENTHUB_MIGRATION_ENABLED:'false'});
  const build=path.join(output,'backend-build');
  const maven=['-B','-q',`-Dqa.build.directory=${build}`,'-Dspring.profiles.active=test','-Dstyle.color=never'];
  const lifecycle= mode==='full' ? ['-Dmaven.test.failure.ignore=true','clean','verify'] : [`-Dtest=${(mode==='integrations'?providerTests:smokeTests).join(',')}`,'test'];
  // Full clean verification also starts a separate Failsafe JVM. Give the whole
  // group enough time; individual load-test assertions remain unchanged.
  if (mode !== 'browser') await stage('backend',path.join(root,'mvnw'),[...maven,...lifecycle],{timeout:mode==='full'?1800000:900000,junit:[path.join(build,'surefire-reports'),...(mode==='full'?[path.join(build,'failsafe-reports')]:[])]});
  if (mode === 'smoke') {
    const billingBuild=path.join(output,'billing-build');
    await stage('billing-database',path.join(root,'mvnw'),['-B','-q',`-Dqa.build.directory=${billingBuild}`,'-Dtest=BillingSessionTriggerIntegrationTest','test'],{junit:[path.join(billingBuild,'surefire-reports')]});
  }
  if (mode === 'full') await stage('coverage-report',path.join(root,'mvnw'),[...maven,'jacoco:report']);
  await stage('frontend-policy',process.execPath,[path.join(root,'scripts/test-billing-ui-policy.mjs'),frontend]);
  await stage('discovery',process.execPath,[path.join(root,'docs/testing/build-source-inventory.cjs'),frontend],{extraEnv:{QA_INVENTORY_OUTPUT:path.join(output,'inventory')}});
  const inventoryFile=path.join(output,'inventory/source-inventory.json');
  if(fs.existsSync(inventoryFile))discovery=JSON.parse(fs.readFileSync(inventoryFile,'utf8'));
  if (mode !== 'integrations') {
    await stage('frontend-typecheck',process.execPath,[path.join(frontend,'node_modules/typescript/bin/tsc'),'-b'],{cwd:frontend});
    if (mode==='full') await stage('frontend-lint','npm',['run','lint'],{cwd:frontend});
    const appBuild=path.join(output,'browser-build');
    // Compile before starting the server readiness clock. Calling Surefire directly
    // also keeps the test-phase JaCoCo HTML report outside the shutdown deadline;
    // the full backend stage still generates its coverage report independently.
    const appMaven=['-B','-q',`-Dqa.build.directory=${appBuild}`,'-Dtest=QaApplicationHarnessTest','-Dqa.browser.enabled=true'];
    if (!await stage('browser-backend-compile',path.join(root,'mvnw'),[...appMaven,'test-compile']))
      throw new Error('Browser backend compilation failed');
    const uiPort=await freePort();
    Object.assign(env,{QA_FRONTEND_PORT:String(uiPort),QA_FRONTEND_URL:`http://127.0.0.1:${uiPort}`,CORS_ALLOWED_ORIGINS:`http://127.0.0.1:${uiPort}`});
    // Reset only this run's owned disposable container, never an externally supplied DB.
    command('docker',['exec',containerId,'psql','-U','postgres','-d','therapyflow_test','-v','ON_ERROR_STOP=1','-c',
      `DROP SCHEMA IF EXISTS public CASCADE; CREATE SCHEMA public; CREATE TABLE public.qa_run_guard (run_id text NOT NULL); INSERT INTO public.qa_run_guard VALUES ('${id}');`]);
    const app=start('backend-browser-server',path.join(root,'mvnw'),[...appMaven,'surefire:test']);
    services.push(app);
    await waitUntil(async()=>fs.existsSync(path.join(runtime,'ready.json')),240000,app);
    const ready=JSON.parse(fs.readFileSync(path.join(runtime,'ready.json'),'utf8'));
    Object.assign(env,{QA_BACKEND_URL:ready.baseUrl,QA_FRONTEND_PORT:String(uiPort),QA_FRONTEND_URL:`http://127.0.0.1:${uiPort}`,VITE_API_BASE_URL:`http://127.0.0.1:${uiPort}`});
    await stage('frontend-build',process.execPath,[path.join(frontend,'node_modules/vite/bin/vite.js'),'build','--config',path.join(root,'qa/vite.config.mjs'),'--outDir',path.join(output,'frontend-build')],{cwd:frontend});
    const vite=start('frontend-server',process.execPath,[path.join(frontend,'node_modules/vite/bin/vite.js'),'--config',path.join(root,'qa/vite.config.mjs')],frontend);services.push(vite);
    await waitUntil(async()=> {try{return(await fetch(env.QA_FRONTEND_URL)).ok;}catch{return false;}},60000,vite);
    const browserPassed=await stage('browser',process.execPath,[path.join(root,'node_modules/@playwright/test/cli.js'),'test','--config',path.join(root,'qa/playwright.config.mjs')]);
    const browserCases=readPlaywright(path.join(output,'playwright.json'));cases.push(...browserCases);
    if(browserPassed&&(!browserCases.length||browserCases.some(c=>c.status==='failed'))) run.stages.push({name:'browser-evidence',status:'failed',reason:'Missing or failed browser case evidence'});
    const shutdownStarted=Date.now();
    fs.writeFileSync(path.join(runtime,'stop'),'stop');
    const appExit=await Promise.race([app.done,pause(20000).then(()=>-1)]);
    run.stages.push({name:'browser-backend-lifecycle',status:appExit===0?'passed':'failed',exitCode:appExit,seconds:(Date.now()-shutdownStarted)/1000,log:app.log});
  }
} catch(error) {
  run.stages.push({name:'runner',status:'blocked',reason:error.message});console.error(`[QA] ${error.message}`);
} finally {
  for(const p of services)kill(p.child);
  await pause(500);
  for(const p of services)kill(p.child,'SIGKILL');
  let cleanupOk=true;
  if(containerId) {try {const owner=command('docker',['inspect','--format','{{index .Config.Labels "smarthub.qa.run"}}',containerId]);if(owner!==id)throw new Error('Container ownership mismatch');command('docker',['stop',containerId]);}catch(e){cleanupOk=false;console.error('[QA] Container cleanup failed; inspect the run-owned container.');}}
  fs.rmSync(runtime,{recursive:true,force:true});
  run.stages.push({name:'cleanup',status:cleanupOk?'passed':'failed'});
  if (run.sourceFingerprint) {
    try {run.finalSourceFingerprint=fingerprint();run.stages.push({name:'source-consistency',status:run.finalSourceFingerprint===run.sourceFingerprint?'passed':'failed',reason:'Source fingerprint must remain unchanged during a run'});}
    catch(e){run.stages.push({name:'source-consistency',status:'blocked',reason:e.message});}
  }
  run.completedAt=new Date().toISOString();
  const catalog=JSON.parse(fs.readFileSync(path.join(root,'qa/scope.json'),'utf8'));
  const report=buildReport(run,cases,catalog,discovery);writeReport(output,report);
  fs.writeFileSync(path.join(outputRoot,'latest.json'),JSON.stringify({id,report:path.join(output,'index.html')},null,2)+'\n');
  fs.rmSync(lock,{recursive:true,force:true});
  console.log(`[QA] ${report.result}: ${report.totals.passed} passed, ${report.totals.failed} failed, ${report.totals.skipped} skipped. Coverage remains incomplete.\n[QA] Report: ${path.join(output,'index.html')}`);
  process.exitCode=aborted?130:report.result==='FAILED'?1:args.includes('--require-complete')?2:0;
}
