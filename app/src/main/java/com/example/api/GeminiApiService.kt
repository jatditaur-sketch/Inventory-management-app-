package com.example.api

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiRetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

object GeminiHelper {
    suspend fun extractSlipInfo(slipText: String): ExtractedSlipResult? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey == "MY_GEMINI_API_KEY" || apiKey.isBlank()) {
            return null // Trigger local mock fallback
        }

        val prompt = """
            Extract structured wholesale inventory items and categories from this hand-written slip text.
            Text Content: $slipText
            
            Determine:
            1. slipType: Is this adding inventory ("RESTOCK") or subtracting inventory ("SALE")? If indeterminate, default to "SALE".
            2. slipNumber: Find item numbers like Slip #821 or log number. If none, generate a short 3-digit random numeric string.
            3. items: A list of products with properties:
               - name: Cleaned item name (e.g. "Premium Wheat (50kg)" or "Refined Sugar")
               - sku: Formatted short SKU (e.g. "WHT-50", "SUG-REF"). If not written, create a clean uppercase SKU.
               - changeAmount: positive integer quantity.
               - category: One of "Grains", "Sweeteners", "Legumes", "Packaging", "Spices" or other wholesale category names.
               - price: Estimated wholesale price in double format (e.g. 50.0).

            Return a valid JSON string containing exactly the schema described. 
            Do not include markdown tags except if requested or make sure output is clean JSON.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(responseMimeType = "application/json"),
            systemInstruction = Content(parts = listOf(Part(text = "You are an expert handwritten receipt/slip parser for wholesale item stock databases. You always answer with complete valid JSON.")))
        )

        return try {
            val response = GeminiRetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) {
                val adapter = GeminiRetrofitClient.moshi.adapter(ExtractedSlipResult::class.java)
                adapter.fromJson(jsonText)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
