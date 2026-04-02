package com.flowcycle.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.flowcycle.app.data.model.PresetScenes
import com.flowcycle.app.data.model.Scene
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "flowcycle_settings")

/**
 * 场景和全局配置持久化管理（DataStore）
 * - scenes_json: JSON 字符串，存储用户自定义场景列表（预设场景不存储，运行时合并）
 * - default_scene_id: 当前默认场景的 id
 */
class FlowCyclePreferences(private val context: Context) {

    companion object {
        val SCENES_JSON = stringPreferencesKey("scenes_json")
        val DEFAULT_SCENE_ID = stringPreferencesKey("default_scene_id")
    }

    // ─── 场景列表流（预设 + 用户自定义） ─────────────────────────────────
    val scenesFlow: Flow<List<Scene>> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            val customJson = prefs[SCENES_JSON] ?: "[]"
            val customScenes = parseScenes(customJson)
            PresetScenes.ALL + customScenes
        }

    // ─── 默认场景 ID 流 ───────────────────────────────────────────────
    val defaultSceneIdFlow: Flow<String> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            prefs[DEFAULT_SCENE_ID] ?: PresetScenes.CLASSIC.id
        }

    // ─── 保存用户自定义场景 ────────────────────────────────────────────
    suspend fun saveCustomScenes(scenes: List<Scene>) {
        val nonPreset = scenes.filter { !it.isPreset }
        val json = scenesToJson(nonPreset)
        context.dataStore.edit { prefs ->
            prefs[SCENES_JSON] = json
        }
    }

    // ─── 设置默认场景 ──────────────────────────────────────────────────
    suspend fun setDefaultSceneId(sceneId: String) {
        context.dataStore.edit { prefs ->
            prefs[DEFAULT_SCENE_ID] = sceneId
        }
    }

    // ─── JSON 序列化 ───────────────────────────────────────────────────

    private fun parseScenes(json: String): List<Scene> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                Scene(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    focusDurationMinutes = obj.getInt("focusDuration"),
                    shortBreakMinutes = obj.getInt("shortBreak"),
                    longBreakMinutes = obj.getInt("longBreak"),
                    longBreakInterval = obj.getInt("longBreakInterval"),
                    soundEnabled = obj.getBoolean("soundEnabled"),
                    vibrationEnabled = obj.getBoolean("vibrationEnabled"),
                    isPreset = false
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun scenesToJson(scenes: List<Scene>): String {
        val arr = JSONArray()
        scenes.forEach { scene ->
            val obj = JSONObject().apply {
                put("id", scene.id)
                put("name", scene.name)
                put("focusDuration", scene.focusDurationMinutes)
                put("shortBreak", scene.shortBreakMinutes)
                put("longBreak", scene.longBreakMinutes)
                put("longBreakInterval", scene.longBreakInterval)
                put("soundEnabled", scene.soundEnabled)
                put("vibrationEnabled", scene.vibrationEnabled)
            }
            arr.put(obj)
        }
        return arr.toString()
    }
}
