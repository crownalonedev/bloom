import { ChevronLeft, ChevronRight, Menu } from "lucide-react";
import { type ComponentType, Suspense, createElement, use, useEffect, useState } from "react";
import { Link, useLocation } from "wouter";
import { docsNav, docsOrder } from "../content/nav";
import { docBySlug, project, type DocMeta } from "../lib/site";
import { NotFoundPage } from "./NotFoundPage";

type DocModule = { default: ComponentType };
const loaders = import.meta.glob<DocModule>("../content/docs/*.mdx");

// Doc modules that are already loaded render synchronously. The server registers every page and the client registers
// the current page before hydrating, so prerendered HTML never contains a pending Suspense boundary (whose inline
// reveal scripts the Content-Security-Policy would block). Loading on demand is only used for client navigation.
const loaded = new Map<string, ComponentType>();
const pending = new Map<string, Promise<DocModule>>();

export function docLoader(slug: string): (() => Promise<DocModule>) | undefined {
  return loaders[`../content/docs/${slug}.mdx`];
}

export function registerDoc(slug: string, component: ComponentType) {
  loaded.set(slug, component);
}

function loadDoc(slug: string): Promise<DocModule> {
  let promise = pending.get(slug);
  if (!promise) {
    promise = docLoader(slug)!().then((module) => {
      registerDoc(slug, module.default);
      return module;
    });
    pending.set(slug, promise);
  }
  return promise;
}

function DocContent({ slug }: { slug: string }) {
  const ready = loaded.get(slug);
  // The component comes from a module-level map of compiled MDX pages, so its identity is stable across renders.
  if (ready) return createElement(ready);
  return (
    <Suspense fallback={<p className="text-muted">Loading…</p>}>
      <LoadingDoc slug={slug} />
    </Suspense>
  );
}

function LoadingDoc({ slug }: { slug: string }) {
  return createElement(use(loadDoc(slug)).default);
}

function sectionOf(slug: string) {
  return docsNav.find((s) => s.items.some((i) => i.slug === slug));
}

function Sidebar({ current }: { current: string }) {
  return (
    <nav aria-label="Documentation" className="text-[0.93rem]">
      {docsNav.map((section) => (
        <div key={section.title} className="mb-7">
          <p className="mb-2 font-bold">{section.title}</p>
          <ul className="grid gap-0.5 border-l border-line">
            {section.items.map((item) => {
              const meta = docBySlug(item.slug);
              const active = item.slug === current;
              return (
                <li key={item.slug}>
                  <Link
                    href={`/docs/${item.slug}`}
                    aria-current={active ? "page" : undefined}
                    className={`-ml-px block border-l py-1 pl-3.5 ${
                      active ? "border-accent font-bold text-accent" : "border-transparent text-muted hover:border-muted hover:text-ink"
                    }`}
                  >
                    {item.label ?? meta?.title ?? item.slug}
                  </Link>
                </li>
              );
            })}
          </ul>
        </div>
      ))}
      <div className="mb-7">
        <p className="mb-2 font-bold">Reference</p>
        <ul className="grid gap-0.5 border-l border-line">
          <li>
            <Link href="/api" className="-ml-px block border-l border-transparent py-1 pl-3.5 text-muted hover:text-ink">
              API reference
            </Link>
          </li>
          <li>
            <Link href="/examples" className="-ml-px block border-l border-transparent py-1 pl-3.5 text-muted hover:text-ink">
              Example plugins
            </Link>
          </li>
        </ul>
      </div>
    </nav>
  );
}

function TableOfContents({ doc }: { doc: DocMeta }) {
  const [active, setActive] = useState<string | null>(null);
  useEffect(() => {
    const headings = doc.toc.map((t) => document.getElementById(t.anchor)).filter(Boolean) as HTMLElement[];
    if (!headings.length || typeof IntersectionObserver === "undefined") return;
    const observer = new IntersectionObserver(
      (entries) => {
        const visible = entries.filter((e) => e.isIntersecting).sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top);
        if (visible[0]) setActive(visible[0].target.id);
      },
      { rootMargin: "-80px 0px -65% 0px" },
    );
    headings.forEach((h) => observer.observe(h));
    return () => observer.disconnect();
  }, [doc]);

  if (!doc.toc.length) return null;
  return (
    <nav aria-label="On this page" className="text-[0.88rem]">
      <p className="mb-2 font-bold">On this page</p>
      <ul className="grid gap-1.5">
        {doc.toc.map((entry) => (
          <li key={entry.anchor} className={entry.depth === 3 ? "pl-3" : ""}>
            <a
              href={`#${entry.anchor}`}
              className={active === entry.anchor ? "text-accent" : "text-muted hover:text-ink"}
              aria-current={active === entry.anchor ? "location" : undefined}
            >
              {entry.text}
            </a>
          </li>
        ))}
      </ul>
    </nav>
  );
}

