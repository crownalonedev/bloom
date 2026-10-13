import { Search } from "lucide-react";
import { Suspense, use, useMemo, useState } from "react";
import { project } from "../lib/site";

type Param = { name: string; type: string; doc: string };
type Member = {
  kind: "method" | "constructor" | "field" | "enumConstant";
  name: string;
  signature: string;
  returns?: string;
  returnsDoc?: string;
  params: Param[];
  throws: { type: string; doc: string }[];
  doc: string;
  since?: string;
  deprecated?: string;
};
type ApiType = {
  name: string;
  qualifiedName: string;
  kind: string;
  signature: string;
  doc: string;
  since?: string;
  deprecated?: string;
  members: Member[];
};
type ApiModule = { name: string; description?: string; packages: { name: string; types: ApiType[] }[] };
type Api = { available: boolean; reason?: string; bloomVersion?: string; generatedAt?: string; modules: ApiModule[] };

// The manifest is large (every public type and member), so it is not part of the main bundle: the server registers
// it before prerendering and the client loads it before hydrating /api or when navigating there. As with doc pages,
// prerendered HTML therefore never contains a pending Suspense boundary.
let loadedApi: Api | undefined;
let pendingApi: Promise<Api> | undefined;

export function registerApi(data: unknown) {
  loadedApi = data as Api;
}

export function loadApi(): Promise<Api> {
  pendingApi ??= import("../generated/api.json").then((module) => {
    registerApi(module.default);
    return loadedApi!;
  });
  return pendingApi;
}

export function ApiReferencePage() {
  if (loadedApi) return <ApiReference api={loadedApi} />;
  return (
    <Suspense fallback={<main id="main" className="mx-auto max-w-[90rem] px-5 py-12 lg:px-8"><h1 className="font-display text-[2.6rem] font-bold">API reference</h1><p className="mt-3 text-muted">Loading…</p></main>}>
      <LoadingApi />
    </Suspense>
  );
}

function LoadingApi() {
  return <ApiReference api={use(loadApi())} />;
}

function matches(type: ApiType, query: string) {
  if (!query) return true;
  const q = query.toLowerCase();
  return (
    type.qualifiedName.toLowerCase().includes(q) ||
    type.members.some((m) => m.name.toLowerCase().includes(q))
  );
}

function anchorOf(type: ApiType) {
  return type.qualifiedName.replace(/\./g, "-");
}

function MemberView({ member }: { member: Member }) {
  return (
    <div className="border-t border-line py-4">
      <pre className="overflow-x-auto whitespace-pre-wrap break-words font-mono text-[0.82rem] text-ink">{member.signature}</pre>
      {member.deprecated !== undefined && (
        <p className="mt-2 text-[0.88rem] text-accent">Deprecated{member.deprecated ? `: ${member.deprecated}` : "."}</p>
      )}
      {member.doc && <p className="mt-2 max-w-prose text-[0.95rem] text-muted">{member.doc}</p>}
      {member.params.length > 0 && (
        <dl className="mt-3 grid gap-1 text-[0.88rem]">
          {member.params.map((p) => (
            <div key={p.name} className="grid grid-cols-[minmax(6rem,max-content)_1fr] gap-3">
              <dt className="font-mono text-[0.82rem]">{p.name}</dt>
              <dd className="text-muted">{p.doc || p.type}</dd>
            </div>
          ))}
        </dl>
      )}
      {member.returnsDoc && <p className="mt-2 text-[0.88rem] text-muted">Returns {member.returnsDoc}</p>}
      {member.throws.map((t) => (
        <p key={t.type} className="mt-1 text-[0.88rem] text-muted">
          Throws <span className="font-mono text-[0.82rem]">{t.type}</span> {t.doc}
        </p>
      ))}
      {member.since && <p className="mt-2 text-[0.8rem] text-muted">Since {member.since}</p>}
    </div>
  );
}

