package com.bicubictwice.videoalphatest

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.nio.ByteBuffer

internal class VideoDataSourceImpl(context: Context, uri: Uri) : VideoDataSource {

    private val mediaExtractor = MediaExtractor().apply {
        setDataSource(context, uri, null)
        setVideoTrack()
    }

    private var mediaFormat: MediaFormat? = null

    private var initialSampleTime: Long = 0L

    private val dataBuffer = ByteBuffer
        .allocate(SAMPLE_DATA_BUFFER_SIZE)
        .apply { limit(0) }

    override fun getMediaFormat(): MediaFormat {
        return mediaFormat!!
    }

    override fun getNextSampleData(): ByteBuffer {
        if (!dataBuffer.hasRemaining()) {
            mediaExtractor.readSampleData(dataBuffer, 0)
            if (!mediaExtractor.advance()) {
                mediaExtractor.seekTo(initialSampleTime, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            }
        }
        return dataBuffer
    }

    private fun MediaExtractor.setVideoTrack() {
        val availableMimeTypes =
            (0 until trackCount).mapNotNull { getTrackFormat(it).getString(MediaFormat.KEY_MIME) }

        val videoTrackIndex = availableMimeTypes
            .indexOfFirst { it.startsWith("video/") }
            .takeIf { it >= 0 }

        this.selectTrack(requireNotNull(videoTrackIndex))

        mediaFormat = this.getTrackFormat(videoTrackIndex)
        initialSampleTime = this.sampleTime
    }
}

private const val SAMPLE_DATA_BUFFER_SIZE = 100_000
