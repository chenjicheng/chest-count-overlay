# 开发、截图与发布

**简体中文** | [English](en/development.md)

## 构建环境

固定目标为 Minecraft 1.21.11、Temurin Java 21、Yarn `1.21.11+build.5`、Fabric Loader `0.18.0`、Fabric API `0.141.4+1.21.11`、Loom `1.16.2`、Gradle Wrapper `9.5.1`。GUI 依赖为 YACL `3.8.1+1.21.11-fabric`；Mod Menu `17.0.0-beta.2` 是编译期可选集成与开发运行依赖。

使用项目 Wrapper，不依赖全局 Gradle。Wrapper 验证 Gradle 发行包 SHA256。已有 Temurin 21 时，仅在当前命令进程设置 `JAVA_HOME`/`PATH`；不要覆盖用户已有全局配置。Windows 可运行现有 `scripts/bootstrap.ps1 -SkipBuild` 检查环境；缺少 JDK 时该脚本会调用包管理器安装，运行前应了解它的作用。

```powershell
.\gradlew.bat build --no-daemon --console=plain
python -m unittest discover -s scripts -p 'test_*.py' -v
```

Linux/macOS 使用 `./gradlew build`。正式产物为 `build/libs/chest_count_overlay-<mod_version>.jar`，源码为同版本 `-sources.jar`。`mod_version` 保持纯语义版本；本项目不会向版本号或文件名追加 Minecraft 后缀。未来构建的两个 JAR 都包含 MIT 许可证。

## 代码结构与边界

| 文件或模块 | 职责 |
| --- | --- |
| `ChestCountOverlayClient` | 加载客户端配置、注册快捷键 |
| `GenericContainerScreenMixin` / `ShulkerBoxScreenMixin` | 复用原版屏幕，在渲染末尾绘制统计栏 |
| `HandledScreenInputMixin` | 只在受支持屏幕处理按钮、快捷键和滚动 |
| `ContainerItemCounter` / `CountedItem` | 限定容器槽位，按物品与完整组件合并，递归读取已同步内容，精确累计与稳定排序 |
| `ChestCountOverlayRenderer` / `OverlayScrollState` | 布局、透明绘制、会话展开状态、滚动与悬停提示 |
| `ChestCountOverlayConfig` / `ChestCountOverlayConfigScreen` | 客户端 JSON 持久化与 YACL 配置界面 |
| `src/gametest` | 独立客户端截图与行为夹具，绝不打包进发行 JAR |
| `scripts/release.py` / `scripts/modrinth.py` | 发行产物校验与 Modrinth 上传 |

