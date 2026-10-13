import { AlertTriangle, Info, Layers } from "lucide-react";
import type { ReactNode } from "react";

const STYLES = {
  note: { icon: Info, label: "Note", border: "border-lavender" },
  warning: { icon: AlertTriangle, label: "Warning", border: "border-accent" },
  compat: { icon: Layers, label: "Compatibility", border: "border-blossom" },
} as const;

/** Highlighted note. Use `compat` for version-specific behaviour so readers can scan for it. */
export function Callout({ type = "note", title, children }: { type?: keyof typeof STYLES; title?: string; children: ReactNode }) {
  const style = STYLES[type];
  const Icon = style.icon;
  return (
    <aside className={`my-6 rounded-r-md border-l-2 ${style.border} bg-surface px-4 py-3 text-[0.98rem]`}>
      <p className="flex items-center gap-2 font-bold">
        <Icon size={16} aria-hidden="true" />
        {title ?? style.label}
      </p>
      <div className="mt-1 [&>p+p]:mt-2">{children}</div>
    </aside>
  );
}
