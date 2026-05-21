package com.minn.langchain4j.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

@AiService(wiringMode = AiServiceWiringMode.EXPLICIT,
           chatModel = "qwenChatModel",
           chatMemoryProvider = "chatMemoryProvider",
           tools = "calculatorTools")
public interface SeparateChatAssistant {
        
        //@SystemMessage("请用上海话回答问题")
        @SystemMessage(fromResource = "prompt-content.txt")
        String chat(@MemoryId int memoryId, @UserMessage String userMessage);
}
