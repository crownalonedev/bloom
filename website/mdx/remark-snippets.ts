// Replaces ```java snippet="id"``` code fences with code extracted from real, compiled Java sources
// (see scripts/build-content.mjs). This keeps documentation examples identical to code the Gradle build compiles.
import { readFileSync } from "node:fs";
import { visit } from "unist-util-visit";

type CodeNode = { type: "code"; lang?: string | null; meta?: string | null; value: string };

export function remarkSnippets(options: { file: string }) {
  return () => {
    const snippets = JSON.parse(readFileSync(options.file, "utf8")) as Record<string, { code: string; source: string }>;
    return (tree: unknown) => {
      visit(tree as never, "code", (node: CodeNode) => {
        const match = node.meta?.match(/snippet="([^"]+)"/);
        if (!match) return;
        const id = match[1]!;
        const snippet = snippets[id];
        if (!snippet) {
          throw new Error(`Unknown code snippet "${id}". Mark it in Java with // @snippet:${id} ... // @end`);
        }
        node.value = snippet.code;
        const rest = node.meta!.replace(match[0], "").trim();
        node.meta = `${rest} source="${snippet.source}"`.trim();
      });
    };
  };
}
