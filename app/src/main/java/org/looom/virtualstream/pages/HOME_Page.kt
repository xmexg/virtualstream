package org.looom.virtualstream.pages

import org.looom.virtualstream.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.hazeSource
import org.looom.virtualstream.VARIABLE.HAZE_STATE
import org.looom.virtualstream.ui.theme.Dimens.div_margin
import org.looom.virtualstream.ui.theme.Dimens.title_size
import org.looom.virtualstream.ui.theme.div_Padding_Modifier
import org.looom.virtualstream.ui.theme.div_Status_Padding_Modifier

@Preview( )
@Composable
fun HomePage(modifier: Modifier = Modifier) {
    var hookFlag by rememberSaveable { mutableStateOf(false) }
    Box( modifier = modifier ){
        /**
         * 这里的 modifier 几何背景无法被模糊
         * 关键问题分析
         * 这是 Jetpack Compose 的 hazeSource 机制的底层限制：
         *
         * ☠️ hazeSource 只能模糊“真正的 UI 图层（Layer）”，它不能模糊通过 drawBehind 或 drawIntoCanvas 绘制的内容，因为这些并不属于 Android View 层，也不会被 RenderEffect 捕捉进图像 buffer。
         *
         * ✅ 为什么 Image(... .hazeSource(...)) 可以模糊？
         * 因为 Image 是真正的 Composable 组件，会创建一个 Layer，这个 Layer 可以被模糊效果“捕捉”并处理。
         *
         * ❌ 为什么 Box(... .drawBehind { drawImage(...) }) 无法模糊？
         * 因为你是手动“画上去的图”，这属于底层绘制，不会生成 Layer。而 hazeSource 依赖 Layer 图像缓冲区（buffer）进行模糊处理，所以这类内容就完全“看不到”。
         */
        Image(
            painter = painterResource(R.drawable.hqsw_l_p),
            contentDescription = null,
            modifier = Modifier.matchParentSize()
                .hazeSource(HAZE_STATE),
            contentScale = ContentScale.Crop
        )

        Column (modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {

            // 顶部模块状态
            Row (modifier = Modifier.div_Status_Padding_Modifier(hookFlag, HAZE_STATE)){ // 堆叠样式，两层padding独立填充
                // 图片 https://developer.android.com/develop/ui/compose/graphics/images/customize?hl=zh-cn
                Image(
                    painter = painterResource(id = R.drawable.hqsw_w_s), // app启动图标
                    contentDescription = "Home Icon",
                    contentScale = ContentScale.Crop, // 将图片居中裁剪到可用空间
                    modifier = Modifier.size(50.dp).clip(CircleShape) // 圆形裁剪
                )
                Spacer(modifier = Modifier.width(div_margin))
                Column {
                    Text("模块状态:", fontSize = title_size, fontWeight = FontWeight.Bold, color = Color.White,
                        style = TextStyle(
                            shadow = Shadow(color = Color.Green, offset = Offset(-2.0f, -2.0f), blurRadius = 2.0f) // 阴影
                        )
                    )
                    Text("模块${if (hookFlag) "已" else "未"}启用", color = if (hookFlag) Color.Blue else Color.Red)
                }
            }

            // 串流选择
            Row (modifier = Modifier.div_Padding_Modifier(hazeState = HAZE_STATE)) {
                Text("选择串流：")
            }
        }
    }
}