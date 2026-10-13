import { useEffect } from "react";
import { Redirect, Route, Switch, useLocation } from "wouter";
import { SiteFooter } from "./components/SiteFooter";
import { SiteHeader } from "./components/SiteHeader";
import { headFor } from "./lib/head";
import { AboutPage } from "./pages/AboutPage";
import { ApiReferencePage } from "./pages/ApiReferencePage";
import { ChangelogPage } from "./pages/ChangelogPage";
import { DocPage } from "./pages/DocPage";
import { DownloadsPage } from "./pages/DownloadsPage";
import { ExamplesPage } from "./pages/ExamplesPage";
import { HomePage } from "./pages/HomePage";
import { NotFoundPage } from "./pages/NotFoundPage";

function useDocumentHead() {
  const [location] = useLocation();
  useEffect(() => {
    const head = headFor(location);
    document.title = head.title;
    document.querySelector('meta[name="description"]')?.setAttribute("content", head.description);
    if (!window.location.hash && !location.startsWith("/docs/")) window.scrollTo(0, 0);
  }, [location]);
}

export function App() {
  useDocumentHead();
  return (
    <>
      <SiteHeader />
      <Switch>
        <Route path="/" component={HomePage} />
        <Route path="/docs">
          <Redirect to="/docs/introduction" replace />
        </Route>
        <Route path="/docs/:slug">{(params) => <DocPage slug={params.slug} />}</Route>
        <Route path="/api" component={ApiReferencePage} />
        <Route path="/downloads" component={DownloadsPage} />
        <Route path="/examples" component={ExamplesPage} />
        <Route path="/changelog" component={ChangelogPage} />
        <Route path="/about" component={AboutPage} />
        <Route component={NotFoundPage} />
      </Switch>
      <SiteFooter />
    </>
  );
}
