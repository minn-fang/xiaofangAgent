<template>
  <div class="app-layout">
    <div class="sidebar" :style="{ width: sidebarWidth + 'px' }">
      <div class="logo-section">
        <img src="@/assets/logo.png" alt="小方施主" width="160" height="160" />
        <span class="logo-text">小方施主（医疗版）</span>
      </div>
      <div class="mobile-actions">
        <el-button class="new-chat-button" @click="newChat">
          <i class="fa-solid fa-plus"></i>
          &nbsp;新会话
        </el-button>
        <el-button class="history-toggle-button" @click="toggleMobileHistory">
          <i class="fa-solid fa-history"></i>
        </el-button>
      </div>
      
      <!-- 历史会话列表 -->
      <div class="history-section">
        <span class="history-title">历史会话</span>
        <div class="history-list">
          <div
            v-for="session in historySessions"
            :key="session.memoryId"
            :class="['history-item', { active: session.memoryId === currentMemoryId }]"
          >
            <div class="session-info" @click="switchSession(session.memoryId)">
              <i class="fa-solid fa-message-circle"></i>
              <span class="session-title">{{ session.title }}</span>
              <span class="session-time">{{ session.time }}</span>
            </div>
            <i
              class="fa-solid fa-ellipsis-v delete-icon"
              @click.stop="deleteSession(session.memoryId)"
              title="删除会话"
            ></i>
          </div>
          <div v-if="historySessions.length === 0" class="empty-history">
            暂无历史会话
          </div>
        </div>
      </div>
      <!-- 左侧栏拖动条 -->
      <div class="sidebar-resizer" @mousedown="startSidebarResize"></div>
      
      <!-- 移动端历史会话弹窗 -->
      <div v-if="showMobileHistory" class="mobile-history-overlay" @click="showMobileHistory = false">
        <div class="mobile-history-panel" @click.stop>
          <div class="mobile-history-header">
            <span>历史会话</span>
            <i class="fa-solid fa-xmark close-btn" @click="showMobileHistory = false"></i>
          </div>
          <div class="mobile-history-list">
            <div
              v-for="session in historySessions"
              :key="session.memoryId"
              :class="['mobile-history-item', { active: session.memoryId === currentMemoryId }]"
            >
              <div class="mobile-session-info" @click="selectMobileSession(session.memoryId)">
                <i class="fa-solid fa-message-circle"></i>
                <span class="mobile-session-title">{{ session.title }}</span>
              </div>
              <i
                class="fa-solid fa-trash mobile-delete-icon"
                @click.stop="deleteSession(session.memoryId)"
                title="删除会话"
              ></i>
            </div>
            <div v-if="historySessions.length === 0" class="mobile-empty-history">
              暂无历史会话
            </div>
          </div>
        </div>
      </div>
    </div>
    <div class="main-content">
      <div class="chat-container">
        <div class="message-list" ref="messaggListRef">
          <div
            v-for="(message, index) in messages"
            :key="index"
            :class="
              message.isUser ? 'message user-message' : 'message bot-message'
            "
          >
            <!-- 会话图标 -->
            <i
              :class="
                message.isUser
                  ? 'fa-solid fa-user message-icon'
                  : 'fa-solid fa-robot message-icon'
              "
            ></i>
            <!-- 会话内容 -->
            <span>
              <span v-html="message.content"></span>
              <!-- loading -->
              <span
                class="loading-dots"
                v-if="message.isThinking || message.isTyping"
              >
                <span class="dot"></span>
                <span class="dot"></span>
              </span>
            </span>
          </div>
        </div>
        <!-- 底部栏拖动条 -->
        <div class="bottom-resizer" @mousedown="startBottomResize"></div>
        <div class="input-container" :style="{ height: inputHeight + 'px' }">
          <el-input
            v-model="inputMessage"
            placeholder="请输入消息"
            @keyup.enter="sendMessage"
          ></el-input>
          <el-button @click="sendMessage" :disabled="isSending" type="primary"
            >发送</el-button
          >
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch, nextTick, computed } from 'vue'
import axios from 'axios'
import { v4 as uuidv4 } from 'uuid'

