package com.example.multiforge_android

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

data class UiMessage(
    val role: String,
    val content: String,
    val proposals: List<MemoryProposal> = emptyList()
)

class MainViewModel : ViewModel() {

    var user = mutableStateOf("jason")
    var conversations = mutableStateListOf<Conversation>()
    var activeConversationId = mutableStateOf<String?>(null)
    var messages = mutableStateListOf<UiMessage>()
    var input = mutableStateOf("")
    var loading = mutableStateOf(false)

    init {
        fetchConversations()
    }

    fun setUser(newUser: String) {
        user.value = newUser
        activeConversationId.value = null
        messages.clear()
        fetchConversations()
    }

    fun fetchConversations() {
        viewModelScope.launch {
            try {
                val result = ApiClient.api.getConversations(user.value)
                conversations.clear()
                conversations.addAll(result)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadConversation(id: String) {
        viewModelScope.launch {
            try {
                val detail = ApiClient.api.getConversation(id)
                activeConversationId.value = id
                messages.clear()
                messages.addAll(detail.messages.map {
                    UiMessage(role = it.role, content = it.content)
                })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendMessage() {
        val text = input.value.trim()
        if (text.isEmpty() || loading.value) return
        input.value = ""
        loading.value = true

        messages.add(UiMessage(role = "user", content = text))

        viewModelScope.launch {
            try {
                val response = ApiClient.api.chat(
                    ChatRequest(
                        message = text,
                        conversation_id = activeConversationId.value,
                        user = user.value
                    )
                )
                if (activeConversationId.value == null) {
                    activeConversationId.value = response.conversation_id
                    fetchConversations()
                }
                messages.add(
                    UiMessage(
                        role = "assistant",
                        content = response.reply,
                        proposals = response.proposals
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
                messages.add(UiMessage(role = "assistant", content = "Error: ${e.message}"))
            } finally {
                loading.value = false
            }
        }
    }

    fun confirmProposal(proposal: MemoryProposal, msgIndex: Int) {
        viewModelScope.launch {
            try {
                ApiClient.api.confirmMemory(
                    user = user.value,
                    request = ConfirmMemoryRequest(listOf(proposal))
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val msg = messages[msgIndex]
        messages[msgIndex] = msg.copy(proposals = msg.proposals.filter { it != proposal })
    }

    fun dismissProposal(proposal: MemoryProposal, msgIndex: Int) {
        val msg = messages[msgIndex]
        messages[msgIndex] = msg.copy(proposals = msg.proposals.filter { it != proposal })
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            try {
                ApiClient.api.deleteConversation(id)
                if (activeConversationId.value == id) {
                    activeConversationId.value = null
                    messages.clear()
                }
                fetchConversations()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun newConversation() {
        activeConversationId.value = null
        messages.clear()
    }
}