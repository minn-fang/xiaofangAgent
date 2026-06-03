package com.minn.langchain4j.store;

import com.minn.langchain4j.bean.ChatMessages;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;

@Component
public class MongoChatMemoryStore implements ChatMemoryStore {
        
        @Autowired
        private MongoTemplate mongoTemplate;
        
        @Override
        public List<ChatMessage> getMessages (Object memoryId) {
                //从MongoDB数据库中获取指定memoryId的聊天记录
                Criteria criteria = Criteria.where("memoryId").is(memoryId);
                //创建查询对象
                Query query = new Query(criteria);
                //使用查询对象查询数据
                ChatMessages chatMessages = mongoTemplate.findOne (query, ChatMessages.class);
                if(chatMessages == null){
                        return  new LinkedList<> ();
                }
                //将JSON字符串转换为ChatMessage对象列表
                String content = chatMessages.getContent ();
                return ChatMessageDeserializer.messagesFromJson (content);
        }
        
        @Override
        public void updateMessages (Object memoryId, List<ChatMessage> list) {
                //将新的聊天记录保存到MongoDB数据库中，criteria对象用于匹配指定的memoryId
                Criteria criteria = Criteria.where("memoryId").is(memoryId);
                Query query = new Query(criteria);
                //创建更新对象
                Update update = new Update();
                //序列化聊天记录
                update.set("content", ChatMessageSerializer.messagesToJson ( list));
                //修改或新增，如果已存在则更新，否则新增
                mongoTemplate.upsert(query, update, ChatMessages.class);
        }
        
        @Override
        public void deleteMessages (Object memoryId) {
                Criteria criteria = Criteria.where("memoryId").is(memoryId);
                Query query = new Query(criteria);
                mongoTemplate.remove(query, ChatMessages.class);
        }
}
