package com.example.api

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiRequest(
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
    val data: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = "application/json",
    val temperature: Double? = 0.1
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?
)

// --- Extracted Objects Schema (The parsed JSON we request) ---
@JsonClass(generateAdapter = true)
data class ExtractedSlipResult(
    val slipType: String, // "RESTOCK" or "SALE"
    val slipNumber: String, // e.g. "821" or generated random numeric if missing
    val items: List<ExtractedSlipItem>
)

@JsonClass(generateAdapter = true)
data class ExtractedSlipItem(
    val name: String,
    val sku: String,
    val changeAmount: Int, // Positive amount (e.g. 24 or 100)
    val category: String, // Wholesale category e.g. "Grains", "Sweeteners", "Legumes", "Beverages"
    val price: Double = 0.0 // Wholesale estimation / item price
)
