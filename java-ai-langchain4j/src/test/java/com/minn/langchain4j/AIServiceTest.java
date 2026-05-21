package com.minn.langchain4j;

import com.minn.langchain4j.assistant.Assistant;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class AIServiceTest {
        @Autowired
        private QwenChatModel qwenChatModel;
        @Test
        public void testDashScopeQwen() {
                Assistant assistant = AiServices.create (Assistant.class, qwenChatModel);
                String answer = assistant.chat ("你用的是什么大模型");
                System.out.println (answer);
        }
        
        @Autowired
        private  Assistant assistant;
        @Test
        public void testAssistant () {
                String answer = assistant.chat ("你用的是什么大模型，我又是谁");
                System.out.println (answer);
        }
}
