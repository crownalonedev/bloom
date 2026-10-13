// Documentation sidebar. Order here is the reading order used for previous/next links.
export type NavSection = { title: string; items: { slug: string; label?: string }[] };

export const docsNav: NavSection[] = [
  {
    title: "Getting started",
    items: [{ slug: "introduction" }, { slug: "installation" }, { slug: "quickstart" }],
  },
  {
    title: "Concepts",
    items: [{ slug: "architecture" }, { slug: "compatibility" }, { slug: "modules" }, { slug: "distribution" }],
  },
  {
    title: "Guides",
    items: [
      { slug: "messaging" },
      { slug: "scheduling" },
      { slug: "events" },
      { slug: "items" },
      { slug: "gui" },
      { slug: "configuration" },
      { slug: "storage" },
      { slug: "utilities" },
      { slug: "testing" },
    ],
  },
  {
    title: "Help",
    items: [{ slug: "troubleshooting" }, { slug: "ai-integration" }, { slug: "migration" }],
  },
];

export const docsOrder = docsNav.flatMap((section) => section.items.map((item) => item.slug));
