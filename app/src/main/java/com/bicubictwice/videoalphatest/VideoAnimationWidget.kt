package com.bicubictwice.videoalphatest

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.RawRes
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext

@Composable
fun VideoAnimationWidget(
    @RawRes resourceId: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var lastFrame by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(resourceId) {
        withContext(Dispatchers.IO) {
            val videoDataSource = VideoDataSourceFactory.getVideoDataSource(
                context = context,
                uri = context.getUri(resourceId = resourceId)
            )
            val videoFramesDecoder = VideoFramesDecoderFactory.getVideoFramesDecoder(
                mediaFormat = videoDataSource.getMediaFormat()
            )

            videoFramesDecoder
                .getOutputFramesFlow(inputSampleDataCallback = { videoDataSource.getNextSampleData() })
                .collectLatest { lastFrame = it }
        }
    }

    Canvas(modifier = modifier) {
        lastFrame?.let { frame ->
            drawImage(
                image = frame.asImageBitmap(),
                topLeft = Offset(
                    x = (size.width - frame.width) / 2,
                    y = (size.height - frame.height) / 2
                ),
                blendMode = BlendMode.SrcOver
            )
        }
    }
}

private fun Context.getUri(@RawRes resourceId: Int): Uri {
    return Uri.Builder()
        .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
        .authority(this.applicationContext.packageName)
        .appendPath("$resourceId")
        .build()
}
