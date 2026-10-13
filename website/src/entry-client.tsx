import "./styles/global.css";
import { StrictMode } from "react";
import { createRoot, hydrateRoot } from "react-dom/client";
import { Router } from "wouter";
import { App } from "./App";
import { docLoader, registerDoc } from "./pages/DocPage";

const container = document.getElementById("root")!;
const app = (
  <StrictMode>
    <Router>
      <App />
    </Router>
  </StrictMode>
);

async function start() {
  // Load the current doc page's chunk before hydrating, so hydration matches the prerendered HTML immediately.
  const match = window.location.pathname.match(/^\/docs\/([^/]+)/);
  const loader = match ? docLoader(match[1]!) : undefined;
  if (loader && match) {
    const module = await loader().catch(() => undefined);
    if (module) registerDoc(match[1]!, module.default);
  }
  if (container.hasChildNodes()) hydrateRoot(container, app);
  else createRoot(container).render(app);
}

void start();
