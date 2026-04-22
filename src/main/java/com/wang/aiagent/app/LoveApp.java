package com.wang.aiagent.app;

import com.wang.aiagent.advisor.MyLoggerAdvisor;
import com.wang.aiagent.advisor.ReReadingAdvisor;
import com.wang.aiagent.chatmemory.FileBasedChatMemory;
import com.wang.aiagent.rag.LoveAppRagCustomAdvisorFactory;
import com.wang.aiagent.rag.QueryRewriter;
import com.wang.aiagent.tools.ToolRegistration;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
@Slf4j
public class LoveApp {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = "扮演深耕恋爱心理领域的专家。开场向用户表明身份，告知用户可倾诉恋爱难题。" +
            "围绕单身、恋爱、已婚三种状态提问：单身状态询问社交圈拓展及追求心仪对象的困扰；" +
            "恋爱状态询问沟通、习惯差异引发的矛盾；已婚状态询问家庭责任与亲属关系处理的问题。" +
            "引导用户详述事情经过、对方反应及自身想法，以便给出专属解决方案。";


    /*可以创建一个工厂类，通过工厂类的方法来实现实例化，在工厂里面获取到所有注入到项目中的chatModelBean,
    这样子在调用createLoveApp时，传入的什么类型的model就能动态切换不同的model了
    @Autowired
    private Map<String, ChatModel> chatModels;
    public LoveApp createLoveApp(String modelName) {
        ChatModel chatModel = chatModels.get(modelName);
        if (chatModel == null) {
            throw new IllegalArgumentException("找不到模型: " + modelName);
        }

        return new LoveApp(chatModel, modelName);
    }*/
    public LoveApp(ChatModel ollamaChatModel) {
        //AI恋爱大师模型自动注入：dashscopeChatModel,ollamaChatModel
        // 打印实际类型
        log.info("AI恋爱大师ChatModel类型: {}", ollamaChatModel.getClass().getName());

        //初始化基于二进制文件的对话记忆
        String fileDir = System.getProperty("user.dir")+"/chat-memory";
        FileBasedChatMemory chatMemory = new FileBasedChatMemory(fileDir);

        // 初始化基于内存的对话记忆
        //ChatMemory chatMemory = new InMemoryChatMemory();

        //自定义Advisor要保证单一职责，执行顺序，高效处理(耗时)，边界处理(异常)
        //MessageChatMemoryAdvisor将对话历史作为一些列独立的消息添加到提示中，保留原始对话的完整结构。PromptChatMemoryAdvisor直接将对话拼接，会失去原始的边界
        //ChatMemory则负责对话的存储
        chatClient = ChatClient.builder(ollamaChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(chatMemory),
                        //默认对话持久化
                        //new MessageChatMemoryAdvisor(chatMemory),
                        //默认日志
                        //new SimpleLoggerAdvisor()
                        // 自定义日志 Advisor,可按需开启
                        new MyLoggerAdvisor(),
                        // 自定义推理增强 Advisor,可按需开启
                        new ReReadingAdvisor()
                )
                .build();
    }


    /**
     * AI 基础对话（支持多轮对话记忆）
     * @param message
     * @param chatId
     * @return
     */
    public String doChat(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }


    record LoveReport(String title, List<String> suggestions){}
    /**
    * AI 恋爱报告功能（实战结构化输出）
    * */
    public LoveReport doChatWithReport(String message, String chatId) {
        LoveReport loveReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成恋爱结果，标题为{用户名}的恋爱报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .entity(LoveReport.class);
        //底层通过MapOutputConverter,BeanOutPutConverter,ListOutPutConverter转换，可使用new ParameterizedTypeReference<Map<String, Object>>(){}封装复杂类型
        //.entity()作为智能路由，动态选择不同的转换器
        log.info("loveReport: {}", loveReport);
        return loveReport;
    }


    @Resource
    private VectorStore loveAppVectorStore;

    @Resource
    private QueryRewriter queryRewriter;

    /**
     * RAG知识库进行对话
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithRag(String message, String chatId) {
        //重写后的消息
        String rewriteMassage = queryRewriter.doQueryRewrite(message);

        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                // RAG应用知识库问答
                .advisors(new QuestionAnswerAdvisor(loveAppVectorStore))
                //文档过滤规则
                .advisors(LoveAppRagCustomAdvisorFactory.createLoveAppRagCustomAdvisor(loveAppVectorStore,"单身"))

                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }


    @Resource
    private Advisor loveAppRagCloudAdvisor;

    @Resource
    private VectorStore pgVectorVectorStore;


    public String doChatWithRagYun(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                // 应用增强检索服务（云知识库服务-本地java包自带）
                .advisors(loveAppRagCloudAdvisor)
                // 应用增强检索服务（云知识库服务-阿里云数据库，本地数据库）
                .advisors(new QuestionAnswerAdvisor(pgVectorVectorStore))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }


    /**
     * AI 恋爱报告功能(支持调用工具)
     */
    @Resource
    private ToolCallback[] allTools;

    public String doChatWithTools(String message, String chatId) {
        //从上下文中获取用户信息ThrealLocal
        //String loginUserName = getLoginUserName();

        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                .tools(allTools)
                //.toolContext(loginUserName)传入用户信息
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }



    //如果调用的第三方的mcp服务器，把mcp模式改为stdio传统模式，增加mcp.json配置文件(需要第三方付费的api-key),即可看到工具被集成到toolCallbackProvider类里面
    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    public String doChatWithMcp(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                .tools(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }




    /**
     * AI 基础对话（支持多轮对话记忆,SSE流式传输）
     * @param message
     * @param chatId
     * @return
     */
    public Flux<String> doChatByStream(String message, String chatId) {
        Flux<String> content = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .stream()
                .content();
        return content;
    }




}

