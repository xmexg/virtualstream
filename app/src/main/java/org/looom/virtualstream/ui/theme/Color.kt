package org.looom.virtualstream.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

val TextGreen = Color(0xFF6bff6b)


/**
 * 模块状态检测 - 未启用
 */
object div_status_disable{
    val background = Color.LightGray
}

/**
 * 模块状态检测 - 已启用
 */
object div_status_able{
    val background = Color(0xFFf38a85)
}

object div_default{

    // 默认的 material3 自适应的背景色
    val background @Composable
        get() = MaterialTheme.colorScheme.background


}