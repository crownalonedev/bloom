import { Check, Copy, FileCode2 } from "lucide-react";
import { type ComponentProps, useRef, useState } from "react";
import { project } from "../lib/site";

const LANGUAGE_NAMES: Record<string, string> = {
  java: "Java",
  kotlin: "Kotlin",
  groovy: "Groovy",
  yaml: "YAML",
  bash: "Shell",
  shell: "Shell",
  json: "JSON",
  xml: "XML",
  properties: "Properties",
  text: "Text",
  tsx: "TSX",
};

type PreProps = ComponentProps<"pre"> & {
  "data-language"?: string;
  "data-title"?: string;
  "data-source"?: string;
};

/** Code block with a language/file label, copy button and, for compiled snippets, a link to the source file. */
export function CodeBlock(props: PreProps) {
  const { "data-language": language, "data-title": title, "data-source": source, className, children, ...rest } = props;
  const ref = useRef<HTMLPreElement>(null);
  const [copied, setCopied] = useState(false);

  async function copy() {
    const text = ref.current?.innerText ?? "";
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 1600);
    } catch {
      setCopied(false);
    }
  }

  const label = title ?? LANGUAGE_NAMES[language ?? ""] ?? language ?? "Code";
  const [path, lines] = (source ?? "").split("#");
  const sourceUrl = source ? `${project.repository}/blob/main/${path}${lines ? `#${lines}` : ""}` : null;

  return (
    <figure className="not-prose group my-6 overflow-hidden rounded-lg border border-line bg-code">
      <figcaption className="flex items-center justify-between gap-3 border-b border-line px-4 py-2 text-[0.8rem] text-muted">
        <span className="flex min-w-0 items-center gap-2">
          <FileCode2 size={14} aria-hidden="true" />
          <span className="truncate">{label}</span>
        </span>
        <span className="flex shrink-0 items-center gap-3">
          {sourceUrl && (
            <a
              href={sourceUrl}
              className="inline-flex min-h-6 items-center hover:text-ink hover:underline"
              title="This example is extracted from code the Bloom build compiles"
            >
              View source
            </a>
          )}
          <button
            type="button"
            onClick={copy}
            className="inline-flex min-h-6 items-center gap-1.5 rounded px-1.5 py-0.5 hover:bg-surface-2 hover:text-ink"
            aria-label={copied ? "Copied" : "Copy code"}
          >
            {copied ? <Check size={14} aria-hidden="true" /> : <Copy size={14} aria-hidden="true" />}
            <span aria-live="polite">{copied ? "Copied" : "Copy"}</span>
          </button>
        </span>
      </figcaption>
      <pre
        ref={ref}
        {...rest}
        className={`${className ?? ""} overflow-x-auto px-4 py-3.5 font-mono text-[0.84rem] leading-[1.65]`}
        tabIndex={0}
      >
        {children}
      </pre>
    </figure>
  );
}
