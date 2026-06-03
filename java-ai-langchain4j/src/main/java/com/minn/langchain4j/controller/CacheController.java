package com.minn.langchain4j.controller;

import com.minn.langchain4j.store.MultiLevelChatMemoryStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "缓存管理")
@RestController
@RequestMapping("/cache")
public class CacheController {
        
        @Autowired
        private MultiLevelChatMemoryStore multiLevelStore;
        
        @Operation(summary = "清除指定会话缓存")
        @DeleteMapping("/memory/{memoryId}")
        public String evictMemoryCache(@PathVariable("memoryId") Long memoryId) {
                multiLevelStore.evictCache(memoryId);
                return "缓存已清除: memoryId=" + memoryId;
        }
}
