package com.bicubictwice.videoalphatest

import android.media.MediaFormat

object VideoFramesDecoderFactory {

    fun getVideoFramesDecoder(mediaFormat: MediaFormat): VideoFramesDecoder {
        return VideoFramesDecoderImpl(mediaFormat = mediaFormat)
    }
}
