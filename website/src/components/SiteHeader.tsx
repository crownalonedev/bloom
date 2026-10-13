import { Menu, X } from "lucide-react";
import { GithubIcon } from "./GithubIcon";
import { useState } from "react";
import { Link, useLocation } from "wouter";
import { mainNav, project } from "../lib/site";
import { Logo } from "./Logo";
import { SearchDialog } from "./SearchDialog";
import { ThemeToggle } from "./ThemeToggle";

export function SiteHeader() {
  const [location] = useLocation();
  // The menu belongs to the page it was opened on, so navigating closes it without an effect.
  const [openAt, setOpenAt] = useState<string | null>(null);
  const open = openAt === location;

  const isActive = (href: string) =>
    href.startsWith("/docs") ? location.startsWith("/docs") : location === href || location.startsWith(`${href}/`);

  return (
    <header className="sticky top-0 z-40 border-b border-line bg-bg/85 backdrop-blur-md">
      <a href="#main" className="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-3 focus:z-50 focus:rounded focus:bg-surface focus:px-3 focus:py-2">
        Skip to content
      </a>
      <div className="mx-auto flex h-16 max-w-[90rem] items-center gap-6 px-5 lg:px-8">
        <Link href="/" className="shrink-0 rounded" aria-label="Bloom home">
          <Logo />
        </Link>
        <nav aria-label="Main" className="hidden lg:block">
          <ul className="flex items-center gap-1 text-[0.94rem]">
            {mainNav.map((item) => (
              <li key={item.href}>
                <Link
                  href={item.href}
                  aria-current={isActive(item.href) ? "page" : undefined}
                  className={`rounded-md px-3 py-1.5 ${isActive(item.href) ? "text-ink" : "text-muted hover:text-ink"}`}
                >
                  {item.label}
                </Link>
              </li>
            ))}
          </ul>
        </nav>
        <div className="ml-auto flex items-center gap-2">
          <SearchDialog />
          <ThemeToggle />
          <a
            href={project.repository}
            className="grid size-9 place-items-center rounded-md text-muted hover:bg-surface hover:text-ink"
            aria-label="Bloom on GitHub"
          >
            <GithubIcon size={18} />
          </a>
          <button
            type="button"
            className="grid size-9 place-items-center rounded-md text-muted hover:bg-surface hover:text-ink lg:hidden"
            aria-expanded={open}
            aria-controls="mobile-nav"
            aria-label={open ? "Close menu" : "Open menu"}
            onClick={() => setOpenAt(open ? null : location)}
          >
            {open ? <X size={18} aria-hidden="true" /> : <Menu size={18} aria-hidden="true" />}
          </button>
        </div>
      </div>
      {open && (
        <nav id="mobile-nav" aria-label="Main" className="border-t border-line px-5 py-3 lg:hidden">
          <ul className="grid gap-1">
            {mainNav.map((item) => (
              <li key={item.href}>
                <Link href={item.href} className="block rounded-md px-2 py-2 hover:bg-surface">
                  {item.label}
                </Link>
              </li>
            ))}
          </ul>
        </nav>
      )}
    </header>
  );
}
