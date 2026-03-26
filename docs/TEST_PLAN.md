# 测试计划 - 番茄钟 Android App

> 版本：v1.0.0 | 最后更新：2026-03-25

---

## 一、测试策略

采用 **测试金字塔** 策略：

```
         ┌─────────────┐
         │  E2E / UI   │  ← 少量，覆盖主流程
         ├─────────────┤
         │ Integration │  ← 中量，覆盖组件协作
         ├─────────────┤
         │  Unit Tests │  ← 大量，覆盖核心逻辑
         └─────────────┘
```

| 测试类型 | 工具 | 覆盖目标 | 目标覆盖率 |
|---|---|---|---|
| 单元测试 | JUnit4 + MockK + Turbine | 数据模型、状态机逻辑、计算逻辑 | ≥ 80% |
| 集成测试 | Room in-memory DB | 数据库 CRUD | ≥ 90% |
| UI 测试 | Espresso + Compose UI Test | 主流程操作 | 主流程 100% |

---

## 二、单元测试用例

### 2.1 PomodoroConfig 测试（`PomodoroModelTest.kt`）

| 用例 ID | 测试方法 | 输入 | 预期结果 |
|---|---|---|---|
| UT-001 | `focusDurationSeconds` | focusDurationMinutes=25 | 返回 1500L |
| UT-002 | `shortBreakSeconds` | shortBreakMinutes=5 | 返回 300L |
| UT-003 | `longBreakSeconds` | longBreakMinutes=15 | 返回 900L |
| UT-004 | `phaseDurationSeconds` | FOCUS/SHORT_BREAK/LONG_BREAK | 分别返回对应秒数 |
| UT-005 | 默认值验证 | PomodoroConfig() | 25/5/15/4/true/true |

### 2.2 TimerUiState 测试（`PomodoroModelTest.kt`）

| 用例 ID | 测试方法 | 输入 | 预期结果 |
|---|---|---|---|
| UT-010 | `progress` 初始 | remaining=total=1500 | 0f |
| UT-011 | `progress` 半程 | remaining=750, total=1500 | 0.5f |
| UT-012 | `progress` 完成 | remaining=0, total=1500 | 1.0f |
| UT-013 | `progress` 防除零 | remaining=0, total=0 | 0f |
| UT-014 | `formattedTime` 标准 | remaining=1500 | "25:00" |
| UT-015 | `formattedTime` 补零 | remaining=65 | "01:05" |
| UT-016 | `formattedTime` 结束 | remaining=0 | "00:00" |

### 2.3 PomodoroPhase 测试（`PomodoroModelTest.kt`）

| 用例 ID | 测试方法 | 输入 | 预期结果 |
|---|---|---|---|
| UT-020 | `displayName` | FOCUS | "专注" |
| UT-021 | `displayName` | SHORT_BREAK | "短休息" |
| UT-022 | `displayName` | LONG_BREAK | "长休息" |

### 2.4 阶段切换逻辑测试（`TimerLogicTest.kt`）

| 用例 ID | 测试方法 | 当前阶段 | 已完成番茄数 | 预期下一阶段 |
|---|---|---|---|---|
| UT-030 | `nextPhase` | FOCUS | 1 | SHORT_BREAK |
| UT-031 | `nextPhase` | FOCUS | 2 | SHORT_BREAK |
| UT-032 | `nextPhase` | FOCUS | 3 | SHORT_BREAK |
| UT-033 | `nextPhase` | FOCUS | 4（整除） | LONG_BREAK |
| UT-034 | `nextPhase` | FOCUS | 8（整除） | LONG_BREAK |
| UT-035 | `nextPhase` | SHORT_BREAK | 任意 | FOCUS |
| UT-036 | `nextPhase` | LONG_BREAK | 任意 | FOCUS |

### 2.5 StateFlow 状态流测试（`TimerLogicTest.kt`）

| 用例 ID | 测试方法 | 操作 | 预期结果 |
|---|---|---|---|
| UT-040 | 初始状态 | 无操作 | IDLE + FOCUS |
| UT-041 | 专注完成事件 | 更新 phaseJustCompleted=FOCUS | flow 接收到事件 |
| UT-042 | 事件清除 | 设置 phaseJustCompleted=null | flow 返回 null |

---

## 三、集成测试用例

### 3.1 数据库测试（Room in-memory）

| 用例 ID | 场景 | 操作 | 预期结果 |
|---|---|---|---|
| IT-001 | 插入记录 | insert(record) | 无异常 |
| IT-002 | 今日统计 | getTodayCount(today) | 返回正确数量 |
| IT-003 | 历史查询 | getRecentRecords() | 返回最多 100 条 |
| IT-004 | 清空记录 | clearAll() + count | 返回 0 |
| IT-005 | 跨日统计隔离 | 插入不同日期记录 | 只返回当天数量 |

