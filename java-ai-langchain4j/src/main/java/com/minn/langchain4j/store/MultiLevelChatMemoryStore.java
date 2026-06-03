package com.minn.langchain4j.store;

import com.alibaba.fastjson2.JSON;
import com.minn.langchain4j.bean.ChatMessages;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class MultiLevelChatMemoryStore implements ChatMemoryStore {
        
        private static final Logger log = LoggerFactory.getLogger(MultiLevelChatMemoryStore.class);
        
        private static final String REDIS_KEY_PREFIX = "chat:memory:";
        private static final long REDIS_TTL_HOURS = 24;
        
        @Autowired
        private RedisTemplate<String, Object> redisTemplate;
        
        @Autowired
        private MongoTemplate mongoTemplate;
        
        @Override
        public List<ChatMessage> getMessages(Object memoryId) {
                String key = REDIS_KEY_PREFIX + memoryId;
                
                try {
                        Object cached = redisTemplate.opsForValue().get(key);
                        if (cached != null) {
                                log.debug("Redis缓存命中: memoryId={}", memoryId);
                                return deserializeMessages(cached.toString());
                        }
                } catch (Exception e) {
                        log.warn("Redis读取失败，降级到MongoDB: {}", e.getMessage());
                }
                
                return loadFromMongoDB(memoryId, key);
        }
        
        private List<ChatMessage> loadFromMongoDB(Object memoryId, String redisKey) {
                log.debug("从MongoDB加载: memoryId={}", memoryId);
                
                Criteria criteria = Criteria.where("memoryId").is(memoryId.toString());
                Query query = new Query(criteria);
                
                Object chatMessagesObj = mongoTemplate.findOne(query, ChatMessages.class);
                if (chatMessagesObj == null) {
                        return new LinkedList<>();
                }
                
                ChatMessages chatMessages = (ChatMessages) chatMessagesObj;
                String content = chatMessages.getContent();
                List<ChatMessage> messages = ChatMessageDeserializer.messagesFromJson(content);
                
                cacheToRedis(redisKey, content);
                
                return messages;
        }
        
        @Override
        public void updateMessages(Object memoryId, List<ChatMessage> messages) {
                String key = REDIS_KEY_PREFIX + memoryId;
                String jsonContent = ChatMessageSerializer.messagesToJson(messages);
                
                cacheToRedis(key, jsonContent);
                
                asyncPersistToMongoDB(memoryId.toString(), jsonContent);
        }
        
        private void cacheToRedis(String key, String content) {
                try {
                        redisTemplate.opsForValue().set(key, content, REDIS_TTL_HOURS, TimeUnit.HOURS);
                        log.debug("Redis缓存更新成功: key={}", key);
                } catch (Exception e) {
                        log.error("Redis写入失败: {}", e.getMessage());
                }
        }
        
        private void asyncPersistToMongoDB(String memoryId, String content) {
                try {
                        Criteria criteria = Criteria.where("memoryId").is(memoryId);
                        Query query = new Query(criteria);
                        Update update = new Update();
                        update.set("content", content);
                        
                        mongoTemplate.upsert(query, update, ChatMessages.class);
                        log.debug("MongoDB持久化成功: memoryId={}", memoryId);
                } catch (Exception e) {
                        log.error("MongoDB写入失败: {}", e.getMessage());
                }
        }
        
        @Override
        public void deleteMessages(Object memoryId) {
                String key = REDIS_KEY_PREFIX + memoryId;
                
                try {
                        redisTemplate.delete(key);
                } catch (Exception e) {
                        log.warn("Redis删除失败: {}", e.getMessage());
                }
                
                try {
                        Criteria criteria = Criteria.where("memoryId").is(memoryId.toString());
                        Query query = new Query(criteria);
                        mongoTemplate.remove(query, ChatMessages.class);
                } catch (Exception e) {
                        log.error("MongoDB删除失败: {}", e.getMessage());
                }
        }
        
        public void evictCache(Object memoryId) {
                String key = REDIS_KEY_PREFIX + memoryId;
                redisTemplate.delete(key);
                log.info("手动清除缓存: memoryId={}", memoryId);
        }
        
        private List<ChatMessage> deserializeMessages(String json) {
                return ChatMessageDeserializer.messagesFromJson(json);
        }
}
