package com.minn.langchain4j.service;

import com.minn.langchain4j.bean.ChatHistoryResponse;

public interface ChatHistoryService {
        
        ChatHistoryResponse getChatHistory(Long memoryId);
        
        boolean hasHistory(Long memoryId);
}
