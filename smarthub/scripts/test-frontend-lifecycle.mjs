// Reusable mounted-component tests. Uses synthetic responses; no backend or Docker.
import fs from 'node:fs';
import path from 'node:path';
import net from 'node:net';
import crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';
import { spawn } from 'node:child_process';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const frontend = path.resolve(process.env.QA_FRONTEND_ROOT || path.join(root, '../trappy-flow-frontend'));
const output = path.join(root, 'qa-results', `frontend-lifecycle-${new Date().toISOString().replace(/[:.]/g, '-')}`);
fs.mkdirSync(output, { recursive: true });
function fingerprint() {
  const hash = crypto.createHash('sha256');
  const visit = directory => {
    for (const entry of fs.readdirSync(directory, { withFileTypes: true }).sort((a, b) => a.name.localeCompare(b.name))) {
      const file = path.join(directory, entry.name);
      if (entry.isDirectory()) visit(file);
      else if (entry.isFile()) { hash.update(file); hash.update(fs.readFileSync(file)); }
    }
  };
  visit(path.join(frontend, 'src'));
  for (const file of ['package.json', 'package-lock.json']) hash.update(fs.readFileSync(path.join(frontend, file)));
  for (const file of ['qa/vite.config.mjs', 'qa/lifecycle-plugin.mjs', 'qa/frontend/lifecycle.tsx', 'qa/browser/frontend-lifecycle.spec.mjs', 'scripts/test-frontend-lifecycle.mjs']) hash.update(fs.readFileSync(path.join(root, file)));
  return hash.digest('hex');
}
const sourceFingerprint = fingerprint();
const listener = net.createServer();
await new Promise(resolve => listener.listen(0, '127.0.0.1', resolve));
const port = listener.address().port;
await new Promise(resolve => listener.close(resolve));
const env = {};
for (const key of ['PATH', 'HOME', 'TMPDIR', 'SystemRoot']) if (process.env[key]) env[key] = process.env[key];
Object.assign(env, { CI: 'true', NO_COLOR: '1', TZ: 'UTC', QA_RUN_DIR: output,
  QA_FRONTEND_ROOT: frontend, QA_FRONTEND_PORT: String(port), QA_FRONTEND_URL: `http://127.0.0.1:${port}`,
  QA_BACKEND_URL: 'http://127.0.0.1:1', VITE_API_BASE_URL: `http://127.0.0.1:${port}` });
const log = fs.openSync(path.join(output, 'vite.log'), 'w');
const server = spawn(process.execPath, [path.join(frontend, 'node_modules/vite/bin/vite.js'), '--config', path.join(root, 'qa/vite.config.mjs')],
  { cwd: frontend, env, stdio: ['ignore', log, log] });
fs.closeSync(log);
let tests;
const stop = () => { tests?.kill('SIGTERM'); server.kill('SIGTERM'); };
process.on('SIGINT', stop); process.on('SIGTERM', stop);
try {
  const deadline = Date.now() + 60000;
  let ready = false;
  while (Date.now() < deadline && server.exitCode === null) {
    try { ready = (await fetch(`${env.QA_FRONTEND_URL}/__qa/lifecycle`)).ok; } catch { /* startup */ }
    if (ready) break;
    await new Promise(resolve => setTimeout(resolve, 250));
  }
  if (!ready) throw new Error(`QA Vite failed to start. See ${output}/vite.log`);
  tests = spawn(process.execPath, [path.join(root, 'node_modules/@playwright/test/cli.js'), 'test',
    '--config', path.join(root, 'qa/playwright.config.mjs'), '--global-timeout=300000', 'frontend-lifecycle.spec.mjs', ...process.argv.slice(2)], { cwd: root, env, stdio: 'inherit' });
  process.exitCode = await new Promise((resolve, reject) => { tests.on('error', reject); tests.on('exit', code => resolve(code ?? 1)); });
} finally {
  const closed = new Promise(resolve => server.exitCode !== null ? resolve() : server.once('exit', resolve));
  stop();
  await closed;
  const finalSourceFingerprint = fingerprint();
  if (sourceFingerprint !== finalSourceFingerprint) process.exitCode = 1;
  fs.writeFileSync(path.join(output, 'summary.json'), JSON.stringify({
    exitCode: process.exitCode ?? 1, sourceFingerprint, finalSourceFingerprint,
    sourcesUnchanged: sourceFingerprint === finalSourceFingerprint, serverStopped: server.exitCode !== null || server.signalCode !== null,
    browserReport: path.join(output, 'playwright.json'),
  }, null, 2));
  console.log(`Frontend lifecycle evidence: ${output}`);
}
