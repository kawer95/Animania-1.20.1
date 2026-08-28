# Tooling guidance

This file supplements the repository-root `AGENTS.md` for `tools`.

## Scope

The scripts in this directory audit migration completeness, resources, models,
IDs, release artifacts, and configuration conversion. They are verification
code and should fail closed when required evidence is missing.

## Rules

- Prefer deterministic, read-only audits. A verification script must not rewrite
  shipping source unless its name and documentation explicitly describe a
  generation/migration operation.
- Keep paths repository-relative where possible and support the existing
  `--root` convention.
- When changing legacy normalization, update both the normalization logic and a
  regression test that scans the live shipping resources.
- Do not weaken an audit merely to make a failing release gate green; establish
  whether source, evidence, or the audit is wrong.

See `tools/README.md` and the root Gradle audit tasks for supported entry points.
