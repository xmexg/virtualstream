package org.looom.virtualstream.vcam

import android.Manifest
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.hardware.Camera.PictureCallback
import android.hardware.Camera.PreviewCallback
import android.hardware.Camera.ShutterCallback
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.InputConfiguration
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.MediaPlayer
import android.media.MediaPlayer.OnPreparedListener
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.view.Surface
import android.view.SurfaceHolder
import android.widget.Toast
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Arrays
import java.util.concurrent.Executor
import kotlin.concurrent.Volatile
import kotlin.math.min

class HookMain : IXposedHookLoadPackage {
    val selfPackageName: String = "org.looom.virtualstream" // 当前模块包名
    val config_path: String = "/DCIM/virtualstream/"
    var imageReaderFormat: Int = 0
    var need_recreate: Boolean = false
    var need_to_show_toast: Boolean = true

    var c2_ori_width: Int = 1280
    var c2_ori_height: Int = 720

    var toast_content: Context? = null

    @Throws(Exception::class)
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        XposedHelpers.findAndHookMethod(
            "android.hardware.Camera",
            lpparam.classLoader,
            "setPreviewTexture",
            SurfaceTexture::class.java,
            object : XC_MethodHook() {
                protected override fun beforeHookedMethod(param: MethodHookParam) {
                    val file = File(video_path + "virtual.mp4")
                    if (file.exists()) {
                        val control_file = File(
                            Environment.getExternalStorageDirectory()
                                .path + "/DCIM/virtualstream/" + "disable.jpg"
                        )
                        if (control_file.exists()) {
                            return
                        }
                        if (is_hooked) {
                            is_hooked = false
                            return
                        }
                        if (param.args[0] == null) {
                            return
                        }
                        if (param.args[0].equals(c1_fake_texture)) {
                            return
                        }
                        if (origin_preview_camera != null && origin_preview_camera == param.thisObject) {
                            param.args[0] = fake_SurfaceTexture
                            XposedBridge.log("【VCAM】发现重复" + origin_preview_camera.toString())
                            return
                        } else {
                            XposedBridge.log("【VCAM】创建预览")
                        }

                        origin_preview_camera = param.thisObject as Camera?
                        mSurfacetexture = param.args[0] as SurfaceTexture?
                        if (fake_SurfaceTexture == null) {
                            fake_SurfaceTexture = SurfaceTexture(10)
                        } else {
                            fake_SurfaceTexture!!.release()
                            fake_SurfaceTexture = SurfaceTexture(10)
                        }
                        param.args[0] = fake_SurfaceTexture
                    } else {
                        val toast_control = File(
                            Environment.getExternalStorageDirectory()
                                .getPath() + "/DCIM/virtualstream/" + "no_toast.jpg"
                        )
                        need_to_show_toast = !toast_control.exists()
                        if (toast_content != null && need_to_show_toast) {
                            try {
                                Toast.makeText(
                                    toast_content,
                                    "不存在替换视频\n" + lpparam.packageName + "当前路径：" + video_path,
                                    Toast.LENGTH_SHORT
                                ).show()
                            } catch (ee: Exception) {
                                XposedBridge.log("【VCAM】[toast]" + ee.toString())
                            }
                        }
                    }
                }
            })

