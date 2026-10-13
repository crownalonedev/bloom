// Prerenders every route into dist/client and writes static hosting files for Cloudflare Workers static assets.
// Set BLOOM_SITE_URL (e.g. https://bloom.example.dev) to emit canonical URLs, Open Graph URLs and sitemap.xml.
import { createHash } from "node:crypto";
import { existsSync, mkdirSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const dist = join(here, "..", "dist");
const client = join(dist, "client");
const siteUrl = (process.env.BLOOM_SITE_URL ?? "").replace(/\/+$/, "");

const { render } = await import(pathToFileURL(join(dist, "server", "entry-server.js")).href);
const docs = JSON.parse(readFileSync(join(here, "..", "src", "generated", "docs.json"), "utf8"));
const template = readFileSync(join(client, "index.html"), "utf8");

const routes = ["/", "/api", "/downloads", "/examples", "/changelog", "/about", ...docs.map((d) => `/docs/${d.slug}`)];

const escape = (s) => s.replace(/&/g, "&amp;").replace(/"/g, "&quot;").replace(/</g, "&lt;");

function headTags(head) {
  const tags = [
    `<title>${escape(head.title)}</title>`,
    `<meta name="description" content="${escape(head.description)}" />`,
    `<meta property="og:site_name" content="Bloom" />`,
    `<meta property="og:type" content="${head.path.startsWith("/docs/") ? "article" : "website"}" />`,
    `<meta property="og:title" content="${escape(head.title)}" />`,
    `<meta property="og:description" content="${escape(head.description)}" />`,
    `<meta name="twitter:card" content="summary_large_image" />`,
    `<meta name="theme-color" content="#140F19" />`,
  ];
  if (siteUrl) {
    tags.push(`<link rel="canonical" href="${siteUrl}${head.path === "/" ? "/" : head.path}" />`);
    tags.push(`<meta property="og:url" content="${siteUrl}${head.path}" />`);
    if (existsSync(join(client, "og.png"))) tags.push(`<meta property="og:image" content="${siteUrl}/og.png" />`);
  }
  if (head.status === 404) tags.push(`<meta name="robots" content="noindex" />`);
  return tags.join("\n    ");
}

function fileFor(route) {
  if (route === "/") return join(client, "index.html");
  return join(client, `${route.slice(1)}.html`);
}

let count = 0;
for (const route of routes) {
  const { html, head } = await render(route);
  if (head.status !== 200) throw new Error(`Route ${route} rendered as ${head.status}`);
  const page = template.replace("<!--app-head-->", headTags(head)).replace("<!--app-html-->", html);
  const file = fileFor(route);
  mkdirSync(dirname(file), { recursive: true });
  writeFileSync(file, page);
  count++;
}
{
  const { html, head } = await render("/this-page-does-not-exist");
  writeFileSync(join(client, "404.html"), template.replace("<!--app-head-->", headTags(head)).replace("<!--app-html-->", html));
}

// ---- static hosting files ----------------------------------------------------------------------------------------
const inline = template.match(/<script>(\/\*theme\*\/[\s\S]*?)<\/script>/);
if (!inline) throw new Error("Theme script not found in index.html");
const themeHash = createHash("sha256").update(inline[1]).digest("base64");

const csp = [
  "default-src 'self'",
  `script-src 'self' 'sha256-${themeHash}'`,
  "style-src 'self' 'unsafe-inline'", // Shiki and React style attributes
  "img-src 'self' data:",
  "font-src 'self'",
  "connect-src 'self'",
  "object-src 'none'",
  "base-uri 'self'",
  "frame-ancestors 'none'",
  "form-action 'self'",
].join("; ");

writeFileSync(
  join(client, "_headers"),
  `/*
  Content-Security-Policy: ${csp}
  X-Content-Type-Options: nosniff
  Referrer-Policy: strict-origin-when-cross-origin
  Permissions-Policy: camera=(), microphone=(), geolocation=(), interest-cohort=()
  X-Frame-Options: DENY
  Cross-Origin-Opener-Policy: same-origin

/assets/*
  Cache-Control: public, max-age=31536000, immutable

/*.html
  Cache-Control: public, max-age=0, must-revalidate

/
  Cache-Control: public, max-age=0, must-revalidate
`,
);
writeFileSync(join(client, "_redirects"), "/docs /docs/introduction 301\n");

const robots = ["User-agent: *", "Allow: /"];
if (siteUrl) {
  robots.push(`Sitemap: ${siteUrl}/sitemap.xml`);
  const urls = routes.map((r) => `  <url><loc>${siteUrl}${r === "/" ? "/" : r}</loc></url>`).join("\n");
  writeFileSync(join(client, "sitemap.xml"), `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${urls}\n</urlset>\n`);
} else {
  rmSync(join(client, "sitemap.xml"), { force: true });
  console.warn("prerender: BLOOM_SITE_URL is not set, so canonical URLs and sitemap.xml were skipped.");
}
writeFileSync(join(client, "robots.txt"), robots.join("\n") + "\n");

// The SSR bundle and Vite manifest are build artefacts, not site content.
rmSync(join(client, ".vite"), { recursive: true, force: true });
console.log(`prerender: ${count} pages + 404.html`);

// Guard: prerendered pages must not contain inline scripts other than the hashed theme script, because the CSP
// blocks them (React emits inline scripts when a Suspense boundary is still pending at render time).
for (const route of routes) {
  const html = readFileSync(fileFor(route), "utf8");
  const inlineScripts = [...html.matchAll(/<script(?![^>]*\bsrc=)[^>]*>([\s\S]*?)<\/script>/g)].filter((m) => !m[1].startsWith("/*theme*/"));
  if (inlineScripts.length) throw new Error(`${route} contains ${inlineScripts.length} inline script(s) the CSP would block`);
}
console.log("prerender: no unexpected inline scripts");
