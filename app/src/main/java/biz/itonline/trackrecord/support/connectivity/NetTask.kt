package biz.itonline.trackrecord.support.connectivity

import biz.itonline.trackrecord.exceptions.HZException
import biz.itonline.trackrecord.exceptions.HZException.InvalidParamException
import biz.itonline.trackrecord.exceptions.HZException.InvalidUserException
import biz.itonline.trackrecord.support.HZConstants
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.net.HttpURLConnection
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.text.substring
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response as OkHttpResponse

class NetTask() {

    suspend fun processOkHZApiCall(
        jsonObject: JSONObject?,
        apiEndPoint: String?,
        userToken: String?,
        method: Method = Method.GET
    ) = suspendCoroutine<JSONObject> { cont ->
        val task = jsonObject?.getString("task")
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(userToken))
            .build()

        try {
            val requestBuilder = Request.Builder()
                .url(HZConstants.serverURL + apiEndPoint)
            when (method) {
                Method.GET -> {
                    requestBuilder.get()
                }

                Method.POST -> {
                    requestBuilder.post(
                        jsonObject.toString()
                            .toRequestBody("application/json; charset=utf-8".toMediaType())
                    )
                }

                Method.PUT -> {
                    requestBuilder.put(
                        jsonObject.toString()
                            .toRequestBody("application/json; charset=utf-8".toMediaType())
                    )
                }

                Method.PATCH -> {
                    requestBuilder.patch(
                        jsonObject.toString()
                            .toRequestBody("application/json; charset=utf-8".toMediaType())
                    )
                }

                Method.DELETE -> {
                    requestBuilder.delete()
                }
            }

            val request = requestBuilder.build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            val responseCode = response.code
            if (responseCode == HttpURLConnection.HTTP_OK && responseBody != null) {
                val response = JSONObject(responseBody)
                cont.resume(response)
            } else {
                when (responseCode) {
                    401 -> {
                        cont.resumeWithException(
                            InvalidUserException(
                                task,
                                "Error in user credentials"
                            )
                        )
                    }

                    403 -> {
                        cont.resumeWithException(
                            HZException.ApiAccessNotAllowed(
                                task,
                                "Invalid API access key"
                            )
                        )
                    }

                    465 -> {
                        cont.resumeWithException(
                            InvalidUserException(
                                task,
                                "No user exist"
                            )
                        )
                    }

                    466 -> cont.resumeWithException(
                        InvalidParamException(
                            task,
                            "Wrong param provided"
                        )
                    )

                    else -> cont.resumeWithException(Exception(toString()))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            cont.resumeWithException(e)
        }
    }

    enum class Method {
        GET, POST, PUT, PATCH, DELETE
    }

}


class AuthInterceptor(private val userToken: String? = null) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): OkHttpResponse {
        val date: DateFormat = SimpleDateFormat("Z", Locale.getDefault())
        val localTime = date.format(Date())
        val localT = localTime.substring(0, 3) + ":" + localTime.substring(3, 5)

        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer ${HZConstants.bearer_token}")
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .addHeader("User-agent", System.getProperty("http.agent") as String)
            .addHeader("Api-Key", HZConstants.api_key)
            .addHeader("User-Token", userToken ?: "N/A")
            .addHeader("Content-Time", localT)
            .build()
        return chain.proceed(request)
    }
}