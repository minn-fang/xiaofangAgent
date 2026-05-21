package com.minn.langchain4j.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import reactor.core.publisher.Flux;

@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT,
        //chatModel = "qwenChatModel",
        streamingChatModel = "qwenStreamingChatModel",
        chatMemoryProvider = "chatMemoryProviderXiaofang",
        tools = "appointmentTools",
        contentRetriever = "contentRetrieverXiaofangPincone"
)

public interface XiaofangAgent {
        
        @SystemMessage(fromResource = "xiaofangAgent-prompt.txt")
        Flux<String> chat(@MemoryId Long memoryId , @UserMessage String userMessage);
}
