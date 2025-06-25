package org.looom.virtualstream.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 透明度
 * 透明度 Alpha Hex
 * 100%	FF
 * 75%	BF
 * 50%	80
 * 25%	40
 * 10%	1A
 * 0%	00
 */
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
val Pink_sw = Color(0xFFF38A85)

val TextGreen = Color(0xFF6bff6b)
val TextPurple = Color(0xFF8a85f3)


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

val div_Box_Title_Text_BG = Color(0x80f2f0f7)