package com.example.data.api

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

// --- Gemini Request / Response Schema ---

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String,
    val data: String // Base64 encoding
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = "application/json",
    val temperature: Double? = 0.1
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?
)

// --- OCR Structured Output Schema ---

@JsonClass(generateAdapter = true)
data class OcrResult(
    val action: String,       // "RESTOCK" or "SALE"
    val slipNumber: String? = "",
    val items: List<OcrItem>
)

@JsonClass(generateAdapter = true)
data class OcrItem(
    val name: String,         // Search name or SKU
    val quantity: Int         // Quantity to modify
)

// --- Retrofit & Endpoints ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiApiClient {
    private const val TAG = "GeminiApiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val apiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Parse handwritten or typed shipping/sales slip using Gemini.
     * Can accept an optional bitmap (for real images) or just a textual transcription
     * in case the user types out or simulates raw handwriting input.
     */
    suspend fun parseHandwrittenSlip(
        bitmap: Bitmap?,
        textLog: String? = null
    ): OcrResult? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API Key is empty or placeholder!")
            throw IllegalStateException("Gemini API key is not configured. Please supply an API key in the Secrets panel in AI Studio.")
        }

        val prompt = """
            You are an advanced wholesale inventory scanner AI.
            Analyze the handwritten or printed slips.
            Determine if this slip represents:
            1. An INBOUND / RESTOCK slip (adding items back into inventory). Look for words like restock, inbound, received, delivery, vendor deposit, addition, + stock.
            2. An OUTBOUND / SALE slip (selling / deducting items). Look for sales, orders, invoices, shipped, dispatched, sold, - stock, billing.

            Extract the item names and their associated quantities. 
            Translate the handwritten/typed names to match any of our main stock items if applicable:
            - 'Premium Wheat (50kg)'
            - 'Wholesale Sugar (Refined)'
            - 'Lentils Grade A (20kg)'
            - 'Basmati Rice Extra Long'
            If an item doesn't closely match, keep its extracted name as listed.

            You MUST output a valid JSON block inside the JSON schema provided below. Do not wrap JSON inside markdown triple backticks.
            
            Schema:
            {
              "action": "RESTOCK" | "SALE",
              "slipNumber": "string containing safe unique invoice or slip identifier, e.g. SLIP-821 or order number",
              "items": [
                {
                  "name": "The matched or extracted product name",
                  "quantity": Integer
                }
              ]
            }
        """.trimIndent()

        val parts = mutableListOf<Part>()
        parts.add(Part(text = prompt))
        
        if (bitmap != null) {
            parts.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = bitmap.toBase64())))
        }
        
        if (!textLog.isNullOrEmpty()) {
            parts.add(Part(text = "Transcribed Slip Context:\n$textLog"))
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = parts)),
            generationConfig = GenerationConfig(responseMimeType = "application/json", temperature = 0.1),
            systemInstruction = Content(parts = listOf(Part(text = "You are a precise, highly-organized JSON translator for logistics inventory.")))
        )

        return try {
            val response = apiService.generateContent(apiKey, request)
            val jsonString = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonString != null) {
                Log.d(TAG, "Gemini Raw Output: $jsonString")
                // Clean markdown tags if they are generated despite our prompt
                val cleanedJson = jsonString.trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()
                moshi.adapter(OcrResult::class.java).fromJson(cleanedJson)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error invoking Gemini OCR api", e)
            null
        }
    }
}
