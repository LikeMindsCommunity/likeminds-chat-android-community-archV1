package com.likeminds.chatmm.media

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.MediaStore.VOLUME_EXTERNAL
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.annotation.WorkerThread
import com.annimon.stream.Stream
import com.likeminds.chatmm.media.model.*
import com.likeminds.chatmm.media.util.MediaUtils
import com.likeminds.chatmm.utils.DateUtil
import com.likeminds.chatmm.utils.ValueUtils
import com.likeminds.chatmm.utils.ValueUtils.getMediaType
import com.likeminds.chatmm.utils.ValueUtils.getMimeType
import com.likeminds.chatmm.utils.downloader.DownloadUtil
import com.likeminds.chatmm.utils.file.util.FileUtil
import com.likeminds.chatmm.utils.file.util.FileUtil.isLargeFile
import com.likeminds.chatmm.utils.file.util.FileUtil.isSmallFile
import com.likeminds.chatmm.utils.model.ITEM_MEDIA_PICKER_AUDIO
import com.likeminds.chatmm.utils.model.ITEM_MEDIA_PICKER_DOCUMENT
import com.likeminds.likemindschat.helper.LMChatLogger
import com.likeminds.likemindschat.helper.model.LMSeverity
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.*
import javax.inject.Inject

/**
 * Handles the retrieval of media present on the user's device.
 */

@SuppressLint("InlinedApi")
class MediaRepository @Inject constructor() {

    companion object {
        private const val CAMERA = "Camera"
        private const val ALL_MEDIA_BUCKET_ID = "com.collabmates.ALL_MEDIA"
    }

    /**
     * Retrieves a list of folders that contain media.
     */
    fun getLocalFolders(
        context: Context, mediaTypes: List<String>,
        callback: (folders: List<MediaFolderViewData>) -> Unit,
    ) {
        callback(getFolders(context, mediaTypes))
    }

    /**
     * Retrieves a list of media items (images and videos) that are present in the specified bucket.
     */
    fun getMediaInBucket(
        context: Context, bucketId: String, mediaTypes: List<String>,
        callback: (medias: List<MediaViewData>) -> Unit,
    ) {
        callback(getMediaInBucket(context, bucketId, mediaTypes))
    }

    /**
     * Retrieves a list of all the document files from local storage.
     */
    fun getLocalDocumentFiles(context: Context, callback: (medias: List<MediaViewData>) -> Unit) {
        callback(getAllDocumentFiles(context))
    }

    /**
     * Retrieves a list of all the audio files from local storage.
     */
    fun getLocalAudioFiles(context: Context, callback: (medias: List<MediaViewData>) -> Unit) {
        callback(getAllAudioFiles(context))
    }

    /**
     * Retrieves basic details of shared Uri from local storage.
     */
    fun getLocalUriDetail(
        context: Context, contentUri: Uri, callback: (media: MediaViewData?) -> Unit,
    ) {
        callback(getUriDetail(context, contentUri))
    }

    /**
     * Retrieves basic details of list of shared Uris from local storage.
     */
    fun getLocalUrisDetails(
        context: Context, contentUris: List<Uri>, callback: (medias: List<MediaViewData>) -> Unit,
    ) {
        callback(getUriDetails(context, contentUris))
    }