### 3.2 DataStore 设置持久化测试

| 用例 ID | 场景 | 操作 | 预期结果 |
|---|---|---|---|
| IT-010 | 保存配置 | saveConfig(config) | 下次读取值一致 |
| IT-011 | 默认值 | 未保存时读取 | 返回默认配置 |
| IT-012 | 异常恢复 | IO 异常 | 返回默认配置，不崩溃 |

---

## 四、UI / E2E 测试用例

### 4.1 主界面功能（Compose UI Test）

| 用例 ID | 场景 | 操作步骤 | 预期结果 |
|---|---|---|---|
| UI-001 | 初始界面 | 启动 App | 显示"25:00"，阶段标签"专注" |
| UI-002 | 开始计时 | 点击开始按钮 | 按钮变为暂停，时间开始倒数 |
| UI-003 | 暂停计时 | 计时中点击暂停 | 时间停止，显示"已暂停" |
| UI-004 | 继续计时 | 暂停后点击继续 | 时间继续倒数 |
| UI-005 | 重置计时 | 计时中点击重置 | 返回初始状态 |
| UI-006 | 跳过阶段 | 计时中点击跳过 | 进入下一阶段 |
| UI-007 | 今日统计 | 完成一个番茄 | 底部小圆点 +1 |
| UI-008 | 进度环 | 计时进行到一半 | 进度环显示约 50% |
| UI-009 | 阶段颜色切换 | 进入短休息 | 进度环变蓝色 |
| UI-010 | 阶段颜色切换 | 进入长休息 | 进度环变紫色 |

### 4.2 设置界面功能

| 用例 ID | 场景 | 操作步骤 | 预期结果 |
|---|---|---|---|
| UI-020 | 进入设置 | 点击右上角设置图标 | 跳转设置页 |
| UI-021 | 修改专注时长 | 拖动滑块到 30 | 显示"30 分钟" |
| UI-022 | 修改后保存 | 修改后点击返回 | 主界面显示新时长 |
| UI-023 | 关闭声音 | 关闭声音开关 | 开关状态变更并保存 |
| UI-024 | 关闭震动 | 关闭震动开关 | 开关状态变更并保存 |
| UI-025 | 设置边界值 | 专注时长设置为 1 | 不崩溃，正常显示 |
| UI-026 | 设置边界值 | 专注时长设置为 120 | 不崩溃，正常显示 |

### 4.3 后台保活测试（手动）

| 用例 ID | 场景 | 操作步骤 | 预期结果 |
|---|---|---|---|
| UI-030 | 锁屏不中断 | 开始计时后锁屏 1 分钟再解锁 | 计时正常继续 |
| UI-031 | 通知栏显示 | 开始计时 | 通知栏出现番茄钟进度 |
| UI-032 | 通知点击跳转 | 点击通知 | 回到 App 主界面 |
| UI-033 | 切换到其他 App | 开始计时后切后台 | 计时不中断 |
| UI-034 | 阶段完成提醒 | 等待专注阶段结束 | 震动/声音提醒触发 |

---

## 五、运行单元测试

```bash
# 运行所有 JVM 单元测试
./gradlew test

# 运行指定测试类
./gradlew test --tests "com.pomodoro.app.PomodoroConfigTest"
./gradlew test --tests "com.pomodoro.app.PhaseTransitionTest"

# 生成测试报告（HTML）
./gradlew test
# 报告路径：app/build/reports/tests/testDebugUnitTest/index.html
```

## 六、运行 UI 测试

```bash
# 需要连接 Android 设备或启动模拟器
./gradlew connectedAndroidTest

# 报告路径：app/build/reports/androidTests/connected/index.html
```

---

## 七、持续集成（CI）建议

建议在 GitHub Actions / Jenkins 中配置：

```yaml
# .github/workflows/android.yml 示例
name: Android CI
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
      - name: Run Unit Tests
        run: ./gradlew test
      - name: Upload Test Report
        uses: actions/upload-artifact@v4
        with:
          name: test-report
          path: app/build/reports/tests/
```

---

## 八、已知限制与待补充测试

| 项目 | 说明 |
|---|---|
| Service 计时精度 | 依赖 `delay(1000)`，实际误差 ±50ms，可接受 |
| 音效测试 | 需真机测试，模拟器音频可能不稳定 |
| 华为后台杀进程 | 需手动测试 EMUI/HarmonyOS 省电策略兼容性 |
| ViewModel 测试 | 需 Application 上下文，建议后续使用 Hilt 注入改造 |
