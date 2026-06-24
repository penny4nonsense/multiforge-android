package com.example.multiforge_android

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

const val BASE_URL = "http://192.168.50.184:8001"

data class ChatRequest(
    val message: String,
    val conversation_id: String? = null,
    val user: String = "jason"
)

data class ChatResponse(
    val conversation_id: String,
    val reply: String,
    val proposals: List<MemoryProposal>
)

data class MemoryProposal(
    val type: String,
    val category: String,
    val content: String
)

data class Conversation(
    val id: String,
    val title: String?,
    val username: String,
    val updated_at: String
)

data class Message(
    val id: String,
    val role: String,
    val content: String,
    val created_at: String
)

data class ConversationDetail(
    val id: String,
    val title: String?,
    val messages: List<Message>
)

data class ConfirmMemoryRequest(
    val proposals: List<MemoryProposal>
)

interface MultiforgeApi {
    @GET("conversations/")
    suspend fun getConversations(@Query("user") user: String): List<Conversation>

    @GET("conversations/{id}")
    suspend fun getConversation(@Path("id") id: String): ConversationDetail

    @DELETE("conversations/{id}")
    suspend fun deleteConversation(@Path("id") id: String)

    @POST("chat/")
    suspend fun chat(@Body request: ChatRequest): ChatResponse

    @POST("memories/confirm")
    suspend fun confirmMemory(
        @Query("user") user: String,
        @Body request: ConfirmMemoryRequest
    )
}

object ApiClient {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: MultiforgeApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MultiforgeApi::class.java)
    }
}