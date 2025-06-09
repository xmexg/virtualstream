package org.looom.virtualstream

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.core.content.edit
import com.google.gson.Gson

/**
 * 存放全局变量和配置
 */
object VARIABLE {
    // 记录 haze 模糊区域
    lateinit var HAZE_STATE: HazeState

    // 串流模式
    enum class STREAM_MODE(val label: String, val config: String){
        NONE("原始流", "stream_none"),
        LOCAL("本地串流", "stream_local"),
        NET("远程串流", "stream_net");

        override fun toString(): String = label
    }

    // 串流摄像头
    enum class STREAM_CAMERA(val label: String, val config: String){
        NONE("停用", "camera_none"),
        FRONT("前置摄像头", "camera_front"),
        REAR("后置摄像头", "camera_rear"),
        ALL( "全部摄像头", "camera_all");

        override fun toString(): String = label
    }
}

/**
 * xposed 配置管理
 *
 * 1. 按json格式存储当前配置
 * 2. 当配置变更时，按hashmap格式发送广播通知其他模块
 *
 * 备注：
 * Config类变量必须全部是String类型
 * 我找不到一种完美的方法进行两app数据交互，读写文件、广播、socket均有各种各样限制，现回退到 https://github.com/Xposed-Modules-Repo/com.example.vcam 文件交互方案
 */
object ConfigManager {

    private const val PREF_NAME = "virtualstream_config" // SharedPreferences 名称
    private const val KEY_CONFIG_JSON = "virtualstream_config" // 文件名

    // 广播动作常量（确保其他模块能订阅）
    const val ACTION_CONFIG_CHANGED = "org.looom.virtualstream.CONFIG_CHANGED" // 配置变更广播

    data class Config(
        var STREAM_MODE: String = VARIABLE.STREAM_MODE.NONE.config,
        var STREAM_CAMERA: String = VARIABLE.STREAM_CAMERA.NONE.config
    )

    private lateinit var appContext: Context
    private lateinit var prefs: SharedPreferences

    private val _configFlow = MutableStateFlow(Config()) // 一个可变的、持有状态的 Flow，可以通过 .value 来改变它的值
    val configFlow: StateFlow<Config> = _configFlow  // 一个只读的、不可更改值的 Flow，它的值只能被观察，不能直接改
    private val gson = Gson()


    fun init(context: Context) {
        appContext = context.applicationContext
        prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE) // 即便写成 MODE_WORLD_READABLE 当程序切到后台时其他应用程序也是不可读的
        val json = prefs.getString(KEY_CONFIG_JSON, null)
        val config = if (json != null) {
            try {
                gson.fromJson(json, Config::class.java)
            } catch (e: Exception) {
                Config()
            }
        } else Config()
        _configFlow.value = config
    }

    fun updateConfig(newConfig: Config) {
        _configFlow.value = newConfig
        val json = gson.toJson(newConfig)
        prefs.edit { putString(KEY_CONFIG_JSON, json) } // 保存到 SharedPreferences
        sendBroadcast(newConfig)
    }

    fun getCurrentConfig(): Config = _configFlow.value

    private fun config2hashmap(config: Config): HashMap<String, String> {
        val hashMap = HashMap<String, String>()
        Config::class.java.declaredFields.forEach { // 通过反射遍历 Config 类的所有字段并转换为 HashMap
            it.isAccessible = true // 允许访问私有字段
            try {
                val value = it.get(config) as String
                hashMap[it.name] = value
            } catch (e: Exception) {
                // 忽略异常，继续处理下一个字段
            }
        }
        return hashMap
    }

    private fun sendBroadcast(config: Config) {
        val intent = Intent(ACTION_CONFIG_CHANGED).apply {
            `package` = null // 必须设为 null 以广播到所有 app
            putExtra(PREF_NAME, config2hashmap(config)) // 传递配置的 HashMap
        }
        appContext.sendBroadcast(intent)
    }
}
