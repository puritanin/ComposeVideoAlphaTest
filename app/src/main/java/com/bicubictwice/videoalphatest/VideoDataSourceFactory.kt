package com.bicubictwice.videoalphatest

import android.content.Context
import android.net.Uri

object VideoDataSourceFactory {

    fun getVideoDataSource(context: Context, uri: Uri): VideoDataSource {
        return VideoDataSourceImpl(context = context, uri = uri)
    }
}
