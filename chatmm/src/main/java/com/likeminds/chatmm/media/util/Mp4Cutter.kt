package com.likeminds.chatmm.media.util

import android.annotation.SuppressLint
import android.content.Context
import android.media.*
import android.net.Uri
import android.util.SparseIntArray
import com.googlecode.mp4parser.FileDataSourceViaHeapImpl
import com.googlecode.mp4parser.authoring.Track
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack
import com.likeminds.chatmm.media.customviews.interfaces.OnTrimVideoListener
import com.likeminds.chatmm.media.model.VideoTrimExtras
import com.likeminds.chatmm.utils.file.util.FileUtil
import com.likeminds.likemindschat.helper.LMChatLogger
import com.likeminds.likemindschat.helper.model.LMSeverity
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.*

object Mp4Cutter {

    private const val DEFAULT_BUFFER_SIZE = 1024 * 1024

    fun startTrim(
        context: Context,
        srcUri: Uri,
        destFile: File,
        startMs: Long,
        endMs: Long,
        callback: OnTrimVideoListener?,
        videoTrimExtras: VideoTrimExtras?
    ) {
        var videoTrimmed = false
        if (!videoTrimmed) {
            try {
                // Trim the video using mp4Parser
                val inputFilePath = FileUtil.getRealPath(context, srcUri).path
                videoTrimmed = genVideoUsingMp4Parser(inputFilePath, destFile, startMs, endMs)
            } catch (e: Exception) {
                LMChatLogger.getInstance()?.handleException(
                    e.message ?: "",
                    e.stackTraceToString(),
                    LMSeverity.EMERGENCY
                )
            }
        }
        if (!videoTrimmed) {
            try {
                // Trim the video using muxer
                videoTrimmed = genVideoUsingMuxer(context, srcUri, destFile, startMs, endMs)
            } catch (e: Exception) {
                LMChatLogger.getInstance()?.handleException(
                    e.message ?: "",
                    e.stackTraceToString(),
                    LMSeverity.EMERGENCY
                )
            }
        }
        if (videoTrimmed) {
            callback?.getResult(Uri.fromFile(destFile), videoTrimExtras)
        } else {
            callback?.onFailed(videoTrimExtras)
        }
    }

    @Throws(Exception::class)
    private fun genVideoUsingMp4Parser(
        filePath: String?,
        dst: File,
        startMs: Long,
        endMs: Long
    ): Boolean {
        if (filePath.isNullOrBlank() || !File(filePath).exists())
            return false
        val movie = MovieCreator.build(FileDataSourceViaHeapImpl(filePath))
        val tracks = movie.tracks
        movie.tracks = LinkedList()
        // remove all tracks we will create new tracks from the old
        var startTime1 = (startMs / 1000).toDouble()
        var endTime1 = (endMs / 1000).toDouble()
        var timeCorrected = false
        // Here we try to find a track that has sync samples. Since we can only start decoding
        // at such a sample we SHOULD make sure that the start of the new fragment is exactly
        // such a frame
        for (track in tracks) {
            if (track.syncSamples != null && track.syncSamples.isNotEmpty()) {
                if (timeCorrected) return false
                startTime1 = correctTimeToSyncSample(track, startTime1, false)
                endTime1 = correctTimeToSyncSample(track, endTime1, true)
                timeCorrected = true
            }
        }
        for (track in tracks) {
            var currentSample: Long = 0
            var currentTime = 0.0
            var lastTime = -1.0
            var startSample: Long = -1
            var endSample: Long = -1
            track.sampleDurations.forEach { element ->
                if (currentTime > lastTime && currentTime <= startTime1) {
                    // current sample is still before the new starttime
                    startSample = currentSample
                }
                if (currentTime > lastTime && currentTime <= endTime1) {
                    // current sample is after the new start time and still before the new endtime
                    endSample = currentSample
                }
                lastTime = currentTime
                currentTime += element.toDouble() / track.trackMetaData.timescale.toDouble()
                ++currentSample
            }
            movie.addTrack(CroppedTrack(track, startSample, endSample))
        }
        if (!dst.exists()) {
            dst.createNewFile()
        }
        val container = DefaultMp4Builder().build(movie)
        val outputStream = FileOutputStream(dst)
        val fileChannel = outputStream.channel
        container.writeContainer(fileChannel)
        fileChannel.close()
        outputStream.close()
        return true
    }

