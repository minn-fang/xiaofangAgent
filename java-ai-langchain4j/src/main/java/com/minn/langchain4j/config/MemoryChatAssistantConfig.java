package com.minn.langchain4j.config;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
/*
* 配置层，创建窗口式聊天记忆，@Configuration表示这是一个配置类，即一个bean容器
* */
@Configuration
public class MemoryChatAssistantConfig {
        @Bean
        public ChatMemory chatMemory() {
                return MessageWindowChatMemory.withMaxMessages (10);
        }
}
