package com.bicubictwice.videoalphatest

import android.media.MediaFormat
import java.nio.ByteBuffer

interface VideoDataSource {
    fun getMediaFormat(): MediaFormat
    fun getNextSampleData(): ByteBuffer
}
