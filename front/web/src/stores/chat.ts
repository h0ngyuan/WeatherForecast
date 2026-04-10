import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: number
  loading?: boolean
}

export const useChatStore = defineStore('chat', () => {
  const messages = ref<ChatMessage[]>([])
  const loading = ref(false)

  function addUserMessage(content: string) {
    messages.value.push({
      id: Date.now().toString(),
      role: 'user',
      content,
      timestamp: Date.now(),
    })
  }

  function addAssistantMessage(content: string, id?: string) {
    messages.value.push({
      id: id || Date.now().toString(),
      role: 'assistant',
      content,
      timestamp: Date.now(),
    })
  }

  function addLoadingMessage() {
    const id = 'loading-' + Date.now()
    messages.value.push({
      id,
      role: 'assistant',
      content: '',
      timestamp: Date.now(),
      loading: true,
    })
    return id
  }

  function updateMessage(id: string, content: string) {
    const msg = messages.value.find((m) => m.id === id)
    if (msg) {
      msg.content = content
      msg.loading = false
    }
  }

  function removeMessage(id: string) {
    messages.value = messages.value.filter((m) => m.id !== id)
  }

  function clearMessages() {
    messages.value = []
  }

  return {
    messages,
    loading,
    addUserMessage,
    addAssistantMessage,
    addLoadingMessage,
    updateMessage,
    removeMessage,
    clearMessages,
  }
})
