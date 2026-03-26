# Changelog

所有版本变更记录。格式遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，版本号遵循 [Semantic Versioning](https://semver.org/lang/zh-CN/)。

---

## [1.0.0] - 2026-03-26

### 新增
- 核心番茄钟计时功能（专注 / 短休息 / 长休息三阶段）
- 自由设置各阶段时长（专注 1-120 分钟，短休 1-30 分钟，长休 5-60 分钟）
- 自定义长休间隔（每完成 N 个番茄后触发长休，默认 4）
- 前台 Service 保活，锁屏、切后台计时不中断
- 锁屏通知栏显示当前阶段和剩余时间
- 阶段完成声音 + 震动双重提醒（可分别开关）
- 今日完成番茄数统计（小圆点 + 数字）
- 深色模式自动跟随系统
- 三阶段颜色区分（专注红 / 短休蓝 / 长休紫）
- Android 13+ 通知权限动态申请

### 技术
- Kotlin + Jetpack Compose + Material3
- MVVM + ForegroundService 架构
- Room 数据库记录历史
- DataStore 持久化设置
- 最低支持 Android 8.0（API 26）

---

_[1.0.0]: 初始发布版本_
