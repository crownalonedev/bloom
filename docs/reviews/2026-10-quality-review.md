# Quality review — October 2026 (Phase 9)

Security, accessibility and performance review of Bloom 0.1.0-SNAPSHOT and its documentation site. Each finding lists
what was checked, the result and, where something was wrong, the fix and the test that now guards it.

## Security

| Area | Finding | Status |
|---|---|---|
| Legacy item deserialization (`Items.deserialize` on 1.7.10 / 1.8.x) | Used `BukkitObjectInputStream`, i.e. unrestricted Java deserialization. A crafted item string (web panel, cross-server message, shared database) could instantiate arbitrary `Serializable` classes on the server class path ("gadget chains"). | **Fixed**: `SafeItemInputStream` resolves only the classes items consist of (Bukkit `Wrapper`, strings, boxed numbers, `java.util` and Guava collections, arrays of those) and rejects dynamic proxies. `SafeItemInputStreamTest`. Modern servers use Paper's byte format and were not affected. |
| Placeholders in MiniMessage messages | `Placeholder.of` values were MiniMessage-escaped but `§` codes passed through; clients apply them inside components, so untrusted text could still change formatting. | **Fixed**: section codes are stripped before escaping (`TextTemplates`). `TextTest.untrustedValuesStayPlainWhenMiniMessageIsRenderedForLegacyServers`. Legacy-format messages already stripped them. |
| Placeholder recursion | Values are substituted in one pass (`{b}` inside a value stays literal). | OK, tested. |
| YAML | `SafeConstructor`, duplicate keys rejected. | OK |
| SQL | Values are always bound parameters; table/column names in `SqlRepository`, `Dialect.upsert` and the migration table are validated as identifiers; error messages omit parameter values. | OK |
| Storage threading | `BlockingDatabase` refuses the main thread; the executor queue is bounded and fails fast. | OK |
| Real-server harness | Downloads only from PaperMC's API over HTTPS and verifies SHA-256; never accepts the EULA on the user's behalf. | OK |
| bloom-doctor | Reads jars without extracting them (no path traversal); parses class files with ASM only. | OK |
| Website | Static, no third-party scripts or fonts; CSP `default-src 'self'` with a single hashed inline script, `frame-ancestors 'none'` (verified in a browser: framing is blocked), `nosniff`, referrer and permissions policies; the build fails if a page would contain an unhashed inline script. | OK |
| Dependencies | `npm audit` reports 0 vulnerabilities; Dependabot watches Gradle, npm and GitHub Actions. | OK |

Residual risk: Java deserialization of the allowed collection classes can still be used for CPU/memory exhaustion
with deeply nested input. Plugins should only deserialize item strings they produced (documented in `SECURITY.md`).

## Accessibility

Automated on every build (`website/scripts/check-site.mjs`): `lang`, title, exactly one `h1`, no skipped heading
levels, `alt` on images, accessible names for links, buttons and form fields, unique ids, a working skip link.

| Finding | Status |
|---|---|
| The search combobox was named only by its placeholder (on every page). | **Fixed**: `aria-label`. |
| Colour contrast (WCAG AA) measured in the browser on the home, installation, menus, compatibility and API pages, dark and light themes, using the rendered colours and effective backgrounds. | OK: no text below 4.5:1 (3:1 for large text). The audit was validated with a planted low-contrast element. |
| Keyboard focus | OK: `:focus-visible` outline on all interactive elements. |
| Motion | OK: petals are hidden for `prefers-reduced-motion`, paused in hidden tabs and can be turned off. |

## Performance

| Area | Finding | Status |
|---|---|---|
| Website main bundle | The API manifest (~450 KB JSON) was bundled into the JavaScript every page loads: 143 KB gzipped. | **Fixed**: loaded only on `/api` (prerendered HTML stays complete). Main bundle 98 KB gzipped, most of it React DOM. |
| Website pages | Prerendered HTML; fonts self-hosted and preloaded; hashed assets cached for a year. | OK |
| Menu clicks | Holder check, map lookup and array index per click; debounce. | OK |
| Event subscriptions | Registered directly with Bukkit; filters and limits run inline. | OK |
| Storage | Bounded executor, pool sizes per dialect, SQLite uses one connection (avoids `SQLITE_BUSY`). | OK |
| Messages | Templates are prepared once per send; no reflection on the hot path (legacy NMS members are resolved at startup). | OK |
