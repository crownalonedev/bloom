import { GithubIcon } from "../components/GithubIcon";
import { Link } from "wouter";
import { CompatibilityTable } from "../components/CompatibilityTable";
import { Petals } from "../components/Petals";
import { StartupDemo } from "../components/StartupDemo";
import { VersionBranch } from "../components/VersionBranch";
import HomePlugin from "../content/snippets/home-plugin.mdx";
import InstallGradle from "../content/snippets/install-gradle.mdx";
import { project } from "../lib/site";

const modules = [
  { name: "bloom-bukkit", what: "Startup, platform detection, adapter loading, scheduler, messages and events." },
  { name: "bloom-gui", what: "Inventory menus with layouts, pagination, back navigation and duplication-safe clicks." },
  { name: "bloom-items", what: "Item builder, modern material names on every version, persistent custom item ids." },
  { name: "bloom-config", what: "YAML with defaults, validation that names the line, migrations and comment-safe saves." },
  { name: "bloom-storage", what: "SQLite, MySQL and MariaDB off the main thread, with transactions and migrations." },
  { name: "bloom-testing", what: "A tick-by-tick scheduler and controllable clock for plugin unit tests." },
  { name: "bloom-legacy", what: "Adapter for Bukkit and Spigot 1.7.10 to 1.8.9 on Java 8." },
  { name: "bloom-modern", what: "Adapter for Paper 1.21.1 to 26.3.x with Adventure and MiniMessage." },
];

const principles = [
  {
    title: "Honest about versions",
    body: "Bloom refuses to start on a server it has no adapter for and tells you why. Features a version lacks are reported as capabilities you can check, never faked.",
  },
  {
    title: "The main thread stays free",
    body: "Database work runs on a bounded pool and fails fast when saturated. Blocking calls on the main thread throw with a message that says how to switch threads.",
  },
  {
    title: "Nothing left running",
    body: "Tasks, listeners, menus, boss bars and database pools belong to your plugin and are closed in order when it shuts down.",
  },
  {
    title: "Items stay in menus",
    body: "Every click and drag inside a Bloom menu is cancelled, including shift-clicks and number keys, and re-cancelled if another plugin interferes.",
  },
];

const steps = [
  { title: "Pick your oldest server", body: "Compile against spigot-api 1.8.8 for one jar that runs everywhere, or against Paper for modern-only plugins." },
  { title: "Add Bloom", body: "Depend on the feature modules you use and add the adapters for the versions you support." },
  { title: "Relocate", body: "Shade Bloom into your plugin under your own package so plugins never conflict." },
  { title: "Enable it", body: "Call Bloom.enable(this) in onEnable and bloom.shutdown() in onDisable." },
];

