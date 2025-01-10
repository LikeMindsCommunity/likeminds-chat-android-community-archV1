package com.likeminds.community.hybrid.chat

import android.content.Context
import android.util.Log
import com.likeminds.community.hybrid.chat.CommunityHybridChatApplication.Companion.LM_CHAT_EXAMPLE_TAG
import com.likeminds.community.hybrid.chat.auth.util.AuthPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

class GetTokensTask {

    private lateinit var authPreferences: AuthPreferences

    // Get tokens from the server
    suspend fun getTokens(context: Context, isProd: Boolean): Pair<String, String> {
        return withContext(Dispatchers.IO) {
            //get api url
            val apiUrl = BuildConfig.INTIATE_URL

            authPreferences = AuthPreferences(context)

            // Create connection
            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection

            // Set HTTP method and required headers
            connection.apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty(
                    "Content-Type",
                    "application/json"
                )
                setRequestProperty("x-api-key", authPreferences.getApiKey())
            }

            // Create request body
            val request = JSONObject().apply {
                put("uuid", authPreferences.getUserId())
                put("user_name", authPreferences.getUserName())
            }

            Log.d(LM_CHAT_EXAMPLE_TAG, "connection : ${connection.requestProperties}")
            Log.d(LM_CHAT_EXAMPLE_TAG, "Request: $request")

            // Write POST data
            val writer = OutputStreamWriter(connection.outputStream)
            writer.write(request.toString())
            writer.flush()
            writer.close()

            // Get response
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()
                response.toString()

                val responseObject = JSONObject(response.toString())
                val data = responseObject.getJSONObject("data")
                val accessToken = data.getString("access_token")
                val refreshToken = data.getString("refresh_token")
                Pair(accessToken, refreshToken)
            } else {
                Log.e(LM_CHAT_EXAMPLE_TAG, "Error: HTTP $responseCode")
                Pair("", "")
            }
        }
    }
}