package org.looom.virtualstream

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.rememberHazeState
import org.looom.virtualstream.VARIABLE.HAZE_STATE
import org.looom.virtualstream.pages.AboutPage
import org.looom.virtualstream.pages.HomePage
import org.looom.virtualstream.pages.LocalStreamPage
import org.looom.virtualstream.pages.NetStreamPage
import org.looom.virtualstream.ui.theme.Box_Main_Background
import org.looom.virtualstream.ui.theme.VirtualStreamTheme



/**
 * xposed json 配置模板
 *
 * 不要使用 SharedPreferences 不同设备当程序后台时可能出现配置不可读
 * 也不要试图文件写公共目录，安卓13+只能通过 MediaStore/SAF 读写公共目录
 */
object xposed_config {
    var STREAM_MODE = VARIABLE.STREAM_MODE.NONE.config
    var STREAM_CAMERA = VARIABLE.STREAM_CAMERA.NONE.config
}

/**
 * xposed json 配置模板的文件保存
 */

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        // 让内容绘制到系统栏区域
//        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            VirtualStreamTheme {
                VirtualStreamApp()
            }
        }
    }
}

/**
 * 自适应导航栏应用程序的入口点。
 * https://developer.android.com/develop/ui/compose/layouts/adaptive/build-adaptive-navigation?hl=zh-cn
 */
@PreviewScreenSizes
@Composable
fun VirtualStreamApp() {
    var currentDestination by rememberSaveable { // 状态存储
        mutableStateOf(AppDestinations.HOME) // 监测状态变化
    }

    // 记录 haze 模糊区域
    HAZE_STATE = rememberHazeState()

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (currentDestination) {
                AppDestinations.HOME -> HomePage(Box_Main_Background(innerPadding))
                AppDestinations.PANEL_LOCAL -> LocalStreamPage(Box_Main_Background(innerPadding))
                AppDestinations.PANEL_NET -> NetStreamPage(Box_Main_Background(innerPadding))
                AppDestinations.ABOUT -> AboutPage(Box_Main_Background(innerPadding))
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("首页", Icons.Default.Home),
    PANEL_LOCAL("本地串流", Icons.Default.Settings),
    PANEL_NET("远程串流", Icons.Default.Settings),
    ABOUT("关于", Icons.Default.Favorite),
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    VirtualStreamTheme {
        Greeting("Android")
    }
}