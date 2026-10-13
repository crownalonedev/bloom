import { Link } from "wouter";
import { project, releases } from "../lib/site";

function formatSize(bytes: number) {
  return bytes > 1024 * 1024 ? `${(bytes / 1024 / 1024).toFixed(1)} MB` : `${Math.max(1, Math.round(bytes / 1024))} KB`;
}

export function DownloadsPage() {
  const stable = releases.releases.filter((r) => !r.prerelease);
  const preview = releases.releases.filter((r) => r.prerelease);

  return (
    <main id="main" className="mx-auto max-w-4xl px-5 py-12 lg:px-8">
      <h1 className="font-display text-[2.6rem] font-bold">Downloads</h1>
      <p className="mt-3 max-w-2xl text-muted">
        Bloom is a library: you add it to your plugin's build rather than installing it on a server. Release artifacts
        are listed from GitHub Releases when this site is built.
      </p>

      {releases.releases.length === 0 ? (
        <section className="mt-10 rounded-lg border border-line bg-surface p-6">
          <h2 className="font-display text-xl font-bold">No releases yet</h2>
          <p className="mt-2 text-muted">
            The current development version is <span className="font-mono text-ink">{project.version}</span>. Until the
            first release, build Bloom from source and publish it to your local Maven repository:
          </p>
          <pre className="mt-4 overflow-x-auto rounded-md border border-line bg-code px-4 py-3 font-mono text-[0.84rem]">
            {`git clone ${project.repository}.git\ncd bloom\n./gradlew publishToMavenLocal`}
          </pre>
          <p className="mt-4 text-muted">
            Then add <code className="font-mono">mavenLocal()</code> to your repositories and follow the{" "}
            <Link href="/docs/installation" className="text-accent underline underline-offset-4">installation guide</Link>.
          </p>
          {!releases.available && releases.reason && (
            <p className="mt-4 text-[0.84rem] text-muted">Release data unavailable at build time: {releases.reason}.</p>
          )}
        </section>
      ) : (
        [
          { title: "Stable releases", list: stable },
          { title: "Preview releases", list: preview },
        ].map(
          (group) =>
            group.list.length > 0 && (
              <section key={group.title} className="mt-12">
                <h2 className="font-display text-2xl font-bold">{group.title}</h2>
                <ul className="mt-4 grid gap-4">
                  {group.list.map((r) => (
                    <li key={r.tag} className="rounded-lg border border-line p-5">
                      <div className="flex flex-wrap items-baseline justify-between gap-2">
                        <a href={r.url} className="text-lg font-bold text-accent underline underline-offset-4">{r.name}</a>
                        <span className="text-[0.86rem] text-muted">{new Date(r.publishedAt).toLocaleDateString("en-GB")}</span>
                      </div>
                      {r.body && <p className="mt-2 line-clamp-4 whitespace-pre-line text-[0.94rem] text-muted">{r.body}</p>}
                      {r.assets.length > 0 && (
                        <ul className="mt-3 grid gap-1 text-[0.9rem]">
                          {r.assets.map((a) => (
                            <li key={a.name}>
                              <a href={a.url} className="text-accent underline underline-offset-4">{a.name}</a>
                              <span className="text-muted"> ({formatSize(a.size)})</span>
                            </li>
                          ))}
                        </ul>
                      )}
                    </li>
                  ))}
                </ul>
              </section>
            ),
        )
      )}

      <section className="mt-14">
        <h2 className="font-display text-2xl font-bold">Requirements</h2>
        <dl className="mt-4 grid gap-4 sm:grid-cols-2">
          <div className="border-t border-line pt-3">
            <dt className="font-bold">Legacy servers</dt>
            <dd className="mt-1 text-muted">Bukkit, Spigot or PaperSpigot 1.7.10 to 1.8.9 on Java 8 or newer.</dd>
          </div>
          <div className="border-t border-line pt-3">
            <dt className="font-bold">Modern servers</dt>
            <dd className="mt-1 text-muted">Paper (or a fork) 1.21.1 to 1.21.11 on Java 21, or 26.1 to 26.3.x on Java 25.</dd>
          </div>
          <div className="border-t border-line pt-3">
            <dt className="font-bold">Building your plugin</dt>
            <dd className="mt-1 text-muted">Gradle 9 with a JDK 21 toolchain; Java 8 bytecode for cross-version plugins.</dd>
          </div>
          <div className="border-t border-line pt-3">
            <dt className="font-bold">Maven coordinates</dt>
            <dd className="mt-1 font-mono text-[0.86rem] text-muted">{project.group}:bloom-gui:{project.version}</dd>
          </div>
        </dl>
      </section>
    </main>
  );
}
