package com.bicubictwice.videoalphatest

import android.graphics.Bitmap
import kotlinx.coroutines.flow.Flow
import java.nio.ByteBuffer

interface VideoFramesDecoder {
    fun getOutputFramesFlow(inputSampleDataCallback: () -> ByteBuffer): Flow<Bitmap>
}
