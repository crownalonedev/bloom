import { Moon, Sun } from "lucide-react";
import { useDarkTheme, writePreference } from "../lib/preferences";

export function ThemeToggle() {
  const dark = useDarkTheme();

  function toggle() {
    const root = document.documentElement;
    root.classList.add("theme-transition");
    root.classList.toggle("dark", !dark);
    window.setTimeout(() => root.classList.remove("theme-transition"), 300);
    writePreference("bloom-theme", dark ? "light" : "dark");
  }

  return (
    <button
      type="button"
      onClick={toggle}
      className="grid size-9 place-items-center rounded-md text-muted hover:bg-surface hover:text-ink"
      aria-label={dark ? "Switch to light theme" : "Switch to dark theme"}
      title={dark ? "Light theme" : "Dark theme"}
    >
      {dark ? <Sun size={18} aria-hidden="true" /> : <Moon size={18} aria-hidden="true" />}
    </button>
  );
}
