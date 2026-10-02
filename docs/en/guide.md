# Installation and usage

[简体中文](../guide.md) | **English**

Chest Count Overlay is a client-only Fabric mod for Minecraft 1.21.11. It reads container data already synchronized to your client, leaves vanilla interaction intact, and displays totals beside the container.

## Install

Requires Java 21, Minecraft **1.21.11**, Fabric Loader **≥0.18.0**, [Fabric API](https://modrinth.com/mod/fabric-api), and [YACL](https://modrinth.com/mod/yacl). Validated with Fabric API `0.141.4+1.21.11` and YACL `3.8.1+1.21.11-fabric`. Choose Fabric files for the correct Minecraft version.

Place the installable mod JAR and both required dependencies in the client's `mods` folder, then restart. The `-sources.jar` is for development. The server does not need this mod. Optional [Mod Menu](https://modrinth.com/mod/modmenu) provides the settings button; tested with `17.0.0-beta.2`.

## Expand and collapse

Open a chest, double/trapped chest, barrel or shulker box. The overlay starts as a small transparent arrow. Click it or press the grave-accent key (`` ` `` / `~`, usually below Esc) to expand. Change the shortcut under **Options → Controls → Key Binds → Chest Count Overlay**. It works only in supported nonempty container screens.

![Expanded count rail showing 128 torches and 96 stone](/images/cco-chest-counts.png)

The chest's `64 + 32` stone stacks total **96**. Another 64 stone in the player's hotbar are excluded.

![Collapsed overlay leaves only the small top tab](/images/cco-collapsed.png)

Expansion state carries across supported containers for the current client session and resets on restart. It is not stored in the mod configuration.

## Counts, nested contents and scrolling

Items merge only when their complete data components match. Names, enchantments and stored contents can create separate entries. Totals sort in descending order, retaining first-seen order for ties. Hover for an item's name and exact total.

Counts below `100000` display as full integers. Larger counts are floored to thousands, for example `154900 → 154k`; tooltips retain the exact integer.

![Synchronized shulker-box and bundle contents included in the large chest's totals](/images/cco-nested-containers.png)

This chest contains 8 loose diamonds and two blue shulker boxes with 16 diamonds each, giving **40** diamonds. The two boxes themselves are counted too. Carrots and gold stored in the bundle also appear. Disabling nested counting leaves only the 8 loose diamonds.

![The same count rail beside an opened shulker box](/images/cco-shulker-box.png)

![The rail after scrolling, with a thin scroll indicator](/images/cco-scrolling.png)

The rail matches the vanilla container height. Hover over it and scroll three rows at a time for additional entries. Placement prefers the left, switches sides when necessary, and clamps inside the screen if neither side fits; extremely narrow layouts may partly overlap the vanilla window.

## Settings

With Mod Menu installed, open **Mods → Chest Count Overlay → Configure**. Client settings live in `config/chest_count_overlay.json`.

![English YACL settings and nested-content explanation](/images/cco-settings-en.png)

| Field | Default | Behavior |
| --- | --- | --- |
| `enabled` | `true` | Enables the overlay |
| `placement` | `LEFT` | `LEFT` / `AUTO` prefer the left; `RIGHT` prefers the right, with space-aware fallback |
| `showWhenEmpty` | `false` | Hides empty containers by default; enabling it keeps the button and an empty panel when expanded |
| `countNestedContainerContents` | `true` | Includes readable synchronized container and bundle contents |

```json
{
  "enabled": true,
  "placement": "LEFT",
  "showWhenEmpty": false,
  "countNestedContainerContents": true
}
```

The shortcut button opens Minecraft's keybind screen. Bindings remain in vanilla `options.txt`, not this JSON. Restart after editing JSON manually. Invalid or unreadable configuration fails explicitly; preserve the original file before repairing it.

## Scope and limitations

- Supports vanilla nine-column chest-like screens and `ShulkerBoxScreen`. Furnaces, hoppers and unrelated custom modded screens are excluded.
- Counts container slots, excluding player inventory. It sends no custom packets and changes no items, servers or server permissions.
- Reads existing synchronized `CONTAINER` and `BUNDLE_CONTENTS` data only. It never resolves `CONTAINER_LOOT` or guesses ungenerated loot.
- Nested traversal expands at most eight levels and visits at most 16384 nested stacks per pass. At the limit, direct stacks and previously visited content remain counted while further nested content is omitted. The visit limit is logged. Ordinary direct-slot counting remains intact.

Images use fixed automated test data, captured from the real game and production renderer. The rail was not painted or composited onto screenshots. See [regeneration instructions](development.md#automated-screenshots).
