import { useDocumentHidden, usePetalsEnabled, writePreference } from "../lib/preferences";

// Deterministic layout so server and client render the same markup (no hydration mismatch).
const PETALS = Array.from({ length: 9 }, (_, i) => {
  const r = (n: number) => ((Math.sin((i + 1) * 9301 + n * 49297) + 1) / 2) % 1;
  return {
    left: `${Math.round(r(1) * 96)}%`,
    duration: `${16 + Math.round(r(2) * 12)}s`,
    delay: `${-Math.round(r(3) * 20)}s`,
    drift: `${Math.round(r(4) * 160 - 60)}px`,
    spin: `${Math.round(r(5) * 360 + 180)}deg`,
    alpha: (0.25 + r(6) * 0.35).toFixed(2),
    scale: (0.6 + r(7) * 0.7).toFixed(2),
  };
});

/**
 * A handful of slowly falling petals. CSS-only animation of transform/opacity, hidden for prefers-reduced-motion,
 * paused while the tab is hidden, and switchable off by the visitor (stored in localStorage).
 */
export function Petals() {
  const enabled = usePetalsEnabled();
  const hidden = useDocumentHidden();
  if (!enabled) return null;
  return (
    <div aria-hidden="true" className={`pointer-events-none absolute inset-0 overflow-hidden ${hidden ? "petals-paused" : ""}`}>
      {PETALS.map((p, i) => (
        <span
          key={i}
          className="petal"
          style={
            {
              left: p.left,
              scale: p.scale,
              "--duration": p.duration,
              "--delay": p.delay,
              "--drift": p.drift,
              "--spin": p.spin,
              "--alpha": p.alpha,
            } as React.CSSProperties
          }
        />
      ))}
    </div>
  );
}

export function PetalsToggle() {
  const enabled = usePetalsEnabled();
  return (
    <button
      type="button"
      onClick={() => writePreference("bloom-petals", enabled ? "off" : "on")}
      className="underline-offset-4 hover:text-ink hover:underline"
    >
      {enabled ? "Turn off falling petals" : "Turn on falling petals"}
    </button>
  );
}
