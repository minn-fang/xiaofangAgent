package com.minn.langchain4j.aspect;

import com.minn.langchain4j.store.MultiLevelChatMemoryStore;
import dev.langchain4j.data.message.ChatMessage;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
public class ChatMemoryAspect {
        
        private static final Logger log = LoggerFactory.getLogger(ChatMemoryAspect.class);
        
        @Autowired
        private MultiLevelChatMemoryStore multiLevelStore;
        
        private final Map<Object, Long> lastUpdateTime = new ConcurrentHashMap<>();
        private static final long DEBOUNCE_INTERVAL_MS = 1000;
        
        @Pointcut("execution(* dev.langchain4j.store.memory.chat.ChatMemoryStore.updateMessages(..))")
        public void updateMessagesPointcut() {}
        
        @Around("updateMessagesPointcut()")
        public Object aroundUpdateMessages(ProceedingJoinPoint joinPoint) throws Throwable {
                Object[] args = joinPoint.getArgs();
                Object memoryId = args[0];
                @SuppressWarnings("unchecked")
                List<ChatMessage> messages = (List<ChatMessage>) args[1];
                
                log.debug("AOP拦截记忆更新: memoryId={}, 消息数={}", memoryId, messages.size());
                
                Object result = joinPoint.proceed();
                
                debouncedPersist(memoryId, messages);
                
                return result;
        }
        
        private void debouncedPersist(Object memoryId, List<ChatMessage> messages) {
                long now = System.currentTimeMillis();
                Long lastTime = lastUpdateTime.get(memoryId);
                
                if (lastTime == null || (now - lastTime) > DEBOUNCE_INTERVAL_MS) {
                        lastUpdateTime.put(memoryId, now);
                        asyncPersist(memoryId, messages);
                }
        }
        
        @Async
        public void asyncPersist(Object memoryId, List<ChatMessage> messages) {
                try {
                        log.debug("异步持久化开始: memoryId={}", memoryId);
                        multiLevelStore.updateMessages(memoryId, messages);
                        log.debug("异步持久化完成: memoryId={}", memoryId);
                } catch (Exception e) {
                        log.error("异步持久化失败: memoryId={}, error={}", memoryId, e.getMessage());
                }
        }
        
        @Pointcut("execution(* dev.langchain4j.store.memory.chat.ChatMemoryStore.getMessages(..))")
        public void getMessagesPointcut() {}
        
        @Around("getMessagesPointcut()")
        public Object aroundGetMessages(ProceedingJoinPoint joinPoint) throws Throwable {
                Object memoryId = joinPoint.getArgs()[0];
                
                log.debug("AOP拦截记忆读取: memoryId={}", memoryId);
                
                long start = System.currentTimeMillis();
                Object result = joinPoint.proceed();
                long duration = System.currentTimeMillis() - start;
                
                log.debug("记忆读取耗时: memoryId={}, {}ms", memoryId, duration);
                
                return result;
        }
}