function ApiReference({ api }: { api: Api }) {
  const [query, setQuery] = useState("");
  const modules = useMemo(
    () =>
      api.modules
        .map((m) => ({
          ...m,
          packages: m.packages
            .map((p) => ({ ...p, types: p.types.filter((t) => matches(t, query)) }))
            .filter((p) => p.types.length),
        }))
        .filter((m) => m.packages.length),
    [api, query],
  );
  const total = api.modules.reduce((n, m) => n + m.packages.reduce((k, p) => k + p.types.length, 0), 0);

  return (
    <main id="main" className="mx-auto max-w-[90rem] px-5 py-12 lg:px-8">
      <h1 className="font-display text-[2.6rem] font-bold">API reference</h1>
      <p className="mt-3 max-w-2xl text-muted">
        Every public type and member of Bloom {api.bloomVersion ?? project.version}, generated from the Javadoc in the
        source. Types under <code className="font-mono">.common</code> and <code className="font-mono">.internal</code>{" "}
        packages are implementation details and are not listed. The same data is available as{" "}
        <a href="/api-manifest.json" className="text-accent underline underline-offset-4">api-manifest.json</a>.
      </p>
      {!api.available ? (
        <p className="mt-10 rounded-md border border-line bg-surface px-4 py-3 text-muted">
          The API manifest has not been generated for this build. {api.reason}
        </p>
      ) : (
        <>
          <label className="mt-8 flex max-w-md items-center gap-2 rounded-md border border-line bg-surface px-3">
            <Search size={16} className="text-muted" aria-hidden="true" />
            <span className="sr-only">Filter types and members</span>
            <input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder={`Filter ${total} types, e.g. Menu or setData`}
              className="h-10 flex-1 bg-transparent outline-none placeholder:text-muted"
            />
          </label>
          <div className="mt-10 grid gap-10 lg:grid-cols-[16rem_minmax(0,1fr)]">
            <nav aria-label="Types" className="hidden lg:block">
              <div className="sticky top-20 max-h-[calc(100vh-6rem)] overflow-y-auto text-[0.88rem]">
                {modules.map((m) => (
                  <div key={m.name} className="mb-5">
                    <p className="font-bold">{m.name}</p>
                    <ul className="mt-1 grid gap-0.5">
                      {m.packages.flatMap((p) => p.types).map((t) => (
                        <li key={t.qualifiedName}>
                          <a href={`#${anchorOf(t)}`} className="text-muted hover:text-ink">{t.name}</a>
                        </li>
                      ))}
                    </ul>
                  </div>
                ))}
              </div>
            </nav>
            <div className="min-w-0">
              {modules.length === 0 && <p className="text-muted">No type or member matches “{query}”.</p>}
              {modules.map((m) => (
                <section key={m.name} aria-labelledby={`module-${m.name}`} className="mb-14">
                  <h2 id={`module-${m.name}`} className="font-display text-[1.7rem] font-bold">{m.name}</h2>
                  {m.description && <p className="mt-1 max-w-prose text-muted">{m.description}</p>}
                  {m.packages.map((p) =>
                    p.types.map((t) => (
                      <article key={t.qualifiedName} id={anchorOf(t)} className="mt-8 scroll-mt-24 rounded-lg border border-line p-5">
                        <p className="font-mono text-[0.78rem] text-muted">{p.name}</p>
                        <h3 className="mt-1 text-[1.25rem] font-bold">
                          {t.name} <span className="text-[0.85rem] font-normal text-muted">{t.kind}</span>
                        </h3>
                        <pre className="mt-2 overflow-x-auto whitespace-pre-wrap font-mono text-[0.8rem] text-muted">{t.signature}</pre>
                        {t.deprecated !== undefined && <p className="mt-2 text-accent">Deprecated. {t.deprecated}</p>}
                        {t.doc && <p className="mt-3 max-w-prose">{t.doc}</p>}
                        {t.since && <p className="mt-2 text-[0.8rem] text-muted">Since {t.since}</p>}
                        {t.members.length > 0 && (
                          <details className="mt-4" open={Boolean(query)}>
                            <summary className="cursor-pointer text-[0.92rem] text-accent">
                              {t.members.length} member{t.members.length === 1 ? "" : "s"}
                            </summary>
                            <div className="mt-2">
                              {t.members
                                .filter((mem) => !query || t.qualifiedName.toLowerCase().includes(query.toLowerCase()) || mem.name.toLowerCase().includes(query.toLowerCase()))
                                .map((mem, i) => (
                                  <MemberView key={`${mem.name}-${i}`} member={mem} />
                                ))}
                            </div>
                          </details>
                        )}
                      </article>
                    )),
                  )}
                </section>
              ))}
            </div>
          </div>
        </>
      )}
    </main>
  );
}
