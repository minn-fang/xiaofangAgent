package com.minn.langchain4j;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class XiaofApp {
        public static void main(String[] args){
                SpringApplication.run(XiaofApp.class, args);
        }
}