const messaggListRef = ref()
const isSending = ref(false)
const uuid = ref()
const inputMessage = ref('')
const messages = ref([])
const isHistoryLoaded = ref(false) // 标记历史记录是否已加载
const historySessions = ref([]) // 历史会话列表
const currentMemoryId = ref(null) // 当前会话的memoryId

// 可拖动尺寸
const sidebarWidth = ref(200) // 左侧栏宽度
const inputHeight = ref(60) // 底部输入框高度
const isResizingSidebar = ref(false)
const isResizingBottom = ref(false)

// 移动端历史会话弹窗
const showMobileHistory = ref(false)

onMounted(() => {
  initUUID()
  loadHistorySessions()
  
  // 监听消息变化自动滚动
  watch(messages, () => {
    nextTick(() => scrollToBottom())
  }, { deep: true })
  
  // 加载历史记录
  loadHistory()
})

const scrollToBottom = () => {
  if (messaggListRef.value) {
    messaggListRef.value.scrollTop = messaggListRef.value.scrollHeight
  }
}

// 加载历史会话列表
const loadHistorySessions = () => {
  const sessions = localStorage.getItem('chat_sessions')
  if (sessions) {
    historySessions.value = JSON.parse(sessions)
  } else {
    historySessions.value = []
  }
  console.log('加载历史会话:', historySessions.value)
}

// 保存会话到历史列表（只有会话不存在时才创建）
const saveSession = (memoryId, title) => {
  // 检查会话是否已存在
  const existingSession = historySessions.value.find(s => s.memoryId === memoryId)
  
  if (existingSession) {
    // 会话已存在，只更新时间，不更新标题
    existingSession.time = formatTime(new Date())
    // 移到列表顶部
    const sessions = historySessions.value.filter(s => s.memoryId !== memoryId)
    sessions.unshift(existingSession)
    historySessions.value = sessions
    localStorage.setItem('chat_sessions', JSON.stringify(historySessions.value))
  } else {
    // 会话不存在，创建新记录
    const newSession = {
      memoryId,
      title: title || '新会话',
      time: formatTime(new Date())
    }
    historySessions.value.unshift(newSession)
    
    // 最多保存20条历史会话
    const limitedSessions = historySessions.value.slice(0, 20)
    historySessions.value = limitedSessions
    localStorage.setItem('chat_sessions', JSON.stringify(limitedSessions))
  }
}

