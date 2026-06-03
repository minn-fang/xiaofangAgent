package com.minn.langchain4j.service.serviceImpl;

import com.minn.langchain4j.bean.ChatHistoryResponse;
import com.minn.langchain4j.bean.ChatMessages;
import com.minn.langchain4j.service.ChatHistoryService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChatHistoryServiceImpl implements ChatHistoryService {
        
        private static final Logger log = LoggerFactory.getLogger(ChatHistoryServiceImpl.class);
        
        @Autowired
        private MongoTemplate mongoTemplate;
        
        @Override
        public ChatHistoryResponse getChatHistory(Long memoryId) {
                log.info("查询聊天记录: memoryId={}", memoryId);
                
                ChatHistoryResponse response = new ChatHistoryResponse();
                response.setMemoryId(memoryId);
                
                ChatMessages chatMessages = loadFromMongoDB(memoryId);
                
                if (chatMessages == null || chatMessages.getContent() == null) {
                        response.setHasHistory(false);
                        response.setMessages(new ArrayList<>());
                        return response;
                }
                
                List<ChatHistoryResponse.ChatMessageDTO> messageDTOs = convertToDTOs(chatMessages.getContent());
                response.setMessages(messageDTOs);
                response.setHasHistory(!messageDTOs.isEmpty());
                
                log.info("返回聊天记录数: memoryId={}, count={}", memoryId, messageDTOs.size());
                
                return response;
        }
        
        @Override
        public boolean hasHistory(Long memoryId) {
                ChatMessages chatMessages = loadFromMongoDB(memoryId);
                return chatMessages != null && chatMessages.getContent() != null;
        }
        
        private ChatMessages loadFromMongoDB(Long memoryId) {
                Criteria criteria = Criteria.where("memoryId").is(memoryId.toString());
                Query query = new Query(criteria);
                return mongoTemplate.findOne(query, ChatMessages.class);
        }
        
        private List<ChatHistoryResponse.ChatMessageDTO> convertToDTOs(String content) {
                List<ChatHistoryResponse.ChatMessageDTO> dtos = new ArrayList<>();
                
                try {
                        List<ChatMessage> messages = dev.langchain4j.data.message.ChatMessageDeserializer.messagesFromJson(content);
                        
                        for (ChatMessage message : messages) {
                                ChatHistoryResponse.ChatMessageDTO dto = new ChatHistoryResponse.ChatMessageDTO ();
                                
                                if (message instanceof UserMessage) {
                                        dto.setRole("user");
                                        dto.setContent(((UserMessage) message).singleText());
                                } else if (message instanceof AiMessage) {
                                        dto.setRole("assistant");
                                        dto.setContent(((AiMessage) message).text());
                                } else {
                                        continue;
                                }
                                
                                dto.setTimestamp(System.currentTimeMillis());
                                dtos.add(dto);
                        }
                } catch (Exception e) {
                        log.error("解析聊天记录失败: {}", e.getMessage());
                }
                
                return dtos;
        }
}
