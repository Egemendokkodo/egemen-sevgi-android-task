package com.uygulamalarim.androidtaskegemensevgi.NetworkOperations

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException

class NetworkTask(private val listener: NetworkTaskListener) {



    interface NetworkTaskListener {
        fun onResult(result: String?)
    }

    fun execute(vararg urls: String) {
        val loginUrl = urls[0]
        val tasksUrl = urls[1]
        val client = OkHttpClient()
        val mediaType = "application/json".toMediaTypeOrNull()
        val body = RequestBody.create(mediaType, "{\"username\":\"365\",\"password\":\"1\"}")
        val request = Request.Builder()
            .url(loginUrl)
            .post(body)
            .addHeader("Authorization", "Basic QVBJX0V4cGxvcmVyOjEyMzQ1NmlzQUxhbWVQYXNz")
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {

                    val responseString = response.body?.string()
                    val jsonObject = responseString?.let { JSONObject(it) }
                    val oauthObject = jsonObject?.getJSONObject("oauth")
                    val token = oauthObject?.getString("access_token")

                    val tasksRequest = Request.Builder()
                        .url(tasksUrl)
                        .addHeader("Authorization", "Bearer $token")
                        .build()

                    client.newCall(tasksRequest).enqueue(object : Callback {
                        override fun onResponse(call: Call, response: Response) {
                            val result = response.body?.string()
                            listener.onResult(result)
                        }

                        override fun onFailure(call: Call, e: IOException) {
                            listener.onResult(null)
                        }
                    })

            }

            override fun onFailure(call: Call, e: IOException) {
                listener.onResult(null)
            }
        })
    }
}