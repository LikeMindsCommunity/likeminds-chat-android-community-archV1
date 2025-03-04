package com.likeminds.chatmm.utils

import android.content.Context
import android.net.Uri
import android.util.Patterns
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import com.likeminds.chatmm.media.model.*
import com.likeminds.chatmm.utils.file.util.FileUtil
import com.likeminds.chatmm.utils.file.util.FileUtil.isLargeFile
import com.likeminds.likemindschat.helper.LMChatLogger
import com.likeminds.likemindschat.helper.model.LMSeverity
import java.io.File
import java.util.*

object ValueUtils {

    private val alphabets = "abcdefghijklmnopqrstuvwxyz".toCharArray()

    fun generateLexicoGraphicalList(totalSize: Int): List<String> {
        val set: TreeSet<String> = TreeSet()
        val builder = StringBuilder()

        var diff = 1
        var possibleCombinations = 1
        for (position in alphabets.indices) {
            builder.append(alphabets[position])
            if (position > 0) {
                diff *= 2
                possibleCombinations += diff
            }
            if (possibleCombinations >= totalSize) break
        }

        generateLexCombinations(set, builder.toString())
        return set.take(totalSize)
    }

    private fun generateLexCombinations(set: MutableSet<String>, string: String) {
        if (string.isEmpty()) return

        // If current string is not already present.
        if (!set.contains(string)) {
            set.add(string)

            // Traverse current string, one by one remove every character and recur.
            for (i in string.indices) {
                var temp = string
                temp = temp.substring(0, i) + temp.substring(i + 1)
                generateLexCombinations(set, temp)
            }
        }
        return
    }

    fun getTemporaryId(): String {
        return "-${System.currentTimeMillis()}"
    }

    fun String?.containsUrl(): Boolean {
        this?.let {
            return Patterns.WEB_URL.matcher(this).matches()
        }
        return false
    }

    fun Int.isValidIndex(itemCount: Int): Boolean {
        return this > -1 && this < itemCount
    }

    fun Int.isValidIndex(items: List<*>? = null): Boolean {
        return if (items != null) {
            this > -1 && this < items.size
        } else {
            this > -1
        }
    }

    fun Uri?.isValidSize(context: Context): Boolean {
        if (this == null) {
            return false
        }
        val path = FileUtil.getRealPath(context, this).path
        if (path.isNotEmpty()) {
            return !File(path).isLargeFile
        }
        return false
    }

    fun Uri?.getMediaType(context: Context): String? {
        var mediaType: String? = null
        this?.let {
            mediaType = this.getMimeType(context).getMediaType()
        }
        return mediaType
    }

    fun Uri.getMimeType(context: Context): String? {
        var type = context.contentResolver.getType(this)
        if (type == null) {
            val fileExtension = MimeTypeMap.getFileExtensionFromUrl(this.toString())
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.lowercase())
        }
        return type
    }

    fun <T> List<T>.getItemInList(position: Int): T? {
        if (position < 0 || position >= this.size) {
            return null
        }
        return this[position]
    }

    fun String.getEmailIfExist(): String? {
        return try {
            val emails: MutableList<String> = ArrayList()
            val matcher = Patterns.EMAIL_ADDRESS.matcher(this)
            while (matcher.find()) {
                val email = matcher.group()
                emails.add(email)
            }
            if (emails.isNotEmpty()) {
                emails.first()
            } else null
        } catch (e: Exception) {
            LMChatLogger.getInstance()?.handleException(
                e.message ?: "",
                e.stackTraceToString(),
                LMSeverity.CRITICAL
            )
            return null
        }
    }

    fun String.getUrlIfExist(): String? {
        return try {
            val links: MutableList<String> = ArrayList()
            val matcher = Patterns.WEB_URL.matcher(this)
            while (matcher.find()) {
                val link = matcher.group()
                if (URLUtil.isValidUrl(link)) {
                    links.add(link)
                } else {
                    val newHttpsLink = "https://$link"
                    if (URLUtil.isValidUrl(newHttpsLink)) {
                        links.add(newHttpsLink)
                    }
                }
            }
            if (links.isNotEmpty()) {
                links.first()
            } else null
        } catch (e: Exception) {
            LMChatLogger.getInstance()?.handleException(
                e.message ?: "",
                e.stackTraceToString(),
                LMSeverity.CRITICAL
            )
            return null
        }
    }

    /**
     * Check mediaType from Media mimeType
     * */
    fun String?.getMediaType(): String? {
        var mediaType: String? = null
        if (this != null) {
            when {
                this == "image/gif" -> mediaType = GIF
                this.startsWith("image") -> mediaType = IMAGE
                this.startsWith("video") -> mediaType = VIDEO
                this == "application/pdf" -> mediaType = PDF
                this.startsWith("audio") -> mediaType = AUDIO
            }
        }
        return mediaType
    }

    fun String.getValidTextForLinkify(): String {
        return this.replace("\u202C", "")
            .replace("\u202D", "")
            .replace("\u202E", "")
    }

    fun Int?.getMaxCountNumberText(): String {
        if (this == null) {
            return "0"
        }
        return if (this > 99) {
            "99+"
        } else {
            this.toString()
        }
    }

    /**
     * http://www.youtube.com/watch?v=-wtIMTCHWuI
     * http://www.youtube.com/v/-wtIMTCHWuI
     * http://youtu.be/-wtIMTCHWuI
     */
    fun String.isValidYoutubeLink(): Boolean {
        val uri = Uri.parse(this)
        return uri.host.equals("youtube") ||
                uri.host.equals("youtu.be") ||
                uri.host.equals("www.youtube.com")
    }

    fun String.getValidYoutubeVideoId(): String {
        val uri = Uri.parse(this)
        return uri.getQueryParameter("v")
            ?: uri.lastPathSegment ?: ""
    }

    @JvmStatic
    fun <K, V> getOrDefault(map: Map<K, V>, key: K, defaultValue: V): V? {
        return if (map.containsKey(key)) map[key] else defaultValue
    }

    /**
     * This function run filter and map operation in single loop
     */
    fun <T, R, P> Iterable<T>.filterThenMap(
        predicate: (T) -> Pair<Boolean, P>,
        transform: (Pair<T, P>) -> R
    ): List<R> {
        return filterThenMap(ArrayList(), predicate, transform)
    }

    fun <T, R, P, C : MutableCollection<in R>>
            Iterable<T>.filterThenMap(
        collection: C, predicate: (T) -> Pair<Boolean, P>,
        transform: (Pair<T, P>) -> R
    ): C {
        for (element in this) {
            val response = predicate(element)
            if (response.first) {
                collection.add(transform(Pair(element, response.second)))
            }
        }
        return collection
    }

    fun <T> Collection<T>?.orEmptyMutable(): MutableList<T> {
        return this?.toMutableList() ?: mutableListOf()
    }
}