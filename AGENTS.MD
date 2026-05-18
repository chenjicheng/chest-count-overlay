# chest_count_overlay Agent Guide

## Project Goal

Build a client-only Fabric mod for Minecraft 1.21.11. When the player opens a 9-column chest-like container or a shulker box screen, the mod renders a vanilla-adjacent item count sidebar next to the original container UI.

Target users are Minecraft players who want a quick, read-only count of items in the currently opened container without changing server behavior.

Success criteria:
- The project builds with `.\gradlew build`.
- The client starts with `.\gradlew runClient`.
- Single chests, double chests, trapped chests, barrels, and shulker boxes show the sidebar.
- Player inventory slots are not counted.
- Items are merged by item type plus data components.
- Readable nested contents inside container-like item stacks are counted when the item stack exposes synchronized data components for those contents.
- Counts are sorted by total count descending, preserving first-seen order for ties.
- The sidebar remains inside the current window at all GUI scales.

## Build Environment

Required versions:
- Java: Temurin 21.
- Minecraft: 1.21.11.
- Yarn: 1.21.11+build.5.
- Fabric Loader: 0.18.0 minimum; development and validation should compile against 0.18.0 to preserve that lower bound.
- Fabric API: 0.141.4+1.21.11.
- Fabric Loom: 1.16.2.
- Loom plugin ID: `net.fabricmc.fabric-loom-remap`.
- Gradle wrapper: Gradle 9.5.1.

Environment rules:
- Check `java -version` before installing Java.
- Check `gradle -v` before installing temporary Gradle.
- If Java 21 is missing, install Chocolatey package `Temurin21`.
- If Gradle is missing, install Chocolatey package `gradle` only to generate the wrapper.
- After wrapper generation, all project commands must use `.\gradlew`.
- Do not damage or overwrite an existing user `JAVA_HOME` or global `PATH`; prefer current-process environment changes when needed.

Environment deviation recorded during implementation:
- Reason: Chocolatey package installation failed in the non-elevated shell. The Gradle Chocolatey package writes machine-level environment variables, and the Temurin MSI returned 1603 without elevation.
- Impact: Chocolatey cannot be the only installation path in this session.
- New constraint: installations must still be package-manager managed. Use winget for Temurin 21 if available. If winget does not provide Gradle, install the user-scope Scoop package manager and install Gradle through Scoop. Do not use manually extracted portable Java or Gradle installations.

Fabric Loader lower-bound update:
- Reason: the target user environment may use Fabric Loader 0.18.x rather than 0.19.x.
- Impact: the mod metadata must declare `fabricloader >=0.18.0`, and validation should compile/run the dev classpath with loader 0.18.0 instead of only testing 0.19.2.
- New constraint: do not use Loader APIs that require 0.19.x unless this file is updated first with a new lower bound.

Commands:
- Bootstrap dependencies and verify the wrapper: `powershell -ExecutionPolicy Bypass -File .\scripts\bootstrap.ps1 -SkipBuild`.
- Bootstrap dependencies and build: `powershell -ExecutionPolicy Bypass -File .\scripts\bootstrap.ps1`.
- Build: `.\gradlew build`.
- Run client: `.\gradlew runClient`.
- Generate sources if mapping names need confirmation: `.\gradlew genSources`.

Release process:
- Releases are created by GitHub Actions when a tag matching `v*` is pushed.
- The tag version must match `mod_version` in `gradle.properties`; for example `mod_version=1.0.0` must use tag `v1.0.0`.
- Release assets are the remapped mod jar, sources jar, and a SHA-256 checksum file from `build/libs`.

## Implementation Constraints

- Client-only.
- Do not modify server protocol.
- Do not send custom network packets.
- Only read currently synchronized client container slots.
- Nested item counting may only read data components already present on the synchronized `ItemStack`, such as `DataComponentTypes.CONTAINER` and `DataComponentTypes.BUNDLE_CONTENTS`.
- Do not infer or resolve unsynchronized loot-table contents such as `DataComponentTypes.CONTAINER_LOOT`.
- Do not count player inventory slots.
- Add a YACL-powered client-only configuration screen exposed through Mod Menu integration.
- Do not add server logic.
- Do not add unrelated third-party libraries.
- Do not silently fall back to a weaker behavior.
- Do not hide incompatibility behind a compatibility fallback.
- If a planned behavior cannot be implemented, fail clearly or document the exact limitation.

