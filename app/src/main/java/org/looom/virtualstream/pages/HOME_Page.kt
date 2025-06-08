package org.looom.virtualstream.pages

import org.looom.virtualstream.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.looom.virtualstream.ui.theme.Div_Padding_Modifier
import org.looom.virtualstream.ui.theme.Div_Status_Padding_Modifier

@Preview( )
@Composable
fun HomePage(modifier: Modifier = Modifier) {
    val HAZE_STATE = rememberHazeState()
    var hookFlag by rememberSaveable { mutableStateOf(true) }
    Box( modifier = modifier ){
        Image(
            painter = painterResource(R.drawable.hqsw_l_p),
            contentDescription = null,
            modifier = Modifier.matchParentSize()
                .hazeSource(HAZE_STATE),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .matchParentSize()
        )

        // 被模糊的内容
        Surface(
            modifier = Modifier
                .padding(160.dp)
                .haze(HAZE_STATE), // 👈 这里才是真正显示模糊的地方
            tonalElevation = 4.dp,
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("模糊效果区域模糊效果区域模糊效果区域模糊效果区域模糊效果区域模糊效果区域模糊效果区域模糊效果区域模糊效果区域模糊效果区域模糊效果区域模糊效果区域模糊效果区域模糊效果区域模糊效果区域模糊效果区域模糊效果区域模糊效果区域模糊效果区域模糊效果区域模糊效果区域模糊效果区域模糊效果区域模糊效果区域模糊效果区域模糊效果区域")
        }

        Column (modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {

            // 顶部模块状态
            Row (modifier = Div_Status_Padding_Modifier(hookFlag, HAZE_STATE)){ // 堆叠样式，两层padding独立填充
                // 图片 https://developer.android.com/develop/ui/compose/graphics/images/customize?hl=zh-cn
                Image(
                    painter = painterResource(id = R.drawable.hqsw_w_s), // app启动图标
                    contentDescription = "Home Icon",
                    contentScale = ContentScale.Crop, // 将图片居中裁剪到可用空间
                    modifier = Modifier.size(50.dp).clip(CircleShape) // 圆形裁剪
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("模块状态:", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White,
                        style = TextStyle(
                            shadow = Shadow(color = Color.Green, offset = Offset(-2.0f, -2.0f), blurRadius = 2.0f) // 阴影
                        )
                    )
                    Text("模块${if (hookFlag) "已" else "未"}启用", color = if (hookFlag) Color.Yellow else Color.Red)
                }
            }

            // 串流选择
            Row (modifier = Div_Padding_Modifier(hazeState = HAZE_STATE)) {
                Text("选择串流：")
            }
        }
    }
}