import { project } from "../lib/site";

const examples = [
  {
    name: "Basic plugin",
    dir: "example-basic",
    commands: "/hello",
    shows: "Starting and stopping Bloom, messages with placeholders, an event subscription, a repeating task and a cooldown.",
  },
  {
    name: "Menus",
    dir: "example-menus",
    commands: "/shop, /players",
    shows: "A layout-based shop, permission-aware buttons, a paginated player list and a confirmation dialog.",
  },
  {
    name: "Items",
    dir: "example-items",
    commands: "/bloomitems",
    shows: "Custom items identified by persistent data, modern material names on 1.8, and capability-aware features.",
  },
  {
    name: "Configuration",
    dir: "example-config",
    commands: "/bloomconfig, /bloomconfig reload",
    shows: "Bundled defaults, POJO mapping, validation, a version migration and reloads that keep the old values on error.",
  },
  {
    name: "Database",
    dir: "example-storage",
    commands: "/stats",
    shows: "SQLite statistics with a migration, a repository, atomic counters and results handed back to the main thread.",
  },
  {
    name: "Cross-version",
    dir: "example-cross-version",
    commands: "/bloominfo",
    shows: "One jar for 1.7.10 to 26.x: capability checks, automatic chat fallbacks and a boss bar only where supported.",
  },
];

export function ExamplesPage() {
  return (
    <main id="main" className="mx-auto max-w-5xl px-5 py-12 lg:px-8">
      <h1 className="font-display text-[2.6rem] font-bold">Example plugins</h1>
      <p className="mt-3 max-w-2xl text-muted">
        Complete plugins in the repository. Each one is built, shaded and relocated by Bloom's build and loaded into a
        test server on every change. Build them all with{" "}
        <code className="rounded bg-surface-2 px-1.5 font-mono text-[0.9em]">./gradlew :bloom-examples:assemble</code>; the jars
        land in each project's <code className="rounded bg-surface-2 px-1.5 font-mono text-[0.9em]">build/libs</code>.
      </p>
      <ul className="mt-10 divide-y divide-line border-y border-line">
        {examples.map((e) => (
          <li key={e.dir} className="grid gap-2 py-6 md:grid-cols-[14rem_1fr_auto] md:items-baseline md:gap-8">
            <h2 className="font-display text-xl font-bold">{e.name}</h2>
            <div>
              <p className="text-muted">{e.shows}</p>
              <p className="mt-1 text-[0.86rem] text-muted">
                Commands: <span className="font-mono">{e.commands}</span>
              </p>
            </div>
            <a
              href={`${project.repository}/tree/main/bloom-examples/${e.dir}`}
              className="text-accent underline underline-offset-4"
            >
              Source
            </a>
          </li>
        ))}
      </ul>
      <p className="mt-8 max-w-2xl text-muted">
        Starting a new plugin? Copy{" "}
        <a href={`${project.repository}/tree/main/bloom-examples/template`} className="text-accent underline underline-offset-4">
          bloom-examples/template
        </a>
        , a standalone Gradle project set up exactly as the installation guide describes.
      </p>
    </main>
  );
}
