import {test} from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {readJUnit,readPlaywright,buildReport,writeReport} from './report.mjs';

test('JUnit failures/errors/skips and parameterized names survive parsing',()=>{
  const dir=fs.mkdtempSync(path.join(os.tmpdir(),'smarthub-report-'));
  try {
    fs.writeFileSync(path.join(dir,'TEST-sample.xml'),'<testsuite name="Sample"><testcase classname="X" name="works[1]"/><testcase classname="X" name="fails"><failure message="bad"/></testcase><testcase name="broken"><error/></testcase><testcase name="skips"><skipped/></testcase></testsuite>');
    const rows=readJUnit(dir);assert.deepEqual(rows.map(r=>r.status),['passed','failed','failed','skipped']);assert.equal(rows[0].name,'X#works[1]');
  } finally {fs.rmSync(dir,{recursive:true,force:true});}
});

test('flaky retry, unexpected pass and skipped browser check cannot masquerade as pass',()=>{
  const dir=fs.mkdtempSync(path.join(os.tmpdir(),'smarthub-report-'));
  try {
    const file=path.join(dir,'browser.json');
    fs.writeFileSync(file,JSON.stringify({suites:[{specs:[{title:'fixture',tests:[
      {status:'expected',results:[{status:'passed'}]}, {status:'flaky',results:[{status:'failed'},{status:'passed'}]},
      {status:'unexpected',results:[{status:'passed'}]}, {status:'skipped',results:[{status:'skipped'}]},
    ]}]}]}));assert.deepEqual(readPlaywright(file).map(r=>r.status),['passed','failed','failed','skipped']);
  }finally{fs.rmSync(dir,{recursive:true,force:true});}
});

test('passing related checks leave a family partial; missing families stay unverified',()=>{
  const report=buildReport({stages:[{status:'passed'}]},[{name:'[TASK-01] example',status:'passed'}],
    [{id:'TASK-01',evidencePatterns:['[TASK-01]']},{id:'ZOOM-09',evidencePatterns:['[ZOOM-09]']}]);
  assert.equal(report.wholeAppVerified,false);assert.equal(report.scope[0].status,'partial');assert.equal(report.scope[1].status,'unverified');
});

test('zero command exit cannot hide JUnit failures, and blocked prerequisite fails the run',()=>{
  assert.equal(buildReport({stages:[{status:'passed',exitCode:0}]},[{name:'failed test',status:'failed'}],[]).result,'FAILED');
  assert.equal(buildReport({stages:[{status:'blocked'}]},[],[]).result,'FAILED');
});

test('report escapes test-controlled HTML',()=>{
  const dir=fs.mkdtempSync(path.join(os.tmpdir(),'smarthub-report-'));
  try {
    const report=buildReport({id:'qa',stages:[]},[{name:'<script>bad()</script>',status:'passed',evidence:path.join(dir,'sample.xml')}],[]);
    writeReport(dir,report);const html=fs.readFileSync(path.join(dir,'index.html'),'utf8');
    assert(!html.includes('<script>'));assert(html.includes('&lt;script&gt;'));
  }finally{fs.rmSync(dir,{recursive:true,force:true});}
});

test('empty or entirely skipped test evidence cannot pass a run',()=>{
  assert.equal(buildReport({stages:[{status:'passed'}]},[],[]).result,'FAILED');
  assert.equal(buildReport({stages:[{status:'passed'}]},[{name:'disabled',status:'skipped'}],[]).result,'FAILED');
});
