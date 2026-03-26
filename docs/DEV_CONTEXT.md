# 开发上下文 (DEV_CONTEXT.md)

> 此文档供开发者本人及 AI 助手在下次迭代时快速还原上下文，**每次迭代后请同步更新**。

---

## 项目概况

| 字段 | 内容 |
| --- | --- |
| 项目名称 | 定义-番茄钟 |
| 包名 | `com.pomodoro.app` |
| 当前版本 | v1.0.0（versionCode = 1） |
| 最低 Android | API 26（Android 8.0） |
| 目标 SDK | API 34（Android 14） |
| 开发语言 | Kotlin |
| UI 框架 | Jetpack Compose + Material3 |
| 本地路径 | `/Users/qiyanping/WorkBuddy/Claw/PomodoroApp` |
| GitHub 仓库 | https://github.com/Tufeng/pomodoro-android |
| 主分支 | `main`（本地为 `master`，推送时映射） |

---

## 技术架构

```
MainActivity
  └── PomodoroApp (NavHost)
        ├── TimerScreen  ← 主页，显示倒计时
        └── SettingsScreen  ← 设置页，保存后自动更新计时页

PomodoroViewModel
  └── combine(PomodoroTimerService.timerState, PomodoroPreferences.configFlow)
        ├── IDLE 状态：从 configFlow 读取最新设置
        └── 运行中状态：从 Service state 读取实时数据

PomodoroTimerService (ForegroundService)
  └── 独立计时逻辑，StateFlow 广播状态

数据层
  ├── PomodoroPreferences (DataStore)  ← 设置持久化
  └── PomodoroDatabase (Room)          ← 历史记录持久化
```

### 关键依赖版本
- Compose BOM：统一管理
- Room：KSP 编译
- DataStore Preferences
- Navigation Compose
- Coroutines + Flow

---

## 已解决的技术问题（重要决策记录）

### 1. 计时器数字抖动
**问题**：AnimatedContent 中，不同数字（如 `1` 和 `8`）字形宽度不同，导致布局左右跳动。  
**方案**：将 `MM:SS` 拆分为 4 个独立的 `DigitSlot` 组件，每个固定宽度 `40.dp`，动画只做 fadeIn/fadeOut，不做位移。  
**文件**：`ui/screens/TimerScreen.kt` → `DigitSlot()` 组件

### 2. 设置保存后计时页面不刷新
**问题**：`timerUiState` 仅订阅 Service 的 StateFlow，Service 未启动时感知不到 Preferences 变化。  
**方案**：用 `combine()` 合并两个 Flow，IDLE 时使用 configFlow 的最新值，运行时使用 Service 数据。  
**文件**：`viewmodel/PomodoroViewModel.kt` → `timerUiState`

### 3. 锁屏通知显示
**方案**：`buildNotification()` 中添加 `.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)`。  
**文件**：`service/PomodoroTimerService.kt`

### 4. Android 13+ 通知权限
**方案**：`MainActivity` 启动时用 `registerForActivityResult(RequestPermission())` 动态申请 `POST_NOTIFICATIONS`。  
**文件**：`MainActivity.kt`

---

## 文件结构说明

```
PomodoroApp/
├── app/src/main/java/com/pomodoro/app/
│   ├── MainActivity.kt              # 入口，通知权限申请
│   ├── PomodoroApplication.kt       # Application 类
│   ├── data/
│   │   ├── model/
│   │   │   ├── PomodoroModels.kt    # 数据模型（TimerConfig 等）
│   │   │   └── PomodoroRecord.kt    # Room 实体
│   │   ├── preferences/
│   │   │   └── PomodoroPreferences.kt  # DataStore 封装
│   │   └── repository/
│   │       ├── PomodoroDatabase.kt  # Room 数据库
│   │       └── PomodoroRecordDao.kt # DAO
│   ├── service/
│   │   └── PomodoroTimerService.kt  # 前台 Service，计时核心
│   ├── ui/
│   │   ├── components/
│   │   │   ├── CircularProgressRing.kt
│   │   │   └── TimerControls.kt
│   │   ├── screens/
│   │   │   ├── TimerScreen.kt       # 主计时页（含 DigitSlot）
│   │   │   └── SettingsScreen.kt    # 设置页
│   │   └── theme/
│   │       ├── Color.kt / Theme.kt / Type.kt
│   └── viewmodel/
│       └── PomodoroViewModel.kt     # combine() 合并双 Flow
├── docs/
│   ├── CHANGELOG.md                 # 版本变更记录
│   ├── PRIVACY_POLICY.md            # 隐私政策（已填写联系信息）
│   └── DEV_CONTEXT.md               # 本文件
├── .github/workflows/
│   └── android-ci.yml               # GitHub Actions：push/PR 自动构建+单测
└── .gitignore
```

---

## 当前待办 / 下次迭代建议

### 功能完善
- [ ] README.md（项目介绍、截图、安装说明）
- [ ] 应用图标（当前为默认图标）
- [ ] 声音提醒（阶段结束播放音效）
- [ ] 统计页面（展示历史番茄数据，Room 数据已有）
- [ ] 深色模式适配优化
- [ ] 多语言（i18n）支持

### 工程化
- [ ] 配置 Release 签名（keystore），生成正式 APK
- [ ] GitHub Release 自动构建（Actions 触发 tag 打包）
- [ ] 应用市场上架（待注册主体后进行）

### 已知可改进点
- ViewModel 目前无依赖注入（DI），如项目复杂度提升建议引入 Hilt
- Room 历史记录暂未在 UI 中展示

---

## 启动迭代的准备

每次开始新一轮迭代，告诉 AI 助手：

> "打开 `docs/DEV_CONTEXT.md`，基于现有上下文继续开发番茄钟"

AI 助手读取此文件后即可准确还原项目状态，无需重新解释背景。

---

## 版本历史

| 版本 | 日期 | 说明 |
| --- | --- | --- |
| v1.0.0 | 2026-03-26 | 初始版本：基础计时、设置、通知、锁屏显示 |

---

*最后更新：2026-03-26*
