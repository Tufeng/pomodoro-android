# Spec Delta: 多场景管理系统

## ADDED Requirements

### Requirement: 场景数据模型
系统 SHALL 支持场景（Scene）实体，包含以下字段：id、name、workDuration（分钟）、shortBreakDuration（分钟）、longBreakDuration（分钟）、cycleCount（循环次数）、isDefault（是否默认）、isPreset（是否预设，预设不可删除）。

#### Scenario: 默认场景加载
GIVEN App 首次启动
WHEN 数据库初始化完成
THEN 系统自动插入3个预设场景：经典番茄(25/5/15/4)、深度学习(45/15/30/2)、健身训练(45/10/20/3)
AND 「经典番茄」被设为默认场景

### Requirement: 场景创建
WHEN 用户在场景列表点击「+新建」,
系统 SHALL 打开场景编辑页，允许用户输入名称和各项时长，保存后场景出现在列表中。

#### Scenario: 创建新场景
GIVEN 用户在场景 Tab
WHEN 点击「+」按钮，填写名称「我的节律」，设置工作时长 40 分钟
AND 点击保存
THEN 场景列表新增「我的节律」条目

#### Scenario: 名称为空不可保存
GIVEN 场景编辑页
WHEN 用户未填写名称直接点击保存
THEN 显示错误提示「请输入场景名称」，不保存

### Requirement: 场景删除
WHEN 用户对自定义场景执行删除操作,
系统 SHALL 弹出确认对话框，确认后删除该场景。
系统 SHALL NOT 允许删除预设场景。

#### Scenario: 删除自定义场景
GIVEN 场景列表中存在自定义场景「我的节律」
WHEN 用户长按并选择删除
THEN 弹出确认对话框
AND 确认后该场景从列表移除

#### Scenario: 预设场景不可删除
GIVEN 场景列表
WHEN 用户长按预设场景
THEN 删除选项不可用（置灰或不显示）

### Requirement: 默认场景设置
WHEN 用户将某场景设为默认,
系统 SHALL 将该场景标记为默认，同时取消其他场景的默认标记。

### Requirement: 计时中切换场景
WHEN 计时进行中用户点击场景名称触发切换,
系统 SHALL 弹出确认对话框提示「切换场景将重置当前计时，是否继续？」。
WHEN 用户确认,
系统 SHALL 停止当前计时，加载新场景配置，重置计时器为新场景的工作时长。

#### Scenario: 计时中切换场景-确认
GIVEN 计时进行中，当前场景为「经典番茄」
WHEN 用户点击场景名，选择「深度学习」
THEN 弹出确认对话框
AND 用户点击「确认切换」
THEN 计时器重置为 45:00，场景名更新为「深度学习」

#### Scenario: 计时中切换场景-取消
GIVEN 计时进行中
WHEN 用户在确认对话框点击「取消」
THEN 计时继续，场景不变

### Requirement: 计时页显示场景名
系统 SHALL 在计时页计时器下方显示当前场景名称。
WHEN 用户点击场景名称区域,
系统 SHALL 弹出场景选择列表。

## MODIFIED Requirements

### Requirement: 计时配置来源
（原：从全局 DataStore 读取工作时长等配置）
现在系统 SHALL 从当前激活的 Scene 对象读取工作时长、休息时长、循环次数配置。
系统 SHALL 在 App 升级时将旧版全局配置自动迁移为名为「我的默认场景」的自定义场景并设为默认。
