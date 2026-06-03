package com.minn.langchain4j.controller;

import com.minn.langchain4j.assistant.XiaofangAgent;
import com.minn.langchain4j.bean.ChatForm;
import com.minn.langchain4j.bean.ChatHistoryResponse;
import com.minn.langchain4j.service.ChatHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.lang.annotation.Target;

@Tag (name="小方施主AI")
@RestController
@RequestMapping ("/xiaofang")
public class XiaofangController {
        
        @Autowired
        private XiaofangAgent xiaofangAgent;
        
        @Autowired
        private ChatHistoryService chatHistoryService;
        
        @Operation (summary = "对话")
        @PostMapping(value = "/chat", produces = "text/stream;charset=utf-8")
        public Flux<String> chat(@RequestBody ChatForm chatForm){
                return xiaofangAgent.chat(chatForm.getMemoryId(), chatForm.getMessage());
        }
        
        @Operation (summary = "获取聊天历史记录")
        @GetMapping ("/history/{memoryId}")
        public ChatHistoryResponse getChatHistory(@PathVariable("memoryId") Long memoryId){
                return chatHistoryService.getChatHistory(memoryId);
        }
        
        @Operation (summary = "检查是否有历史记录")
        @GetMapping("/history/{memoryId}/exists")
        public boolean hasHistory(@PathVariable("memoryId") Long memoryId){
                return chatHistoryService.hasHistory(memoryId);
        }
        
}