## UI Constraints

- Default placement is to the left of the vanilla container UI when expanded.
- If the left side lacks space, place the sidebar on the right.
- If both sides lack space, clamp the sidebar so it remains inside the screen.
- Show only item icons and counts in the main sidebar.
- Tooltip must show the item display name and exact total count.
- Counts from 0 through 99999 are displayed as full integers.
- Counts >= 100000 are displayed as floored integer `k` values, for example:
  - 99999 -> 99999
  - 100000 -> 100k
  - 154900 -> 154k
  - 999999 -> 999k
- Tooltip counts always use the exact integer value.
- Empty containers must not render the sidebar at all.
- The sidebar should be a compact, JEI-like transparent edge-aligned count rail, not a hand-drawn fake chest frame.
- Do not render a full-height collapsed handle; collapsed state only shows the compact top tab needed to reopen the overlay.
- The expanded sidebar height must align with the vanilla container UI height.
- For `GenericContainerScreen`, calculate the aligned visual height from the vanilla layout formula `114 + rows * 18` instead of shrinking to content height.
- Prefer the runtime `HandledScreen#backgroundHeight` value for layout positioning.
- For visual height, align to the actual `GenericContainerScreen` texture draw height, which is `backgroundHeight - 1` in Yarn 1.21.11 because vanilla draws `rows * 18 + 17` pixels plus `96` pixels.
- The expanded sidebar top must align with the vanilla container UI top when the screen has enough room; if clamped, it must still remain fully inside the screen.
- The sidebar width is fixed and compact so it does not grow based on large counts.
- The sidebar uses fixed 18 px rows to match the vanilla slot grid rhythm.
- The visible row count is derived from the aligned sidebar height without compressing individual rows.
- If the list content area has a bottom remainder smaller than one row, draw it as a panel continuation area rather than resizing rows.
- If there are fewer items than visible rows, leave the unused lower area empty instead of shrinking the sidebar.
- If there are more item types than visible rows, additional item types are reachable by internal scrolling.
- When scrolling is possible, draw only a thin 1-2 px scroll indicator.
- Register a default keyboard shortcut for toggling the sidebar.
- The default shortcut is `` ` `` / `~` (`key.keyboard.grave.accent`) because the inspected `CJC整合包` keybinds do not occupy it and common single-letter/minimap/JEI/Jade/Iris/QuickShulker keys are already crowded.
- The shortcut toggles only while a supported non-empty container screen is open.
- Render a small transparent expand/collapse button for non-empty supported containers.
- The expand/collapse button must use a hand-drawn geometric chevron or triangle, not a font `>`/`<` glyph, so it can be visually centered.
- The expand/collapse button must be integrated into a compact top header area of the expanded rail, not rendered as a separate side control column.
- The item list starts below the integrated header, so the button cannot overlap the item icon or count text.
- In the collapsed state, render only the compact top tab/button area, not a full-height clipped strip.
- Item icons should be drawn directly on the row background without an extra slot-style icon frame.
- Clicking the expand/collapse button toggles the sidebar between collapsed and expanded states.
- The expanded/collapsed state is global for the client session, so opening another supported non-empty container preserves the previous state.
- Expanding and collapsing should use a short subtle horizontal slide/fade using GUI clipping.

UI redesign recorded during implementation:
- Reason: the earlier content-height translucent panel did not visually align with the vanilla container and the fake chest-frame direction looked awkward.
- Impact: the sidebar no longer shrinks to the item count and no longer attempts to mimic the full chest texture frame.
- New constraint: use a fixed-width, vanilla-adjacent count rail whose height is tied to the opened container UI, with subtle row separation and internal scrolling.

Transparent UI update recorded during implementation:
- Reason: the gray rail still looked too heavy next to the vanilla container and visually diverged from common JEI-style overlays.
- Impact: the sidebar background, rows, slot cells, borders, and scrollbar should be dark translucent with light separators instead of an opaque gray panel.
- New constraint: use a compact transparent overlay style while keeping the exact container-height alignment and clear per-item row separation.

Transparent UI refinement recorded during implementation:
- Reason: the full dark panel and font-based `>` button still looked visually heavy and the glyph could not be centered precisely.
- Impact: remove the dedicated header area, keep the rail height tied to the vanilla container, use lighter full-height bounds plus row-level translucent strips, and draw the button icon manually.
- New constraint: item rows start at the aligned rail top instead of below a header, and the button must not be drawn with text rendering.

Integrated button and alignment update:
- Reason: an external button looked detached from the expanded UI, and using raw `backgroundHeight` made the overlay one pixel taller than the visible vanilla chest texture.
- Impact: the overlay rail is split into an integrated button column plus a list column, and its visible height follows the vanilla texture draw height.
- New constraint: use `backgroundHeight - 1` for visual rail height, while preserving `containerY` for top alignment.

Top-header button update:
- Reason: a side button column still looked like a separate control attached to the UI rather than part of the popped-out rail.
- Impact: remove the side control column and place the toggle affordance inside a thin top header.
- New constraint: the expanded rail is a single list panel with a compact top header; the collapsed affordance uses the same top-header visual language rather than a standalone button block.

Collapsed and icon update:
- Reason: clipping the full panel to the collapsed tab width left a full-height visual strip, and the additional icon frame made rows look cluttered.
- Impact: collapsed state draws only the top tab, and item icons render directly on translucent rows.
- New constraint: no full-height rail should be visible when collapsed; no separate icon frame should be drawn around item icons.

Input redesign recorded during implementation:
- Reason: a default key binding is unnecessary for this small overlay and makes the interaction less discoverable than an on-screen control.
- Impact: remove default keybinding registration and key handling.
- New constraint: use a small on-screen transparent button for toggle behavior, and keep the toggle state global across supported container screens.

Default shortcut update:
- Reason: the target CJC modpack has many existing keybinds, but the `` ` `` / `~` key is unoccupied in inspected `options.txt` and major client-mod config files.
- Impact: restore a registered client keybinding for users who prefer keyboard control, while keeping the on-screen button.
- New constraint: the default keybinding must use `key.keyboard.grave.accent` and must not toggle outside supported non-empty container screens.

