package org.looom.virtualstream

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
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
import dev.chrisbanes.haze.rememberHazeState
import org.looom.virtualstream.VARIABLE.HAZE_STATE
import org.looom.virtualstream.pages.AboutPage
import org.looom.virtualstream.pages.HomePage
import org.looom.virtualstream.pages.LocalStreamPage
import org.looom.virtualstream.pages.NetStreamPage
import org.looom.virtualstream.ui.theme.Box_Main_Background
import org.looom.virtualstream.ui.theme.VirtualStreamTheme
import androidx.core.net.toUri

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 文件读取权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO,
            ), 101)
        } else {
            requestPermissions(arrayOf(
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), 101)
        }
        // 申请所有文件访问权限
        if (!Environment.isExternalStorageManager()) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = "package:${applicationContext.packageName}".toUri()
            }
            startActivity(intent)
        }



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
