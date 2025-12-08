package pro.sihao.jarvis.data.network.api

import pro.sihao.jarvis.data.network.dto.ModelsResponse
import pro.sihao.jarvis.data.network.dto.OpenAIRequest
import pro.sihao.jarvis.data.network.dto.OpenAIResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenAICompatibleApiService {
    @POST("chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: OpenAIRequest
    ): Response<OpenAIResponse>

    @GET("models")
    suspend fun listModels(
        @Header("Authorization") authorization: String
    ): Response<ModelsResponse>
}