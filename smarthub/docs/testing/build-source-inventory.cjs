// Read-only source discovery. Writes planning artifacts only; does not execute app tests.
// Usage: node docs/testing/build-source-inventory.cjs [frontend-root]
const fs = require('node:fs');
const path = require('node:path');
const cp = require('node:child_process');
const backend = path.resolve(__dirname, '../..');
const frontend = path.resolve(process.argv[2] || path.join(backend, '../trappy-flow-frontend'));
const output = process.env.QA_INVENTORY_OUTPUT ? path.resolve(process.env.QA_INVENTORY_OUTPUT) : __dirname;
const ts = require(path.join(frontend, 'node_modules/typescript'));
const files = root => fs.readdirSync(root, {withFileTypes: true}).flatMap(e =>
  e.isDirectory() ? files(path.join(root, e.name)) : [path.join(root, e.name)]).sort();
const line = (s, pos) => s.slice(0, pos).split('\n').length;
const compact = s => s.replace(/\s+/g, ' ').trim();
const revision = root => cp.execFileSync('git', ['rev-parse', 'HEAD'], {cwd: root, encoding: 'utf8'}).trim();
// Remove comments while preserving strings and offsets; Java is lexically scanned, not type-resolved.
const cleanJava = s => s.replace(/"""[\s\S]*?"""|"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'|\/\*[\s\S]*?\*\/|\/\/[^\n]*/g,
  x => x.startsWith('/') ? x.replace(/[^\n]/g, ' ') : x);
function balanced(s, start) {
  let depth = 0, quote = null;
  for (let i = start; i < s.length; i++) {
    const c = s[i];
    if (quote) { if (c === '\\') i++; else if (c === quote) quote = null; continue; }
    if (c === '"' || c === "'") { quote = c; continue; }
    if (c === '(') depth++;
    if (c === ')' && --depth === 0) return i + 1;
  }
  return start;
}
const result = {generatedAt: new Date().toISOString(), backend, frontend,
  backendRevision: revision(backend), frontendRevision: revision(frontend),
  backendWorkingTree: cp.execFileSync('git', ['status', '--short', '--untracked-files=no'], {cwd: backend, encoding: 'utf8'}).trim(),
  frontendWorkingTree: cp.execFileSync('git', ['status', '--short', '--untracked-files=no'], {cwd: frontend, encoding: 'utf8'}).trim(),
  status: 'DISCOVERED_NOT_TESTED',
  limitations: ['Working-tree source snapshot, not a test execution or deployed-state audit.',
    'Java annotation scan requires reconciliation with runtime route registration; composed/inherited mappings may be missed.',
    'Frontend AST declarations and control candidates do not prove reachability or backend wiring.',
    'No existing test is credited as passing. Every discovered item still requires scenario mapping and execution.'],
  endpoints: [], queryInputs: [], requestFields: [], jobsAndListeners: [],
  routes: [], filterTypes: [], filterState: [], uiControls: [], frontendFiles: [], testFiles: []};
