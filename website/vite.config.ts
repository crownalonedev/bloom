import mdx from "@mdx-js/rollup";
import rehypeShiki from "@shikijs/rehype";
import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import rehypeSlug from "rehype-slug";
import remarkFrontmatter from "remark-frontmatter";
import remarkGfm from "remark-gfm";
import remarkMdxFrontmatter from "remark-mdx-frontmatter";
import { defineConfig } from "vite";
import { fileURLToPath } from "node:url";
import { bloomDark, bloomLight } from "./mdx/shiki-themes.ts";
import { remarkSnippets } from "./mdx/remark-snippets.ts";

const snippetsFile = fileURLToPath(new URL("./src/generated/snippets.json", import.meta.url));

type ShikiNode = { properties: Record<string, unknown> };

export default defineConfig({
  plugins: [
    {
      enforce: "pre",
      ...mdx({
        providerImportSource: "/src/components/mdx-components.tsx",
        remarkPlugins: [remarkFrontmatter, remarkMdxFrontmatter, remarkGfm, remarkSnippets({ file: snippetsFile })],
        rehypePlugins: [
          rehypeSlug,
          [
            rehypeShiki,
            {
              themes: { light: bloomLight, dark: bloomDark },
              defaultColor: false,
              langs: ["java", "kotlin", "groovy", "yaml", "bash", "shell", "json", "xml", "properties", "text", "tsx"],
              transformers: [
                {
                  pre(this: { options: { lang: string; meta?: { __raw?: string } } }, node: ShikiNode) {
                    node.properties["data-language"] = this.options.lang;
                    const raw = this.options.meta?.__raw ?? "";
                    const title = raw.match(/title="([^"]+)"/);
                    const source = raw.match(/source="([^"]+)"/);
                    if (title) node.properties["data-title"] = title[1];
                    if (source) node.properties["data-source"] = source[1];
                  },
                },
              ],
            },
          ],
        ],
      }),
    },
    react({ include: /\.(mdx|tsx|ts|jsx|js)$/ }),
    tailwindcss(),
  ],
  build: {
    manifest: true,
    target: "es2022",
    // Never inline fonts as data: URIs; the CSP only allows fonts from 'self'.
    assetsInlineLimit: (file) => (/\.(woff2?|ttf|otf)$/.test(file) ? false : undefined),
  },
  ssr: {
    noExternal: ["wouter"],
  },
});
