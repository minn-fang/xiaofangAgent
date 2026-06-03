# xiaofangAgent
	小智医疗（医疗领域垂直智能体系统/后端/AI Agent）

项目描述：基于LangChain4j构建医疗垂直领域智能问答与业务办理一体化Agent，融合RAG、ReAct推理框架与FunctionCalling，实现医疗问答、挂号预约及路线规划等业务闭环。

	Agent架构与RAG优化：基于LangChain4j构建AIService动态代理架构，设计Agent + Memory + Tool一体化执行链路；基于PDFBox + TextEmbedding + Pinecone构建医疗知识库，通过语义分块与向量检索优化，提升复杂问答召回与抗幻觉能力。

	FunctionCalling与业务工具：基于@Tool封装挂号、医务查询等业务逻辑，实现自然语言到Java方法的自动映射与参数校验，构建ReAct推理驱动的工具调用闭环。

	流式交互与上下文管理：基于WebFlux + Reacto r实现SSE流式输出与背压控制，设计Redis + MongoDB 多级存储架构，利用 Redis 缓存活跃会话实现毫秒级上下文恢复，结合 MongoDB 持久化全量历史对话，多用户 @MemoryId 会话隔离与上下文持久化。

	高德MCP集成与多模型适配：引入MCP协议对接高德地图服务，实现路径规划等能力的标准化调用；基于SPI机制实现DeepSeek/Qwen/Ollama多模型插件化接入与动态切换。

技术栈：Java / LangChain4j / RAG / Agent / ReAct / MCP / Pinecone / Redis / MongoDB / WebFlux / Reactor / SpringBoot / 高德。


<img width="1908" height="1063" alt="小方施主（医疗版）1" src="https://github.com/user-attachments/assets/f5189fec-37a2-449e-9210-c85c5ef6d8fb" />
<img width="1915" height="1025" alt="小方施主（医疗版）2" src="https://github.com/user-attachments/assets/d52ddb76-0917-4890-8353-f21f339e50f3" />
<img width="1905" height="1016" alt="小方施主（医疗版）3" src="https://github.com/user-attachments/assets/6393e4b2-2ea0-4eae-a0d2-a294163ab3ce" />
<img width="1914" height="1070" alt="小方施主（医疗版）4" src="https://github.com/user-attachments/assets/eebbddcf-1ff1-4c70-8e53-64a643bb3882" />
<img width="1919" height="1072" alt="小方施主（医疗版）5" src="https://github.com/user-attachments/assets/eb1566fd-a062-4fd4-912c-303f62b285e4" />
<img width="700" height="153" alt="小方施主（医疗版）数据库" src="https://github.com/user-attachments/assets/2d3595da-88e1-4c6d-b0c0-95c564cd9b46" />
<img width="1200" height="2670" alt="小方施主（医疗版，手机页面）" src="https://github.com/user-attachments/assets/ae7d7de5-5655-4f1b-9d1a-de1658875b53" />



