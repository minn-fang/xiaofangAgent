package com.minn.langchain4j;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.WanxImageModel;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class LLMTest {
        @Test
        public  void testGPTdemo(){
                //初始化模型
                OpenAiChatModel model = OpenAiChatModel.builder()
                        //LangChain4j提供的代理服务器，该代理服务器会将演示密钥替换成真实密钥， 再将请求转发给OpenAI API
                        .baseUrl("http://langchain4j.dev/demo/openai/v1")
                        // 设置模型api地址（如果apiKey="demo"，则可省略baseUrl的配置）
                        .apiKey("demo") //设置模型apiKey
                         .modelName("gpt-4o-mini") //设置模型名称
                         .build();
                        //向模型提问
                        String answer = model.chat("你好");
                        //输出结果
                        System.out.println(answer);
        }
        
        /**
         * 整合SpringBoot
         */
        @Autowired
        private OpenAiChatModel openAiChatModel;
        @Test
        public void testSpringBoot() {
                //向模型提问
                String answer = openAiChatModel.chat ("你用的是什么大模型");
                //输出结果
                System.out.println (answer);
        }
        
        @Autowired
        private OllamaChatModel ollamaChatModel;
        @Test
        public void testOllama() {
                //向模型提问
                String answer = ollamaChatModel.chat ("你用的是什么大模型");
                //输出结果
                System.out.println (answer);
        }
        
        
        /**
         * 通义千问大模型
         */
        @Autowired
        private QwenChatModel qwenChatModel;
        @Test
        public void testDashScopeQwen() {
                //向模型提问
                String answer = qwenChatModel.chat("你好，你是谁");
                //输出结果
                System.out.println(answer);
        }
        
        @Test
        public void testDashScopeWanX(){
                WanxImageModel wanxImageModel = WanxImageModel
                                                        .builder ()
                                                        .modelName ("wanx2.1-t2i-turbo")
                                                        .apiKey (System.getenv ("DASH_SCOPE_API_KEY"))
                                                        .build ();
                Response<Image> response = wanxImageModel.generate ("古风庭院雅集：一座宁静优美的中式庭院，亭台楼阁错落有致。池塘中，荷花盛开，荷叶田田，红色的锦鲤在水中悠然游动。庭院中央的石桌上摆放着笔墨纸砚和茶具，几位身着古装的文人雅士围坐在一起，或挥毫泼墨，或品茗吟诗。一位身着淡蓝色长袍的书生正低头沉思，旁边的一位女子手持团扇，微笑着看向他。庭院四周，翠竹环绕，石径通幽，空气中弥漫着淡淡的花香，营造出一种闲适雅致的氛围。");
                System.out.println (response.content ().url());
        }
        
}