`GenericContainerScreenHandler` 只读取前 `rows * 9` 格；`ShulkerBoxScreenHandler` 只读取前 27 格。只使用客户端已同步的 `ItemStack`，不发送自定义网络包，不读取服务端未同步的数据。嵌套遍历上限及显示格式见[使用说明](guide.md#统计范围与限制)。

Generic 箱类屏幕的可见高度为 `backgroundHeight - 1`，行高固定 18、顶部标题区 14、面板宽 74；布局按可用空间选择左/右并限制在窗口内。展开状态是客户端会话状态；快捷键由原版 `options.txt` 管理，JSON 仅保存四个配置字段。

## 自动截图

```powershell
.\scripts\screenshots.ps1 -AcceptMinecraftEula
```

仅在接受 [Minecraft EULA](https://aka.ms/MinecraftEULA) 后使用该开关。脚本运行 `runClientGameTest -PacceptMinecraftEula`，Loom 在 `build/run/clientGameTest` 创建隔离环境与测试世界，不使用个人存档。测试基于 [Fabric 客户端测试 API](https://docs.fabricmc.net/1.21.11/develop/automatic-testing)。

`DocumentationScreenshots` 通过集成服务器打开真实原版菜单，等待同步后的物品计数，检查玩家背包排除、嵌套开关、默认快捷键、折叠和滚动，再打开英文/中文 YACL 设置。测试世界先以默认小窗口和 2 区块视距加载，减少软件渲染 CI 的启动开销；截图时切换到 1440×900、GUI 缩放 3。截图使用实际生产渲染代码。图片生成时只清理游戏配方提示，不绘制或修补统计栏。

截图首先保存到 `build/run/clientGameTest/documentation-screenshots`；只有全部场景完成才写 `completed.json`。运行失败不会复制图片。`collect_screenshots.py` 验证完整场景、PNG 头和分辨率，再原字节复制到 `docs/public/images`。发布前仍要逐张视觉检查，测试成功不等于画面合格。

Linux 图形测试可使用：

```sh
xvfb-run -a ./gradlew runClientGameTest -PacceptMinecraftEula --no-daemon --console=plain
python3 scripts/collect_screenshots.py
```

需要 Xvfb 与支持 Minecraft 的 OpenGL 环境。CI 上传截图和日志供检查；CI 不自动提交或覆盖仓库中的文档图片。修改夹具后先重新生成并检查图片，再更新相关描述。

## 发布流程

- **CI**：分支、PR 与手动运行，复用 `build.yml`；运行 Python 发布/截图收集测试、Gradle 构建、客户端行为与截图测试，校验并上传当前版本的发行产物。
- **Release**：仅匹配 `v*` 标签触发。`verify` 完成后，独立任务使用同一测试产物发布 GitHub Release 与 Modrinth。构建和 Modrinth 任务仅有仓库读取权限；只有 GitHub 发布任务有写权限。
- **Documentation**：VitePress `1.6.4`，固定 Vite `6.4.3`，使用 npm 锁文件。分支/PR 校验文档，`main` 构建并部署到 GitHub Pages，基路径为 `/chest-count-overlay/`。

发行前更新 `gradle.properties` 的 `mod_version`，添加 `docs/releases/<version>.md` 双语说明，运行检查并提交。标签必须与配置一致，例如未来发布 `1.0.2` 时，先将配置和说明更新为该版本，再执行：

```sh
git tag -a v1.0.2 -m "Chest Count Overlay 1.0.2"
git push origin v1.0.2
```

不要移动既有标签或覆盖已发布 JAR。最初 GitHub 的 1.0.1 产物保留原字节；后续新增许可证、文档或构建元数据不会回写该资产。

首次配置 GitHub **Settings → Secrets and variables → Actions**：

- Variable `MODRINTH_PROJECT_ID`：本项目的 Modrinth ID `MfEXlclW`，不能使用其他项目 ID。
- Secret `MODRINTH_TOKEN`：专用 CI PAT，仅需读取项目、读取版本和创建版本。创建/编辑 Modrinth 项目的 Token 单独保管，不放入 CI 或仓库。

GitHub Pages 的发布来源设置为 **GitHub Actions**。GitHub Release 使用内置 `GITHUB_TOKEN`，无需额外 GitHub PAT。首次 Modrinth 项目需要提交审核；创建草稿/上传文件不等于已经公开。

发行脚本只选择当前版本的两个 JAR，核对模组 ID、版本、`client` 环境、Minecraft 版本与 MIT 许可证，拒绝混入截图测试、Mod Menu/YACL/Fabric/Kotlin 库或旧版本文件。上传前核对 SHA256；GitHub 先上传完整草稿再公开。Modrinth 仅上传正式 JAR：环境 `client_only`，Fabric API 与 YACL 必需，Mod Menu 可选；稳定版为 `release`，alpha 为 `alpha`，其他预发布版为 `beta`。

Modrinth 上传后读回主文件 SHA512、版本信息、更新说明和依赖。重复运行跳过完全相同的版本，冲突则失败而不覆盖。网络写入失败不盲目重发；重新运行前先查询已有版本。如果只有 Modrinth 失败，使用 Actions 的 **Re-run failed jobs**，保留已成功的 GitHub Release。

## 文档本地预览

```sh
npm ci
npm run docs:build
npm run docs:preview
```

开发可用 `npm run docs:dev`。保留 VitePress 的站内死链接检查，检查中英文页面、截图、导航、窄窗口和深色模式。公开文档不包含 Token、本地凭证文件、测试存档或任务记录；工作进展与待办由 Plane 管理。
