import { changelog, project } from "../lib/site";

export function ChangelogPage() {
  return (
    <main id="main" className="mx-auto max-w-3xl px-5 py-12 lg:px-8">
      <h1 className="font-display text-[2.6rem] font-bold">Changelog</h1>
      <p className="mt-3 text-muted">
        Generated from{" "}
        <a href={`${project.repository}/blob/main/CHANGELOG.md`} className="text-accent underline underline-offset-4">
          CHANGELOG.md
        </a>
        . Bloom follows semantic versioning from 1.0; before 1.0, minor versions may change APIs.
      </p>
      {changelog.length === 0 && <p className="mt-10 text-muted">No entries yet.</p>}
      {changelog.map((entry) => (
        <section key={entry.version} className="mt-12 border-t border-line pt-6" aria-labelledby={`v-${entry.version}`}>
          <h2 id={`v-${entry.version}`} className="font-display text-2xl font-bold">
            {entry.version}
            {entry.date && <span className="ml-3 text-[1rem] font-normal text-muted">{entry.date}</span>}
          </h2>
          {entry.groups.map((group) => (
            <div key={group.title} className="mt-5">
              <h3 className="font-bold">{group.title}</h3>
              <ul className="mt-2 grid list-disc gap-1.5 pl-5 text-muted marker:text-muted">
                {group.items.map((item, i) => (
                  <li key={i}>{item.replace(/\*\*/g, "").replace(/`/g, "")}</li>
                ))}
              </ul>
            </div>
          ))}
        </section>
      ))}
    </main>
  );
}
