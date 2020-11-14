@file:Suppress("unused")

package io.nekohasekai.spamwatch

import cn.hutool.http.*
import cn.hutool.json.JSONObject
import com.google.gson.Gson
import io.nekohasekai.spamwatch.models.BanRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

class SpamWatch private constructor(configuration: Builder) {

    constructor(apiKey: String) : this(Builder().withKey(apiKey))

    private val gson by configuration::gson
    private val apiUrl by configuration::apiUrl
    private val apiKey by configuration::apiKey

    private fun create(path: String) = HttpUtil.createGet("$apiUrl/$path")
            .header("Authorization", "Bearer $apiKey")

    private fun checkResponse(response: HttpResponse) {

        if (!response.isOk) checkJSON(response, Unit::class.java)

    }

    private fun <T> checkJSON(response: HttpResponse, clazz: Class<T>): T {

        val body = JSONObject(response.body())

        if (body.containsKey("code") && body.getInt("code") != 200) {
            throw SpamWatchException(body.getInt("code"), body.getStr("error"))
        }

        return gson.fromJson(body.toString(), clazz)

    }

    private inline fun <reified T> executeJson(request: HttpRequest) = checkJSON(request.execute(), T::class.java)

    @Throws(SpamWatchException::class)
    suspend fun getSpecific(userId: Int): BanRecord = withContext(Dispatchers.IO) {
        executeJson(create("banlist/$userId"))
    }

    @Throws(SpamWatchException::class)
    suspend fun getIDs(): InputStream = withContext(Dispatchers.IO) {
        create("banlist/all").execute()
                .also { checkResponse(it) }
                .bodyStream()
    }

    class Builder {

        var gson = Gson()
        var apiUrl = "https://api.spamwat.ch"
        var apiKey = ""

        fun withGson(gson: Gson): Builder {
            this.gson = gson
            return this
        }

        fun withUrl(url: String): Builder {
            apiUrl = url
            return this
        }

        fun withKey(key: String): Builder {
            apiKey = key
            return this
        }

        fun build(): SpamWatch {
            check(apiKey.isNotBlank()) { "Missing api key" }
            return SpamWatch(this)
        }

    }

}