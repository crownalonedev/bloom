import { Children, type ReactNode, isValidElement, useId, useState } from "react";

export function Tab({ children }: { label: string; children: ReactNode }) {
  return <>{children}</>;
}

/** Accessible tabs (arrow keys move between tabs). Used for alternatives such as legacy vs modern setup. */
export function Tabs({ children }: { children: ReactNode }) {
  const tabs = Children.toArray(children).filter(isValidElement) as React.ReactElement<{ label: string; children: ReactNode }>[];
  const [active, setActive] = useState(0);
  const id = useId();

  function onKeyDown(event: React.KeyboardEvent) {
    if (event.key !== "ArrowRight" && event.key !== "ArrowLeft") return;
    event.preventDefault();
    const next = (active + (event.key === "ArrowRight" ? 1 : tabs.length - 1)) % tabs.length;
    setActive(next);
    document.getElementById(`${id}-tab-${next}`)?.focus();
  }

  return (
    <div className="my-6">
      <div role="tablist" className="flex gap-1 border-b border-line" onKeyDown={onKeyDown}>
        {tabs.map((tab, i) => (
          <button
            key={i}
            id={`${id}-tab-${i}`}
            role="tab"
            type="button"
            aria-selected={i === active}
            aria-controls={`${id}-panel-${i}`}
            tabIndex={i === active ? 0 : -1}
            onClick={() => setActive(i)}
            className={`-mb-px border-b-2 px-3 py-2 text-[0.92rem] ${
              i === active ? "border-accent font-bold text-ink" : "border-transparent text-muted hover:text-ink"
            }`}
          >
            {tab.props.label}
          </button>
        ))}
      </div>
      {tabs.map((tab, i) => (
        <div key={i} id={`${id}-panel-${i}`} role="tabpanel" aria-labelledby={`${id}-tab-${i}`} hidden={i !== active}>
          {tab.props.children}
        </div>
      ))}
    </div>
  );
}
