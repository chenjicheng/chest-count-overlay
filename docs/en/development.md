# Development, screenshots and releases

[简体中文](../development.md) | **English**

## Build environment

Pins Minecraft 1.21.11, Temurin Java 21, Yarn `1.21.11+build.5`, Fabric Loader `0.18.0`, Fabric API `0.141.4+1.21.11`, Loom `1.16.2`, and Gradle Wrapper `9.5.1`. YACL `3.8.1+1.21.11-fabric` is required; Mod Menu `17.0.0-beta.2` is an optional integration and development runtime dependency.

Use the project Wrapper, which verifies the Gradle distribution SHA256. Set `JAVA_HOME` and `PATH` only for the current command when selecting an installed Temurin 21; preserve global configuration. On Windows, `scripts/bootstrap.ps1 -SkipBuild` checks the environment and uses a package manager if the JDK is absent; review its installation behavior first.

```powershell
.\gradlew.bat build --no-daemon --console=plain
python -m unittest discover -s scripts -p 'test_*.py' -v
```

On Linux/macOS use `./gradlew build`. Installable output is `build/libs/chest_count_overlay-<mod_version>.jar`, with a matching `-sources.jar`. This project's version and filename retain plain semantic versions without a Minecraft suffix. Future installable JARs include the MIT license.

## Code map and boundaries

| Component | Responsibility |
| --- | --- |
| `ChestCountOverlayClient` | Client configuration and key registration |
| `GenericContainerScreenMixin` / `ShulkerBoxScreenMixin` | Append the rail to vanilla screen rendering |
| `HandledScreenInputMixin` | Supported-screen clicks, shortcut and scrolling |
| `ContainerItemCounter` / `CountedItem` | Slot limits, complete-component merging, synchronized nested traversal, exact totals and stable sorting |
| `ChestCountOverlayRenderer` / `OverlayScrollState` | Layout, translucent drawing, session expansion, scroll and tooltips |
| `ChestCountOverlayConfig` / `ChestCountOverlayConfigScreen` | Client JSON persistence and YACL UI |
| `src/gametest` | Separate client screenshot/behavior fixtures, excluded from distributed JARs |
| `scripts/release.py` / `scripts/modrinth.py` | Artifact validation and Modrinth upload |

Only the first `rows * 9` generic-container slots or first 27 shulker-box slots are counted. All data comes from synchronized client `ItemStack`s, with no custom packets or server-only reads. See [counting limitations](guide.md#scope-and-limitations).

Generic-container visual height is `backgroundHeight - 1`; rows are 18 pixels, header 14, rail width 74. Layout chooses a side and clamps inside the window. Expansion is session state; vanilla `options.txt` owns key bindings while JSON stores the four settings.

## Automated screenshots

```powershell
.\scripts\screenshots.ps1 -AcceptMinecraftEula
```

Use the switch only after accepting the [Minecraft EULA](https://aka.ms/MinecraftEULA). Loom creates the test environment/world under `build/run/clientGameTest`, without touching a personal save. The harness uses the [Fabric client test API](https://docs.fabricmc.net/1.21.11/develop/automatic-testing).

`DocumentationScreenshots` opens real vanilla menus through the integrated server and waits for synchronized counts. It checks inventory exclusion, nested counting, the default shortcut, collapse and scrolling, then opens English/Chinese YACL settings. Window size is 1440×900 with GUI scale 3. Screenshots use the production renderer; the harness only clears vanilla recipe notifications before capture.

Raw images are saved under `build/run/clientGameTest/documentation-screenshots`. Only a complete run writes `completed.json`; failed runs never copy images. `collect_screenshots.py` checks the full scene list, PNG headers and dimensions before copying unchanged bytes into `docs/public/images`. Visually inspect every image before publishing.

Linux graphical testing:

```sh
xvfb-run -a ./gradlew runClientGameTest -PacceptMinecraftEula --no-daemon --console=plain
python3 scripts/collect_screenshots.py
```

Requires Xvfb and an OpenGL environment supporting Minecraft. CI uploads screenshots/logs, without committing or replacing documentation images automatically. Regenerate and inspect images after changing a fixture.

## Release process

- **CI** reuses `build.yml` for branches, PRs and manual runs. Runs Python release/collection tests, Gradle builds, client behavior and screenshot tests, then stages the exact current-version distribution.
- **Release** runs only for matching `v*` tags. Independent GitHub and Modrinth jobs receive the same tested artifact after `verify`. Only GitHub publishing has repository write permission.
- **Documentation** uses VitePress `1.6.4`, pinned Vite `6.4.3`, and the npm lockfile. Branches/PRs validate docs; `main` deploys to GitHub Pages with base `/chest-count-overlay/`.

Update `mod_version`, add bilingual `docs/releases/<version>.md`, run checks and commit before tagging. For a future `1.0.2`, first update configuration and notes, then:

```sh
git tag -a v1.0.2 -m "Chest Count Overlay 1.0.2"
git push origin v1.0.2
```

Never move existing tags or replace published JARs. The original GitHub 1.0.1 asset retains its bytes; later license, documentation or metadata additions do not alter it.

Configure **Settings → Secrets and variables → Actions**:

- Variable `MODRINTH_PROJECT_ID`: this mod's ID `MfEXlclW`, never another project's.
- Secret `MODRINTH_TOKEN`: a dedicated CI PAT with read-project, read-version and create-version permissions. Keep the project creation/edit token separate and outside CI/the repository.

Set the Pages source to **GitHub Actions**. GitHub Releases use the built-in `GITHUB_TOKEN`, without an extra PAT. A new Modrinth project needs review; drafts and uploaded files are not automatically public.

Packaging selects only the two current JARs, verifies mod ID/version/client environment/Minecraft/MIT license, and rejects screenshot tests, bundled Mod Menu/YACL/Fabric/Kotlin libraries or stale files. Downloads are SHA256 checked. GitHub first uploads a complete draft, then publishes it. Modrinth receives only the installable JAR, using `client_only`, required Fabric API/YACL, and optional Mod Menu. Stable/alpha/other prerelease channels map to `release`/`alpha`/`beta`.

Modrinth readback verifies the primary file's SHA512, metadata, notes and dependencies. Identical reruns skip uploading; conflicts fail without overwriting. Failed writes are never blindly repeated: reruns inspect existing versions first. If only Modrinth fails, select **Re-run failed jobs** to retain a successful GitHub release.

## Documentation preview

```sh
npm ci
npm run docs:build
npm run docs:preview
```

Use `npm run docs:dev` during editing. Retain internal dead-link checks and verify both languages, screenshots, navigation, narrow windows and dark mode. Public docs exclude tokens, credential files, saves and task records; Plane stores progress and follow-ups.