    @SuppressLint("WrongConstant")
    @Throws(Exception::class)
    private fun genVideoUsingMuxer(
        context: Context, srcUri: Uri, destFile: File, startMs: Long, endMs: Long
    ): Boolean {
        val extractor = MediaExtractor()
        val fileDescriptor =
            context.contentResolver.openFileDescriptor(srcUri, "r")!!.fileDescriptor
        extractor.setDataSource(fileDescriptor)
        val trackCount = extractor.trackCount
        // Set up MediaMuxer for the destination.
        val muxer = MediaMuxer(destFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        // Set up the tracks and retrieve the max buffer size for selected tracks.
        val indexMap = SparseIntArray(trackCount)
        var bufferSize = -1
        try {
            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                var selectCurrentTrack = false
                if (mime?.startsWith("audio/") == true) {
                    selectCurrentTrack = true
                } else if (mime?.startsWith("video/") == true) {
                    selectCurrentTrack = true
                }
                if (selectCurrentTrack) {
                    extractor.selectTrack(i)
                    val dstIndex = muxer.addTrack(format)
                    indexMap.put(i, dstIndex)
                    if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                        val newSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                        bufferSize = if (newSize > bufferSize) newSize else bufferSize
                    }
                }
            }
            if (bufferSize < 0)
                bufferSize = DEFAULT_BUFFER_SIZE
            // Set up the orientation and starting time for extractor.
            val retrieverSrc = MediaMetadataRetriever()
            retrieverSrc.setDataSource(fileDescriptor)
            val degreesString =
                retrieverSrc.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            if (degreesString != null) {
                val degrees = Integer.parseInt(degreesString)
                if (degrees >= 0)
                    muxer.setOrientationHint(degrees)
            }
            if (startMs > 0)
                extractor.seekTo(startMs * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            // Copy the samples from MediaExtractor to MediaMuxer. We will loop
            // for copying each sample and stop when we get to the end of the source
            // file or exceed the end time of the trimming.
            val offset = 0
            var trackIndex: Int
            val dstBuf = ByteBuffer.allocate(bufferSize)
            val bufferInfo = MediaCodec.BufferInfo()
            muxer.start()
            while (true) {
                bufferInfo.offset = offset
                bufferInfo.size = extractor.readSampleData(dstBuf, offset)
                if (bufferInfo.size < 0) {
                    bufferInfo.size = 0
                    break
                } else {
                    bufferInfo.presentationTimeUs = extractor.sampleTime
                    if (endMs > 0 && bufferInfo.presentationTimeUs > endMs * 1000) {
                        break
                    } else {
                        bufferInfo.flags = extractor.sampleFlags
                        trackIndex = extractor.sampleTrackIndex
                        muxer.writeSampleData(indexMap.get(trackIndex), dstBuf, bufferInfo)
                        extractor.advance()
                    }
                }
            }
            muxer.stop()
            return true
        } catch (e: Exception) {
            LMChatLogger.getInstance()?.handleException(
                e.message ?: "",
                e.stackTraceToString(),
                LMSeverity.EMERGENCY
            )
            e.printStackTrace()
        } finally {
            muxer.release()
        }
        return false
    }

    private fun correctTimeToSyncSample(track: Track, cutHere: Double, next: Boolean): Double {
        val timeOfSyncSamples = DoubleArray(track.syncSamples.size)
        var currentSample: Long = 0
        var currentTime = 0.0
        track.sampleDurations.forEach { element ->
            if (Arrays.binarySearch(track.syncSamples, currentSample + 1) >= 0) {
                // samples always start with 1 but we start with zero therefore +1
                timeOfSyncSamples[Arrays.binarySearch(track.syncSamples, currentSample + 1)] =
                    currentTime
            }
            currentTime += element.toDouble() / track.trackMetaData.timescale.toDouble()
            ++currentSample
        }
        var previous = 0.0
        for (timeOfSyncSample in timeOfSyncSamples) {
            if (timeOfSyncSample > cutHere) {
                return if (next) {
                    timeOfSyncSample
                } else {
                    previous
                }
            }
            previous = timeOfSyncSample
        }
        return timeOfSyncSamples[timeOfSyncSamples.size - 1]
    }
}