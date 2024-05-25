package com.bicubictwice.videoalphatest

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.media.Image
import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Handler
import android.os.HandlerThread
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.runBlocking
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.random.Random

internal class VideoFramesDecoderImpl(private val mediaFormat: MediaFormat) : VideoFramesDecoder {

    private val mimeType = requireNotNull(mediaFormat.getString(MediaFormat.KEY_MIME))
    private val frameRate = requireNotNull(mediaFormat.getInteger(MediaFormat.KEY_FRAME_RATE))

    private val nowMillis: Long
        get() = System.currentTimeMillis()

    private val rnd = Random(nowMillis)

    override fun getOutputFramesFlow(inputSampleDataCallback: () -> ByteBuffer): Flow<Bitmap> {
        return channelFlow {
            val threadName = "${this.javaClass.name}_HandlerThread_${rnd.nextLong()}"
            val handlerThread = HandlerThread(threadName).apply { start() }
            val handler = Handler(handlerThread.looper)

            val decoder = MediaCodec.createDecoderByType(mimeType)

            val frameIntervalMillis = (1000f / frameRate).toLong()
            var nextFrameTimestamp = nowMillis

            val callback = object : MediaCodec.Callback() {

                override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
                    runCatching {
                        val sampleDataBuffer = inputSampleDataCallback()
                        val bytesCopied = sampleDataBuffer.remaining()
                        codec.getInputBuffer(index)?.put(sampleDataBuffer)
                        codec.queueInputBuffer(index, 0, bytesCopied, 0, 0)
                    }
                }

                override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
                    runCatching {
                        codec.getOutputImage(index)?.let { frame ->
                            val bitmap = frame.toBitmap()
                            val diff = (nextFrameTimestamp - nowMillis).coerceAtLeast(0L)
                            runBlocking { delay(diff) }
                            trySend(bitmap)
                            nextFrameTimestamp = nowMillis + frameIntervalMillis
                        }
                        codec.releaseOutputBuffer(index, false)
                    }
                }

                override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {}

                override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {}
            }

            decoder.setCallback(callback, handler)
            decoder.configure(mediaFormat, null, null, 0)
            decoder.start()

            awaitClose {
                decoder.stop()
                decoder.release()
            }
        }.conflate()
    }

    private fun Image.toBitmap(): Bitmap {
        require(this.format == ImageFormat.YUV_420_888)
        val buffers = getBuffers(requiredImageMaxSideSize = max(this.width, this.height))
        val bitmapWithAlpha = this.getBitmapWithAlpha(buffers)
        return Bitmap
            .createBitmap(this.width / 2, this.height, Bitmap.Config.ARGB_8888)
            .apply { copyPixelsFromBuffer(ByteBuffer.wrap(bitmapWithAlpha)) }
    }

    private fun Image.getBitmapWithAlpha(buffers: Buffers): ByteArray {
        val yBuffer = this.planes[0].buffer
        yBuffer.get(buffers.yBytes, 0, yBuffer.remaining())

        val uBuffer = this.planes[1].buffer
        uBuffer.get(buffers.uBytes, 0, uBuffer.remaining())

        val vBuffer = this.planes[2].buffer
        vBuffer.get(buffers.vBytes, 0, vBuffer.remaining())

        val yRowStride = this.planes[0].rowStride
        val yPixelStride = this.planes[0].pixelStride

        val uvRowStride = this.planes[1].rowStride
        val uvPixelStride = this.planes[1].pixelStride

        val halfWidth = this.width / 2

        for (y in 0 until this.height) {
            for (x in 0 until halfWidth) {

                val yIndex = y * yRowStride + x * yPixelStride
                val yValue = (buffers.yBytes[yIndex].toInt() and 0xff) - 16

                val uvIndex = (y / 2) * uvRowStride + (x / 2) * uvPixelStride
                val uValue = (buffers.uBytes[uvIndex].toInt() and 0xff) - 128
                val vValue = (buffers.vBytes[uvIndex].toInt() and 0xff) - 128

                val r = 1.164f * yValue + 1.596f * vValue
                val g = 1.164f * yValue - 0.392f * uValue - 0.813f * vValue
                val b = 1.164f * yValue + 2.017f * uValue

                val yAlphaIndex = yIndex + halfWidth * yPixelStride
                val yAlphaValue = (buffers.yBytes[yAlphaIndex].toInt() and 0xff) - 16

                val uvAlphaIndex = uvIndex + this.width * uvPixelStride
                val vAlphaValue = (buffers.vBytes[uvAlphaIndex].toInt() and 0xff) - 128

                val alpha = 1.164f * yAlphaValue + 1.596f * vAlphaValue

                val pixelIndex = x * 4 + y * 4 * halfWidth

                buffers.bitmapBytes[pixelIndex + 0] = (r * alpha / 255f).toInt().coerceIn(0, 255).toByte()
                buffers.bitmapBytes[pixelIndex + 1] = (g * alpha / 255f).toInt().coerceIn(0, 255).toByte()
                buffers.bitmapBytes[pixelIndex + 2] = (b * alpha / 255f).toInt().coerceIn(0, 255).toByte()
                buffers.bitmapBytes[pixelIndex + 3] = alpha.toInt().coerceIn(0, 255).toByte()
            }
        }

        return buffers.bitmapBytes
    }

    private fun getBuffers(requiredImageMaxSideSize: Int): Buffers {
        return when {
            buffers.imageMaxSideSize >= requiredImageMaxSideSize -> buffers
            else -> Buffers(imageMaxSideSize = requiredImageMaxSideSize).also { buffers = it }
        }
    }

    private var buffers = Buffers(imageMaxSideSize = 1000)

    private class Buffers(val imageMaxSideSize: Int) {
        private val size = imageMaxSideSize * imageMaxSideSize
        val yBytes = ByteArray(size)
        val uBytes = ByteArray(size)
        val vBytes = ByteArray(size)
        val bitmapBytes = ByteArray(size * 4)
    }
}
