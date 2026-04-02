package com.flowcycle.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flowcycle.app.R
import com.flowcycle.app.data.model.Scene
import com.flowcycle.app.ui.theme.PomodoroRed
import com.flowcycle.app.viewmodel.FlowCycleViewModel
import java.util.UUID

/**
 * 场景管理页面
 * - 展示所有场景（预设 + 用户自定义）
 * - 点击设为默认场景
 * - FAB 新建场景
 * - 自定义场景可编辑和删除
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenesScreen(
    viewModel: FlowCycleViewModel,
    onEditorVisibilityChange: (Boolean) -> Unit = {}
) {
    val scenes by viewModel.scenes.collectAsStateWithLifecycle()
    val defaultSceneId by viewModel.defaultSceneId.collectAsStateWithLifecycle()

    // 编辑/新建场景页面状态
    var editingScene by remember { mutableStateOf<Scene?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    // 删除确认弹窗
    var deletingScene by remember { mutableStateOf<Scene?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        // 场景列表主页面（不再套 Scaffold，FAB 用 Box 绝对定位）
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                Text(
                    text = "SCENES",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
            }

            // 预设场景组
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column {
                        val presetScenes = scenes.filter { it.isPreset }
                        presetScenes.forEachIndexed { index, scene ->
                            SceneRow(
                                scene = scene,
                                isDefault = scene.id == defaultSceneId,
                                onSetDefault = { viewModel.setDefaultScene(scene.id) },
                                onEdit = null,
                                onDelete = null
                            )
                            if (index < presetScenes.size - 1) {
                                HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                            }
                        }
                    }
                }
            }

            // 自定义场景组
            val customScenes = scenes.filter { !it.isPreset }
            if (customScenes.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "自定义",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column {
                            customScenes.forEachIndexed { index, scene ->
                                SceneRow(
                                    scene = scene,
                                    isDefault = scene.id == defaultSceneId,
                                    onSetDefault = { viewModel.setDefaultScene(scene.id) },
                                    onEdit = {
                                        editingScene = scene
                                        showEditor = true
                                    },
                                    onDelete = { deletingScene = scene }
                                )
                                if (index < customScenes.size - 1) {
                                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        // FAB：新建场景（右下角绝对定位）
        if (!showEditor) {
            FloatingActionButton(
                onClick = {
                    editingScene = null
                    showEditor = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp),
                containerColor = PomodoroRed,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "新建场景")
            }
        }

        // 删除确认弹窗
        if (deletingScene != null) {
            AlertDialog(
                onDismissRequest = { deletingScene = null },
                title = { Text("删除场景") },
                text = { Text("确认删除「${deletingScene!!.name}」吗？此操作不可撤销。") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteScene(deletingScene!!.id)
                        deletingScene = null
                    }) {
                        Text("删除", color = PomodoroRed)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deletingScene = null }) {
                        Text("取消")
                    }
                }
            )
        }

        // 编辑场景全屏页面（从右侧滑入 + 淡入）
        AnimatedVisibility(
            visible = showEditor,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300)),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(200))
        ) {
            LaunchedEffect(showEditor) { onEditorVisibilityChange(showEditor) }
            SceneEditorPage(
                scene = editingScene,
                onBack = {
                    showEditor = false
                    editingScene = null
                    onEditorVisibilityChange(false)
                },
                onSave = { scene ->
                    viewModel.saveScene(scene)
                    showEditor = false
                    editingScene = null
                    onEditorVisibilityChange(false)
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 场景行
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SceneRow(
    scene: Scene,
    isDefault: Boolean,
    onSetDefault: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSetDefault() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = scene.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isDefault) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isDefault) PomodoroRed
                    else MaterialTheme.colorScheme.onSurface
                )
                if (isDefault) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = PomodoroRed.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "默认",
                            style = MaterialTheme.typography.labelSmall,
                            color = PomodoroRed,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text(
                text = buildString {
                    append("专注 ${scene.focusDurationMinutes}分钟")
                    append(" · 短休 ${scene.shortBreakMinutes}分钟")
                    append(" · 长休 ${scene.longBreakMinutes}分钟")
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }

        if (onEdit != null || onDelete != null) {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more),
                        contentDescription = "更多",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (!isDefault) {
                        DropdownMenuItem(
                            text = { Text("设为默认") },
                            onClick = {
                                showMenu = false
                                onSetDefault()
                            }
                        )
                    }
                    if (onEdit != null) {
                        DropdownMenuItem(
                            text = { Text("编辑") },
                            onClick = {
                                showMenu = false
                                onEdit()
                            }
                        )
                    }
                    if (onDelete != null) {
                        DropdownMenuItem(
                            text = { Text("删除", color = PomodoroRed) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 场景编辑器 — 全屏独立页面（一屏显示，无底部Tab）
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SceneEditorPage(
    scene: Scene?,     // null = 新建，非 null = 编辑
    onBack: () -> Unit,
    onSave: (Scene) -> Unit
) {
    val isNew = scene == null

    // 拦截系统返回手势，行为与返回按钮一致
    BackHandler { onBack() }

    var name by remember { mutableStateOf(scene?.name ?: "我的场景") }
    var focusDuration     by remember { mutableIntStateOf(scene?.focusDurationMinutes ?: 25) }
    var shortBreak        by remember { mutableIntStateOf(scene?.shortBreakMinutes     ?: 5)  }
    var longBreak         by remember { mutableIntStateOf(scene?.longBreakMinutes      ?: 15) }
    var longBreakInterval by remember { mutableIntStateOf(scene?.longBreakInterval     ?: 4)  }
    var soundEnabled      by remember { mutableStateOf(scene?.soundEnabled      ?: true) }
    var vibrationEnabled  by remember { mutableStateOf(scene?.vibrationEnabled  ?: true) }

    val canSave = name.isNotBlank()

    // 完全覆盖父层（隐藏底部 Tab 栏）
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        // ── 顶部导航栏 ────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = PomodoroRed
                )
            }
            Text(
                text = if (isNew) "新建场景" else "编辑场景",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))

        // ── 内容区（自然高度排列，超出时可滚动）────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ── 场景名称 ──────────────────────────────────
            EditorCard {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = {
                        Text(
                            "输入场景名称",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PomodoroRed,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            // ── 专注节律卡片 ──────────────────────────────
            EditorCard {
                SectionLabel(title = "专注节律")
                SnapSliderRow(
                    label = "专注时长",
                    value = focusDuration,
                    range = 25..60,
                    snapPoints = listOf(25, 30, 35, 40, 45, 50, 55, 60),
                    unit = "分钟",
                    onValueChange = { focusDuration = it }
                )
                SliderDivider()
                SnapSliderRow(
                    label = "短休时长",
                    value = shortBreak,
                    range = 1..15,
                    snapPoints = listOf(5, 10, 15),
                    unit = "分钟",
                    onValueChange = { shortBreak = it }
                )
            }

            // ── 长休设置卡片 ──────────────────────────────
            EditorCard {
                SectionLabel(title = "长休设置")
                SnapSliderRow(
                    label = "长休时长",
                    value = longBreak,
                    range = 10..30,
                    snapPoints = listOf(10, 15, 20, 25, 30),
                    unit = "分钟",
                    onValueChange = { longBreak = it }
                )
                SliderDivider()
                SnapSliderRow(
                    label = "长休间隔",
                    value = longBreakInterval,
                    range = 2..6,
                    snapPoints = listOf(2, 3, 4, 5, 6),
                    unit = "个",
                    onValueChange = { longBreakInterval = it }
                )
            }

            // ── 提醒方式卡片 ──────────────────────────────
            EditorCard {
                SectionLabel(title = "提醒方式")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("声音提醒", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = { soundEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PomodoroRed
                        )
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 2.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("震动提醒", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = vibrationEnabled,
                        onCheckedChange = { vibrationEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PomodoroRed
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // ── 底部保存按钮 ──────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
        ) {
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
            Button(
                onClick = {
                    if (canSave) {
                        onSave(
                            Scene(
                                id = scene?.id ?: UUID.randomUUID().toString(),
                                name = name.trim(),
                                focusDurationMinutes = focusDuration,
                                shortBreakMinutes    = shortBreak,
                                longBreakMinutes     = longBreak,
                                longBreakInterval    = longBreakInterval,
                                soundEnabled         = soundEnabled,
                                vibrationEnabled     = vibrationEnabled,
                                isPreset             = false
                            )
                        )
                    }
                },
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(13.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PomodoroRed,
                    disabledContainerColor = PomodoroRed.copy(alpha = 0.38f)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
            ) {
                Text(
                    text = "保存",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 公共子组件
// ─────────────────────────────────────────────────────────────────────────────

/** 编辑器卡片容器 */
@Composable
private fun EditorCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
            content = content
        )
    }
}

