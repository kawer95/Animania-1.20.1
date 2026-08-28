# Configuration migrator guidance

This file supplements the repository-root `AGENTS.md` for `config-migrator`.

## Scope and safety

The migrator is a standalone Java 17 CLI. It must read legacy configuration
without modifying the input and must refuse to overwrite existing output.
Keep it independent from Minecraft and Forge client classes.

## Focused verification

- `.\gradlew.bat :config-migrator:test`
- `.\gradlew.bat :config-migrator:build`
- `.\gradlew.bat configConverterRuntimeAudit` when migration behavior changes

Add a fixture and regression test for each new legacy key or conversion rule.