## Architecture

- `ChestCountOverlayClient` is the `ClientModInitializer` entrypoint.
- `GenericContainerScreenMixin` hooks `GenericContainerScreen` render handling only, then delegates.
- `ShulkerBoxScreenMixin` hooks `ShulkerBoxScreen` render handling only, then delegates.
- `HandledScreenInputMixin` hooks inherited `HandledScreen#mouseScrolled` and `HandledScreen#mouseClicked`, and only delegates when the current screen instance is a supported chest-like screen.
- `ContainerItemCounter` reads the screen handler slots, counts only the supported container slot range, merges stacks by item plus data components, and sorts results.
- `ContainerItemCounter` also expands readable nested item contents from synchronized item-stack components. The direct container item itself remains counted, and readable nested contents are added on top of it.
- Nested contents are multiplied by the parent stack count. For example, two identical container items with the same synchronized contents count both containers and two copies of their contents.
- Nested expansion is recursive with a fixed depth cap to avoid pathological or corrupt component graphs. If the cap is reached, the current item stack is still counted, but deeper contents are not expanded.
- Nested expansion also has a total nested stack visit cap per count pass to avoid pathological or corrupt high-branching component graphs. If the cap is reached, direct container slot stacks and already visited nested stacks remain counted, but additional nested stacks and deeper contents are not expanded; this limitation must be logged or otherwise made clear during implementation.
- Counts must remain exact for all counted stacks; use an overflow-safe numeric representation instead of saturating or wrapping totals.
- `CountedItem` represents one counted item entry.
- `OverlayScrollState` owns scroll offset and clamp logic.
- `ChestCountOverlayRenderer` owns layout, transparent panel drawing, item rendering, scrollbar drawing, mouse hit testing, count text, and tooltip behavior.

Mapping-dependent implementation notes:
- Confirm Yarn 1.21.11+build.5 names before relying on `GenericContainerScreen` rows or `HandledScreen` position/background fields.
- Use protected access where available.
- If a required field is inaccessible, add an accessor mixin instead of guessing names.
- Keep short comments near mapping-sensitive hooks.

Mapping deviation recorded during implementation:
- Reason: Generated Yarn 1.21.11+build.5 sources show `GenericContainerScreen` does not declare `mouseScrolled`; the method is declared on `HandledScreen`.
- Impact: injecting `mouseScrolled` into `GenericContainerScreen` builds with a remap warning and may not hook the inherited method reliably.
- New constraint: keep `GenericContainerScreenMixin` for rendering, and add a `HandledScreen` scroll mixin guarded by `instanceof GenericContainerScreen` so non-container screens are unaffected.