for (const file of files(path.join(backend, 'src/main/java')).filter(f => f.endsWith('.java'))) {
  const original = fs.readFileSync(file, 'utf8'), s = cleanJava(original);
  const ref = pos => ({file: path.relative(backend, file), line: line(s, pos)});
  if (/@RestController\b|@Controller\b/.test(s)) {
    const classPos = s.search(/\bclass\s+\w+/);
    const baseMatch = /@RequestMapping\s*(\([^]*?\))/.exec(s.slice(0, classPos));
    const base = baseMatch ? compact(baseMatch[1]) : '(no class mapping)';
    for (const m of s.matchAll(/@(Get|Post|Put|Patch|Delete|Request)Mapping\b/g)) {
      if (m.index < classPos) continue;
      let end = m.index + m[0].length;
      while (/\s/.test(s[end] || '') && end < s.length) end++;
      const annotation = s[end] === '(' ? s.slice(m.index, balanced(s, end)) : m[0];
      const rest = s.slice(m.index + annotation.length);
      const method = /^\s*public\s+[^;{=]+?\b(\w+)\s*\(/m.exec(rest);
      let signature = '';
      if (method) {
        const start = m.index + annotation.length + method.index;
        const paren = start + method[0].lastIndexOf('(');
        signature = s.slice(start, balanced(s, paren));
      }
      const id = 'API-' + String(result.endpoints.length + 1).padStart(4, '0');
      result.endpoints.push({id, ...ref(m.index), base, mapping: compact(annotation), method: method?.[1] || 'UNRESOLVED', signature: compact(signature), status: 'unmapped'});
    }
    for (const m of s.matchAll(/@RequestParam\b/g)) {
      let end = m.index + m[0].length;
      while (/\s/.test(s[end] || '') && end < s.length) end++;
      if (s[end] === '(') end = balanced(s, end);
      const after = s.slice(end).match(/^\s*(?:@[\w.]+(?:\([^)]*\))?\s*)*([\w.<>?, \[\]]+?)\s+(\w+)\s*[,)]/);
      const owner = result.endpoints.filter(e => e.file === path.relative(backend, file) && e.line <= line(s, m.index)).at(-1);
      result.queryInputs.push({...ref(m.index), endpointId: owner?.id, field: after?.[2] || 'UNRESOLVED', type: compact(after?.[1] || ''), annotation: compact(s.slice(m.index, end)), status: 'unmapped'});
    }
  }
  if (/Request|Filter/.test(path.basename(file)) && /\/dto\//.test(file)) {
    for (const m of s.matchAll(/\bprivate\s+(?!static\b|final\b)([\w.<>?, \[\]]+?)\s+(\w+)\s*(?:=[^;]*)?;/g))
      result.requestFields.push({...ref(m.index), field: m[2], type: compact(m[1]), status: 'unmapped'});
  }
  for (const m of s.matchAll(/@(Scheduled|EventListener|TransactionalEventListener|KafkaListener|RabbitListener)\b/g))
    result.jobsAndListeners.push({...ref(m.index), kind: m[1], excerpt: compact(s.slice(m.index, m.index + 240)), status: 'unmapped'});
}
for (const file of files(path.join(frontend, 'src')).filter(f => /\.tsx?$/.test(f))) {
  const s = fs.readFileSync(file, 'utf8');
  const sf = ts.createSourceFile(file, s, ts.ScriptTarget.Latest, true, file.endsWith('tsx') ? ts.ScriptKind.TSX : ts.ScriptKind.TS);
  const ref = n => ({file: path.relative(frontend, file), line: sf.getLineAndCharacterOfPosition(n.getStart(sf)).line + 1});
  result.frontendFiles.push(path.relative(frontend, file));
  function walk(n, parentRoute = '') {
    let nextRoute = parentRoute;
    if (ts.isJsxElement(n) || ts.isJsxSelfClosingElement(n)) {
      const open = ts.isJsxElement(n) ? n.openingElement : n;
      const tag = open.tagName.getText(sf);
      const attrs = open.attributes.properties.filter(ts.isJsxAttribute);
      const attr = name => attrs.find(a => a.name.getText(sf) === name);
      if (tag === 'Route') {
        const p = attr('path');
        const value = p?.initializer && ts.isStringLiteral(p.initializer) ? p.initializer.text : '';
        nextRoute = value.startsWith('/') ? value : [parentRoute.replace(/\/$/, ''), value].filter(Boolean).join('/');
        result.routes.push({...ref(n), route: nextRoute || '/', index: !!attr('index'), declaration: compact(open.getText(sf)), status: 'unmapped'});
      }
      if (/button|input|select|textarea|switch|checkbox|radio|dropdown|filter|date.*picker|tabs?trigger/i.test(tag) || attrs.some(a => /^on(Change|Click|Submit|ValueChange|CheckedChange)$/.test(a.name.getText(sf))))
        result.uiControls.push({...ref(n), tag, attributes: attrs.filter(a => !/^(className|style|icon)$/.test(a.name.getText(sf))).map(a => compact(a.getText(sf))), status: 'unmapped'});
    }
    if ((ts.isInterfaceDeclaration(n) || ts.isTypeAliasDeclaration(n)) && /filter|params/i.test(n.name.text)) {
      const fields = [];
      function collect(p) { if (ts.isPropertySignature(p)) fields.push(compact(p.getText(sf))); ts.forEachChild(p, collect); }
      collect(n);
      result.filterTypes.push({...ref(n), name: n.name.text, fields, declaration: compact(n.getText(sf)), status: 'unmapped'});
    }
    if (ts.isVariableDeclaration(n) && /filter|search|sort/i.test(n.name.getText(sf)))
      result.filterState.push({...ref(n), name: n.name.getText(sf), declaration: compact(n.getText(sf)).slice(0, 700), status: 'unmapped'});
    ts.forEachChild(n, child => walk(child, nextRoute));
  }
  walk(sf);
}
result.testFiles = files(path.join(backend, 'src/test')).filter(f => /\.java$/.test(f)).map(f => path.relative(backend, f));
fs.mkdirSync(output, {recursive: true});
fs.writeFileSync(path.join(output, 'source-inventory.json'), JSON.stringify(result, null, 2) + '\n');
const link = (item, root) => `[${item.file}:${item.line}](${path.join(root, item.file)}:${item.line})`;
const esc = v => String(v).replace(/\|/g, '\\|').replace(/`/g, "'");
let md = '# SmartHub source-discovered testing inventory\n\nStatus: **discovered, not tested**. Generated from local working-tree source. IDs are snapshot identifiers; assign stable scenario IDs during reconciliation.\n\n';
md += `Backend revision: \`${result.backendRevision}\`. Frontend revision: \`${result.frontendRevision}\`.\n\n`;
md += 'Tracked working-tree changes at discovery: ' + (result.backendWorkingTree || result.frontendWorkingTree ? 'present; see JSON manifest. Revision IDs alone do not identify all inspected content.' : 'none.') + '\n\n';
md += result.limitations.map(x => '- ' + x).join('\n') + '\n\n';
md += '## Counts\n\n' + ['endpoints','queryInputs','requestFields','jobsAndListeners','routes','filterTypes','filterState','uiControls','frontendFiles','testFiles'].map(k => `- ${k}: ${result[k].length}`).join('\n') + '\n\n';
md += '## Frontend route declarations\n\n';
for (const r of result.routes) md += `- [ ] \`${r.route}\`${r.index ? ' (index)' : ''} — ${link(r, frontend)}\n`;
md += '\n## Frontend filter and query type fields\n\n';
for (const f of result.filterTypes) md += `### ${f.name}\n\n${link(f, frontend)}\n\n${f.fields.length ? f.fields.map(x => '- [ ] `' + esc(x) + '`').join('\n') : 'Resolve inherited/aliased fields from declaration in JSON.'}\n\n`;
md += '## Backend mapped operations and query inputs\n\n';
for (const e of result.endpoints) {
  md += `- [ ] **${e.id} ${e.method}** — base \`${esc(e.base)}\`, \`${esc(e.mapping)}\` — ${link(e, backend)}\n`;
  const qs = result.queryInputs.filter(q => q.endpointId === e.id);
  if (qs.length) md += '  - Query inputs (one scenario family per field): ' + qs.map(q => `\`${q.field}\``).join(', ') + '\n';
}
md += '\n## Backend request/filter fields\n\n';
let lastFile;
for (const f of result.requestFields) {
  if (f.file !== lastFile) { md += `\n### ${path.basename(f.file)}\n\n${link(f, backend)}\n\n`; lastFile = f.file; }
  md += `- [ ] \`${f.field}\` (${esc(f.type)})\n`;
}
md += '\n## Background entry points\n\n';
for (const j of result.jobsAndListeners) md += `- [ ] ${j.kind} — ${link(j, backend)}\n`;
md += '\n## Reconciliation queue\n\nFull UI control candidates, filter/search/sort state declarations, source-file inventory and existing test-file inventory are in `source-inventory.json`. Review dynamic controls, API request bodies, inherited fields, service rules and websocket handlers before calling discovery complete. Existing test files are not mapped or credited as coverage by this scanner.\n';
fs.writeFileSync(path.join(output, 'source-inventory.md'), md);
console.log(JSON.stringify(Object.fromEntries(Object.entries(result).filter(([,v]) => Array.isArray(v)).map(([k,v]) => [k,v.length])), null, 2));