export function DocPage({ slug }: { slug: string }) {
  const doc = docBySlug(slug);
  const [location] = useLocation();
  const [menuOpenAt, setMenuOpenAt] = useState<string | null>(null);
  const menuOpen = menuOpenAt === location;
  useEffect(() => {
    if (window.location.hash) {
      document.getElementById(decodeURIComponent(window.location.hash.slice(1)))?.scrollIntoView();
    } else {
      window.scrollTo(0, 0);
    }
  }, [slug]);

  if (!doc || !docLoader(slug)) return <NotFoundPage />;
  const position = docsOrder.indexOf(slug);
  const previous = position > 0 ? docBySlug(docsOrder[position - 1]!) : undefined;
  const next = position >= 0 && position < docsOrder.length - 1 ? docBySlug(docsOrder[position + 1]!) : undefined;
  const section = sectionOf(slug);
  const preRelease = /SNAPSHOT|alpha|beta|rc/i.test(project.version) || project.version.startsWith("0.");

  return (
    <div className="mx-auto grid max-w-[90rem] gap-10 px-5 lg:grid-cols-[15rem_minmax(0,1fr)] lg:px-8 xl:grid-cols-[15rem_minmax(0,1fr)_13rem]">
      <aside className="hidden lg:block">
        <div className="sticky top-16 max-h-[calc(100vh-4rem)] overflow-y-auto py-10 pr-2">
          <Sidebar current={slug} />
        </div>
      </aside>
      <div className="min-w-0 py-8 lg:py-10">
        <div className="mb-6 flex items-center justify-between gap-3">
          <nav aria-label="Breadcrumb" className="text-[0.86rem] text-muted">
            <ol className="flex flex-wrap items-center gap-1.5">
              <li>
                <Link href="/docs/introduction" className="hover:text-ink">Docs</Link>
              </li>
              {section && (
                <li className="flex items-center gap-1.5">
                  <ChevronRight size={13} aria-hidden="true" />
                  {section.title}
                </li>
              )}
              <li className="flex items-center gap-1.5" aria-current="page">
                <ChevronRight size={13} aria-hidden="true" />
                <span className="text-ink">{doc.title}</span>
              </li>
            </ol>
          </nav>
          <button
            type="button"
            onClick={() => setMenuOpenAt(menuOpen ? null : location)}
            aria-expanded={menuOpen}
            aria-controls="docs-mobile-nav"
            className="inline-flex items-center gap-1.5 rounded-md border border-line px-2.5 py-1 text-[0.86rem] lg:hidden"
          >
            <Menu size={15} aria-hidden="true" /> Pages
          </button>
        </div>
        {menuOpen && (
          <div id="docs-mobile-nav" className="mb-8 rounded-lg border border-line p-4 lg:hidden">
            <Sidebar current={slug} />
          </div>
        )}
        {preRelease && (
          <p className="mb-8 rounded-md border border-line bg-surface px-4 py-2.5 text-[0.88rem] text-muted">
            These docs describe Bloom {project.version}, a pre-release. APIs may change before 1.0, and nothing is
            published to a public Maven repository yet.
          </p>
        )}
        <main id="main" className="prose-bloom">
          <h1>{doc.title}</h1>
          <p className="text-[1.15rem] text-muted">{doc.description}</p>
          <DocContent slug={slug} />
        </main>
        <nav aria-label="Previous and next pages" className="mt-16 grid gap-4 border-t border-line pt-6 sm:grid-cols-2">
          {previous ? (
            <Link href={`/docs/${previous.slug}`} className="group rounded-lg border border-line p-4 hover:border-accent">
              <span className="flex items-center gap-1 text-[0.82rem] text-muted"><ChevronLeft size={14} aria-hidden="true" /> Previous</span>
              <span className="mt-1 block font-bold group-hover:text-accent">{previous.title}</span>
            </Link>
          ) : <span />}
          {next && (
            <Link href={`/docs/${next.slug}`} className="group rounded-lg border border-line p-4 text-right hover:border-accent">
              <span className="flex items-center justify-end gap-1 text-[0.82rem] text-muted">Next <ChevronRight size={14} aria-hidden="true" /></span>
              <span className="mt-1 block font-bold group-hover:text-accent">{next.title}</span>
            </Link>
          )}
        </nav>
      </div>
      <aside className="hidden xl:block">
        <div className="sticky top-16 max-h-[calc(100vh-4rem)] overflow-y-auto py-10">
          <TableOfContents doc={doc} />
        </div>
      </aside>
    </div>
  );
}
