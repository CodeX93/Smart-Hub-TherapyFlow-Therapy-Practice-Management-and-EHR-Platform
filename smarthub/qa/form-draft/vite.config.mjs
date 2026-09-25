import fs from 'node:fs';
import path from 'node:path';
import { createRequire } from 'node:module';
import { pathToFileURL } from 'node:url';
const frontend = process.env.QA_FRONTEND_ROOT;
const require = createRequire(path.join(frontend, 'package.json'));
const react = (await import(pathToFileURL(require.resolve('@vitejs/plugin-react')).href)).default;
const tailwind = (await import(pathToFileURL(require.resolve('@tailwindcss/vite')).href)).default;
const id = path.join(frontend, '__qa_form_draft.tsx');
export default {
 root: frontend, envDir: false, cacheDir: path.join(process.env.QA_RUN_DIR, 'vite-cache'),
 resolve: {alias: {'@':path.join(frontend,'src')}},
 plugins: [{name:'form-draft-fixture', resolveId(source) {if(source==='/__qa_form_draft.tsx') return id;},
 load(source) {if(source===id) return fs.readFileSync(new URL('./fixture.tsx',import.meta.url),'utf8');},
 configureServer(server) {server.middlewares.use(async(req,res,next)=>{
  if (!['/staff','/user/clinical-forms/1','/user/clinical-forms/2'].includes(req.url?.split('?')[0])) return next();
  res.setHeader('Content-Type','text/html');res.end(await server.transformIndexHtml(req.url,'<html><body><div id="root"></div><script type="module" src="/__qa_form_draft.tsx"></script></body></html>'));
 });}},react(),tailwind()], server:{host:'127.0.0.1',port:Number(process.env.QA_FRONTEND_PORT),strictPort:true}
};
