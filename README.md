# 番茄钟 Android App

一款极简风格的番茄工作法计时工具，采用 Apple 设计语言，支持完全自定义时长。

## 功能特性

- 🍅 **自由设置时长**：专注、短休、长休时长均可独立调整
- ⏰ **后台保活**：锁屏不中断，前台服务保持计时
- 🔔 **到点提醒**：声音 + 震动双重提醒
- 📊 **今日统计**：实时显示当天完成的番茄数
- 🌙 **深色模式**：自动跟随系统

## 技术栈

- Kotlin + Jetpack Compose
- MVVM + ForegroundService
- Room + DataStore
- Material3（Apple 风格定制）

## 快速开始

1. 用 Android Studio 打开项目
2. 连接设备或启动模拟器
3. 点击 Run

**在 `app/src/main/res/raw/` 放置 `timer_complete.mp3` 以启用音效提醒。**

## 文档

- [技术文档](docs/TECHNICAL.md)
- [测试计划](docs/TEST_PLAN.md)

## 运行测试

```bash
./gradlew test                  # 单元测试
./gradlew connectedAndroidTest  # UI 测试（需设备）
```

## 版本规划

| 版本 | 功能 |
|---|---|
| v1.0 | 核心计时 + 自定义设置 + 提醒 + 后台保活 |
| v1.1 | 多套方案 + 历史统计 |
| v1.2 | 任务绑定 + 图表 |
