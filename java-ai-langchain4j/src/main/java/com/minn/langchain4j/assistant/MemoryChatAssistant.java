package com.minn.langchain4j.assistant;

import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

/*
* 初级智能体
* */
@AiService(wiringMode = AiServiceWiringMode.EXPLICIT,
           chatModel = "qwenChatModel",
           chatMemory = "chatMemory")
public interface MemoryChatAssistant {
        String chat(String userMessage);
}
