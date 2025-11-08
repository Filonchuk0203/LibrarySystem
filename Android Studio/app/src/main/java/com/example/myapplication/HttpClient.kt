package com.example.myapplication

import android.app.Activity
import android.widget.Toast
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class HttpClient {

    private val client = OkHttpClient()

    // Базовий запит без обробки — залишаємо, якщо десь потрібно кастомно
    fun postRequest(url: String, json: String, callback: Callback) {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = json.toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).enqueue(callback)
    }

    // 🔹 Універсальний метод із автоматичною обробкою типових помилок
    fun safePostRequest(
        activity: Activity,
        url: String,
        json: String,
        onSuccess: (JSONObject) -> Unit
    ) {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = json.toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity.runOnUiThread {
                    Toast.makeText(
                        activity,
                        "Помилка: Перевірте з'єднання з інтернетом або повторіть спробу пізніше.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    activity.runOnUiThread {
                        Toast.makeText(
                            activity,
                            "Помилка на сервері, вибачте за незручності.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return
                }

                try {
                    val bodyStr = response.body?.string()
                    if (bodyStr.isNullOrEmpty()) {
                        activity.runOnUiThread {
                            Toast.makeText(
                                activity,
                                "Помилка: порожня відповідь від сервера.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        return
                    }

                    val jsonResponse = JSONObject(bodyStr)
                    activity.runOnUiThread {
                        onSuccess(jsonResponse)
                    }
                } catch (e: Exception) {
                    activity.runOnUiThread {
                        Toast.makeText(
                            activity,
                            "Помилка при обробці відповіді сервера.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }
}
