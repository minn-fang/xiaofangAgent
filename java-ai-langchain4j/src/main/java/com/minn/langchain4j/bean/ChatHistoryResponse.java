package com.minn.langchain4j.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatHistoryResponse {
        
        private Long memoryId;
        
        private List<ChatMessageDTO> messages;
        
        private boolean hasHistory;
        
        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class ChatMessageDTO {
                private String role;      // "user" 或 "assistant"
                private String content;
                private Long timestamp;   // 消息时间戳（可选）
        }
}
