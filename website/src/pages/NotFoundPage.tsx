import { Link } from "wouter";
import { BloomMark } from "../components/Logo";

export function NotFoundPage() {
  return (
    <main id="main" className="mx-auto max-w-2xl px-5 py-28 text-center">
      <BloomMark size={56} className="mx-auto opacity-80" />
      <h1 className="mt-6 font-display text-4xl font-bold">This page has not bloomed yet</h1>
      <p className="mt-4 text-muted">
        The address may be mistyped, or the page moved. Search the docs with Ctrl K, or start from one of these.
      </p>
      <ul className="mt-8 flex flex-wrap justify-center gap-3">
        <li>
          <Link href="/" className="inline-block rounded-md border border-line px-4 py-2 hover:border-accent">Home</Link>
        </li>
        <li>
          <Link href="/docs/introduction" className="inline-block rounded-md border border-line px-4 py-2 hover:border-accent">Documentation</Link>
        </li>
        <li>
          <Link href="/api" className="inline-block rounded-md border border-line px-4 py-2 hover:border-accent">API reference</Link>
        </li>
      </ul>
    </main>
  );
}
