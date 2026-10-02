# 安装与使用

**简体中文** | [English](en/guide.md)

Chest Count Overlay（箱子数量统计）是 Minecraft 1.21.11 的纯客户端 Fabric 模组。它读取已经同步到客户端的容器物品，保留原版交互，在旁边显示合计数量。

## 安装

需要 Java 21、Minecraft **1.21.11**、Fabric Loader **≥0.18.0**、[Fabric API](https://modrinth.com/mod/fabric-api) 和 [YACL](https://modrinth.com/mod/yacl)。验证组合为 Fabric API `0.141.4+1.21.11`、YACL `3.8.1+1.21.11-fabric`。请选择对应 Minecraft 版本的 Fabric 文件。

把本模组正式 JAR 与两个必需依赖放入客户端的 `mods` 文件夹，重启游戏。`-sources.jar` 是开发源码，不是可安装模组。服务端不需要本模组；可选 [Mod Menu](https://modrinth.com/mod/modmenu) 用于打开设置，测试版本为 `17.0.0-beta.2`。

## 展开与收起

打开箱子、大箱子、陷阱箱、木桶或潜影盒。第一次打开时只出现一个透明小箭头，点击它或按反引号键（`` ` `` / `~`，通常位于 Esc 下方）展开。快捷键可在 **选项 → 控制 → 按键设置 → 箱子数量统计** 修改；只在支持的非空容器界面生效。

![展开的箱子统计栏：128 个火把、96 个石头](/images/cco-chest-counts.png)

图中箱子里的 `64 + 32` 个石头合并成 **96**。玩家快捷栏还有 64 个石头，统计结果仍为 96，因为玩家背包不计入。

![收起后仅保留顶部小箭头](/images/cco-collapsed.png)

关闭容器再打开另一容器，会保留本次客户端会话的展开状态。重启游戏后恢复收起。展开状态不写入模组配置。

## 数量、嵌套与滚动

相同物品按完整数据组件合并：名称、附魔、容器内容等不同的数据会保留为不同条目。按合计数量降序排列，数量相同则保留首次出现顺序。悬停条目可查看物品名称与精确数量。

`0–99999` 显示完整整数；`100000` 及以上向下取整为 `k`，例如 `154900 → 154k`。悬停提示仍显示精确整数。

![潜影盒和收纳袋的内容合并到大箱子统计中](/images/cco-nested-containers.png)

这个大箱子直接放着 8 个钻石，两个蓝色潜影盒各装 16 个钻石，所以统计栏显示 **40**。潜影盒本身也统计为 2；收纳袋中的胡萝卜和金锭同样加入合计。关闭“统计嵌套容器内容”后，钻石只显示直接放在箱中的 8 个。

![打开潜影盒时显示相同的统计栏](/images/cco-shulker-box.png)

![鼠标滚轮滚动后的统计栏与细滚动指示条](/images/cco-scrolling.png)

统计栏与原版容器高度对齐，物品种类超过可显示行数时，在统计栏内滚动，每次滚动 3 行。优先在左侧显示；空间不足时换侧，两侧都不足则限制在屏幕范围内，可能与原版窗口部分重叠。

## 设置

安装 Mod Menu 后，进入 **模组 → Chest Count Overlay → 配置**。设置文件为客户端 `config/chest_count_overlay.json`。

![YACL 中文设置与嵌套统计说明](/images/cco-settings-zh.png)

| 字段 | 默认值 | 作用 |
| --- | --- | --- |
| `enabled` | `true` | 是否显示统计栏 |
| `placement` | `LEFT` | `LEFT` / `AUTO` 优先左侧，`RIGHT` 优先右侧；不够空间时换侧或限制范围 |
| `showWhenEmpty` | `false` | 空容器默认不显示；开启后仍显示按钮，展开时显示没有条目的面板 |
| `countNestedContainerContents` | `true` | 展开物品栈中已同步的容器和收纳袋内容 |

```json
{
  "enabled": true,
  "placement": "LEFT",
  "showWhenEmpty": false,
  "countNestedContainerContents": true
}
```

快捷键按钮会打开 Minecraft 原版按键设置。绑定由 `options.txt` 管理，不在这个 JSON 中重复保存。手动修改 JSON 后重启游戏；文件损坏或读取失败会明确报错，修复前先保留原文件。

## 统计范围与限制

- 原版 9 列箱类界面与 `ShulkerBoxScreen` 受支持；使用自定义界面的熔炉、漏斗或其他模组容器不自动支持。
- 只统计容器自身槽位，不统计玩家背包；不发送自定义网络包，不修改物品、服务器或服务端权限。
- 嵌套只读取 `CONTAINER`、`BUNDLE_CONTENTS` 中已有的同步内容，不解析 `CONTAINER_LOOT` 或推测未生成的战利品。
- 嵌套最多展开 8 层，每次统计最多访问 16384 个嵌套物品栈。达到上限时保留直接物品和已遍历内容，其余嵌套内容不计入；访问上限会写入日志。普通容器统计不受这种特殊数据限制。

本页图片由自动化客户端测试使用固定示例数据生成；图片来自真实游戏和本模组的渲染代码，未绘制或合成统计栏。重生成方法见[开发文档](development.md#自动截图)。
