// Real React/RTK Query browser checks with synthetic HTTP responses; no backend writes.
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import net from 'node:net';
import { fileURLToPath } from 'node:url';
import { spawn } from 'node:child_process';
import { execFile } from 'node:child_process';
import { promisify } from 'node:util';
const exec = promisify(execFile);
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const frontend = path.resolve(process.env.QA_FRONTEND_ROOT || path.join(root, '../trappy-flow-frontend'));
const output = path.join(root, 'output/playwright', `form-draft-${Date.now()}`);
fs.mkdirSync(output, {recursive:true});
const cli = process.env.QA_PLAYWRIGHT_CLI || path.join(os.homedir(), '.codex/skills/playwright/scripts/playwright_cli.sh');
const session = `draft-${process.pid}`;
const listener = net.createServer();
await new Promise(resolve => listener.listen(0, '127.0.0.1', resolve));
const port = listener.address().port;
await new Promise(resolve => listener.close(resolve));
const env = {};
for (const key of ['PATH','HOME','TMPDIR','SystemRoot']) if(process.env[key]) env[key]=process.env[key];
Object.assign(env,{QA_FRONTEND_ROOT:frontend,QA_RUN_DIR:output,QA_FRONTEND_PORT:String(port)});
const log=fs.openSync(path.join(output,'vite.log'),'w');
const server=spawn(process.execPath,[path.join(frontend,'node_modules/vite/bin/vite.js'),'--config',path.join(root,'qa/form-draft/vite.config.mjs')],{cwd:root,env,stdio:['ignore',log,log]});
fs.closeSync(log);
const browser=async(...args)=> (await exec('bash',[cli,`-s=${session}`,...args],{cwd:root,timeout:240000,maxBuffer:4*1024*1024})).stdout;
let result;
try {
 const deadline=Date.now()+60000;let ready=false;
 while(Date.now()<deadline&&server.exitCode===null){
  try{ready=(await fetch(`http://127.0.0.1:${port}/staff`)).ok;}catch{}
  if(ready)break;await new Promise(resolve=>setTimeout(resolve,250));
 }
 if(!ready)throw new Error('QA Vite did not start');
 await browser('open',`http://127.0.0.1:${port}/staff`);
 const text=await browser('run-code','--filename',path.join(root,'qa/form-draft/checks.js'));
 fs.writeFileSync(path.join(output,'browser.txt'),text);
 const json=text.match(/### Result\s*\n(\{[^\n]+\})/);
 if(!json)throw new Error(`Browser checks did not return a result: ${output}/browser.txt`);
 result=JSON.parse(json[1]);
 process.exitCode=result.failed===0?0:1;
 console.log(JSON.stringify(result,null,2));
} catch(error){process.exitCode=1;result={error:error.message};console.error(error.message);}
finally {
 try{await browser('close');}catch(error){result={...result,browserCleanupError:error.message};process.exitCode=1;}
 const exited=new Promise(resolve=>server.exitCode!==null||server.signalCode!==null?resolve():server.once('exit',resolve));
 server.kill('SIGTERM');
 const killTimer=setTimeout(()=>server.kill('SIGKILL'),5000);
 await exited;clearTimeout(killTimer);
 fs.writeFileSync(path.join(output,'summary.json'),JSON.stringify({...result,exitCode:process.exitCode??1,serverStopped:true},null,2));
 console.log(`Form draft evidence: ${output}`);
}