/** 卡片内分组标题（紧凑版） */
@Composable
private fun SectionLabel(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

/** 卡片内分割线 */
@Composable
private fun SliderDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 6.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 带刻度时间戳的滑块行
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 滑块行 - 刻度精确对齐方案
 *
 * ─── 核心思路 ─────────────────────────────────────────────────────────────
 * Material3 Slider 有内部 HorizontalPadding，导致 Canvas(fillMaxWidth) 与
 * Slider 轨道坐标系不同，越两端偏差越大。
 *
 * 解法：用 onGloballyPositioned 直接测量 Slider 自身的像素宽度，
 * Canvas 也加相同的 fillMaxWidth + onGloballyPositioned 获取自身宽度，
 * 算出 Slider 相对父容器的偏移 offset = (canvasWidth - sliderWidth) / 2
 * 然后 Canvas 绘制时 X 起点 = offset + thumbRadius，完全对齐。
 *
 * 或者更简单：把 Canvas 绝对叠在 Slider 下方，和 Slider 等宽，
 * 用 onGloballyPositioned 同步拿到 Slider 宽度，Canvas 用同样的宽度。
 * ─────────────────────────────────────────────────────────────────────────
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnapSliderRow(
    label: String,
    value: Int,
    range: IntRange,
    snapPoints: List<Int>,
    unit: String,
    onValueChange: (Int) -> Unit
) {
    val density = androidx.compose.ui.platform.LocalDensity.current

    val activeColor   = PomodoroRed
    val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    val trackActive   = PomodoroRed
    val trackInactive = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
    val tickLineInactive = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)

    val activeColorArgb   = activeColor.toArgb()
    val inactiveColorArgb = inactiveColor.toArgb()
    val trackActiveArgb   = trackActive.toArgb()
    val trackInactiveArgb = trackInactive.toArgb()
    val tickInactiveArgb  = tickLineInactive.toArgb()

    val rangeSpan = (range.last - range.first).toFloat()

    Column(modifier = Modifier.fillMaxWidth()) {
        // ── 标签行 ────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = PomodoroRed.copy(alpha = 0.1f)
            ) {
                Text(
                    text = "$value $unit",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = PomodoroRed,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        // ── Slider：刻度直接嵌入自定义 track，坐标系完全相同 ──
        Slider(
            value = value.toFloat(),
            onValueChange = { raw ->
                onValueChange(raw.toInt().coerceIn(range.first, range.last))
            },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                // 加高以容纳轨道 + 刻度线 + 刻度数字
                .height(52.dp),
            colors = SliderDefaults.colors(
                thumbColor = PomodoroRed,
                // track 颜色在自定义 track 里手动画，这里设透明避免重叠
                activeTrackColor   = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
                activeTickColor    = Color.Transparent,
                inactiveTickColor  = Color.Transparent
            ),
            track = { sliderState ->
                // sliderState.value / valueRange / coercedValue 都可用
                // Canvas 的 size 就是 track 区域大小
                // Material3 内部：track 两端已经去掉了 thumbRadius，直接用 size.width 就是轨道可用宽度
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    val trackHeight = with(density) { 4.dp.toPx() }
                    val tickAreaTop = size.height / 2f + trackHeight / 2f + with(density) { 4.dp.toPx() }
                    val trackTop    = size.height / 2f - trackHeight / 2f
                    val trackW      = size.width
                    val currentFrac = (value - range.first) / rangeSpan

                    // 画轨道（未激活部分）
                    drawRoundRect(
                        color = trackInactive,
                        topLeft = Offset(0f, trackTop),
                        size = androidx.compose.ui.geometry.Size(trackW, trackHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackHeight / 2f)
                    )
                    // 画轨道（激活部分）
                    val activeW = trackW * currentFrac
                    if (activeW > 0f) {
                        drawRoundRect(
                            color = trackActive,
                            topLeft = Offset(0f, trackTop),
                            size = androidx.compose.ui.geometry.Size(activeW, trackHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackHeight / 2f)
                        )
                    }

                    // 画刻度线 + 刻度数字（坐标与轨道完全相同）
                    val tickPaint = android.graphics.Paint().apply {
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize  = with(density) { 9.sp.toPx() }
                        isAntiAlias = true
                    }

                    snapPoints.forEach { tick ->
                        val fraction = (tick - range.first) / rangeSpan
                        val cx       = fraction * trackW
                        val isActive = value == tick

                        drawLine(
                            color       = if (isActive) activeColor else tickLineInactive,
                            start       = Offset(cx, tickAreaTop),
                            end         = Offset(cx, tickAreaTop + with(density) { 5.dp.toPx() }),
                            strokeWidth = with(density) { 1.5.dp.toPx() }
                        )

                        tickPaint.color        = if (isActive) activeColorArgb else inactiveColorArgb
                        tickPaint.isFakeBoldText = isActive
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawText(
                                "$tick",
                                cx,
                                tickAreaTop + with(density) { 5.dp.toPx() } + tickPaint.textSize,
                                tickPaint
                            )
                        }
                    }
                }
            }
        )

        // 刻度区点击（跳到最近 snap 点）
        Spacer(modifier = Modifier.height(2.dp))
    }
}
