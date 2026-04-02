# Proposal: 品牌升级

**Change ID**: update-brand-identity  
**状态**: 待审批

## Why（为什么做）

App 原名「番茄钟」绑定了固定 25+5 的概念认知，与 v2.0 强调个性化节律的定位不符：
- 在应用商店与所有番茄钟 App 直接竞争，无差异化
- 「番茄钟」暗示死板的固定时长，与产品实际能力不符

## What Changes（做什么）

| 项目 | 旧值 | 新值 |
|------|------|------|
| 中文名 | 番茄钟 | 节律 |
| 英文名 | Pomodoro | FlowCycle |
| 包名 | com.pomodoro.app | com.flowcycle.app |
| App ID（applicationId） | com.pomodoro.app | com.flowcycle.app |
| 应用商店关键词 | — | 番茄钟、专注、计时器、pomodoro、节律 |
| Slogan | — | 按你的节奏，专注每一刻 |

**保留策略**：
- 应用商店描述中保留「番茄钟」关键词，保证搜索命中
- 包名变更意味着设备上视为新 App 安装，旧版用户需手动迁移（此为独立 App，可接受）

## Impact（影响）

- **build.gradle.kts**：applicationId 改为 com.flowcycle.app
- **strings.xml**：app_name 更新
- **AndroidManifest.xml**：package 相关声明检查
- **源码包名**：所有 Kotlin 文件包声明从 com.pomodoro.app 改为 com.flowcycle.app（全局重命名）
