import changelogData from "../generated/changelog.json";
import compatibilityData from "../generated/compatibility.json";
import docsData from "../generated/docs.json";
import projectData from "../generated/project.json";
import releasesData from "../generated/releases.json";

export type TocEntry = { depth: number; text: string; anchor: string };
export type DocMeta = { slug: string; title: string; description: string; toc: TocEntry[] };
export type CompatibilityRow = {
  minecraft: string;
  servers: string;
  java: string;
  adapter: string;
  runtime: string;
  verification: string[];
  notes: string;
};
export type Release = {
  tag: string;
  name: string;
  prerelease: boolean;
  publishedAt: string;
  url: string;
  body: string;
  assets: { name: string; url: string; size: number }[];
};

export const project = projectData as { version: string; group: string; repository: string };
export const docs = docsData as DocMeta[];
export const compatibility = compatibilityData as {
  updated: string;
  rows: CompatibilityRow[];
  capabilities: { columns: string[]; rows: { capability: string; values: string[] }[] };
};
export const changelog = changelogData as { version: string; date: string | null; groups: { title: string; items: string[] }[] }[];
export const releases = releasesData as { available: boolean; reason?: string; releases: Release[] };

export const siteName = "Bloom";
export const tagline = "Let your ideas bloom.";

export function docBySlug(slug: string): DocMeta | undefined {
  return docs.find((d) => d.slug === slug);
}

export const mainNav = [
  { href: "/docs/introduction", label: "Docs" },
  { href: "/api", label: "API" },
  { href: "/examples", label: "Examples" },
  { href: "/downloads", label: "Downloads" },
  { href: "/changelog", label: "Changelog" },
  { href: "/about", label: "About" },
];
