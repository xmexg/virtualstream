package org.looom.virtualstream.pages

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import org.looom.virtualstream.VARIABLE.VIDEO_PATH
import org.looom.virtualstream.ui.theme.div_Box_Title_Text
import org.looom.virtualstream.ui.theme.div_Box_Title_Text_Head
import org.looom.virtualstream.ui.theme.div_Padding_Modifier
import java.io.File
import java.io.FileOutputStream

@Composable
fun LocalStreamPage(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var videoUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var updateVideoFlag by remember { mutableIntStateOf(0) } // 用于触发 ExoPlayer 重新加载

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) videoUri = uri
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Column(modifier = Modifier.div_Padding_Modifier()) {
                div_Box_Title_Text_Head("选择本地串流视频")

                Button(onClick = { videoPickerLauncher.launch("video/mp4") }) {
                    Text("选择视频")
                }

                Spacer(modifier = Modifier.height(16.dp))

                videoUri?.let { uri ->
                    ExoPlayerPreview(uri = uri)

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = {
                        val success = copyVideoToTarget(context, uri)
                        Toast.makeText(
                            context,
                            if (success) "应用视频成功" else "应用视频失败",
                            Toast.LENGTH_SHORT
                        ).show()
                        if (success) updateVideoFlag++ // 触发重新加载
                    }) {
                        Text("应用视频")
                    }
                }
            }

            Column(modifier = Modifier.div_Padding_Modifier()) {
                div_Box_Title_Text_Head("当前视频")
                if (File(VIDEO_PATH).exists()) {
                    // 每次更新都会重新触发 Composable 重新渲染播放器
                    key(updateVideoFlag) {
                        ExoPlayerPreview(uri = Uri.fromFile(File(VIDEO_PATH)))
                    }
                } else {
                    div_Box_Title_Text("未应用视频", modifier = Modifier.div_Padding_Modifier())
                }
            }
        }
    }
}

@Composable
fun ExoPlayerPreview(uri: Uri) {
    val context = LocalContext.current
    val exoPlayer = remember { // remember可以缓存复杂对象，在 Composable 生命周期内有效。rememberSaveable只能用于简单类型
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(uri) {
        exoPlayer.setMediaItem(MediaItem.fromUri(uri))
        exoPlayer.prepare()
        onDispose { exoPlayer.release() }
    }

    AndroidView(
        factory = {
            PlayerView(it).apply {
                player = exoPlayer
                useController = true
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    )
}

private fun copyVideoToTarget(context: Context, uri: Uri): Boolean {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return false
        val targetFile = File(VIDEO_PATH)
        targetFile.parentFile?.mkdirs()
        val outputStream = FileOutputStream(targetFile)

        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()

        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}