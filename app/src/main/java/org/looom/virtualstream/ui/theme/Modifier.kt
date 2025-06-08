package org.looom.virtualstream.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import dev.chrisbanes.haze.HazeDefaults.blurRadius
import dev.chrisbanes.haze.HazeDefaults.noiseFactor
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource


/**
 * 首页模块检测的专属样式
 */
@Composable
fun Div_Status_Padding_Modifier(hookFlag: Boolean, hazeState: HazeState): Modifier {
    return Div_Padding_Modifier(
        background = if (hookFlag) div_status_able.background
                else div_status_disable.background,
        hazeState = hazeState
    )
}

/**
 * 普通的div填充样式
 * marge： 外边框
 * width： 盒子宽度
 * shape： 盒子形状
 * background： 盒子背景色
 * padding： 盒子内边距
 * blurRadius： 模糊程度
 * alpha： 半透明度
 * hazeState: 模糊区域
 */
@Composable
fun Div_Padding_Modifier(
    marge: Dp =Dimens.div_outerPadding,
    width: Float = 0.9f,
    shape: Shape = RoundedCornerShape(Dimens.div_cornerRadius),
    background: Color = div_default.background,
    padding: Dp = Dimens.div_innerPadding,
    blurRadius: Dp = 5.dp,
    alpha: Float = 0.5f,
    hazeState: HazeState
): Modifier {
    // haze 高斯模糊: https://chrisbanes.github.io/haze/latest/materials/
    // haze 样式： https://chrisbanes.github.io/haze/latest/api/haze/dev.chrisbanes.haze/-haze-style/
    return Modifier
        .padding(marge)
        .fillMaxWidth(width)
        .clip(shape)
        .hazeEffect(
            state = hazeState,
            style = HazeStyle(
                backgroundColor = background.copy(alpha = alpha),
                blurRadius = blurRadius,
                tints = emptyList()
            )
        )
        .padding(padding)
}

/**
 * 全局背景Box
 */
@Composable
fun Box_Main_Background(innerPadding: PaddingValues): Modifier{
    return Modifier
        .background(MaterialTheme.colorScheme.background)
        .drawBehind {
            val shapeSize = 20.dp.toPx()    // 图形边长或直径
            val spacing = 16.dp.toPx()      // 图形间距
            val step = shapeSize + spacing  // 每个图形+间距占用大小

            val columns = (size.width / step).toInt()
            val rows = (size.height / step).toInt()

            for (row in 0..rows) {
                for (col in 0..columns) {
                    val x = col * step + shapeSize / 2
                    val y = row * step + shapeSize / 2
                    val center = Offset(x, y)

                    val shapeType = (row + col) % 3  // 0: circle, 1: triangle, 2: square

                    when(shapeType) {
                        0 -> drawCircleShape(center, shapeSize / 2, Color(0xFFFFC0CB))   // 粉红圆圈
                        1 -> drawTriangle(center, shapeSize, Color(0xFF80DEEA))         // 蓝绿色三角形
                        2 -> drawSquare(center, shapeSize, Color(0xFFAED581))           // 浅绿色正方形
                    }
                }
            }
        }
        .padding(innerPadding)
}

// 画三角形函数
private fun DrawScope.drawTriangle(center: Offset, size: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - size / 2)                     // 顶点
        lineTo(center.x - size / 2, center.y + size / 2)          // 左下点
        lineTo(center.x + size / 2, center.y + size / 2)          // 右下点
        close()
    }
    drawPath(path = path, color = color, style = Fill)
}

// 画正方形函数
private fun DrawScope.drawSquare(center: Offset, size: Float, color: Color) {
    val half = size / 2
    drawRect(
        color = color,
        topLeft = Offset(center.x - half, center.y - half),
        size = androidx.compose.ui.geometry.Size(size, size)
    )
}

// 画圆圈函数（你已有）
private fun DrawScope.drawCircleShape(center: Offset, radius: Float, color: Color) {
    drawCircle(color = color, radius = radius, center = center)
}