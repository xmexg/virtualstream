package org.looom.virtualstream.vcam
import android.annotation.SuppressLint
import android.graphics.ImageFormat
import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo.CodecCapabilities
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import de.robv.android.xposed.XposedBridge
import java.io.File
import java.io.IOException
import java.util.concurrent.LinkedBlockingQueue

//以下代码修改自 https://github.com/zhantong/Android-VideoToImages
class VideoToFrames : Runnable {
    private val decodeColorFormat = CodecCapabilities.COLOR_FormatYUV420Flexible

    private var mQueue: LinkedBlockingQueue<ByteArray?>? = null
    private var outputImageFormat: OutputImageFormat? = null
    private var stopDecode = false

    private var videoFilePath: String? = null
    private var throwable: Throwable? = null
    private var childThread: Thread? = null
    private var play_surf: Surface? = null

    private var callback: Callback? = null

    interface Callback {
        fun onFinishDecode()

        fun onDecodeFrame(index: Int)
    }

    fun setCallback(callback: Callback?) {
        this.callback = callback
    }

    fun setEnqueue(queue: LinkedBlockingQueue<ByteArray?>?) {
        mQueue = queue
    }

    //设置输出位置，没啥用
    @Throws(IOException::class)
    fun setSaveFrames(dir: String?, imageFormat: OutputImageFormat?) {
        outputImageFormat = imageFormat
    }

    fun set_surfcae(player_surface: Surface?) {
        if (player_surface != null) {
            play_surf = player_surface
        }
    }

    fun stopDecode() {
        stopDecode = true
    }

    @Throws(Throwable::class)
    fun decode(videoFilePath: String) {
        this.videoFilePath = videoFilePath
        if (childThread == null) {
            childThread = Thread(this, "decode")
            childThread!!.start()
            if (throwable != null) {
                throw throwable!!
            }
        }
    }

    override fun run() {
        try {
            videoDecode(videoFilePath!!)
        } catch (t: Throwable) {
            throwable = t
        }
    }

