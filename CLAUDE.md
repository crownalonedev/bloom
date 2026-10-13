@AGENTS.md

## Claude Code notes

- The owner develops on Windows. Shell scripts run under Git Bash; `scripts/realserver/run.bat` wraps the real-server
  harness for PowerShell/cmd. Python on Windows prints CRLF, so strip `\r` when parsing its output in Bash.
- The plugin-author skill lives in `.claude/skills/bloom/`. Keep it consistent with the API manifest when public APIs
  change.
- `.claude/launch.json` defines `website-dev` (Vite, port 5173) and `website-preview` (wrangler, port 8787; open it at
  `http://127.0.0.1:8787`).
