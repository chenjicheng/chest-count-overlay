# Chest Count Overlay / 箱子数量统计

**简体中文** | [English](README.en.md)

适用于 **Minecraft Java 1.21.11 · Fabric · Java 21** 的纯客户端模组。在原版容器界面旁显示物品图标与合计数量，支持箱子、大箱子、陷阱箱、木桶和潜影盒。

![箱子中两组火把合并为 128、两组石头合并为 96；玩家背包里的石头不计入](docs/public/images/cco-chest-counts.png)

- 同种物品按完整数据组件合并，数量从多到少排序；同数量保留首次出现顺序。
- 可统计物品栈中已同步的潜影盒、收纳袋等嵌套内容，容器物品自身也计入。
- 默认收起。点击顶部小箭头或按反引号键（`` ` `` / `~`）展开；物品种类过多时将鼠标放到统计栏上滚动。
- 展开状态在本次客户端会话内保留，重启后重新收起；不会修改容器、物品或服务器协议。
- Mod Menu 提供 YACL 中英文设置入口；客户端单独安装即可使用，无需服务端安装。

[Modrinth](https://modrinth.com/mod/chest-count-overlay) · [GitHub Releases](https://github.com/chenjicheng/chest-count-overlay/releases) · [使用说明](docs/guide.md) · [开发与发布](docs/development.md)

## 安装

在客户端安装 `chest_count_overlay-1.0.1.jar`、[Fabric API](https://modrinth.com/mod/fabric-api) 和 [YACL](https://modrinth.com/mod/yacl)，使用 Fabric Loader **0.18.0 或更新版**。

测试版本为 Fabric API `0.141.4+1.21.11`、YACL `3.8.1+1.21.11-fabric`；YACL 是必需依赖，[Mod Menu](https://modrinth.com/mod/modmenu) 为可选设置入口。不要安装 `-sources.jar`。升级时移走旧版 JAR，避免重复加载。

只统计当前打开的容器及客户端已收到的数据；玩家背包、未同步的战利品表内容、自定义界面的其他容器不在统计范围内。嵌套遍历有深度与数量上限，详见[使用说明](docs/guide.md#统计范围与限制)。

## 构建与截图

```powershell
.\gradlew.bat build
.\scripts\screenshots.ps1 -AcceptMinecraftEula
```

截图脚本在 `build` 中创建隔离测试世界，运行真实容器与配置界面、验证数量及输入行为，再复制七张原始 PNG 到文档。只有同意 [Minecraft EULA](https://aka.ms/MinecraftEULA) 后才传入该开关。图中使用自动化测试数据，无需操作个人存档。

推送与 `mod_version` 匹配的 `v*` 标签，经构建、发布校验和客户端测试后，由独立任务把同一正式 JAR 发布到 GitHub 与 Modrinth。文档使用 VitePress，`main` 文档由 Actions 部署到 GitHub Pages。详见[发布配置](docs/development.md#发布流程)。

本项目采用 [MIT 许可证](LICENSE)。