        XposedHelpers.findAndHookMethod(
            "android.hardware.camera2.CameraManager",
            lpparam.classLoader,
            "openCamera",
            String::class.java,
            CameraDevice.StateCallback::class.java,
            Handler::class.java,
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                protected override fun beforeHookedMethod(param: MethodHookParam) {
                    if (param.args[1] == null) {
                        return
                    }
                    if (param.args[1].equals(c2_state_cb)) {
                        return
                    }
                    c2_state_cb = param.args[1] as CameraDevice.StateCallback?
                    c2_state_callback = param.args[1].javaClass
                    val control_file = File(
                        Environment.getExternalStorageDirectory()
                            .getPath() + "/DCIM/virtualstream/" + "disable.jpg"
                    )
                    if (control_file.exists()) {
                        return
                    }
                    val file = File(video_path + "virtual.mp4")
                    val toast_control = File(
                        Environment.getExternalStorageDirectory()
                            .getPath() + "/DCIM/virtualstream/" + "no_toast.jpg"
                    )
                    need_to_show_toast = !toast_control.exists()
                    if (!file.exists()) {
                        if (toast_content != null && need_to_show_toast) {
                            try {
                                Toast.makeText(
                                    toast_content,
                                    "不存在替换视频\n" + lpparam.packageName + "当前路径：" + video_path,
                                    Toast.LENGTH_SHORT
                                ).show()
                            } catch (ee: Exception) {
                                XposedBridge.log("【VCAM】[toast]" + ee.toString())
                            }
                        }
                        return
                    }
                    XposedBridge.log("【VCAM】1位参数初始化相机，类：" + c2_state_callback.toString())
                    is_first_hook_build = true
                    process_camera2_init(c2_state_callback)
                }
            })


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            XposedHelpers.findAndHookMethod(
                "android.hardware.camera2.CameraManager",
                lpparam.classLoader,
                "openCamera",
                String::class.java,
                Executor::class.java,
                CameraDevice.StateCallback::class.java,
                object : XC_MethodHook() {
                    @Throws(Throwable::class)
                    protected override fun afterHookedMethod(param: MethodHookParam) {
                        if (param.args[2] == null) {
                            return
                        }
                        if (param.args[2].equals(c2_state_cb)) {
                            return
                        }
                        c2_state_cb = param.args[2] as CameraDevice.StateCallback?
                        val control_file = File(
                            Environment.getExternalStorageDirectory()
                                .getPath() + "/DCIM/virtualstream/" + "disable.jpg"
                        )
                        if (control_file.exists()) {
                            return
                        }
                        val file = File(video_path + "virtual.mp4")
                        val toast_control = File(
                            Environment.getExternalStorageDirectory()
                                .getPath() + "/DCIM/virtualstream/" + "no_toast.jpg"
                        )
                        need_to_show_toast = !toast_control.exists()
                        if (!file.exists()) {
                            if (toast_content != null && need_to_show_toast) {
                                try {
                                    Toast.makeText(
                                        toast_content,
                                        "不存在替换视频\n" + lpparam.packageName + "当前路径：" + video_path,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } catch (ee: Exception) {
                                    XposedBridge.log("【VCAM】[toast]" + ee.toString())
                                }
                            }
                            return
                        }
                        c2_state_callback = param.args[2].javaClass
                        XposedBridge.log("【VCAM】2位参数初始化相机，类：" + c2_state_callback.toString())
                        is_first_hook_build = true
                        process_camera2_init(c2_state_callback)
                    }
                })
        }


        XposedHelpers.findAndHookMethod(
            "android.hardware.Camera",
            lpparam.classLoader,
            "setPreviewCallbackWithBuffer",
            PreviewCallback::class.java,
            object : XC_MethodHook() {
                protected override fun beforeHookedMethod(param: MethodHookParam) {
                    if (param.args[0] != null) {
                        process_callback(param)
                    }
                }
            })

        XposedHelpers.findAndHookMethod(
            "android.hardware.Camera",
            lpparam.classLoader,
            "addCallbackBuffer",
            ByteArray::class.java,
            object : XC_MethodHook() {
                protected override fun beforeHookedMethod(param: MethodHookParam) {
                    if (param.args[0] != null) {
                        param.args[0] = ByteArray((param.args[0] as ByteArray).size)
                    }
                }
            })

        XposedHelpers.findAndHookMethod(
            "android.hardware.Camera",
            lpparam.classLoader,
            "setPreviewCallback",
            PreviewCallback::class.java,
            object : XC_MethodHook() {
                protected override fun beforeHookedMethod(param: MethodHookParam) {
                    if (param.args[0] != null) {
                        process_callback(param)
                    }
                }
            })

        XposedHelpers.findAndHookMethod(
            "android.hardware.Camera",
            lpparam.classLoader,
            "setOneShotPreviewCallback",
            PreviewCallback::class.java,
            object : XC_MethodHook() {
                protected override fun beforeHookedMethod(param: MethodHookParam) {
                    if (param.args[0] != null) {
                        process_callback(param)
                    }
                }
            })

        XposedHelpers.findAndHookMethod(
            "android.hardware.Camera",
            lpparam.classLoader,
            "takePicture",
            ShutterCallback::class.java,
            PictureCallback::class.java,
            PictureCallback::class.java,
            PictureCallback::class.java,
            object : XC_MethodHook() {
                protected override fun afterHookedMethod(param: MethodHookParam) {
                    XposedBridge.log("【VCAM】4参数拍照")
                    if (param.args[1] != null) {
                        process_a_shot_YUV(param)
                    }

                    if (param.args[3] != null) {
                        process_a_shot_jpeg(param, 3)
                    }
                }
            })

        XposedHelpers.findAndHookMethod(
            "android.media.MediaRecorder",
            lpparam.classLoader,
            "setCamera",
            Camera::class.java,
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                protected override fun beforeHookedMethod(param: MethodHookParam?) {
                    super.beforeHookedMethod(param)
                    val toast_control = File(
                        Environment.getExternalStorageDirectory()
                            .getPath() + "/DCIM/virtualstream/" + "no_toast.jpg"
                    )
                    need_to_show_toast = !toast_control.exists()
                    XposedBridge.log("【VCAM】[record]" + lpparam.packageName)
                    if (toast_content != null && need_to_show_toast) {
                        try {
                            Toast.makeText(
                                toast_content,
                                "应用：" + lpparam.appInfo.name + "(" + lpparam.packageName + ")" + "触发了录像，但目前无法拦截",
                                Toast.LENGTH_SHORT
                            ).show()
                        } catch (ee: Exception) {
                            XposedBridge.log("【VCAM】[toast]" + ee.stackTrace.contentToString())
                        }
                    }
                }
            })

        XposedHelpers.findAndHookMethod(
            "android.app.Instrumentation",
            lpparam.classLoader,
            "callApplicationOnCreate",
            Application::class.java,
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                protected override fun afterHookedMethod(param: MethodHookParam) {
                    super.afterHookedMethod(param)
                    if (param.args[0] is Application) {
                        try {
                            toast_content = (param.args[0] as Application).getApplicationContext()
                        } catch (ee: Exception) {
                            XposedBridge.log("【VCAM】" + ee.toString())
                        }
                        val force_private = File(
                            Environment.getExternalStorageDirectory()
                                .getAbsolutePath() + "/DCIM/virtualstream/private_dir.jpg"
                        )
                        if (toast_content != null) { //后半段用于强制私有目录
                            var auth_statue = 0
                            try {
                                auth_statue += (toast_content!!.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) + 1)
                            } catch (ee: Exception) {
                                XposedBridge.log("【VCAM】[permission-check]" + ee.toString())
                            }
                            try {
                                auth_statue += (toast_content!!.checkSelfPermission(Manifest.permission.MANAGE_EXTERNAL_STORAGE) + 1)
                            } catch (ee: Exception) {
                                XposedBridge.log("【VCAM】[permission-check]" + ee.toString())
                            }
                            //权限判断完毕
                            if (auth_statue < 1 || force_private.exists()) {
                                var shown_file = File(
                                    toast_content!!.getExternalFilesDir(null)!!
                                        .getAbsolutePath() + "/virtualstream/"
                                )
                                if ((!shown_file.isDirectory()) && shown_file.exists()) {
                                    shown_file.delete()
                                }
                                if (!shown_file.exists()) {
                                    shown_file.mkdir()
                                }
                                shown_file = File(
                                    toast_content!!.getExternalFilesDir(null)!!
                                        .getAbsolutePath() + "/virtualstream/" + "has_shown"
                                )
                                val toast_force_file = File(
                                    Environment.getExternalStorageDirectory()
                                        .getPath() + "/DCIM/virtualstream/force_show.jpg"
                                )
                                // AGP 9.0 后已彻底删除buildConfig, 无法再使用BuildConfig.APPLICATION_ID
                                if ((!lpparam.packageName.equals(selfPackageName)) && ((!shown_file.exists()) || toast_force_file.exists())) {
                                    try {
                                        Toast.makeText(
                                            toast_content,
                                            lpparam.packageName + "未授予读取本地目录权限，请检查权限\nvirtualstream目前重定向为 " + toast_content!!.getExternalFilesDir(
                                                null
                                            )!!
                                                .getAbsolutePath() + "/virtualstream/",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        val fos = FileOutputStream(
                                            toast_content!!.getExternalFilesDir(null)!!
                                                .getAbsolutePath() + "/virtualstream/" + "has_shown"
                                        )
                                        val info = "shown"
                                        fos.write(info.toByteArray())
                                        fos.flush()
                                        fos.close()
                                    } catch (e: Exception) {
                                        XposedBridge.log("【VCAM】[switch-dir]" + e.toString())
                                    }
                                }
                                video_path = toast_content!!.getExternalFilesDir(null)!!
                                    .getAbsolutePath() + "/virtualstream/"
                            } else {
                                video_path = Environment.getExternalStorageDirectory()
                                    .getPath() + "/DCIM/virtualstream/"
                            }
                        } else {
                            video_path = Environment.getExternalStorageDirectory()
                                .getPath() + "/DCIM/virtualstream/"
                            val uni_DCIM_path = File(
                                Environment.getExternalStorageDirectory()
                                    .getPath() + "/DCIM/virtualstream/"
                            )
                            if (uni_DCIM_path.canWrite()) {
                                val uni_virtualstream_path = File(video_path)
                                if (!uni_virtualstream_path.exists()) {
                                    uni_virtualstream_path.mkdir()
                                }
                            }
                        }
                    }
                }
            })

        XposedHelpers.findAndHookMethod(
            "android.hardware.Camera",
            lpparam.classLoader,
            "startPreview",
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                protected override fun beforeHookedMethod(param: MethodHookParam) {
                    val file = File(video_path + "virtual.mp4")
                    val toast_control = File(
                        Environment.getExternalStorageDirectory()
                            .getPath() + "/DCIM/virtualstream/" + "no_toast.jpg"
                    )
                    need_to_show_toast = !toast_control.exists()
                    if (!file.exists()) {
                        if (toast_content != null && need_to_show_toast) {
                            try {
                                Toast.makeText(
                                    toast_content,
                                    "不存在替换视频\n" + lpparam.packageName + "当前路径：" + video_path,
                                    Toast.LENGTH_SHORT
                                ).show()
                            } catch (ee: Exception) {
                                XposedBridge.log("【VCAM】[toast]" + ee.toString())
                            }
                        }
                        return
                    }
                    val control_file = File(
                        Environment.getExternalStorageDirectory()
                            .getPath() + "/DCIM/virtualstream/" + "disable.jpg"
                    )
                    if (control_file.exists()) {
                        return
                    }
                    is_someone_playing = false
                    XposedBridge.log("【VCAM】开始预览")
                    start_preview_camera = param.thisObject as Camera?
                    if (ori_holder != null) {
                        if (mplayer1 == null) {
                            mplayer1 = MediaPlayer()
                        } else {
                            mplayer1!!.release()
                            mplayer1 = null
                            mplayer1 = MediaPlayer()
                        }
                        if (!ori_holder!!.surface.isValid || ori_holder == null) {
                            return
                        }
                        mplayer1!!.setSurface(ori_holder!!.surface)
                        val sfile = File(
                            Environment.getExternalStorageDirectory()
                                .getPath() + "/DCIM/virtualstream/" + "no-silent.jpg"
                        )
                        if (!(sfile.exists() && (!is_someone_playing))) {
                            mplayer1!!.setVolume(0f, 0f)
                            is_someone_playing = false
                        } else {
                            is_someone_playing = true
                        }
                        mplayer1!!.isLooping = true

                        mplayer1!!.setOnPreparedListener { mplayer1!!.start() }

                        try {
                            mplayer1!!.setDataSource(video_path + "virtual.mp4")
                            mplayer1!!.prepare()
                        } catch (e: IOException) {
                            XposedBridge.log("【VCAM】" + e.toString())
                        }
                    }


                    if (mSurfacetexture != null) {
                        if (mSurface == null) {
                            mSurface = Surface(mSurfacetexture)
                        } else {
                            mSurface!!.release()
                            mSurface = Surface(mSurfacetexture)
                        }

                        if (mMediaPlayer == null) {
                            mMediaPlayer = MediaPlayer()
                        } else {
                            mMediaPlayer!!.release()
                            mMediaPlayer = MediaPlayer()
                        }

                        mMediaPlayer!!.setSurface(mSurface)

                        val sfile = File(
                            Environment.getExternalStorageDirectory()
                                .getPath() + "/DCIM/virtualstream/" + "no-silent.jpg"
                        )
                        if (!(sfile.exists() && (!is_someone_playing))) {
                            mMediaPlayer!!.setVolume(0f, 0f)
                            is_someone_playing = false
                        } else {
                            is_someone_playing = true
                        }
                        mMediaPlayer!!.isLooping = true

                        mMediaPlayer!!.setOnPreparedListener { mMediaPlayer!!.start() }

                        try {
                            mMediaPlayer!!.setDataSource(video_path + "virtual.mp4")
                            mMediaPlayer!!.prepare()
                        } catch (e: IOException) {
                            XposedBridge.log("【VCAM】" + e.toString())
                        }
                    }
                }
            })

        XposedHelpers.findAndHookMethod(
            "android.hardware.Camera",
            lpparam.classLoader,
            "setPreviewDisplay",
            SurfaceHolder::class.java,
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                protected override fun beforeHookedMethod(param: MethodHookParam) {
                    XposedBridge.log("【VCAM】添加Surfaceview预览")
                    val file = File(video_path + "virtual.mp4")
                    val toast_control = File(
                        Environment.getExternalStorageDirectory()
                            .path + "/DCIM/virtualstream/" + "no_toast.jpg"
                    )
                    need_to_show_toast = !toast_control.exists()
                    if (!file.exists()) {
                        if (toast_content != null && need_to_show_toast) {
                            try {
                                Toast.makeText(
                                    toast_content,
                                    "不存在替换视频\n" + lpparam.packageName + "当前路径：" + video_path,
                                    Toast.LENGTH_SHORT
                                ).show()
                            } catch (ee: Exception) {
                                XposedBridge.log("【VCAM】[toast]" + ee.toString())
                            }
                        }
                        return
                    }
                    val control_file = File(
                        Environment.getExternalStorageDirectory()
                            .path + "/DCIM/virtualstream/" + "disable.jpg"
                    )
                    if (control_file.exists()) {
                        return
                    }
                    mvirtualstream = param.thisObject as Camera
                    ori_holder = param.args[0] as SurfaceHolder?
                    if (c1_fake_texture == null) {
                        c1_fake_texture = SurfaceTexture(11)
                    } else {
                        c1_fake_texture!!.release()
                        c1_fake_texture = null
                        c1_fake_texture = SurfaceTexture(11)
                    }

                    if (c1_fake_surface == null) {
                        c1_fake_surface = Surface(c1_fake_texture)
                    } else {
                        c1_fake_surface!!.release()
                        c1_fake_surface = null
                        c1_fake_surface = Surface(c1_fake_texture)
                    }
                    is_hooked = true
                    mvirtualstream!!.setPreviewTexture(c1_fake_texture)
                    param.setResult(null)
                }
            })

        XposedHelpers.findAndHookMethod(
            "android.hardware.camera2.CaptureRequest.Builder",
            lpparam.classLoader,
            "addTarget",
            Surface::class.java,
            object : XC_MethodHook() {
                protected override fun beforeHookedMethod(param: MethodHookParam) {
                    if (param.args[0] == null) {
                        return
                    }
                    if (param.thisObject == null) {
                        return
                    }
                    val file = File(video_path + "virtual.mp4")
                    val toast_control = File(
                        Environment.getExternalStorageDirectory()
                            .getPath() + "/DCIM/virtualstream/" + "no_toast.jpg"
                    )
                    need_to_show_toast = !toast_control.exists()
                    if (!file.exists()) {
                        if (toast_content != null && need_to_show_toast) {
                            try {
                                Toast.makeText(
                                    toast_content,
                                    "不存在替换视频\n" + lpparam.packageName + "当前路径：" + video_path,
                                    Toast.LENGTH_SHORT
                                ).show()
                            } catch (ee: Exception) {
                                XposedBridge.log("【VCAM】[toast]" + ee.toString())
                            }
                        }
                        return
                    }
                    if (param.args[0].equals(c2_virtual_surface)) {
                        return
                    }
                    val control_file = File(
                        Environment.getExternalStorageDirectory()
                            .getPath() + "/DCIM/virtualstream/" + "disable.jpg"
                    )
                    if (control_file.exists()) {
                        return
                    }
                    val surfaceInfo = param.args[0].toString()
                    if (surfaceInfo.contains("Surface(name=null)")) {
                        if (c2_reader_Surfcae == null) {
                            c2_reader_Surfcae = param.args[0] as Surface?
                        } else {
                            if ((c2_reader_Surfcae != param.args[0]) && c2_reader_Surfcae_1 == null) {
                                c2_reader_Surfcae_1 = param.args[0] as Surface?
                            }
                        }
                    } else {
                        if (c2_preview_Surfcae == null) {
                            c2_preview_Surfcae = param.args[0] as Surface?
                        } else {
                            if ((c2_preview_Surfcae != param.args[0]) && c2_preview_Surfcae_1 == null) {
                                c2_preview_Surfcae_1 = param.args[0] as Surface?
                            }
                        }
                    }
                    XposedBridge.log("【VCAM】添加目标：" + param.args[0].toString())
                    param.args[0] = c2_virtual_surface
                }
            })

        XposedHelpers.findAndHookMethod(
            "android.hardware.camera2.CaptureRequest.Builder",
            lpparam.classLoader,
            "removeTarget",
            Surface::class.java,
            object : XC_MethodHook() {
                protected override fun beforeHookedMethod(param: MethodHookParam) {
                    if (param.args[0] == null) {
                        return
                    }
                    if (param.thisObject == null) {
                        return
                    }
                    val file = File(video_path + "virtual.mp4")
                    val toast_control = File(
                        Environment.getExternalStorageDirectory()
                            .getPath() + "/DCIM/virtualstream/" + "no_toast.jpg"
                    )
                    need_to_show_toast = !toast_control.exists()
                    if (!file.exists()) {
                        if (toast_content != null && need_to_show_toast) {
                            try {
                                Toast.makeText(
                                    toast_content,
                                    "不存在替换视频\n" + lpparam.packageName + "当前路径：" + video_path,
                                    Toast.LENGTH_SHORT
                                ).show()
                            } catch (ee: Exception) {
                                XposedBridge.log("【VCAM】[toast]" + ee.toString())
                            }
                        }
                        return
                    }
                    val control_file = File(
                        Environment.getExternalStorageDirectory()
                            .getPath() + "/DCIM/virtualstream/" + "disable.jpg"
                    )
                    if (control_file.exists()) {
                        return
                    }
                    val rm_surf = param.args[0] as Surface
                    if (rm_surf == c2_preview_Surfcae) {
                        c2_preview_Surfcae = null
                    }
                    if (rm_surf == c2_preview_Surfcae_1) {
                        c2_preview_Surfcae_1 = null
                    }
                    if (rm_surf == c2_reader_Surfcae_1) {
                        c2_reader_Surfcae_1 = null
                    }
                    if (rm_surf == c2_reader_Surfcae) {
                        c2_reader_Surfcae = null
                    }

                    XposedBridge.log("【VCAM】移除目标：" + param.args[0].toString())
                }
            })

        XposedHelpers.findAndHookMethod(
            "android.hardware.camera2.CaptureRequest.Builder",
            lpparam.classLoader,
            "build",
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                protected override fun beforeHookedMethod(param: MethodHookParam) {
                    if (param.thisObject == null) {
                        return
                    }
                    if (param.thisObject.equals(c2_builder)) {
                        return
                    }
                    c2_builder = param.thisObject as CaptureRequest.Builder?
                    val file = File(video_path + "virtual.mp4")
                    val toast_control = File(
                        Environment.getExternalStorageDirectory()
                            .getPath() + "/DCIM/virtualstream/" + "no_toast.jpg"
                    )
                    need_to_show_toast = !toast_control.exists()
                    if (!file.exists() && need_to_show_toast) {
                        if (toast_content != null) {
                            try {
                                Toast.makeText(
                                    toast_content,
                                    "不存在替换视频\n" + lpparam.packageName + "当前路径：" + video_path,
                                    Toast.LENGTH_SHORT
                                ).show()
                            } catch (ee: Exception) {
                                XposedBridge.log("【VCAM】[toast]" + ee.toString())
                            }
                        }
                        return
                    }

                    val control_file = File(
                        Environment.getExternalStorageDirectory()
                            .getPath() + "/DCIM/virtualstream/" + "disable.jpg"
                    )
                    if (control_file.exists()) {
                        return
                    }
                    XposedBridge.log("【VCAM】开始build请求")
                    process_camera2_play()
                }
            })

        /*        XposedHelpers.findAndHookMethod("android.hardware.Camera", lpparam.classLoader, "stopPreview", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (param.thisObject.equals(HookMain.origin_preview_camera) || param.thisObject.equals(HookMain.camera_onPreviewFrame) || param.thisObject.equals(HookMain.mvirtualstream)) {
                    if (hw_decode_obj != null) {
                        hw_decode_obj.stopDecode();
                    }
                    if (mplayer1 != null) {
                        mplayer1.release();
                        mplayer1 = null;
                    }
                    if (mMediaPlayer != null) {
                        mMediaPlayer.release();
                        mMediaPlayer = null;
                    }
                    is_someone_playing = false;

                    XposedBridge.log("停止预览");
                }
            }
        });*/
        XposedHelpers.findAndHookMethod(
            "android.media.ImageReader",
            lpparam.classLoader,
            "newInstance",
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            object : XC_MethodHook() {
                protected override fun beforeHookedMethod(param: MethodHookParam) {
                    XposedBridge.log("【VCAM】应用创建了渲染器：宽：" + param.args[0] + " 高：" + param.args[1] + "格式" + param.args[2])
                    c2_ori_width = param.args[0] as Int
                    c2_ori_height = param.args[1] as Int
                    imageReaderFormat = param.args[2] as Int
                    val toast_control = File(
                        Environment.getExternalStorageDirectory()
                            .getPath() + "/DCIM/virtualstream/" + "no_toast.jpg"
                    )
                    need_to_show_toast = !toast_control.exists()
                    if (toast_content != null && need_to_show_toast) {
                        try {
                            Toast.makeText(
                                toast_content,
                                "应用创建了渲染器：\n宽：" + param.args[0] + "\n高：" + param.args[1] + "\n一般只需要宽高比与视频相同",
                                Toast.LENGTH_SHORT
                            ).show()
                        } catch (e: Exception) {
                            XposedBridge.log("【VCAM】[toast]" + e.toString())
                        }
                    }
                }
            })


        XposedHelpers.findAndHookMethod(
            "android.hardware.camera2.CameraCaptureSession.CaptureCallback",
            lpparam.classLoader,
            "onCaptureFailed",
            CameraCaptureSession::class.java,
            CaptureRequest::class.java,
            CaptureFailure::class.java,
            object : XC_MethodHook() {
                protected override fun beforeHookedMethod(param: MethodHookParam) {
                    XposedBridge.log("【VCAM】onCaptureFailed" + "原因：" + (param.args[2] as CaptureFailure).getReason())
                }
            })
    }

    private fun process_camera2_play() {
        if (c2_reader_Surfcae != null) {
            if (c2_hw_decode_obj != null) {
                c2_hw_decode_obj!!.stopDecode()
                c2_hw_decode_obj = null
            }

            c2_hw_decode_obj = VideoToFrames()
            try {
                if (imageReaderFormat == 256) {
                    c2_hw_decode_obj!!.setSaveFrames("null", OutputImageFormat.JPEG)
                } else {
                    c2_hw_decode_obj!!.setSaveFrames("null", OutputImageFormat.NV21)
                }
                c2_hw_decode_obj!!.set_surfcae(c2_reader_Surfcae)
                c2_hw_decode_obj!!.decode(video_path + "virtual.mp4")
            } catch (throwable: Throwable) {
                XposedBridge.log("【VCAM】" + throwable)
            }
        }

        if (c2_reader_Surfcae_1 != null) {
            if (c2_hw_decode_obj_1 != null) {
                c2_hw_decode_obj_1!!.stopDecode()
                c2_hw_decode_obj_1 = null
            }

            c2_hw_decode_obj_1 = VideoToFrames()
            try {
                if (imageReaderFormat == 256) {
                    c2_hw_decode_obj_1!!.setSaveFrames("null", OutputImageFormat.JPEG)
                } else {
                    c2_hw_decode_obj_1!!.setSaveFrames("null", OutputImageFormat.NV21)
                }
                c2_hw_decode_obj_1!!.set_surfcae(c2_reader_Surfcae_1)
                c2_hw_decode_obj_1!!.decode(video_path + "virtual.mp4")
            } catch (throwable: Throwable) {
                XposedBridge.log("【VCAM】" + throwable)
            }
        }


        if (c2_preview_Surfcae != null) {
            if (c2_player == null) {
                c2_player = MediaPlayer()
            } else {
                c2_player!!.release()
                c2_player = MediaPlayer()
            }
            c2_player!!.setSurface(c2_preview_Surfcae)
            val sfile = File(
                Environment.getExternalStorageDirectory()
                    .getPath() + "/DCIM/virtualstream/" + "no-silent.jpg"
            )
            if (!sfile.exists()) {
                c2_player!!.setVolume(0f, 0f)
            }
            c2_player!!.setLooping(true)

            try {
                c2_player!!.setOnPreparedListener(object : OnPreparedListener {
                    override fun onPrepared(mp: MediaPlayer?) {
                        c2_player!!.start()
                    }
                })
                c2_player!!.setDataSource(video_path + "virtual.mp4")
                c2_player!!.prepare()
            } catch (e: Exception) {
                XposedBridge.log("【VCAM】[c2player][" + c2_preview_Surfcae.toString() + "]" + e)
            }
        }

        if (c2_preview_Surfcae_1 != null) {
            if (c2_player_1 == null) {
                c2_player_1 = MediaPlayer()
            } else {
                c2_player_1!!.release()
                c2_player_1 = MediaPlayer()
            }
            c2_player_1!!.setSurface(c2_preview_Surfcae_1)
            val sfile = File(
                Environment.getExternalStorageDirectory()
                    .getPath() + "/DCIM/virtualstream/" + "no-silent.jpg"
            )
            if (!sfile.exists()) {
                c2_player_1!!.setVolume(0f, 0f)
            }
            c2_player_1!!.setLooping(true)

            try {
                c2_player_1!!.setOnPreparedListener(object : OnPreparedListener {
                    override fun onPrepared(mp: MediaPlayer?) {
                        c2_player_1!!.start()
                    }
                })
                c2_player_1!!.setDataSource(video_path + "virtual.mp4")
                c2_player_1!!.prepare()
            } catch (e: Exception) {
                XposedBridge.log("【VCAM】[c2player1]" + "[ " + c2_preview_Surfcae_1.toString() + "]" + e)
            }
        }
        XposedBridge.log("【VCAM】Camera2处理过程完全执行")
    }

    private fun create_virtual_surface(): Surface? {
        if (need_recreate) {
            if (c2_virtual_surfaceTexture != null) {
                c2_virtual_surfaceTexture!!.release()
                c2_virtual_surfaceTexture = null
            }
            if (c2_virtual_surface != null) {
                c2_virtual_surface!!.release()
                c2_virtual_surface = null
            }
            c2_virtual_surfaceTexture = SurfaceTexture(15)
            c2_virtual_surface = Surface(c2_virtual_surfaceTexture)
            need_recreate = false
        } else {
            if (c2_virtual_surface == null) {
                need_recreate = true
                c2_virtual_surface = create_virtual_surface()
            }
        }
        XposedBridge.log("【VCAM】【重建垃圾场】" + c2_virtual_surface.toString())
        return c2_virtual_surface
    }

    private fun process_camera2_init(hooked_class: Class<*>?) {
        XposedHelpers.findAndHookMethod(
            hooked_class,
            "onOpened",
            CameraDevice::class.java,
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                protected override fun beforeHookedMethod(param: MethodHookParam) {
                    need_recreate = true
                    create_virtual_surface()
                    if (c2_player != null) {
                        c2_player!!.stop()
                        c2_player!!.reset()
                        c2_player!!.release()
                        c2_player = null
                    }
                    if (c2_hw_decode_obj_1 != null) {
                        c2_hw_decode_obj_1!!.stopDecode()
                        c2_hw_decode_obj_1 = null
                    }
                    if (c2_hw_decode_obj != null) {
                        c2_hw_decode_obj!!.stopDecode()
                        c2_hw_decode_obj = null
                    }
                    if (c2_player_1 != null) {
                        c2_player_1!!.stop()
                        c2_player_1!!.reset()
                        c2_player_1!!.release()
                        c2_player_1 = null
                    }
                    c2_preview_Surfcae_1 = null
                    c2_reader_Surfcae_1 = null
                    c2_reader_Surfcae = null
                    c2_preview_Surfcae = null
                    is_first_hook_build = true
                    XposedBridge.log("【VCAM】打开相机C2")

                    val file = File(video_path + "virtual.mp4")
                    val toast_control = File(
                        Environment.getExternalStorageDirectory()
                            .getPath() + "/DCIM/virtualstream/" + "no_toast.jpg"
                    )
                    need_to_show_toast = !toast_control.exists()
                    if (!file.exists()) {
                        if (toast_content != null && need_to_show_toast) {
                            try {
                                Toast.makeText(
                                    toast_content,
                                    "不存在替换视频\n" + toast_content!!.getPackageName() + "当前路径：" + video_path,
                                    Toast.LENGTH_SHORT
                                ).show()
                            } catch (ee: Exception) {
                                XposedBridge.log("【VCAM】[toast]" + ee.toString())
                            }
                        }
                        return
                    }
                    XposedHelpers.findAndHookMethod(
                        param.args[0].javaClass,
                        "createCaptureSession",
                        MutableList::class.java,
                        CameraCaptureSession.StateCallback::class.java,
                        Handler::class.java,
                        object : XC_MethodHook() {
                            @Throws(Throwable::class)
                            protected override fun beforeHookedMethod(paramd: MethodHookParam) {
                                if (paramd.args[0] != null) {
                                    XposedBridge.log("【VCAM】createCaptureSession创捷捕获，原始:" + paramd.args[0].toString() + "虚拟：" + c2_virtual_surface.toString())
                                    paramd.args[0] = Arrays.asList<Surface?>(c2_virtual_surface)
                                    if (paramd.args[1] != null) {
                                        process_camera2Session_callback(paramd.args[1] as CameraCaptureSession.StateCallback?)
                                    }
                                }
                            }
                        })

                    /*                XposedHelpers.findAndHookMethod(param.args[0].javaClass, "close", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam paramd) throws Throwable {
                        XposedBridge.log("C2终止预览");
                        if (c2_hw_decode_obj != null) {
                            c2_hw_decode_obj.stopDecode();
                            c2_hw_decode_obj = null;
                        }
                        if (c2_hw_decode_obj_1 != null) {
                            c2_hw_decode_obj_1.stopDecode();
                            c2_hw_decode_obj_1 = null;
                        }
                        if (c2_player != null) {
                            c2_player.release();
                            c2_player = null;
                        }
                        if (c2_player_1 != null){
                            c2_player_1.release();
                            c2_player_1 = null;
                        }
                        c2_preview_Surfcae_1 = null;
                        c2_reader_Surfcae_1 = null;
                        c2_reader_Surfcae = null;
                        c2_preview_Surfcae = null;
                        need_recreate = true;
                        is_first_hook_build= true;
                    }
                });*/
                    XposedHelpers.findAndHookMethod(
                        param.args[0].javaClass,
                        "createCaptureSessionByOutputConfigurations",
                        MutableList::class.java,
                        CameraCaptureSession.StateCallback::class.java,
                        Handler::class.java,
                        object : XC_MethodHook() {
                            @Throws(Throwable::class)
                            protected override fun beforeHookedMethod(param: MethodHookParam) {
                                super.beforeHookedMethod(param)
                                if (param.args[0] != null) {
                                    outputConfiguration =
                                        OutputConfiguration(c2_virtual_surface!!)
                                    param.args[0] = Arrays.asList<OutputConfiguration?>(
                                        outputConfiguration
                                    )

                                    XposedBridge.log("【VCAM】执行了createCaptureSessionByOutputConfigurations-144777")
                                    if (param.args[1] != null) {
                                        process_camera2Session_callback(param.args[1] as CameraCaptureSession.StateCallback?)
                                    }
                                }
                            }
                        })


                    XposedHelpers.findAndHookMethod(
                        param.args[0].javaClass,
                        "createConstrainedHighSpeedCaptureSession",
                        MutableList::class.java,
                        CameraCaptureSession.StateCallback::class.java,
                        Handler::class.java,
                        object : XC_MethodHook() {
                            @Throws(Throwable::class)
                            protected override fun beforeHookedMethod(param: MethodHookParam) {
                                super.beforeHookedMethod(param)
                                if (param.args[0] != null) {
                                    param.args[0] = Arrays.asList<Surface?>(c2_virtual_surface)
                                    XposedBridge.log("【VCAM】执行了 createConstrainedHighSpeedCaptureSession -5484987")
                                    if (param.args[1] != null) {
                                        process_camera2Session_callback(param.args[1] as CameraCaptureSession.StateCallback?)
                                    }
                                }
                            }
                        })


                    XposedHelpers.findAndHookMethod(
                        param.args[0].javaClass,
                        "createReprocessableCaptureSession",
                        InputConfiguration::class.java,
                        MutableList::class.java,
                        CameraCaptureSession.StateCallback::class.java,
                        Handler::class.java,
                        object : XC_MethodHook() {
                            @Throws(Throwable::class)
                            protected override fun beforeHookedMethod(param: MethodHookParam) {
                                super.beforeHookedMethod(param)
                                if (param.args[1] != null) {
                                    param.args[1] = Arrays.asList<Surface?>(c2_virtual_surface)
                                    XposedBridge.log("【VCAM】执行了 createReprocessableCaptureSession ")
                                    if (param.args[2] != null) {
                                        process_camera2Session_callback(param.args[2] as CameraCaptureSession.StateCallback?)
                                    }
                                }
                            }
                        })


                    XposedHelpers.findAndHookMethod(
                        param.args[0].javaClass,
                        "createReprocessableCaptureSessionByConfigurations",
                        InputConfiguration::class.java,
                        MutableList::class.java,
                        CameraCaptureSession.StateCallback::class.java,
                        Handler::class.java,
                        object : XC_MethodHook() {
                            @Throws(Throwable::class)
                            protected override fun beforeHookedMethod(param: MethodHookParam) {
                                super.beforeHookedMethod(param)
                                if (param.args[1] != null) {
                                    outputConfiguration =
                                        OutputConfiguration(c2_virtual_surface!!)
                                    param.args[0] = listOf(
                                        outputConfiguration
                                    )
                                    XposedBridge.log("【VCAM】执行了 createReprocessableCaptureSessionByConfigurations")
                                    if (param.args[2] != null) {
                                        process_camera2Session_callback(param.args[2] as CameraCaptureSession.StateCallback?)
                                    }
                                }
                            }
                        })

                    XposedHelpers.findAndHookMethod(
                        param.args[0].javaClass,
                        "createCaptureSession",
                        SessionConfiguration::class.java,
                        object : XC_MethodHook() {
                            @Throws(Throwable::class)
                            protected override fun beforeHookedMethod(param: MethodHookParam) {
                                super.beforeHookedMethod(param)
                                if (param.args[0] != null) {
                                    XposedBridge.log("【VCAM】执行了 createCaptureSession -5484987")
                                    sessionConfiguration = param.args[0] as SessionConfiguration
                                    outputConfiguration =
                                        OutputConfiguration(c2_virtual_surface!!)
                                    fake_sessionConfiguration = SessionConfiguration(
                                        sessionConfiguration!!.getSessionType(),
                                        Arrays.asList<OutputConfiguration?>(outputConfiguration),
                                        sessionConfiguration!!.getExecutor(),
                                        sessionConfiguration!!.getStateCallback()
                                    )
                                    param.args[0] = fake_sessionConfiguration
                                    process_camera2Session_callback(sessionConfiguration!!.getStateCallback())
                                }
                            }
                        })
                }
            })


        XposedHelpers.findAndHookMethod(
            hooked_class,
            "onError",
            CameraDevice::class.java,
            Int::class.javaPrimitiveType,
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                protected override fun beforeHookedMethod(param: MethodHookParam) {
                    XposedBridge.log("【VCAM】相机错误onerror：" + param.args[1] as Int)
                }
            })


        XposedHelpers.findAndHookMethod(
            hooked_class,
            "onDisconnected",
            CameraDevice::class.java,
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                protected override fun beforeHookedMethod(param: MethodHookParam?) {
                    XposedBridge.log("【VCAM】相机断开onDisconnected ：")
                }
            })
    }

    private fun process_a_shot_jpeg(param: XC_MethodHook.MethodHookParam, index: Int) {
        try {
            XposedBridge.log("【VCAM】第二个jpeg:" + param.args[index].toString())
        } catch (eee: Exception) {
            XposedBridge.log("【VCAM】" + eee)
        }
        val callback: Class<*>? = param.args[index].javaClass

        XposedHelpers.findAndHookMethod(
            callback,
            "onPictureTaken",
            ByteArray::class.java,
            Camera::class.java,
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                protected override fun beforeHookedMethod(paramd: MethodHookParam) {
                    try {
                        val loaclcam = paramd.args[1] as Camera
                        onemwidth = loaclcam.getParameters().getPreviewSize().width
                        onemhight = loaclcam.getParameters().getPreviewSize().height
                        XposedBridge.log("【VCAM】JPEG拍照回调初始化：宽：" + onemwidth + "高：" + onemhight + "对应的类：" + loaclcam.toString())
                        val toast_control = File(
                            Environment.getExternalStorageDirectory()
                                .path + "/DCIM/virtualstream/" + "no_toast.jpg"
                        )
                        need_to_show_toast = !toast_control.exists()
                        if (toast_content != null && need_to_show_toast) {
                            try {
                                Toast.makeText(
                                    toast_content,
                                    "发现拍照\n宽：" + onemwidth + "\n高：" + onemhight + "\n格式：JPEG",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } catch (e: Exception) {
                                XposedBridge.log("【VCAM】[toast]" + e.toString())
                            }
                        }
                        val control_file = File(
                            Environment.getExternalStorageDirectory()
                                .getPath() + "/DCIM/virtualstream/" + "disable.jpg"
                        )
                        if (control_file.exists()) {
                            return
                        }

                        val pict = getBMP(video_path + "1000.bmp")
                        val temp_array = ByteArrayOutputStream()
                        pict.compress(Bitmap.CompressFormat.JPEG, 100, temp_array)
                        val jpeg_data = temp_array.toByteArray()
                        paramd.args[0] = jpeg_data
                    } catch (ee: Exception) {
                        XposedBridge.log("【VCAM】" + ee.toString())
                    }
                }
            })
    }

    private fun process_a_shot_YUV(param: XC_MethodHook.MethodHookParam) {
        try {
            XposedBridge.log("【VCAM】发现拍照YUV:" + param.args[1].toString())
        } catch (eee: Exception) {
            XposedBridge.log("【VCAM】" + eee)
        }
        val callback: Class<*>? = param.args[1].javaClass
        XposedHelpers.findAndHookMethod(
            callback,
            "onPictureTaken",
            ByteArray::class.java,
            Camera::class.java,
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                protected override fun beforeHookedMethod(paramd: MethodHookParam) {
                    try {
                        val loaclcam = paramd.args[1] as Camera
                        onemwidth = loaclcam.getParameters().getPreviewSize().width
                        onemhight = loaclcam.getParameters().getPreviewSize().height
                        XposedBridge.log("【VCAM】YUV拍照回调初始化：宽：" + onemwidth + "高：" + onemhight + "对应的类：" + loaclcam.toString())
                        val toast_control = File(
                            Environment.getExternalStorageDirectory()
                                .getPath() + "/DCIM/virtualstream/" + "no_toast.jpg"
                        )
                        need_to_show_toast = !toast_control.exists()
                        if (toast_content != null && need_to_show_toast) {
                            try {
                                Toast.makeText(
                                    toast_content,
                                    "发现拍照\n宽：" + onemwidth + "\n高：" + onemhight + "\n格式：YUV_420_888",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } catch (ee: Exception) {
                                XposedBridge.log("【VCAM】[toast]" + ee.toString())
                            }
                        }
                        val control_file = File(
                            Environment.getExternalStorageDirectory()
                                .path + "/DCIM/virtualstream/" + "disable.jpg"
                        )
                        if (control_file.exists()) {
                            return
                        }
                        input = getYUVByBitmap(getBMP(video_path + "1000.bmp"))
                        paramd.args[0] = input
                    } catch (ee: Exception) {
                        XposedBridge.log("【VCAM】" + ee.toString())
                    }
                }
            })
    }

    private fun process_callback(param: XC_MethodHook.MethodHookParam) {
        val preview_cb_class: Class<*>? = param.args[0].javaClass
        var need_stop = 0
        val control_file = File(
            Environment.getExternalStorageDirectory().getPath() + "/DCIM/virtualstream/" + "disable.jpg"
        )
        if (control_file.exists()) {
            need_stop = 1
        }
        val file = File(video_path + "virtual.mp4")
        val toast_control = File(
            Environment.getExternalStorageDirectory().getPath() + "/DCIM/virtualstream/" + "no_toast.jpg"
        )
        need_to_show_toast = !toast_control.exists()
        if (!file.exists()) {
            if (toast_content != null && need_to_show_toast) {
                try {
                    Toast.makeText(
                        toast_content,
                        "不存在替换视频\n" + toast_content!!.getPackageName() + "当前路径：" + video_path,
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (ee: Exception) {
                    XposedBridge.log("【VCAM】[toast]" + ee)
                }
            }
            need_stop = 1
        }
        val finalNeed_stop = need_stop
        XposedHelpers.findAndHookMethod(
            preview_cb_class,
            "onPreviewFrame",
            ByteArray::class.java,
            Camera::class.java,
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                protected override fun beforeHookedMethod(paramd: MethodHookParam) {
                    val localcam = paramd.args[1] as Camera
                    if (localcam == camera_onPreviewFrame) {
                        while (data_buffer == null) {
                        }
                        System.arraycopy(
                            data_buffer,
                            0,
                            paramd.args[0],
                            0,
                            min(data_buffer!!.size, (paramd.args[0] as ByteArray).size)
                        )
                    } else {
                        camera_callback_calss = preview_cb_class
                        camera_onPreviewFrame = paramd.args[1] as Camera
                        mwidth = camera_onPreviewFrame!!.getParameters().getPreviewSize().width
                        mhight = camera_onPreviewFrame!!.getParameters().getPreviewSize().height
                        val frame_Rate =
                            camera_onPreviewFrame!!.getParameters().getPreviewFrameRate()
                        XposedBridge.log("【VCAM】帧预览回调初始化：宽：" + mwidth + " 高：" + mhight + " 帧率：" + frame_Rate)
                        val toast_control = File(
                            Environment.getExternalStorageDirectory()
                                .getPath() + "/DCIM/virtualstream/" + "no_toast.jpg"
                        )
                        need_to_show_toast = !toast_control.exists()
                        if (toast_content != null && need_to_show_toast) {
                            try {
                                Toast.makeText(
                                    toast_content,
                                    "发现预览\n宽：" + mwidth + "\n高：" + mhight + "\n" + "需要视频分辨率与其完全相同",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } catch (ee: Exception) {
                                XposedBridge.log("【VCAM】[toast]" + ee.toString())
                            }
                        }
                        if (finalNeed_stop == 1) {
                            return
                        }
                        if (hw_decode_obj != null) {
                            hw_decode_obj!!.stopDecode()
                        }
                        hw_decode_obj = VideoToFrames()
                        hw_decode_obj!!.setSaveFrames("", OutputImageFormat.NV21)
                        hw_decode_obj!!.decode(video_path + "virtual.mp4")
                        System.arraycopy(
                            data_buffer,
                            0,
                            paramd.args[0],
                            0,
                            min(data_buffer!!.size, (paramd.args[0] as ByteArray).size)
                        )
                    }
                }
            })
    }

    private fun process_camera2Session_callback(callback_calss: CameraCaptureSession.StateCallback?) {
        if (callback_calss == null) {
            return
        }
        XposedHelpers.findAndHookMethod(
            callback_calss.javaClass,
            "onConfigureFailed",
            CameraCaptureSession::class.java,
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                protected override fun beforeHookedMethod(param: MethodHookParam) {
                    XposedBridge.log("【VCAM】onConfigureFailed ：" + param.args[0].toString())
                }
            })

        XposedHelpers.findAndHookMethod(
            callback_calss.javaClass,
            "onConfigured",
            CameraCaptureSession::class.java,
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                protected override fun beforeHookedMethod(param: MethodHookParam) {
                    XposedBridge.log("【VCAM】onConfigured ：" + param.args[0].toString())
                }
            })

        XposedHelpers.findAndHookMethod(
            callback_calss.javaClass,
            "onClosed",
            CameraCaptureSession::class.java,
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                protected override fun beforeHookedMethod(param: MethodHookParam) {
                    XposedBridge.log("【VCAM】onClosed ：" + param.args[0].toString())
                }
            })
    }


    //以下代码来源：https://blog.csdn.net/jacke121/article/details/73888732
    @Throws(Throwable::class)
    private fun getBMP(file: String?): Bitmap {
        return BitmapFactory.decodeFile(file)
    }

    companion object {
        var mSurface: Surface? = null
        var mSurfacetexture: SurfaceTexture? = null
        var mMediaPlayer: MediaPlayer? = null
        var fake_SurfaceTexture: SurfaceTexture? = null
        var origin_preview_camera: Camera? = null

        var camera_onPreviewFrame: Camera? = null
        var start_preview_camera: Camera? = null

        @Volatile
        var data_buffer: ByteArray? = byteArrayOf(0)
        var input: ByteArray? = null
        var mhight: Int = 0
        var mwidth: Int = 0
        var is_someone_playing: Boolean = false
        var is_hooked: Boolean = false
        var hw_decode_obj: VideoToFrames? = null
        var c2_hw_decode_obj: VideoToFrames? = null
        var c2_hw_decode_obj_1: VideoToFrames? = null
        var c1_fake_texture: SurfaceTexture? = null
        var c1_fake_surface: Surface? = null
        var ori_holder: SurfaceHolder? = null
        var mplayer1: MediaPlayer? = null
        var mvirtualstream: Camera? = null
        var is_first_hook_build: Boolean = true

        var onemhight: Int = 0
        var onemwidth: Int = 0
        var camera_callback_calss: Class<*>? = null

        var video_path: String = "/storage/emulated/0/DCIM/virtualstream/"

        var c2_preview_Surfcae: Surface? = null
        var c2_preview_Surfcae_1: Surface? = null
        var c2_reader_Surfcae: Surface? = null
        var c2_reader_Surfcae_1: Surface? = null
        var c2_player: MediaPlayer? = null
        var c2_player_1: MediaPlayer? = null
        var c2_virtual_surface: Surface? = null
        var c2_virtual_surfaceTexture: SurfaceTexture? = null
        var c2_state_cb: CameraDevice.StateCallback? = null
        var c2_builder: CaptureRequest.Builder? = null
        var fake_sessionConfiguration: SessionConfiguration? = null
        var sessionConfiguration: SessionConfiguration? = null
        var outputConfiguration: OutputConfiguration? = null
        var c2_state_callback: Class<*>? = null
        private fun rgb2YCbCr420(pixels: IntArray, width: Int, height: Int): ByteArray {
            val len = width * height
            // yuv格式数组大小，y亮度占len长度，u,v各占len/4长度。
            val yuv = ByteArray(len * 3 / 2)
            var y: Int
            var u: Int
            var v: Int
            for (i in 0..<height) {
                for (j in 0..<width) {
                    val rgb = (pixels[i * width + j]) and 0x00FFFFFF
                    val r = rgb and 0xFF
                    val g = (rgb shr 8) and 0xFF
                    val b = (rgb shr 16) and 0xFF
                    // 套用公式
                    y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                    u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                    v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                    y = if (y < 16) 16 else (min(y, 255))
                    u = if (u < 0) 0 else (min(u, 255))
                    v = if (v < 0) 0 else (min(v, 255))
                    // 赋值
                    yuv[i * width + j] = y.toByte()
                    yuv[len + (i shr 1) * width + (j and 1.inv())] = u.toByte()
                    yuv[len + +(i shr 1) * width + (j and 1.inv()) + 1] = v.toByte()
                }
            }
            return yuv
        }

        private fun getYUVByBitmap(bitmap: Bitmap?): ByteArray? {
            if (bitmap == null) {
                return null
            }
            val width = bitmap.getWidth()
            val height = bitmap.getHeight()
            val size = width * height
            val pixels = IntArray(size)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            return rgb2YCbCr420(pixels, width, height)
        }
    }
}

