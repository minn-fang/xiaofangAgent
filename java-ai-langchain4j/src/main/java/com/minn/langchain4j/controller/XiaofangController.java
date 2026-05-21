package com.minn.langchain4j.controller;

import com.minn.langchain4j.assistant.XiaofangAgent;
import com.minn.langchain4j.bean.ChatForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.lang.annotation.Target;

@Tag (name="小方施主AI")
@RestController
@RequestMapping ("/xiaofang")
public class XiaofangController {
        
        @Autowired
        private XiaofangAgent xiaofangAgent;
        
        @Operation (summary = "对话")
        @PostMapping(value = "/chat", produces = "text/stream;charset=utf-8")
        public Flux<String> chat(@RequestBody ChatForm chatForm){
                return xiaofangAgent.chat(chatForm.getMemoryId(), chatForm.getMessage());
        }
        
}