    @WorkerThread
    private fun getFolders(context: Context, mediaTypes: List<String>): List<MediaFolderViewData> {
        val mediaFolders = ArrayList<MediaFolderViewData>()
        var data: Triple<MutableMap<String, FolderData>, String?, Uri?>? = null

        when {
            InternalMediaType.isBothImageAndVideo(mediaTypes) -> {
                val imageFolders = getFolders(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                val videoFolders = getFolders(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                data = getFoldersMap(imageFolders, videoFolders)
            }
            InternalMediaType.isImage(mediaTypes) -> {
                val imageFolders = getFolders(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                data = getFoldersMap(imageFolders, null)
            }
            InternalMediaType.isVideo(mediaTypes) -> {
                val videoFolders = getFolders(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                data = getFoldersMap(null, videoFolders)
            }
        }

        if (data != null) {
            val cameraFolder = if (data.second != null) data.first.remove(data.second) else null
            mediaFolders.addAll(
                Stream.of(data.first.values).map { folder ->
                    val folderType =
                        if (folder.getFolderTitle() == DownloadUtil.DOWNLOAD_DIRECTORY) {
                            MediaFolderType.LIKEMINDS
                        } else {
                            MediaFolderType.NORMAL
                        }
                    MediaFolderViewData.Builder()
                        .thumbnailUri(folder.thumbnail)
                        .title(folder.getFolderTitle())
                        .itemCount(folder.count)
                        .bucketId(folder.bucketId)
                        .folderType(folderType).build()
                }.toList().sortedWith(
                    compareBy({ it.title != DownloadUtil.DOWNLOAD_DIRECTORY }, { it.title })
                )
            )

            if (data.third != null) {
                var allMediaCount = Stream.of(mediaFolders).reduce(
                    0
                ) { count: Int, folder: MediaFolderViewData -> count + folder.itemCount } ?: 0
                if (cameraFolder != null) {
                    allMediaCount += cameraFolder.count
                }
                mediaFolders.add(
                    0, MediaFolderViewData.Builder()
                        .thumbnailUri(data.third)
                        .title("All media")
                        .itemCount(allMediaCount)
                        .bucketId(ALL_MEDIA_BUCKET_ID)
                        .folderType(MediaFolderType.NORMAL)
                        .build()
                )
            }
            if (cameraFolder != null) {
                mediaFolders.add(
                    0, MediaFolderViewData.Builder()
                        .thumbnailUri(cameraFolder.thumbnail)
                        .title(cameraFolder.getFolderTitle())
                        .itemCount(cameraFolder.count)
                        .bucketId(cameraFolder.bucketId)
                        .folderType(MediaFolderType.CAMERA)
                        .build()
                )
            }
        }
        return mediaFolders
    }

    /**
     * @return Triple<A,B,C> where
     * A denotes All available folders map
     * B denotes Camera bucket id if there is any
     * C denotes AllMedia bucket Thumbnail Uri if there is any
     * */
    private fun getFoldersMap(
        imageFolders: FolderResult?,
        videoFolders: FolderResult?,
    ): Triple<MutableMap<String, FolderData>, String?, Uri?> {
        val folders: MutableMap<String, FolderData> = HashMap()
        if (imageFolders != null) {
            updateFoldersMap(imageFolders, folders)
        }

        if (videoFolders != null) {
            updateFoldersMap(videoFolders, folders)
        }

        val cameraBucketId = imageFolders?.cameraBucketId ?: videoFolders?.cameraBucketId
        val allMediaThumbnail =
            if (imageFolders?.thumbnailTimestamp ?: 0 > videoFolders?.thumbnailTimestamp ?: 0) {
                imageFolders?.thumbnail
            } else {
                videoFolders?.thumbnail
            }
        return Triple(folders, cameraBucketId, allMediaThumbnail)
    }

    private fun updateFoldersMap(
        folderResult: FolderResult,
        folders: MutableMap<String, FolderData>,
    ) {
        for ((key, value) in folderResult.folderData) {
            if (folders.containsKey(key)) {
                folders[key]?.incrementCount(value.count)
            } else {
                folders[key] = value
            }
        }
    }

    @WorkerThread
    private fun getFolders(context: Context, contentUri: Uri): FolderResult {
        var cameraBucketId: String? = null
        var globalThumbnail: Uri? = null
        var thumbnailTimestamp: Long = 0
        val folders: MutableMap<String, FolderData> = HashMap()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATE_MODIFIED
        )
        val selection = isNotPending
        val sortBy =
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME + " COLLATE NOCASE ASC, " + MediaStore.Images.Media.DATE_MODIFIED + " DESC"
        context.contentResolver.query(contentUri, projection, selection, null, sortBy)
            .use { cursor ->
                while (cursor != null && cursor.moveToNext()) {
                    val rowId = cursor.getLong(cursor.getColumnIndexOrThrow(projection[0]))
                    val thumbnail = ContentUris.withAppendedId(contentUri, rowId)
                    val bucketId = cursor.getString(cursor.getColumnIndexOrThrow(projection[1]))
                    val title = cursor.getString(cursor.getColumnIndexOrThrow(projection[2]))
                    val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(projection[3]))
                    val folder = ValueUtils.getOrDefault(
                        folders, bucketId,
                        FolderData(thumbnail, title, bucketId)
                    )
                    if (folder != null) {
                        folder.incrementCount()
                        folders[bucketId] = folder
                        if (cameraBucketId == null && CAMERA == title) {
                            cameraBucketId = bucketId
                        }
                        if (timestamp > thumbnailTimestamp) {
                            globalThumbnail = thumbnail
                            thumbnailTimestamp = timestamp
                        }
                    }
                }
            }
        return FolderResult(cameraBucketId, globalThumbnail, thumbnailTimestamp, folders)
    }

    @WorkerThread
    private fun getMediaInBucket(
        context: Context,
        bucketId: String,
        mediaTypes: List<String>,
    ): List<MediaViewData> {
        val media: MutableList<MediaViewData> = ArrayList()
        when {
            InternalMediaType.isBothImageAndVideo(mediaTypes) -> {
                media.addAll(
                    getMediaInBucket(
                        context,
                        bucketId,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        true
                    )
                )
                media.addAll(
                    getMediaInBucket(
                        context,
                        bucketId,
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        false
                    )
                )
            }
            InternalMediaType.isImage(mediaTypes) -> {
                media.addAll(
                    getMediaInBucket(
                        context,
                        bucketId,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        true
                    )
                )
            }
            InternalMediaType.isVideo(mediaTypes) -> {
                media.addAll(
                    getMediaInBucket(
                        context,
                        bucketId,
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        false
                    )
                )
            }
        }
        return media.sortedByDescending { it.date }
    }

    @WorkerThread
    private fun getMediaInBucket(
        context: Context, bucketId: String, contentUri: Uri, isImage: Boolean,
    ): List<MediaViewData> {
        val media: MutableList<MediaViewData> = LinkedList()
        var selection = MediaStore.Images.Media.BUCKET_ID + " = ? AND " + isNotPending
        var selectionArgs: Array<String>? = arrayOf(bucketId)
        val sortBy = MediaStore.Images.Media.DATE_MODIFIED + " DESC"
        val projection: Array<String> = if (isImage) {
            arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.DATE_MODIFIED,
                MediaStore.Images.Media.ORIENTATION,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DISPLAY_NAME
            )
        } else {
            arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.MIME_TYPE,
                MediaStore.Video.Media.DATE_MODIFIED,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.DISPLAY_NAME
            )
        }
        if (ALL_MEDIA_BUCKET_ID == bucketId) {
            selection = isNotPending
            selectionArgs = null
        }
        context.contentResolver.query(contentUri, projection, selection, selectionArgs, sortBy)
            .use { cursor ->
                try {
                    while (cursor != null && cursor.moveToNext()) {
                        val rowId = cursor.getLong(cursor.getColumnIndexOrThrow(projection[0]))
                        val uri = ContentUris.withAppendedId(contentUri, rowId)
                        val mimetype =
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE))
                        val date =
                            cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED))
                        val size =
                            cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE))

                        val duration = if (!isImage) {
                            val d = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                            if (!cursor.isNull(d) && d >= 0) {
                                cursor.getInt(d) / 1000
                            } else {
                                null
                            }
                        } else {
                            0
                        }
                        val mediaName =
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME))
                        val mediaType = if (isImage) {
                            IMAGE
                        } else {
                            VIDEO
                        }
                        media.add(
                            MediaViewData.Builder().uri(uri)
                                .mimeType(mimetype)
                                .mediaType(mediaType)
                                .date(date)
                                .size(size)
                                .duration(duration)
                                .bucketId(bucketId)
                                .dateTimeStampHeader(DateUtil.getDateTitleForGallery(date))
                                .mediaName(mediaName)
                                .build()
                        )
                    }
                } catch (e: Exception) {
                    LMChatLogger.getInstance()?.handleException(
                        e.message ?: "",
                        e.stackTraceToString(),
                        LMSeverity.EMERGENCY
                    )
                    e.localizedMessage?.let { Log.e("SDK", it) }
                }
            }
        return media
    }

    @WorkerThread
    private fun getAllDocumentFiles(context: Context): List<MediaViewData> {
        val supportedMimeTypes = arrayOf(MimeTypeMap.getSingleton().getMimeTypeFromExtension("pdf"))
        return getAllDocumentFiles(context, supportedMimeTypes)
    }

    @WorkerThread
    private fun getAllDocumentFiles(
        context: Context,
        mimeTypes: Array<String?>,
    ): List<MediaViewData> {
        val contentUri = MediaStore.Files.getContentUri(VOLUME_EXTERNAL)
        val media: MutableList<MediaViewData> = LinkedList()
        val sortBy = MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC"
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.TITLE
        )
        val selection = MediaStore.Files.FileColumns.MIME_TYPE + "=? AND " + isNotPending
        context.contentResolver.query(contentUri, projection, selection, mimeTypes, sortBy)
            .use { cursor ->
                while (cursor != null && cursor.moveToNext()) {
                    val rowId = cursor.getLong(cursor.getColumnIndexOrThrow(projection[0]))
                    val uri = ContentUris.withAppendedId(contentUri, rowId)
                    val mimetype =
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE))
                    val date =
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED))
                    val size =
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE))
                    var mediaName =
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME))
                    if (mediaName == null) {
                        mediaName =
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.TITLE))
                    }
                    if (!size.isLargeFile) {
                        media.add(
                            MediaViewData.Builder()
                                .uri(uri)
                                .mimeType(mimetype)
                                .mediaType(PDF)
                                .date(date)
                                .size(size)
                                .dateTimeStampHeader(DateUtil.getDateTitleForGallery(date))
                                .mediaName(mediaName)
                                .dynamicViewType(ITEM_MEDIA_PICKER_DOCUMENT)
                                .pdfPageCount(getPdfPageCount(context, uri, mimetype))
                                .build()
                        )
                    }
                }
            }
        return media
    }

    @WorkerThread
    private fun getAllAudioFiles(context: Context): List<MediaViewData> {
        val supportedMimeTypes = arrayOf(
            MimeTypeMap.getSingleton().getMimeTypeFromExtension("aac"),
            MimeTypeMap.getSingleton().getMimeTypeFromExtension("flac"),
            MimeTypeMap.getSingleton().getMimeTypeFromExtension("mp3"),
            MimeTypeMap.getSingleton().getMimeTypeFromExtension("opus"),
            MimeTypeMap.getSingleton().getMimeTypeFromExtension("ogg"),
            MimeTypeMap.getSingleton().getMimeTypeFromExtension("wav"),
        )

        val finalList = supportedMimeTypes.filterNotNull().toTypedArray()

        return getAllAudioFiles(context, finalList)
    }

    @WorkerThread
    private fun getAllAudioFiles(context: Context, mimeTypes: Array<String>): List<MediaViewData> {
        val contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val media: MutableList<MediaViewData> = LinkedList()
        val sortBy = MediaStore.Audio.Media.DISPLAY_NAME
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DISPLAY_NAME
        )

        val selectionNew = buildString {
            mimeTypes.forEach { _ ->
                if (!isEmpty()) {
                    this.append("=? or ")
                }
                this.append(MediaStore.Audio.Media.MIME_TYPE)
            }
            this.append("=? AND $isNotPending")
        }

        context.contentResolver.query(contentUri, projection, selectionNew, mimeTypes, sortBy)
            .use { cursor ->
                while (cursor != null && cursor.moveToNext()) {
                    val rowId = cursor.getLong(cursor.getColumnIndexOrThrow(projection[0]))
                    val uri = ContentUris.withAppendedId(contentUri, rowId)
                    val mimeType =
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE))
                    val date =
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED))
                    val size =
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE))
                    val mediaName =
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME))
                    val duration =
                        cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))

                    if (!size.isLargeFile && !size.isSmallFile) {
                        media.add(
                            MediaViewData.Builder()
                                .uri(uri)
                                .mimeType(mimeType)
                                .mediaType(AUDIO)
                                .date(date)
                                .playOrPause(MEDIA_ACTION_NONE)
                                .audioProgress(0)
                                .duration(duration / 1000)
                                .size(size)
                                .dateTimeStampHeader(DateUtil.getDateTitleForGallery(date))
                                .mediaName(mediaName)
                                .dynamicViewType(ITEM_MEDIA_PICKER_AUDIO)
                                .build()
                        )
                    }
                }
            }
        return media
    }

    @WorkerThread
    private fun getUriDetails(context: Context, contentUris: List<Uri>): List<MediaViewData> {
        val media: MutableList<MediaViewData> = LinkedList()
        contentUris.forEach { contentUri ->
            val mediaViewData = getUriDetail(context, contentUri)
            if (mediaViewData != null) media.add(mediaViewData)
        }
        return media
    }

    @WorkerThread
    private fun getUriDetail(context: Context, contentUri: Uri): MediaViewData? {
        var media: MediaViewData? = null
        context.contentResolver.query(contentUri, null, null, null, null).use { cursor ->
            if (cursor != null && cursor.moveToNext()) {
                val mimetype =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE))
                val size =
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE))
                val mediaName =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME))
                val duration =
                    if (InternalMediaType.isVideo(media?.mediaType)) {
                        if (!cursor.isNull(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DURATION))) {
                            cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DURATION)) / 1000
                        } else {
                            null
                        }
                    } else {
                        0
                    }

                val mediaType = mimetype.getMediaType() ?: contentUri.getMediaType(context)
                ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(mediaName).getMediaType()

                if (mediaType != null) {
                    media = MediaViewData.Builder()
                        .uri(contentUri)
                        .mimeType(mimetype)
                        .mediaType(mediaType)
                        .size(size)
                        .mediaName(mediaName)
                        .duration(duration)
                        .pdfPageCount(getPdfPageCount(context, contentUri, mimetype))
                        .build()
                }
            }
        }
        return media
    }

    fun getExternallySharedUriDetail(context: Context, contentUri: Uri?): SingleUriData? {
        if (contentUri == null) {
            return null
        }
        context.contentResolver.query(contentUri, null, null, null, null).use { cursor ->
            if (cursor != null && cursor.moveToNext()) {
                val mimetypeIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)
                val mimetype = if (mimetypeIndex != -1) {
                    cursor.getString(mimetypeIndex)
                } else {
                    contentUri.getMimeType(context)
                }
                val size =
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE))
                val mediaName =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME))
                val durationIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DURATION)
                val duration = if (durationIndex != -1 && !cursor.isNull(durationIndex)) {
                    cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DURATION)) / 1000
                } else {
                    getDuration(context, contentUri, mimetype)
                }
                val mediaType = mimetype.getMediaType() ?: contentUri.getMediaType(context)
                ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(mediaName).getMediaType()

                if (mediaType != null) {
                    return SingleUriData.Builder()
                        .uri(contentUri)
                        .fileType(mediaType)
                        .size(size)
                        .mediaName(mediaName)
                        .duration(duration)
                        .pdfPageCount(getPdfPageCount(context, contentUri, mimetype))
                        .build()
                }
            }
        }
        return null
    }

    private fun getDuration(context: Context, uri: Uri, mimeType: String?): Int? {
        if (MediaUtils.isVideoType(mimeType) || MediaUtils.isAudioType(mimeType)) {
            try {
                val retriever = MediaMetadataRetriever()
                val fd = context.contentResolver.openFileDescriptor(uri, "r")?.fileDescriptor
                    ?: return null
                retriever.setDataSource(fd)
                val duration = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION
                )?.toInt() ?: return null
                retriever.release()
                return duration / 1000
            } catch (e: Exception) {
                LMChatLogger.getInstance()?.handleException(
                    e.message ?: "",
                    e.stackTraceToString(),
                    LMSeverity.CRITICAL
                )
                e.printStackTrace()
            }
        }
        return null
    }

    private fun getPdfPageCount(context: Context, uri: Uri, mimeType: String?): Int? {
        if (MediaUtils.isPdfType(mimeType)) {
            try {
                val parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
                if (parcelFileDescriptor != null) {
                    val renderer = PdfRenderer(parcelFileDescriptor)
                    parcelFileDescriptor.close()
                    return renderer.pageCount
                }
            } catch (e: Exception) {
                LMChatLogger.getInstance()?.handleException(
                    e.message ?: "",
                    e.stackTraceToString(),
                    LMSeverity.CRITICAL
                )
                e.printStackTrace()
            }
        }
        return null
    }

    fun createThumbnailForAudio(
        context: Context,
        mediaUris: MutableList<SingleUriData>?,
    ): MutableList<SingleUriData>? {
        val mediaMetadataRetriever = MediaMetadataRetriever()
        val bfo = BitmapFactory.Options()
        return mediaUris?.map {
            mediaMetadataRetriever.setDataSource(context, it.uri)
            val rawArt = mediaMetadataRetriever.embeddedPicture
            val art = if (rawArt != null) {
                val bitmap = BitmapFactory.decodeByteArray(rawArt, 0, rawArt.size, bfo)
                FileUtil.getUriFromBitmapWithRandomName(context, bitmap)
            } else {
                null
            }
            it.toBuilder()
                .thumbnailUri(art)
                .build()
        } as? MutableList<SingleUriData>
    }

    fun convertUriToByteArray(context: Context, uri: Uri): ByteArray {
        val iStream = context.contentResolver.openInputStream(uri)
        return getBytes(iStream)
    }

    private fun getBytes(iStream: InputStream?): ByteArray {
        val byteBuffer = ByteArrayOutputStream()
        val bufferSize = 1024
        val buffer = ByteArray(bufferSize)

        var len: Int
        while (iStream?.read(buffer).also { len = it ?: 0 } != -1) {
            byteBuffer.write(buffer, 0, len)
        }

        return byteBuffer.toByteArray()
    }

    private val isNotPending: String
        get() = if (Build.VERSION.SDK_INT <= 28) MediaStore.Images.Media.DATA + " NOT NULL" else MediaStore.MediaColumns.IS_PENDING + " != 1"

    private class FolderResult(
        val cameraBucketId: String?,
        val thumbnail: Uri?,
        val thumbnailTimestamp: Long,
        val folderData: Map<String, FolderData>,
    )

    class FolderData(val thumbnail: Uri, private val title: String?, val bucketId: String) {
        var count = 0
            private set

        @JvmOverloads
        fun incrementCount(amount: Int = 1) {
            count += amount
        }

        fun getFolderTitle(): String {
            return title ?: ""
        }
    }
}