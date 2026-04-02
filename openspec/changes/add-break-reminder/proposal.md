# Proposal: 休息提醒增强

**Change ID**: add-break-reminder  
**状态**: 待审批

## Why（为什么做）

工作段结束时，用户需要及时感知「该休息了」：
- 当前仅有通知，无声音/震动提醒
- 锁屏或专注状态下，单一通知容易被忽视

## What Changes（做什么）

1. 工作段结束时，触发多通道提醒：
   - 🔔 声音提醒（系统默认提示音，可在设置中关闭）
   - 📳 震动提醒（可在设置中关闭）
   - 📱 通知（已有，保留）
2. 休息段结束时，同样触发提醒（提示开始下一轮工作）
3. 设置页增加「提醒方式」配置项：
   - 声音开关（默认开）
   - 震动开关（默认开）

## Impact（影响）

- **权限**：已有 VIBRATE 权限，确认 AndroidManifest 已声明
- **ForegroundService**：在现有 TimerService 的阶段切换逻辑中加入提醒调用
- **设置**：DataStore 新增 soundEnabled、vibrationEnabled 两个 key
