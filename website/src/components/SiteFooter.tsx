import { Link } from "wouter";
import { project } from "../lib/site";
import { BloomMark } from "./Logo";
import { PetalsToggle } from "./Petals";

const columns = [
  {
    title: "Learn",
    links: [
      { href: "/docs/quickstart", label: "Quickstart" },
      { href: "/docs/installation", label: "Installation" },
      { href: "/docs/compatibility", label: "Compatibility" },
      { href: "/examples", label: "Examples" },
    ],
  },
  {
    title: "Reference",
    links: [
      { href: "/api", label: "API reference" },
      { href: "/docs/troubleshooting", label: "Troubleshooting" },
      { href: "/docs/ai-integration", label: "AI integration" },
      { href: "/llms.txt", label: "llms.txt", external: true },
    ],
  },
  {
    title: "Project",
    links: [
      { href: "/downloads", label: "Downloads" },
      { href: "/changelog", label: "Changelog" },
      { href: "/about", label: "About" },
      { href: project.repository, label: "GitHub", external: true },
    ],
  },
];

export function SiteFooter() {
  return (
    <footer className="mt-24 border-t border-line">
      <div className="mx-auto grid max-w-[90rem] gap-10 px-5 py-12 md:grid-cols-[1.4fr_repeat(3,1fr)] lg:px-8">
        <div>
          <p className="flex items-center gap-2 font-display text-lg font-bold">
            <BloomMark size={22} /> Bloom
          </p>
          <p className="mt-3 max-w-xs text-[0.92rem] text-muted">
            A cross-version framework for Minecraft plugins. Version {project.version}. MIT licensed.
          </p>
        </div>
        {columns.map((column) => (
          <nav key={column.title} aria-label={column.title}>
            <p className="font-bold">{column.title}</p>
            <ul className="mt-3 grid gap-2 text-[0.92rem] text-muted">
              {column.links.map((link) => (
                <li key={link.href}>
                  {link.external ? (
                    <a href={link.href} className="hover:text-ink">
                      {link.label}
                    </a>
                  ) : (
                    <Link href={link.href} className="hover:text-ink">
                      {link.label}
                    </Link>
                  )}
                </li>
              ))}
            </ul>
          </nav>
        ))}
      </div>
      <div className="mx-auto flex max-w-[90rem] flex-wrap justify-between gap-3 border-t border-line px-5 py-5 text-[0.82rem] text-muted lg:px-8">
        <p>Bloom is not affiliated with Mojang, Microsoft, PaperMC or SpigotMC. Minecraft is a trademark of Mojang.</p>
        <PetalsToggle />
      </div>
    </footer>
  );
}