    @SuppressLint("WrongConstant")
    @Throws(IOException::class)
    fun videoDecode(videoFilePath: String) {
        XposedBridge.log("【VCAM】【decoder】开始解码")
        var extractor: MediaExtractor? = null
        var decoder: MediaCodec? = null
        try {
            val videoFile = File(videoFilePath)
            extractor = MediaExtractor()
            extractor.setDataSource(videoFilePath)
            val trackIndex = selectTrack(extractor)
            if (trackIndex < 0) {
                XposedBridge.log("【VCAM】【decoder】No video track found in " + videoFilePath)
            }
            extractor.selectTrack(trackIndex)
            val mediaFormat = extractor.getTrackFormat(trackIndex)
            val mime = mediaFormat.getString(MediaFormat.KEY_MIME)
            decoder = MediaCodec.createDecoderByType(mime!!)
            showSupportedColorFormat(decoder.getCodecInfo().getCapabilitiesForType(mime))
            if (isColorFormatSupported(
                    decodeColorFormat,
                    decoder.getCodecInfo().getCapabilitiesForType(mime)
                )
            ) {
                mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, decodeColorFormat)
                XposedBridge.log("【VCAM】【decoder】set decode color format to type " + decodeColorFormat)
            } else {
                Log.i(
                    TAG,
                    "unable to set decode color format, color format type " + decodeColorFormat + " not supported"
                )
                XposedBridge.log("【VCAM】【decoder】unable to set decode color format, color format type " + decodeColorFormat + " not supported")
            }
            decodeFramesToImage(decoder, extractor, mediaFormat)
            decoder.stop()
            while (!stopDecode) {
                extractor.seekTo(0, 0)
                decodeFramesToImage(decoder, extractor, mediaFormat)
                decoder.stop()
            }
        } catch (e: Exception) {
            XposedBridge.log("【VCAM】[videofile]" + e.toString())
        } finally {
            if (decoder != null) {
                decoder.stop()
                decoder.release()
                decoder = null
            }
            if (extractor != null) {
                extractor.release()
                extractor = null
            }
        }
    }

    private fun showSupportedColorFormat(caps: CodecCapabilities) {
        print("supported color format: ")
        for (c in caps.colorFormats) {
            print(c.toString() + "\t")
        }
        println()
    }

    private fun isColorFormatSupported(colorFormat: Int, caps: CodecCapabilities): Boolean {
        for (c in caps.colorFormats) {
            if (c == colorFormat) {
                return true
            }
        }
        return false
    }

    private fun decodeFramesToImage(
        decoder: MediaCodec,
        extractor: MediaExtractor,
        mediaFormat: MediaFormat
    ) {
        var is_first = false
        var startWhen: Long = 0
        val info = MediaCodec.BufferInfo()
        decoder.configure(mediaFormat, play_surf, null, 0)
        var sawInputEOS = false
        var sawOutputEOS = false
        decoder.start()
        val width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH)
        val height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT)
        var outputFrameCount = 0
        while (!sawOutputEOS && !stopDecode) {
            if (!sawInputEOS) {
                val inputBufferId = decoder.dequeueInputBuffer(DEFAULT_TIMEOUT_US)
                if (inputBufferId >= 0) {
                    val inputBuffer = decoder.getInputBuffer(inputBufferId)
                    val sampleSize = extractor.readSampleData(inputBuffer!!, 0)
                    if (sampleSize < 0) {
                        decoder.queueInputBuffer(
                            inputBufferId,
                            0,
                            0,
                            0L,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        sawInputEOS = true
                    } else {
                        val presentationTimeUs = extractor.getSampleTime()
                        decoder.queueInputBuffer(
                            inputBufferId,
                            0,
                            sampleSize,
                            presentationTimeUs,
                            0
                        )
                        extractor.advance()
                    }
                }
            }
            val outputBufferId = decoder.dequeueOutputBuffer(info, DEFAULT_TIMEOUT_US)
            if (outputBufferId >= 0) {
                if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    sawOutputEOS = true
                }
                val doRender = (info.size != 0)
                if (doRender) {
                    outputFrameCount++
                    if (callback != null) {
                        callback!!.onDecodeFrame(outputFrameCount)
                    }
                    if (!is_first) {
                        startWhen = System.currentTimeMillis()
                        is_first = true
                    }
                    if (play_surf == null) {
                        val image = decoder.getOutputImage(outputBufferId)
                        val buffer = image!!.getPlanes()[0].getBuffer()
                        val arr = ByteArray(buffer.remaining())
                        buffer.get(arr)
                        if (mQueue != null) {
                            try {
                                mQueue!!.put(arr)
                            } catch (e: InterruptedException) {
                                XposedBridge.log("【VCAM】" + e.toString())
                            }
                        }
                        if (outputImageFormat != null) {
                            HookMain.data_buffer =
                                Companion.getDataFromImage(image, COLOR_FormatNV21)
                        }
                        image.close()
                    }
                    val sleepTime =
                        info.presentationTimeUs / 1000 - (System.currentTimeMillis() - startWhen)
                    if (sleepTime > 0) {
                        try {
                            Thread.sleep(sleepTime)
                        } catch (e: InterruptedException) {
                            XposedBridge.log("【VCAM】" + e.toString())
                            XposedBridge.log("【VCAM】线程延迟出错")
                        }
                    }
                    decoder.releaseOutputBuffer(outputBufferId, true)
                }
            }
        }
        if (callback != null) {
            callback!!.onFinishDecode()
        }
    }

    companion object {
        private const val TAG = "VideoToFrames"
        private const val VERBOSE = false
        private const val DEFAULT_TIMEOUT_US: Long = 10000

        private const val COLOR_FormatI420 = 1
        private const val COLOR_FormatNV21 = 2


        private fun selectTrack(extractor: MediaExtractor): Int {
            val numTracks = extractor.getTrackCount()
            for (i in 0..<numTracks) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime!!.startsWith("video/")) {
                    if (VERBOSE) {
                        Log.d(TAG, "Extractor selected track " + i + " (" + mime + "): " + format)
                    }
                    return i
                }
            }
            return -1
        }

        private fun isImageFormatSupported(image: Image): Boolean {
            val format = image.getFormat()
            when (format) {
                ImageFormat.YUV_420_888, ImageFormat.NV21, ImageFormat.YV12 -> return true
            }
            return false
        }

        private fun getDataFromImage(image: Image, colorFormat: Int): ByteArray {
            require(!(colorFormat != COLOR_FormatI420 && colorFormat != COLOR_FormatNV21)) { "only support COLOR_FormatI420 " + "and COLOR_FormatNV21" }
            if (!isImageFormatSupported(image)) {
                throw RuntimeException("can't convert Image to byte array, format " + image.getFormat())
            }
            val crop = image.getCropRect()
            val format = image.getFormat()
            val width = crop.width()
            val height = crop.height()
            val planes = image.getPlanes()
            val data = ByteArray(width * height * ImageFormat.getBitsPerPixel(format) / 8)
            val rowData = ByteArray(planes[0]!!.getRowStride())
            if (VERBOSE) Log.v(TAG, "get data from " + planes.size + " planes")
            var channelOffset = 0
            var outputStride = 1
            for (i in planes.indices) {
                when (i) {
                    0 -> {
                        channelOffset = 0
                        outputStride = 1
                    }

                    1 -> if (colorFormat == COLOR_FormatI420) {
                        channelOffset = width * height
                        outputStride = 1
                    } else if (colorFormat == COLOR_FormatNV21) {
                        channelOffset = width * height + 1
                        outputStride = 2
                    }

                    2 -> if (colorFormat == COLOR_FormatI420) {
                        channelOffset = (width * height * 1.25).toInt()
                        outputStride = 1
                    } else if (colorFormat == COLOR_FormatNV21) {
                        channelOffset = width * height
                        outputStride = 2
                    }
                }
                val buffer = planes[i]!!.getBuffer()
                val rowStride = planes[i]!!.getRowStride()
                val pixelStride = planes[i]!!.getPixelStride()
                if (VERBOSE) {
                    Log.v(TAG, "pixelStride " + pixelStride)
                    Log.v(TAG, "rowStride " + rowStride)
                    Log.v(TAG, "width " + width)
                    Log.v(TAG, "height " + height)
                    Log.v(TAG, "buffer size " + buffer.remaining())
                }
                val shift = if (i == 0) 0 else 1
                val w = width shr shift
                val h = height shr shift
                buffer.position(rowStride * (crop.top shr shift) + pixelStride * (crop.left shr shift))
                for (row in 0..<h) {
                    val length: Int
                    if (pixelStride == 1 && outputStride == 1) {
                        length = w
                        buffer.get(data, channelOffset, length)
                        channelOffset += length
                    } else {
                        length = (w - 1) * pixelStride + 1
                        buffer.get(rowData, 0, length)
                        for (col in 0..<w) {
                            data[channelOffset] = rowData[col * pixelStride]
                            channelOffset += outputStride
                        }
                    }
                    if (row < h - 1) {
                        buffer.position(buffer.position() + rowStride - length)
                    }
                }
                if (VERBOSE) Log.v(TAG, "Finished reading data from plane " + i)
            }
            return data
        }
    }
}

enum class OutputImageFormat(friendlyName: String) {
    I420("I420"),
    NV21("NV21"),
    JPEG("JPEG");

    private val friendlyName: String?

    init {
        this.friendlyName = friendlyName
    }

    override fun toString(): String {
        return friendlyName!!
    }
}



