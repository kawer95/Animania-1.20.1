# Base module guidance

This file supplements the repository-root `AGENTS.md` for `base`.

## Scope

Base owns shared entity infrastructure, capabilities and networking, common
AI/utilities, shared blocks and items, configuration, the native handbook, and
optional compatibility. Addon-specific animals and content do not belong here.

## Important constraints

- Mod ID and live resource namespace: `animania`.
- Keep Base usable on a dedicated server and independently of all addons.
- Client config screens and manual rendering must remain behind the client
  bootstrap; common initialization may not resolve GUI or renderer classes.
- Changes to shared animal state, AI, breeding, hunger/thirst, sleeping, or
  synchronization can affect every addon. Search all modules for callers and
  run addon tests proportionally.
- Handbook fixes must preserve page ordering, navigation, localization, item
  ingredients, images, and stable entity previews. Run the manual/resource
  audits when those assets or renderers change.

## Focused verification

- `.\gradlew.bat :base:test`
- `.\gradlew.bat :base:runGameTestServer`
- `.\gradlew.bat :base:runFullGameTestServer` for cross-addon shared changes
- `.\gradlew.bat manualAudit resourceAudit` for handbook/resource changes
