// Checks the prerendered site in dist/client after `vite build` + prerender:
//   links          every internal href points to an existing page or file, and every #fragment to an id on it
//   accessibility  lang, one <h1>, no skipped heading levels, img alt, accessible names for links, buttons and
//                  form fields, unique ids, a working skip link
// Exits with 1 and a list of problems if anything fails. Runs as part of `npm run build`.
import { existsSync, readdirSync, readFileSync, statSync } from "node:fs";
import { join, relative, sep } from "node:path";
import { fileURLToPath } from "node:url";
import { parse } from "parse5";

const root = fileURLToPath(new URL("../dist/client", import.meta.url));

function htmlFiles(dir, acc = []) {
  for (const name of readdirSync(dir)) {
    const path = join(dir, name);
    if (statSync(path).isDirectory()) htmlFiles(path, acc);
    else if (name.endsWith(".html")) acc.push(path);
  }
  return acc;
}

const routeOf = (file) => {
  const rel = relative(root, file).split(sep).join("/").replace(/\.html$/, "");
  return rel === "index" ? "/" : `/${rel}`;
};

const attr = (node, name) => node.attrs?.find((a) => a.name === name)?.value;
const isHidden = (node) => attr(node, "aria-hidden") === "true" || attr(node, "hidden") !== undefined;

function* walk(node) {
  yield node;
  for (const child of node.childNodes ?? []) yield* walk(child);
  if (node.content) yield* walk(node.content); // <template>
}

/** Visible text plus img alt text, skipping aria-hidden subtrees (a simplified accessible-name computation). */
function textOf(node) {
  if (node.nodeName === "#text") return node.value;
  if (isHidden(node)) return "";
  if (node.nodeName === "img") return attr(node, "alt") ?? "";
  if (node.nodeName === "svg") return [...walk(node)].find((n) => n.nodeName === "title") ? "svg-title" : "";
  return (node.childNodes ?? []).map(textOf).join("");
}

const pages = new Map(); // route -> { file, document, ids }
for (const file of htmlFiles(root)) {
  const document = parse(readFileSync(file, "utf8"));
  const ids = new Map();
  for (const node of walk(document)) {
    const id = attr(node, "id");
    if (id !== undefined) ids.set(id, (ids.get(id) ?? 0) + 1);
  }
  pages.set(routeOf(file), { file, document, ids });
}

function resolvePage(path) {
  if (pages.has(path)) return pages.get(path);
  const trimmed = path.length > 1 && path.endsWith("/") ? path.slice(0, -1) : path;
  return pages.get(trimmed) ?? pages.get(`${trimmed}/index`);
}

const problems = [];
const report = (route, message) => problems.push(`${route}: ${message}`);
let links = 0;

for (const [route, { document, ids }] of pages) {
  const nodes = [...walk(document)];
  const labelFor = new Set(nodes.filter((n) => n.nodeName === "label").map((n) => attr(n, "for")).filter(Boolean));

  // ---- document --------------------------------------------------------------------------------------------------
  const html = nodes.find((n) => n.nodeName === "html");
  if (!attr(html, "lang")) report(route, "<html> has no lang attribute");
  const title = nodes.find((n) => n.nodeName === "title");
  if (!title || !textOf(title).trim()) report(route, "missing or empty <title>");
  for (const [id, count] of ids) if (count > 1) report(route, `duplicate id "${id}" (${count}x)`);

  // ---- headings --------------------------------------------------------------------------------------------------
  const headings = nodes.filter((n) => /^h[1-6]$/.test(n.nodeName));
  const h1s = headings.filter((n) => n.nodeName === "h1");
  if (h1s.length !== 1) report(route, `expected exactly one <h1>, found ${h1s.length}`);
  let previous = 0;
  for (const h of headings) {
    const level = Number(h.nodeName[1]);
    if (previous && level > previous + 1) {
      report(route, `heading level skips from h${previous} to h${level} ("${textOf(h).trim().slice(0, 50)}")`);
    }
    if (!textOf(h).trim()) report(route, `empty <${h.nodeName}>`);
    previous = level;
  }

  // ---- skip link -------------------------------------------------------------------------------------------------
  const skip = nodes.find((n) => n.nodeName === "a" && attr(n, "href") === "#main");
  if (!skip) report(route, 'no skip link (<a href="#main">)');
  if (!ids.has("main")) report(route, 'no element with id="main" for the skip link');

  // ---- elements --------------------------------------------------------------------------------------------------
  for (const node of nodes) {
    const name = () =>
      (attr(node, "aria-label") ?? "").trim() || (attr(node, "aria-labelledby") ? "labelledby" : "") ||
      textOf(node).trim() || (attr(node, "title") ?? "").trim();
    switch (node.nodeName) {
      case "img":
        if (attr(node, "alt") === undefined) report(route, `<img src="${attr(node, "src")}"> has no alt attribute`);
        break;
      case "a": {
        if (isHidden(node)) break;
        const href = attr(node, "href");
        if (!name()) report(route, `link to ${href} has no accessible name`);
        if (href === undefined) break;
        if (href.startsWith("#")) {
          links++;
          const id = decodeURIComponent(href.slice(1));
          if (id && !ids.has(id)) report(route, `link to #${id}: no element with that id`);
        } else if (href.startsWith("/") && !href.startsWith("//")) {
          links++;
          const [pathAndQuery, fragment] = href.split("#");
          const path = pathAndQuery.split("?")[0];
          if (existsSync(join(root, path)) && statSync(join(root, path)).isFile()) break; // /llms.txt etc.
          const target = resolvePage(path);
          if (!target) report(route, `broken link: ${href}`);
          else if (fragment && !target.ids.has(decodeURIComponent(fragment))) report(route, `broken anchor: ${href}`);
        }
        break;
      }
      case "button":
        if (!isHidden(node) && !name()) report(route, "<button> has no accessible name");
        break;
      case "input":
      case "select":
      case "textarea": {
        const type = attr(node, "type");
        if (type === "hidden" || isHidden(node)) break;
        const wrapped = (() => {
          for (let p = node.parentNode; p; p = p.parentNode) if (p.nodeName === "label") return true;
          return false;
        })();
        const labelled = attr(node, "aria-label") || attr(node, "aria-labelledby") || wrapped ||
          (attr(node, "id") && labelFor.has(attr(node, "id")));
        if (!labelled) report(route, `<${node.nodeName}${type ? ` type="${type}"` : ""}> has no label`);
        break;
      }
      default:
        break;
    }
  }
}

if (problems.length) {
  console.error(`check-site: ${problems.length} problem(s):\n  ` + problems.join("\n  "));
  process.exit(1);
}
console.log(`check-site: ${pages.size} pages, ${links} internal links and anchors OK, accessibility checks passed`);
