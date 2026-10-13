import { useSyncExternalStore } from "react";

// Visitor preferences stored in localStorage, read through useSyncExternalStore so server rendering uses a fixed
// snapshot and the client updates without effects.

const EVENT = "bloom-preferences";

function read(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

export function writePreference(key: string, value: string) {
  try {
    localStorage.setItem(key, value);
  } catch {
    // Storage unavailable (private mode): the choice lasts for this page only.
  }
  window.dispatchEvent(new Event(EVENT));
}

function subscribe(callback: () => void) {
  window.addEventListener(EVENT, callback);
  window.addEventListener("storage", callback);
  return () => {
    window.removeEventListener(EVENT, callback);
    window.removeEventListener("storage", callback);
  };
}

/** Whether decorative petals are enabled (default on). */
export function usePetalsEnabled(): boolean {
  return useSyncExternalStore(subscribe, () => read("bloom-petals") !== "off", () => true);
}

/** Whether the dark theme is active (the class on <html> is the source of truth; default dark). */
export function useDarkTheme(): boolean {
  return useSyncExternalStore(subscribe, () => document.documentElement.classList.contains("dark"), () => true);
}

/** Whether the tab is hidden, to pause decorative animation. */
export function useDocumentHidden(): boolean {
  return useSyncExternalStore(
    (callback) => {
      document.addEventListener("visibilitychange", callback);
      return () => document.removeEventListener("visibilitychange", callback);
    },
    () => document.hidden,
    () => false,
  );
}
