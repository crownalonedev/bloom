import { useState } from "react";
import { project } from "../lib/site";

// Startup banner lines in the exact format Bloom logs (see Bloom.logBanner). Capability lists follow enum order.
const platforms = [
  {
    id: "legacy",
    label: "Spigot 1.8.8 on Java 8",
    lines: [
      `Bloom ${project.version} · Spigot 1.8.8 (Java 1.8.0_472) · adapter: legacy (Bukkit 1.7.10-1.8.9)`,
      "  capabilities: ACTION_BAR, TITLES, ITEM_NBT_TAGS, ITEM_FLAGS, UNBREAKABLE_ITEMS, LEGACY_DATA_VALUES",
      "  unavailable:  ADVENTURE_TEXT, MINIMESSAGE, HEX_COLORS, TITLE_TIMINGS, BOSS_BAR, COMPONENT_INVENTORY_TITLES, PERSISTENT_DATA_CONTAINER, CUSTOM_MODEL_DATA",
    ],
    caption: "Legacy capabilities come from probes run at startup; this is the list when every probe succeeds.",
  },
  {
    id: "modern",
    label: "Paper 26.3 on Java 25",
    lines: [
      `Bloom ${project.version} · Paper 26.3 (Java 25.0.1) · adapter: modern (Paper 1.21.1-26.3.x, Adventure)`,
      "  capabilities: ADVENTURE_TEXT, MINIMESSAGE, HEX_COLORS, ACTION_BAR, TITLES, TITLE_TIMINGS, BOSS_BAR, COMPONENT_INVENTORY_TITLES, PERSISTENT_DATA_CONTAINER, ITEM_FLAGS, UNBREAKABLE_ITEMS, CUSTOM_MODEL_DATA",
      "  unavailable:  ITEM_NBT_TAGS, LEGACY_DATA_VALUES",
    ],
    caption: "The modern adapter enables every Paper feature Bloom wraps; nothing needs probing.",
  },
];

/** The same plugin jar starting on two servers: Bloom picks the adapter and reports what each server can do. */
export function StartupDemo() {
  const [active, setActive] = useState(0);
  const platform = platforms[active]!;
  return (
    <div className="overflow-hidden rounded-lg border border-line bg-code">
      <div role="tablist" aria-label="Server" className="flex flex-wrap gap-1 border-b border-line px-2 pt-2">
        {platforms.map((p, i) => (
          <button
            key={p.id}
            role="tab"
            type="button"
            aria-selected={i === active}
            aria-controls="startup-log"
            onClick={() => setActive(i)}
            className={`-mb-px rounded-t-md border-b-2 px-3 py-2 text-[0.88rem] ${
              i === active ? "border-accent font-bold text-ink" : "border-transparent text-muted hover:text-ink"
            }`}
          >
            {p.label}
          </button>
        ))}
      </div>
      <div id="startup-log" role="tabpanel" className="px-4 py-4">
        <pre className="overflow-x-auto font-mono text-[0.8rem] leading-[1.75] text-ink" aria-live="polite">
          {platform.lines.map((line, i) => (
            <span key={i} className="block whitespace-pre">
              <span className="text-muted">[INFO] [ShopPlugin] </span>
              {line}
            </span>
          ))}
        </pre>
        <p className="mt-3 text-[0.82rem] text-muted">{platform.caption}</p>
      </div>
    </div>
  );
}
