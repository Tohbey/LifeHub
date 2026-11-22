// Serves a production build for the offline tests: files as they are, index.html for client routes, the test
// app-config.json (never the developer's own), and 404 for the API (there is no backend).
// Usage: node e2e/support/serve-dist.mjs <directory> <port>
import { createReadStream, statSync } from 'node:fs';
import { createServer } from 'node:http';
import { dirname, extname, join, normalize, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(process.argv[2]);
const port = Number(process.argv[3] ?? 4301);
const testConfig = join(dirname(fileURLToPath(import.meta.url)), 'app-config.e2e.json');
const TYPES = {
  '.html': 'text/html', '.js': 'text/javascript', '.css': 'text/css', '.json': 'application/json',
  '.webmanifest': 'application/manifest+json', '.ico': 'image/x-icon', '.png': 'image/png', '.svg': 'image/svg+xml',
};

createServer((req, res) => {
  const path = normalize(decodeURIComponent(new URL(req.url, 'http://x').pathname));
  if (path.startsWith('/api/')) {
    res.writeHead(404, { 'Content-Type': 'application/problem+json' });
    res.end('{"title":"Not Found","status":404}');
    return;
  }
  let file = path === '/app-config.json' ? testConfig : join(root, path);
  if (file !== testConfig && (!file.startsWith(root) || !isFile(file))) {
    file = join(root, 'index.html');
  }
  res.writeHead(200, { 'Content-Type': TYPES[extname(file)] ?? 'application/octet-stream', 'Cache-Control': 'no-cache' });
  createReadStream(file).pipe(res);
}).listen(port, () => console.log(`Serving ${root} on http://localhost:${port}`));

function isFile(file) {
  try {
    return statSync(file).isFile();
  } catch {
    return false;
  }
}