// 格式化时间
const formatTime = (date) => {
  const pad = (n) => n.toString().padStart(2, '0')
  return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

// 切换会话
const switchSession = (memoryId) => {
  if (memoryId === currentMemoryId.value) return
  
  console.log('切换到会话:', memoryId)
  uuid.value = memoryId
  currentMemoryId.value = memoryId
  messages.value = []
  isHistoryLoaded.value = false
  loadHistory()
}

// 加载历史记录
const loadHistory = async () => {
  try {
    console.log('开始加载历史记录, memoryId:', uuid.value)
    currentMemoryId.value = uuid.value
    
    const response = await axios.get(`/api/xiaofang/history/${uuid.value}`)
    const history = response.data
    
    console.log('历史记录响应:', history)
    
    if (history && history.hasHistory && history.messages && history.messages.length > 0) {
      // 将历史消息转换为前端格式
      history.messages.forEach(msg => {
        messages.value.push({
          isUser: msg.role === 'user',
          content: msg.content,
          isTyping: false,
          isThinking: false,
        })
      })
      
      isHistoryLoaded.value = true
      console.log(`成功加载 ${history.messages.length} 条历史记录`)
      
      // 更新会话标题（取最后一条用户消息）
      const userMessages = history.messages.filter(m => m.role === 'user')
      if (userMessages.length > 0) {
        const lastUserMsg = userMessages[userMessages.length - 1]
        const title = lastUserMsg.content.substring(0, 20) + (lastUserMsg.content.length > 20 ? '...' : '')
        saveSession(uuid.value, title)
      }
      
      await nextTick()
      scrollToBottom()
    } else {
      console.log('没有历史记录，发送默认问候')
      isHistoryLoaded.value = true
      // 没有历史记录才发送默认问候
      hello()
    }
  } catch (error) {
    console.error('加载历史记录失败:', error)
    isHistoryLoaded.value = true
    // 加载失败时发送默认问候
    hello()
  }
}

const hello = () => {
  // 只有在历史记录加载完成后才发送问候
  if (isHistoryLoaded.value) {
    sendRequest('你好', true)
  }
}

const sendMessage = () => {
  if (inputMessage.value.trim()) {
    sendRequest(inputMessage.value.trim(), false)
    inputMessage.value = ''
  }
}

const sendRequest = (message, isHello = false) => {
  isSending.value = true
  
  const userMsg = {
    isUser: true,
    content: message,
    isTyping: false,
    isThinking: false,
  }
  
  // 第一条默认发送的用户消息"你好"不放入会话列表
  if (messages.value.length > 0 || !isHello) {
    messages.value.push(userMsg)
    
    // 只有当会话不存在时才保存（标题取第一条消息）
    const existingSession = historySessions.value.find(s => s.memoryId === uuid.value)
    if (!existingSession) {
      const title = message.substring(0, 20) + (message.length > 20 ? '...' : '')
      saveSession(uuid.value, title)
    } else {
      // 更新会话时间
      saveSession(uuid.value, existingSession.title)
    }
  }

  // 添加机器人加载消息
  const botMsg = {
    isUser: false,
    content: '', // 增量填充
    isTyping: true, // 显示加载动画
    isThinking: false,
  }
  messages.value.push(botMsg)
  const lastMsg = messages.value[messages.value.length - 1]
  
  nextTick(() => scrollToBottom())

  let previousLength = 0 // 记录上一次处理的文本长度

  axios
    .post(
      '/api/xiaofang/chat',
      { memoryId: uuid.value, message },
      {
        responseType: 'text',
        onDownloadProgress: (e) => {
          const fullText = e.event.target.responseText // 累积的完整文本
          const newText = fullText.substring(previousLength)
          
          previousLength = fullText.length
          
          if (newText) {
            const convertedText = convertStreamOutput(newText)
            lastMsg.content += convertedText
            nextTick(() => scrollToBottom())
          }
        },
      }
    )
    .then(() => {
      // 流结束后隐藏加载动画
      messages.value.at(-1).isTyping = false
      isSending.value = false
    })
    .catch((error) => {
      console.error('流式错误:', error)
      messages.value.at(-1).content = '请求失败，请重试'
      messages.value.at(-1).isTyping = false
      isSending.value = false
    })
}

// 初始化 UUID
const initUUID = () => {
  let storedUUID = localStorage.getItem('user_uuid')
  if (!storedUUID) {
    storedUUID = uuidToNumber(uuidv4())
    localStorage.setItem('user_uuid', storedUUID)
  }
  uuid.value = storedUUID
  console.log('当前 memoryId:', uuid.value)
}

const uuidToNumber = (uuid) => {
  let number = 0
  for (let i = 0; i < uuid.length && i < 6; i++) {
    const hexValue = uuid[i]
    number = number * 16 + (parseInt(hexValue, 16) || 0)
  }
  return number % 1000000
}

// 转换特殊字符
const convertStreamOutput = (output) => {
  return output
    .replace(/&/g, '&amp;')      // 1. 先转义 &
    .replace(/</g, '&lt;')       // 2. 再转义 <
    .replace(/>/g, '&gt;')       // 3. 再转义 >
    .replace(/\n/g, '<br>')      // 4. 最后替换换行为 <br>
    .replace(/\t/g, '&nbsp;&nbsp;&nbsp;&nbsp;')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')  // 5. 替换加粗
}

const newChat = () => {
  // 生成新的memoryId
  const newMemoryId = uuidToNumber(uuidv4())
  localStorage.setItem('user_uuid', newMemoryId)
  
  // 清空当前消息列表
  messages.value = []
  uuid.value = newMemoryId
  isHistoryLoaded.value = false
  
  // 发送默认问候
  hello()
}

// 删除会话
const deleteSession = async (memoryId) => {
  if (confirm('确定要删除这个会话吗？')) {
    try {
      // 调用后端接口清除缓存
      await axios.delete(`/api/cache/memory/${memoryId}`)
      console.log('后端缓存已清除:', memoryId)
    } catch (error) {
      console.error('清除后端缓存失败:', error)
    }
    
    // 从前端列表中删除
    historySessions.value = historySessions.value.filter(s => s.memoryId !== memoryId)
    localStorage.setItem('chat_sessions', JSON.stringify(historySessions.value))
    
    // 如果删除的是当前会话
    if (memoryId === currentMemoryId.value) {
      if (historySessions.value.length > 0) {
        // 切换到第一个会话
        const firstSession = historySessions.value[0]
        switchSession(firstSession.memoryId)
      } else {
        // 没有会话了，创建新会话
        newChat()
      }
    }
    
    console.log('会话已删除:', memoryId)
  }
}

// 移动端历史会话操作
const toggleMobileHistory = () => {
  showMobileHistory.value = !showMobileHistory.value
}

const selectMobileSession = (memoryId) => {
  showMobileHistory.value = false
  switchSession(memoryId)
}

// 左侧栏拖动
const startSidebarResize = (e) => {
  isResizingSidebar.value = true
  document.addEventListener('mousemove', resizeSidebar)
  document.addEventListener('mouseup', stopSidebarResize)
}

const resizeSidebar = (e) => {
  if (!isResizingSidebar.value) return
  const newWidth = e.clientX
  // 限制宽度范围 150-500px
  sidebarWidth.value = Math.max(150, Math.min(500, newWidth))
}

const stopSidebarResize = () => {
  isResizingSidebar.value = false
  document.removeEventListener('mousemove', resizeSidebar)
  document.removeEventListener('mouseup', stopSidebarResize)
}

// 底部栏拖动
const startBottomResize = (e) => {
  isResizingBottom.value = true
  document.addEventListener('mousemove', resizeBottom)
  document.addEventListener('mouseup', stopBottomResize)
}

const resizeBottom = (e) => {
  if (!isResizingBottom.value) return
  const chatContainer = document.querySelector('.chat-container')
  if (!chatContainer) return
  
  const containerRect = chatContainer.getBoundingClientRect()
  const newHeight = containerRect.height - (e.clientY - containerRect.top)
  // 限制高度范围 40-200px
  inputHeight.value = Math.max(40, Math.min(200, newHeight))
}

const stopBottomResize = () => {
  isResizingBottom.value = false
  document.removeEventListener('mousemove', resizeBottom)
  document.removeEventListener('mouseup', stopBottomResize)
}
</script>

<style scoped>
.app-layout {
  display: flex;
  height: 100vh;
}

.sidebar {
  background-color: #f4f4f9;
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  overflow: hidden;
}

.logo-section {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.logo-text {
  font-size: 18px;
  font-weight: bold;
  margin-top: 10px;
}

.mobile-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.new-chat-button {
  width: 100%;
  margin-top: 20px;
}

.history-toggle-button {
  display: none;
}

/* 移动端历史会话弹窗 */
.mobile-history-overlay {
  display: none;
}

.mobile-history-panel {
  display: none;
}

.history-section {
  width: 100%;
  margin-top: 20px;
  border-top: 1px solid #e0e0e0;
  padding-top: 15px;
}

.history-title {
  font-size: 14px;
  font-weight: bold;
  color: #666;
  display: block;
  margin-bottom: 10px;
}

.history-list {
  max-height: 400px;
  overflow-y: auto;
}

.history-item {
  display: flex;
  align-items: center;
  padding: 10px;
  cursor: pointer;
  border-radius: 4px;
  margin-bottom: 5px;
  transition: background-color 0.2s;
}

.history-item:hover {
  background-color: #e8e8e8;
}

.history-item.active {
  background-color: #d1ecf1;
}

.history-item .fa-message-circle {
  margin-right: 10px;
  color: #10b981;
  font-size: 14px;
}

.session-title {
  flex: 1;
  font-size: 13px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-time {
  font-size: 11px;
  color: #999;
  margin-left: 8px;
}

.empty-history {
  text-align: center;
  color: #999;
  font-size: 13px;
  padding: 20px;
}

.delete-icon {
  color: #999;
  font-size: 16px;
  cursor: pointer;
  padding: 4px 6px;
  border-radius: 4px;
  transition: all 0.2s;
}

.delete-icon:hover {
  color: #666;
  background-color: #e8e8e8;
}

.main-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  background-color: #fff;
  margin-bottom: 10px;
  display: flex;
  flex-direction: column;
}

.message {
  margin-bottom: 10px;
  padding: 10px;
  border-radius: 4px;
  display: flex;
  /* align-items: center; */
}

.user-message {
  max-width: 70%;
  background-color: #e1f5fe;
  align-self: flex-end;
  flex-direction: row-reverse;
}

.bot-message {
  max-width: 100%;
  background-color: #f1f8e9;
  align-self: flex-start;
}

.message-icon {
  margin: 0 10px;
  font-size: 1.2em;
}

.loading-dots {
  padding-left: 5px;
}

.dot {
  display: inline-block;
  margin-left: 5px;
  width: 8px;
  height: 8px;
  background-color: #000000;
  border-radius: 50%;
  animation: pulse 1.2s infinite ease-in-out both;
}

.dot:nth-child(2) {
  animation-delay: -0.6s;
}

@keyframes pulse {
  0%,
  100% {
    transform: scale(0.6);
    opacity: 0.4;
  }

  50% {
    transform: scale(1);
    opacity: 1;
  }
}
.sidebar-resizer {
  position: absolute;
  right: 0;
  top: 0;
  width: 4px;
  height: 100%;
  cursor: col-resize;
  background-color: transparent;
  transition: background-color 0.2s;
}

.sidebar-resizer:hover {
  background-color: #ccc;
}

.sidebar-resizer:active {
  background-color: #999;
}

.bottom-resizer {
  height: 4px;
  cursor: row-resize;
  background-color: transparent;
  transition: background-color 0.2s;
  margin-bottom: 5px;
}

.bottom-resizer:hover {
  background-color: #ccc;
}

.bottom-resizer:active {
  background-color: #999;
}

.input-container {
  display: flex;
  align-items: center;
  min-height: 40px;
}

.input-container .el-input {
  flex: 1;
  margin-right: 10px;
}

/* 媒体查询，当设备宽度小于等于 768px 时应用以下样式 */
@media (max-width: 768px) {
  .app-layout {
    flex-direction: column;
    height: 100vh;
    overflow: hidden;
  }

  .sidebar {
    width: 100% !important;
    flex-direction: row;
    justify-content: space-between;
    align-items: center;
    padding: 10px 15px;
    background-color: #fff;
    border-bottom: 1px solid #e0e0e0;
  }

  .logo-section {
    flex-direction: row;
    align-items: center;
    flex: 1;
  }

  .logo-text {
    font-size: 18px;
    font-weight: bold;
    margin-left: 10px;
  }

  .logo-section img {
    width: 36px;
    height: 36px;
  }

  .mobile-actions {
    flex-direction: row;
    gap: 8px;
  }

  .new-chat-button {
    width: auto;
    margin-top: 0;
    margin-left: 0;
    padding: 6px 12px;
    font-size: 14px;
  }

  .history-toggle-button {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 40px;
    padding: 0;
  }

  /* 移动端隐藏历史会话列表 */
  .history-section {
    display: none;
  }

  /* 移动端历史会话弹窗 */
  .mobile-history-overlay {
    display: block;
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: rgba(0, 0, 0, 0.5);
    z-index: 1000;
    display: flex;
    justify-content: flex-end;
  }

  .mobile-history-panel {
    display: flex;
    flex-direction: column;
    width: 280px;
    max-width: 90%;
    height: 100%;
    background-color: #fff;
    animation: slideIn 0.3s ease;
  }

  @keyframes slideIn {
    from {
      transform: translateX(100%);
    }
    to {
      transform: translateX(0);
    }
  }

  .mobile-history-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px 20px;
    border-bottom: 1px solid #e0e0e0;
    font-size: 16px;
    font-weight: bold;
  }

  .mobile-history-header .close-btn {
    font-size: 20px;
    cursor: pointer;
    color: #999;
    padding: 4px;
  }

  .mobile-history-list {
    flex: 1;
    overflow-y: auto;
    padding: 10px;
  }

  .mobile-history-item {
    display: flex;
    align-items: center;
    padding: 12px;
    border-radius: 8px;
    margin-bottom: 8px;
    transition: background-color 0.2s;
  }

  .mobile-history-item:hover {
    background-color: #f5f5f5;
  }

  .mobile-history-item.active {
    background-color: #e3f2fd;
  }

  .mobile-session-info {
    flex: 1;
    display: flex;
    align-items: center;
    cursor: pointer;
  }

  .mobile-session-info .fa-message-circle {
    margin-right: 10px;
    color: #10b981;
    font-size: 14px;
  }

  .mobile-session-title {
    font-size: 14px;
    color: #333;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .mobile-delete-icon {
    color: #999;
    font-size: 16px;
    cursor: pointer;
    padding: 6px;
    border-radius: 6px;
    transition: all 0.2s;
  }

  .mobile-delete-icon:hover {
    color: #ff4d4f;
    background-color: #fff2f0;
  }

  .mobile-empty-history {
    text-align: center;
    color: #999;
    font-size: 14px;
    padding: 40px 20px;
  }

  /* 移动端隐藏拖动条 */
  .sidebar-resizer {
    display: none;
  }

  .bottom-resizer {
    display: none;
  }

  .main-content {
    flex: 1;
    padding: 0;
    overflow: hidden;
  }

  .chat-container {
    height: 100%;
    padding: 10px;
  }

  .message-list {
    margin-bottom: 5px;
    border-radius: 8px;
    padding: 10px;
  }

  .message {
    padding: 12px 15px;
    margin-bottom: 8px;
  }

  .user-message {
    max-width: 85%;
  }

  .bot-message {
    max-width: 95%;
  }

  .input-container {
    padding: 10px;
    padding-bottom: calc(10px + env(safe-area-inset-bottom));
    background-color: #fff;
    border-top: 1px solid #e0e0e0;
  }

  .input-container .el-input {
    margin-right: 8px;
  }
}

/* 媒体查询，当设备宽度大于 768px 时应用原来的样式 */
@media (min-width: 769px) {
  .app-layout {
    display: flex;
    height: 100vh;
  }

  .sidebar {
    background-color: #f4f4f9;
    padding: 20px;
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .logo-section {
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .logo-text {
    font-size: 18px;
    font-weight: bold;
    margin-top: 10px;
  }

  .new-chat-button {
    width: 100%;
    margin-top: 20px;
  }
}
</style>