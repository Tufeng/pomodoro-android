# 技术文档 - 番茄钟 Android App

> 版本：v1.0.0 | 最后更新：2026-03-25

---

## 一、项目概述

番茄钟 App 是一款极简风格的时间管理工具，采用 Apple 设计语言，运行于 Android 平台。
目标市场为中国大陆安卓应用市场（小米、华为等）。

---

## 二、技术栈

| 类别 | 技术选型 | 版本 |
|---|---|---|
| 开发语言 | Kotlin | 2.0.21 |
| UI 框架 | Jetpack Compose | BOM 2024.09.03 |
| 架构模式 | MVVM + Clean Architecture | - |
| 导航 | Navigation Compose | 2.8.2 |
| 持久化（设置） | DataStore Preferences | 1.1.1 |
| 持久化（历史） | Room Database | 2.6.1 |
| 后台计时 | ForegroundService | Android 系统 |
| 协程 | Kotlinx Coroutines | 1.8.1 |
| 单元测试 | JUnit4 + MockK + Turbine | - |
| 最低 API | Android 8.0 (API 26) | - |
| 目标 API | Android 15 (API 35) | - |

---

## 三、项目结构

```
PomodoroApp/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/pomodoro/app/
│   │   │   │   ├── MainActivity.kt          # 入口 Activity
│   │   │   │   ├── PomodoroApplication.kt   # Application 类
│   │   │   │   ├── data/
│   │   │   │   │   ├── model/
│   │   │   │   │   │   ├── PomodoroModels.kt    # 核心数据模型（枚举、配置、UI状态）
│   │   │   │   │   │   └── PomodoroRecord.kt    # Room 历史记录实体
│   │   │   │   │   ├── repository/
│   │   │   │   │   │   ├── PomodoroDatabase.kt  # Room 数据库
│   │   │   │   │   │   └── PomodoroRecordDao.kt # DAO 接口
│   │   │   │   │   └── preferences/
│   │   │   │   │       └── PomodoroPreferences.kt # DataStore 设置持久化
│   │   │   │   ├── service/
│   │   │   │   │   └── PomodoroTimerService.kt  # 前台计时服务（核心）
│   │   │   │   ├── viewmodel/
│   │   │   │   │   └── PomodoroViewModel.kt     # 主 ViewModel
│   │   │   │   └── ui/
│   │   │   │       ├── screens/
│   │   │   │       │   ├── TimerScreen.kt       # 主计时界面
│   │   │   │       │   └── SettingsScreen.kt    # 设置界面
│   │   │   │       ├── components/
│   │   │   │       │   ├── CircularProgressRing.kt # 圆形进度环
│   │   │   │       │   └── TimerControls.kt     # 控制按钮组
│   │   │   │       └── theme/
│   │   │   │           ├── Color.kt             # 色彩系统
│   │   │   │           ├── Type.kt              # 字体系统
│   │   │   │           └── Theme.kt             # Material3 主题
│   │   │   ├── res/
│   │   │   │   ├── drawable/                    # 矢量图标
│   │   │   │   ├── values/strings.xml           # 字符串资源
│   │   │   │   └── raw/timer_complete.mp3       # 提示音（需自行添加）
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                                # JVM 单元测试
│   │   └── androidTest/                         # Instrumented 测试
│   └── build.gradle.kts
├── gradle/libs.versions.toml                    # 统一依赖版本管理
└── docs/
    ├── TECHNICAL.md                             # 本文档
    └── TEST_PLAN.md                             # 测试计划
```

---

## 四、架构设计

### 4.1 整体架构（MVVM）

```
UI Layer（Compose Screens）
    ↕ StateFlow / Events
ViewModel Layer（PomodoroViewModel）
    ↕ StateFlow（跨进程共享）
Service Layer（PomodoroTimerService）
    ↕
Data Layer（DataStore / Room）
```

### 4.2 核心状态流转

```
PomodoroTimerService.timerState（MutableStateFlow）
    ← Service 写入（每秒更新）
    → ViewModel 订阅，转换为 TimerUiState
    → Compose UI 响应式渲染
```

