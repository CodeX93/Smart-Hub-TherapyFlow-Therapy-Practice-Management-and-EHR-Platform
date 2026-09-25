import fs from 'node:fs';
import path from 'node:path';

// This fixture is served only by the isolated QA Vite configuration.
export function lifecyclePlugin(frontend) {
  const moduleId = path.join(frontend, '__qa_lifecycle.tsx');
  return {
    name: 'qa-frontend-lifecycle',
    resolveId(id) { if (id === '/__qa/lifecycle.tsx') return moduleId; },
    load(id) {
      if (id === moduleId) return fs.readFileSync(new URL('./frontend/lifecycle.tsx', import.meta.url), 'utf8');
    },
    configureServer(server) {
      server.middlewares.use(async (request, response, next) => {
        if (request.url?.split('?')[0] !== '/__qa/lifecycle') return next();
        const html = await server.transformIndexHtml('/__qa/lifecycle',
          '<html><head></head><body><div id="qa-root"></div><script type="module" src="/__qa/lifecycle.tsx"></script></body></html>');
        response.setHeader('Content-Type', 'text/html');
        response.end(html);
      });
    },
  };
}
