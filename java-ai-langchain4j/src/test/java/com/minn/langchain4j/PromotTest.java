package com.minn.langchain4j;

import com.minn.langchain4j.assistant.MemoryChatAssistant;
import com.minn.langchain4j.assistant.SeparateChatAssistant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class PromotTest {
        @Autowired
        private SeparateChatAssistant separateChatAssistant;
        @Test
        public void testPrompt() {
                String answer = separateChatAssistant.chat(4, "我是小方施主，打球吗");
                System.out.println (answer);
        }
}