Mouse input mapping update:
- Reason: Yarn 1.21.11+build.5 sources show `HandledScreen#mouseClicked` uses `net.minecraft.client.gui.Click` plus a `boolean doubled` parameter, not the older `double mouseX, double mouseY, int button` signature.
- Impact: using the old injection descriptor crashes during Mixin application at client startup.
- New constraint: inject `HandledScreen#mouseClicked(Click, boolean)` and read `click.x()`, `click.y()`, and `click.button()` from the `Click` record.

Input behavior update:
- Superseded by Input behavior update 2 for default keybinding registration.
- Handle the small on-screen toggle button in the inherited `HandledScreen#mouseClicked` path while the current screen is a supported chest-like screen.
- The button toggles only local client overlay state and must not send packets.

Input behavior update 2:
- Register the default keybinding `key.chest_count_overlay.toggle_overlay` in category `key.category.chest_count_overlay.controls`.
- The keybinding toggles the same global expanded/collapsed state as the on-screen button.
- Keep the on-screen button available even when the shortcut exists.

Configuration UI update:
- Reason: users expect the Mod Menu configuration button to open a real settings screen, and the target CJC modpack already includes YACL.
- Impact: add a Mod Menu entrypoint that opens a YACL screen.
- New constraint: YACL is a required runtime dependency declared in `fabric.mod.json`; compile against `dev.isxander:yet-another-config-lib`.
- New constraint: Mod Menu is a compile-time optional integration and is not bundled into this mod jar.
- New constraint: do not hand-write a vanilla settings screen for this mod unless YACL support is deliberately removed later.
- New constraint: store settings in a client-only `config/chest_count_overlay.json` file.
- Initial settings: enable overlay, placement, show empty containers, and count nested container contents.
- Version note: Xander Maven currently resolves `3.8.1+1.21.11-fabric`; the inspected modpack has YACL `3.8.2+1.21.11-fabric`, which satisfies the runtime lower bound.

YACL layout update:
- Reason: YACL 3.8's public builder does not expose controls for removing its built-in search field or hard-coded right-side description pane, and it does not include a keybinding capture controller.
- Impact: the settings screen should rely on YACL's native option and description layout instead of trying to hide or custom-draw around those built-in widgets.
- New constraint: use YACL option descriptions normally so the right-side details pane has matching YACL-managed content.
- New constraint: expose shortcut configuration through a YACL button that opens Minecraft's vanilla keybind screen, where the Fabric-registered keybinding can be changed.
- New constraint: do not serialize a duplicate shortcut setting in this mod config; the vanilla `options.txt` keybinding system remains the source of truth.

Shortcut handling update:
- Reason: registered `KeyBinding#wasPressed` polling did not reliably toggle while a handled container screen had focus.
- Impact: handle the toggle key inside `HandledScreen#keyPressed` and consume it when the current screen is a supported non-empty container.
- New constraint: the shortcut path must use the registered keybinding's current bound key so user changes in Controls still apply.

Shulker screen support update:
- Reason: vanilla uses `ShulkerBoxScreen` and `ShulkerBoxScreenHandler` for opened shulker boxes, not `GenericContainerScreen`.
- Impact: render and input hooks must explicitly support both `GenericContainerScreen` and `ShulkerBoxScreen`.
- New constraint: for `GenericContainerScreenHandler`, count `rows * 9` slots; for `ShulkerBoxScreenHandler`, count exactly the first 27 container slots.

## Reference Links

- https://docs.fabricmc.net/
- https://docs.fabricmc.net/develop/loom/
- https://docs.fabricmc.net/develop/loader/
- https://docs.fabricmc.net/develop/loader/fabric-mod-json/
- https://maven.fabricmc.net/net/fabricmc/fabric-loom-remap/net.fabricmc.fabric-loom-remap.gradle.plugin/maven-metadata.xml
- https://maven.fabricmc.net/net/fabricmc/yarn/
- https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/
- https://meta.fabricmc.net/v2/versions/loader/1.21.11

## Deviation Rule

All implementation work must follow this file. If implementation discovers that a requirement must change, update this file first with:
- the reason,
- the impact,
- the new constraint or behavior.

Only then change code. Silent fallback is not allowed.
