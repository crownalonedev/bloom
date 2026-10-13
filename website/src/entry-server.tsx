import type { ComponentType } from "react";
import { prerender } from "react-dom/static";
import { Router } from "wouter";
import { App } from "./App";
import { headFor } from "./lib/head";
import { registerDoc } from "./pages/DocPage";

// Every doc page is available synchronously during prerendering (see DocPage).
const docModules = import.meta.glob<{ default: ComponentType }>("./content/docs/*.mdx", { eager: true });
for (const [path, module] of Object.entries(docModules)) {
  registerDoc(path.replace(/^.*\/(.+)\.mdx$/, "$1"), module.default);
}

async function streamToString(stream: ReadableStream<Uint8Array>): Promise<string> {
  const reader = stream.getReader();
  const decoder = new TextDecoder();
  let html = "";
  for (;;) {
    const { done, value } = await reader.read();
    if (done) break;
    html += decoder.decode(value, { stream: true });
  }
  return html + decoder.decode();
}

/** Renders one route to static HTML. Waits for lazily loaded doc pages (Suspense) to resolve. */
export async function render(path: string) {
  const { prelude } = await prerender(
    <Router ssrPath={path}>
      <App />
    </Router>,
    // Never outline boundaries into hidden segments revealed by inline scripts (blocked by the CSP).
    { progressiveChunkSize: Number.POSITIVE_INFINITY },
  );
  return { html: await streamToString(prelude), head: headFor(path) };
}
