import { docBySlug, project } from "./site";

export type Head = { title: string; description: string; path: string };

const pages: Record<string, { title: string; description: string }> = {
  "/": {
    title: "Bloom: one Minecraft plugin framework from 1.7.10 to 26.x",
    description:
      "Bloom is a Java framework for Bukkit-family plugins: menus, items, messages, config, storage and scheduling with one API on Spigot 1.8 and Paper 26.",
  },
  "/api": { title: "API reference | Bloom", description: "Every public Bloom type and method, generated from the source." },
  "/downloads": { title: "Downloads | Bloom", description: `Bloom releases, requirements and Maven coordinates. Current version ${project.version}.` },
  "/examples": { title: "Example plugins | Bloom", description: "Complete Bloom example plugins with source code." },
  "/changelog": { title: "Changelog | Bloom", description: "Release history of the Bloom framework." },
  "/about": { title: "About | Bloom", description: "Why Bloom exists, how it is designed and where it is going." },
};

export function headFor(path: string): Head & { status: number } {
  const clean = path.replace(/\/+$/, "") || "/";
  if (pages[clean]) return { ...pages[clean]!, path: clean, status: 200 };
  const doc = clean.startsWith("/docs/") ? docBySlug(clean.slice(6)) : undefined;
  if (doc) return { title: `${doc.title} | Bloom docs`, description: doc.description, path: clean, status: 200 };
  return { title: "Page not found | Bloom", description: "This page does not exist.", path: clean, status: 404 };
}
