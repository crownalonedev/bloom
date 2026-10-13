import { Search } from "lucide-react";
import { Fragment, useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useLocation } from "wouter";

type Entry = { slug: string; page: string; heading: string; anchor: string; text: string };
type Result = Entry & { score: number };

let indexPromise: Promise<Entry[]> | null = null;
function loadIndex(): Promise<Entry[]> {
  indexPromise ??= import("../generated/search-index.json").then((m) => m.default as Entry[]);
  return indexPromise;
}

function terms(query: string): string[] {
  return query.toLowerCase().split(/\s+/).filter((t) => t.length > 0);
}

/** Every term must match the page title, heading or text; titles and headings weigh more. */
export function rank(index: Entry[], query: string): Result[] {
  const words = terms(query);
  if (!words.length) return [];
  const results: Result[] = [];
  for (const entry of index) {
    const page = entry.page.toLowerCase();
    const heading = entry.heading.toLowerCase();
    const text = entry.text.toLowerCase();
    let score = 0;
    let all = true;
    for (const word of words) {
      const s = (page.includes(word) ? 6 : 0) + (heading.includes(word) ? 10 : 0) + (text.includes(word) ? 1 : 0);
      if (!s) {
        all = false;
        break;
      }
      score += s + (heading.startsWith(word) ? 3 : 0);
    }
    if (all) results.push({ ...entry, score });
  }
  return results.sort((a, b) => b.score - a.score).slice(0, 12);
}

function Highlight({ text, query }: { text: string; query: string }) {
  const words = terms(query).map((w) => w.replace(/[.*+?^${}()|[\]\\]/g, "\\$&"));
  if (!words.length) return <>{text}</>;
  const parts = text.split(new RegExp(`(${words.join("|")})`, "gi"));
  return (
    <>
      {parts.map((part, i) =>
        i % 2 === 1 ? (
          <mark key={i} className="rounded-sm bg-blossom/30 text-ink">
            {part}
          </mark>
        ) : (
          <Fragment key={i}>{part}</Fragment>
        ),
      )}
    </>
  );
}

function excerpt(text: string, query: string): string {
  const first = terms(query)[0];
  const at = first ? text.toLowerCase().indexOf(first) : -1;
  if (at < 0) return text.slice(0, 140);
  const start = Math.max(0, at - 50);
  return (start > 0 ? "…" : "") + text.slice(start, start + 150) + "…";
}

export function SearchDialog() {
  const dialog = useRef<HTMLDialogElement>(null);
  const input = useRef<HTMLInputElement>(null);
  const [index, setIndex] = useState<Entry[]>([]);
  const [query, setQuery] = useState("");
  const [selected, setSelected] = useState(0);
  const [, navigate] = useLocation();
  const results = useMemo(() => rank(index, query), [index, query]);

  const open = useCallback(() => {
    loadIndex().then(setIndex).catch(() => setIndex([]));
    dialog.current?.showModal();
    window.setTimeout(() => input.current?.focus(), 0);
  }, []);

  useEffect(() => {
    function onKey(event: KeyboardEvent) {
      const target = event.target as HTMLElement | null;
      const typing = target && (target.tagName === "INPUT" || target.tagName === "TEXTAREA" || target.isContentEditable);
      if ((event.key === "k" && (event.metaKey || event.ctrlKey)) || (event.key === "/" && !typing)) {
        event.preventDefault();
        open();
      }
    }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open]);

  function go(result: Result) {
    dialog.current?.close();
    setQuery("");
    navigate(`/docs/${result.slug}${result.anchor ? `#${result.anchor}` : ""}`);
  }

  function onInputKey(event: React.KeyboardEvent) {
    if (event.key === "ArrowDown") {
      event.preventDefault();
      setSelected((s) => Math.min(s + 1, results.length - 1));
    } else if (event.key === "ArrowUp") {
      event.preventDefault();
      setSelected((s) => Math.max(s - 1, 0));
    } else if (event.key === "Enter" && results[selected]) {
      event.preventDefault();
      go(results[selected]);
    }
  }

  return (
    <>
      <button
        type="button"
        onClick={open}
        className="flex h-9 items-center gap-2 rounded-md border border-line bg-surface px-3 text-[0.88rem] text-muted hover:text-ink md:w-56"
        aria-label="Search documentation"
      >
        <Search size={16} aria-hidden="true" />
        <span className="hidden md:inline">Search docs</span>
        <kbd className="ml-auto hidden rounded border border-line px-1.5 font-mono text-[0.72rem] md:inline">Ctrl K</kbd>
      </button>
      <dialog
        ref={dialog}
        aria-label="Search documentation"
        className="m-auto mt-[10vh] w-[min(40rem,calc(100vw-2rem))] rounded-xl border border-line bg-bg p-0 text-ink shadow-2xl backdrop:bg-plum/60 backdrop:backdrop-blur-sm"
        onClick={(event) => event.target === dialog.current && dialog.current?.close()}
      >
        <div className="flex items-center gap-3 border-b border-line px-4">
          <Search size={18} className="text-muted" aria-hidden="true" />
          <input
            ref={input}
            value={query}
            onChange={(event) => {
              setQuery(event.target.value);
              setSelected(0);
            }}
            onKeyDown={onInputKey}
            placeholder="Search guides, APIs and errors"
            className="h-14 flex-1 bg-transparent text-[1rem] outline-none placeholder:text-muted"
            role="combobox"
            aria-expanded={results.length > 0}
            aria-controls="search-results"
            aria-activedescendant={results[selected] ? `search-result-${selected}` : undefined}
            aria-autocomplete="list"
          />
          <kbd className="rounded border border-line px-1.5 font-mono text-[0.72rem] text-muted">Esc</kbd>
        </div>
        <ul id="search-results" role="listbox" className="max-h-[60vh] overflow-y-auto p-2">
          {query && !results.length && (
            <li className="px-3 py-8 text-center text-muted">
              No results for “{query}”. Try a class name such as <code className="font-mono">Menu</code> or a topic such
              as <em>relocation</em>.
            </li>
          )}
          {results.map((result, i) => (
            <li
              key={`${result.slug}-${result.anchor}`}
              id={`search-result-${i}`}
              role="option"
              aria-selected={i === selected}
              onMouseEnter={() => setSelected(i)}
              onClick={() => go(result)}
              className={`cursor-pointer rounded-lg px-3 py-2.5 ${i === selected ? "bg-surface" : ""}`}
            >
              <p className="text-[0.8rem] text-muted">{result.page}</p>
              <p className="font-bold">
                <Highlight text={result.heading} query={query} />
              </p>
              <p className="mt-0.5 line-clamp-2 text-[0.88rem] text-muted">
                <Highlight text={excerpt(result.text, query)} query={query} />
              </p>
            </li>
          ))}
        </ul>
      </dialog>
    </>
  );
}