export function HomePage() {
  return (
    <main id="main">
      <section className="relative overflow-hidden border-b border-line">
        <Petals />
        <div className="relative mx-auto max-w-[90rem] px-5 pb-10 pt-20 lg:px-8 lg:pt-28">
          <div className="max-w-3xl">
            <h1 className="font-display text-[clamp(2.8rem,1.6rem+5vw,5.6rem)] font-bold leading-[1.02] tracking-[-0.02em]">
              Let your ideas bloom.
            </h1>
            <p className="mt-6 max-w-xl text-[1.25rem] leading-relaxed text-muted">
              One elegant framework. Every generation of Minecraft development.
            </p>
            <p className="mt-4 max-w-xl text-[1.02rem] leading-relaxed text-muted">
              Write a plugin once with Bloom's menus, items, messages, config and storage. Bloom runs it on Spigot
              1.8 and on Paper 26, and handles the differences behind version adapters.
            </p>
            <div className="mt-9 flex flex-wrap gap-3">
              <Link
                href="/docs/quickstart"
                className="rounded-md bg-accent px-5 py-2.5 font-bold text-on-accent hover:bg-accent-strong"
              >
                Get started
              </Link>
              <Link href="/docs/introduction" className="rounded-md border border-line px-5 py-2.5 hover:border-accent">
                Read documentation
              </Link>
              <a
                href={project.repository}
                className="inline-flex items-center gap-2 rounded-md border border-line px-5 py-2.5 hover:border-accent"
              >
                <GithubIcon size={17} /> View on GitHub
              </a>
            </div>
          </div>
          <div className="mt-16 lg:mt-20">
            <VersionBranch />
          </div>
        </div>
      </section>

      <section aria-labelledby="what" className="mx-auto max-w-[90rem] px-5 py-20 lg:px-8">
        <div className="[&>*]:min-w-0 grid gap-12 lg:grid-cols-[minmax(0,5fr)_minmax(0,7fr)]">
          <div>
            <h2 id="what" className="font-display text-[2rem] font-bold leading-tight">
              The same jar on every server you support
            </h2>
            <p className="mt-5 max-w-prose text-muted">
              Bukkit's API changed shape across a decade of releases: online players became a collection, inventory
              views became interfaces, materials were renamed, and text moved to components. Bloom compiles your plugin
              against the oldest API, detects the server at startup, and loads the adapter that speaks its dialect.
            </p>
            <p className="mt-4 max-w-prose text-muted">
              Switch the server on the right to see what Bloom reports when the same plugin starts.
            </p>
          </div>
          <StartupDemo />
        </div>
      </section>

      <section aria-labelledby="code" className="border-y border-line bg-surface">
        <div className="[&>*]:min-w-0 mx-auto grid max-w-[90rem] gap-12 px-5 py-20 lg:grid-cols-[minmax(0,5fr)_minmax(0,7fr)] lg:px-8">
          <div>
            <h2 id="code" className="font-display text-[2rem] font-bold leading-tight">
              A shop in thirty lines
            </h2>
            <p className="mt-5 max-w-prose text-muted">
              Items use modern names everywhere: on 1.8, <code className="font-mono text-[0.9em]">COOKED_BEEF</code>{" "}
              and <code className="font-mono text-[0.9em]">LIME_WOOL</code> resolve to their legacy materials and
              data values. The action bar is sent where the server supports it and skipped with one warning where it
              does not.
            </p>
            <p className="mt-4 max-w-prose text-muted">
              This code is compiled by Bloom's build on every change, so it matches the current API.
            </p>
          </div>
          <div className="min-w-0 [&_figure]:my-0">
            <HomePlugin />
          </div>
        </div>
      </section>

      <section aria-labelledby="compat" className="mx-auto max-w-[90rem] px-5 py-20 lg:px-8">
        <h2 id="compat" className="font-display text-[2rem] font-bold leading-tight">Compatibility</h2>
        <p className="mt-4 max-w-2xl text-muted">
          Supported ranges, and how each one has been verified so far. Bloom labels a version tested only after it has
          actually been run.
        </p>
        <CompatibilityTable />
        <Link href="/docs/compatibility" className="text-accent underline underline-offset-4">
          Capabilities by version and Java requirements
        </Link>
      </section>

      <section aria-labelledby="modules" className="border-t border-line">
        <div className="[&>*]:min-w-0 mx-auto grid max-w-[90rem] gap-12 px-5 py-20 lg:grid-cols-[minmax(0,4fr)_minmax(0,8fr)] lg:px-8">
          <div>
            <h2 id="modules" className="font-display text-[2rem] font-bold leading-tight">Modules</h2>
            <p className="mt-4 text-muted">
              Take only what you use. Each module is a small jar; the adapters are added at runtime only.
            </p>
            <Link href="/docs/modules" className="mt-5 inline-block text-accent underline underline-offset-4">
              How the modules fit together
            </Link>
          </div>
          <dl className="grid gap-x-10 gap-y-7 sm:grid-cols-2">
            {modules.map((m) => (
              <div key={m.name} className="border-t border-line pt-4">
                <dt className="font-mono text-[0.92rem] text-accent">{m.name}</dt>
                <dd className="mt-1.5 text-[0.97rem] text-muted">{m.what}</dd>
              </div>
            ))}
          </dl>
        </div>
      </section>

      <section aria-labelledby="dx" className="border-y border-line bg-surface">
        <div className="[&>*]:min-w-0 mx-auto grid max-w-[90rem] gap-14 px-5 py-20 lg:grid-cols-2 lg:px-8">
          <div>
            <h2 id="dx" className="font-display text-[2rem] font-bold leading-tight">Errors that tell you what to do</h2>
            <p className="mt-5 max-w-prose text-muted">
              Bloom's exceptions name the file, the line, the server and the fix. A typo in a config value reads
              like this, not like a stack trace:
            </p>
            <pre className="mt-5 overflow-x-auto rounded-lg border border-line bg-code px-4 py-3 font-mono text-[0.8rem] leading-relaxed">
              config.yml line 7: 'homes.max-homes' expected a whole number but found 'ten'
            </pre>
            <p className="mt-5 max-w-prose text-muted">
              Missing an adapter? Bloom names the dependency to add. Calling a database on the main thread? It says
              which executor to use instead.
            </p>
          </div>
          <div>
            <h2 className="font-display text-[2rem] font-bold leading-tight">Built for coding agents too</h2>
            <p className="mt-5 max-w-prose text-muted">
              Every public type and method is published as a machine-readable manifest generated from the source,
              alongside an <code className="font-mono text-[0.9em]">llms.txt</code> index and a Claude Code skill
              that teaches agents to check the installed Bloom version, pick the right adapter and avoid inventing
              methods.
            </p>
            <ul className="mt-5 grid gap-2 text-[0.97rem]">
              <li>
                <a href="/llms.txt" className="text-accent underline underline-offset-4">llms.txt</a>
                <span className="text-muted"> for documentation discovery</span>
              </li>
              <li>
                <a href="/api-manifest.json" className="text-accent underline underline-offset-4">api-manifest.json</a>
                <span className="text-muted"> with signatures, Javadoc and availability</span>
              </li>
              <li>
                <Link href="/docs/ai-integration" className="text-accent underline underline-offset-4">AI integration guide</Link>
                <span className="text-muted"> for Claude Code, Codex and OpenCode</span>
              </li>
            </ul>
          </div>
        </div>
      </section>

      <section aria-labelledby="principles" className="mx-auto max-w-[90rem] px-5 py-20 lg:px-8">
        <h2 id="principles" className="font-display text-[2rem] font-bold leading-tight">What Bloom promises</h2>
        <div className="mt-10 grid gap-x-12 gap-y-10 md:grid-cols-2">
          {principles.map((p) => (
            <div key={p.title} className="max-w-prose">
              <h3 className="text-[1.1rem] font-bold">{p.title}</h3>
              <p className="mt-2 text-muted">{p.body}</p>
            </div>
          ))}
        </div>
      </section>

      <section aria-labelledby="start" className="border-t border-line">
        <div className="[&>*]:min-w-0 mx-auto grid max-w-[90rem] gap-12 px-5 py-20 lg:grid-cols-[minmax(0,5fr)_minmax(0,7fr)] lg:px-8">
          <div>
            <h2 id="start" className="font-display text-[2rem] font-bold leading-tight">Get started</h2>
            <ol className="mt-8 grid gap-6">
              {steps.map((step, i) => (
                <li key={step.title} className="grid grid-cols-[2.25rem_1fr] gap-3">
                  <span className="font-display text-[1.5rem] font-bold leading-none text-accent" aria-hidden="true">
                    {i + 1}
                  </span>
                  <div>
                    <p className="font-bold">{step.title}</p>
                    <p className="mt-1 text-muted">{step.body}</p>
                  </div>
                </li>
              ))}
            </ol>
            <Link
              href="/docs/installation"
              className="mt-9 inline-block rounded-md bg-accent px-5 py-2.5 font-bold text-on-accent hover:bg-accent-strong"
            >
              Follow the installation guide
            </Link>
          </div>
          <div className="min-w-0 [&_figure]:my-0">
            <InstallGradle />
            <p className="mt-3 text-[0.86rem] text-muted">
              Bloom {project.version} is not on a public Maven repository yet; the installation guide shows how to
              publish it to Maven Local.
            </p>
          </div>
        </div>
      </section>
    </main>
  );
}