### 4.3 计时状态机

```
IDLE ──(start)──→ RUNNING ──(pause)──→ PAUSED
  ↑                  |                    |
  └──────(reset)─────┘       (resume)────┘
                  ↓
              时间结束
                  ↓
            triggerAlert()
                  ↓
            moveToNextPhase()
      FOCUS → SHORT_BREAK / LONG_BREAK
      SHORT_BREAK / LONG_BREAK → FOCUS
```

### 4.4 阶段切换规则

- 每完成 `longBreakInterval`（默认 4）个专注后，触发一次长休
- 判断方式：`completedPomodoros % config.longBreakInterval == 0`
- 短休/长休结束后均回到专注阶段

---

## 五、关键技术实现

### 5.1 后台保活（ForegroundService）

- 使用 `ForegroundService` + `FOREGROUND_SERVICE_TYPE_SPECIAL_USE`
- 通知栏持续显示计时进度，通知类型为 `IMPORTANCE_LOW`（静音）
- 锁屏不中断，息屏不杀进程

**权限要求：**
```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.VIBRATE" />
```

### 5.2 跨进程状态共享

Service 通过 `companion object` 中的 `MutableStateFlow` 向外暴露状态，
ViewModel 直接订阅，无需 Binder 通信，简洁高效。

```kotlin
// Service 中
companion object {
    val timerState: MutableStateFlow<ServiceTimerState> = MutableStateFlow(ServiceTimerState())
}

// ViewModel 中
val timerUiState = PomodoroTimerService.timerState
    .map { it.toUiState() }
    .stateIn(viewModelScope, ...)
```

### 5.3 提醒实现

- **震动**：使用 `VibrationEffect.createWaveform`（Android 8+）
- **声音**：`MediaPlayer.create(context, R.raw.timer_complete)`
- 音效文件需放置于 `res/raw/timer_complete.mp3`（或 .ogg）

### 5.4 数据持久化

| 数据类型 | 存储方案 | 说明 |
|---|---|---|
| 用户设置（时长、开关等） | DataStore Preferences | 类型安全，支持 Flow |
| 历史记录 | Room Database | 支持按日期查询统计 |

---

## 六、构建与运行

### 6.1 环境要求

- Android Studio Ladybug（2024.2.1）或更高
- JDK 17
- Gradle 8.x

### 6.2 运行步骤

```bash
# 克隆项目
cd PomodoroApp

# 用 Android Studio 打开，或直接 Gradle 构建
./gradlew assembleDebug

# 安装到设备
./gradlew installDebug
```

### 6.3 发布构建（上架用）

```bash
./gradlew assembleRelease
```

需在 `app/build.gradle.kts` 配置签名：
```kotlin
signingConfigs {
    create("release") {
        storeFile = file("../keystore/release.jks")
        storePassword = "..."
        keyAlias = "..."
        keyPassword = "..."
    }
}
```

---

## 七、注意事项

### 7.1 音效文件

项目中引用了 `R.raw.timer_complete`，需在 `app/src/main/res/raw/` 目录下放置音效文件（mp3/ogg 均可）。
若未放置，代码会捕获异常静默处理，不影响其他功能。

### 7.2 国内应用市场适配

- **华为**：需在华为开发者中心申请 `FOREGROUND_SERVICE_SPECIAL_USE` 权限白名单
- **小米**：MIUI 自启动管理中需引导用户开启自启动权限
- **建议**：首次启动时检测通知权限并引导用户开启

### 7.3 Android 14+ 前台服务限制

Android 14 起，前台服务需声明 `foregroundServiceType`，本项目已使用 `specialUse` 类型。

---

## 八、后续迭代建议

| 版本 | 功能 |
|---|---|
| v1.1 | 多套方案保存与切换（学习/工作/自定义） |
| v1.1 | 通知权限动态申请引导 |
| v1.2 | 历史统计图表（周/月趋势） |
| v1.2 | 任务绑定（每个番茄关联任务名） |
| v2.0 | 桌面 Widget |
| v2.0 | 云同步（可选） |
