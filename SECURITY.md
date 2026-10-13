# Security policy

## Supported versions

Bloom is pre-release (0.x). Security fixes go into the latest release and `main`.

## Reporting a vulnerability

Please report vulnerabilities privately through GitHub: **Security → Report a vulnerability** on
[crownalonedev/bloom](https://github.com/crownalonedev/bloom/security/advisories/new). Do not open a public issue.
Include the Bloom version, the server software and version, and steps or a proof of concept. You will get a reply
within a week.

## What Bloom does to keep plugins safe

- **Item deserialization** on 1.7.10 and 1.8.x uses Java serialization under the hood; Bloom only resolves the
  classes an item consists of (maps, lists, strings, numbers, Bukkit's wrapper), so a crafted item string cannot
  instantiate arbitrary classes. Still, only deserialize item strings your plugin produced.
- **Placeholders** created with `Placeholder.of` are plain text: colour codes, section signs and MiniMessage tags
  in untrusted values (player names, chat) are neutralised. `Placeholder.formatted` is for trusted values only.
- **YAML** is loaded with SnakeYAML's `SafeConstructor` (no arbitrary object construction).
- **SQL**: all values are bound parameters; table and column names given to `SqlRepository` and migrations are
  validated as plain identifiers. Error messages never include parameter values.
- **The documentation site** is static, with a strict Content-Security-Policy and no third-party scripts.

The full review is in [docs/reviews/2026-10-quality-review.md](docs/reviews/2026-10-quality-review.md).
