import { Link } from "wouter";
import { project } from "../lib/site";

export function AboutPage() {
  return (
    <main id="main" className="mx-auto max-w-3xl px-5 py-12 lg:px-8">
      <div className="prose-bloom">
        <h1>About Bloom</h1>
        <p className="text-[1.15rem] text-muted">
          Bloom exists because Minecraft servers did not all move forward together. PvP networks still run 1.8;
          survival servers run the newest Paper; many developers write for both.
        </p>

        <h2>Why it exists</h2>
        <p>
          Writing a plugin that runs on Spigot 1.8 and on Paper 26 usually means one of two things: two codebases, or a
          tangle of version checks and reflection inside gameplay code. Bloom moves that work into one place. Your
          plugin talks to a single API; version adapters translate it for the server it is running on.
        </p>

        <h2>Architectural philosophy</h2>
        <ul>
          <li>Small, stable interfaces in <code>bloom-api</code>; implementations behind them.</li>
          <li>Version differences live only in adapters, which are loaded by name after the server is identified.</li>
          <li>Plain Java 8 where the code must run on legacy servers, verified by running the tests on Java 8.</li>
          <li>No hidden behaviour: what Bloom starts, it stops, in a documented order.</li>
          <li>No large abstractions where Bukkit's own types are clear: menus take Bukkit players and item stacks.</li>
        </ul>

        <h2>Compatibility approach</h2>
        <p>
          Bloom distinguishes between versions it accepts, versions it compiled against, and versions it actually ran
          on. The <Link href="/docs/compatibility">compatibility page</Link> shows each separately, and Bloom refuses
          to start on a version without an adapter instead of guessing.
        </p>

        <h2>Contributing</h2>
        <p>
          Bloom is developed in the open on <a href={project.repository}>GitHub</a> under the MIT license. Read
          CONTRIBUTING.md before opening a pull request: every change needs tests, and changes to shared Bukkit code must
          keep working on both adapter families. Report security problems privately as described in SECURITY.md.
        </p>

        <h2>Roadmap</h2>
        <ol>
          <li>Verify the legacy and modern adapters on real 1.7.10, 1.8.8, 1.21.11 and 26.3 servers.</li>
          <li>Bloom Doctor: build-time checks for relocation, Java targets and cross-version API linkage.</li>
          <li>First public release with artifacts on a Maven repository.</li>
          <li>A standalone runtime plugin that several plugins can share.</li>
          <li>More adapters (1.12, 1.16 to 1.20) and Folia scheduling, based on demand.</li>
        </ol>
      </div>
    </main>
  );
}
