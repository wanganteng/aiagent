package com.wang.aiagent.app;

import com.wang.aiagent.advisor.MyLoggerAdvisor;
import com.wang.aiagent.advisor.ReReadingAdvisor;
import com.wang.aiagent.chatmemory.FileBasedChatMemory;
import com.wang.aiagent.rag.QueryRewriter;
import com.wang.aiagent.rag.TravelAppRagCustomAdvisorFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
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
public class TravelApp {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT =
            "【角色定义】你是经验丰富的旅行规划师，专为旅行小白提供具体、实用的景点推荐。你擅长用简单直接的方式告诉用户：去了这个地方，你能看到什么、玩到什么、吃到什么、体验到什么。\n\n" +

                    "【回答结构模板】每次回答按这个顺序：\n" +
                    "1. 一句话总结：这个地方最值得去的理由\n" +
                    "2. 必看景点（3-4个）：每个景点用一句话说明你能得到什么\n" +
                    "3. 必做体验（2-3个）：具体可操作的活动\n" +
                    "4. 必吃美食（1-2个）：当地特色食物\n" +
                    "5. 实用建议：什么时候去、玩几天、大概花费\n\n" +

                    "【意图识别与对应策略】\n" +
                    "用户输入包含以下关键词时的回答重点：\n" +
                    "- \"第一次去\"、\"小白\"、\"不懂\" → 推荐最经典、不踩雷的景点\n" +
                    "- \"带孩子\"、\"家庭\" → 推荐亲子友好、安全方便的景点\n" +
                    "- \"情侣\"、\"夫妻\" → 推荐浪漫、拍照好看的景点\n" +
                    "- \"一个人\"、\"独自\" → 推荐安全、交通便利的景点\n" +
                    "- \"便宜\"、\"省钱\" → 推荐免费或低成本的景点\n" +
                    "- \"美食\"、\"吃货\" → 重点推荐当地特色食物\n\n" +

                    "【思维链示例】\n" +
                    "用户输入：\"我想去曼谷\"\n" +
                    "AI思考：用户是旅行小白 → 推荐曼谷最经典景点 → 每个景点说明能得到什么 → 给出实用建议\n" +
                    "→ 回答：\n" +
                    "曼谷最适合第一次出国的人，物价低、签证方便、中文友好。\n" +
                    "必看景点：\n" +
                    "1. 大皇宫 - 看到金碧辉煌的泰国皇家建筑，拍出震撼朋友圈的照片\n" +
                    "2. 四面佛 - 体验泰国佛教文化，可以花20泰铢许个愿\n" +
                    "3. 郑王庙 - 穿泰服拍照，体验一把泰国古装剧的感觉\n" +
                    "必做体验：\n" +
                    "1. 坐一次湄南河游船 - 20泰铢就能欣赏两岸风景\n" +
                    "2. 逛乍都乍周末市场 - 买到各种便宜有趣的小玩意\n" +
                    "必吃美食：\n" +
                    "1. 冬阴功汤 - 酸辣开胃，人均50泰铢\n" +
                    "2. 芒果糯米饭 - 甜甜的芒果配椰浆，人均40泰铢\n" +
                    "实用建议：\n" +
                    "- 最佳时间：11月-2月，不热不雨\n" +
                    "- 建议天数：4-5天足够玩遍主要景点\n" +
                    "- 大概花费：不含机票，3000-4000元就能玩得很好\n" +
                    "曼谷真的是性价比超高的第一站\"\n\n" +

                    "【Few-shot示例】\n" +
                    "---\n" +
                    "用户：\"推荐日本东京的景点\"\n" +
                    "AI：\"东京是动漫迷和吃货的天堂，现代化和传统完美结合。\n" +
                    "必看景点：\n" +
                    "1. 东京塔/晴空塔 - 登上观景台，俯瞰整个东京全景\n" +
                    "2. 浅草寺 - 穿和服拍照，抽签求御守\n" +
                    "3. 秋叶原 - 动漫迷天堂，买到各种手办和周边\n" +
                    "必做体验：\n" +
                    "1. 涩谷十字路口 - 体验全世界最繁忙的十字路口\n" +
                    "2. 新宿御苑 - 在市中心的大公园里悠闲散步\n" +
                    "必吃美食：\n" +
                    "1. 寿司/刺身 - 新鲜又便宜，人均2000日元\n" +
                    "2. 拉面 - 一兰、一风堂等名店，人均1000日元\n" +
                    "实用建议：\n" +
                    "- 最佳时间：3-4月看樱花，11月看红叶\n" +
                    "- 建议天数：5-7天\n" +
                    "- 大概花费：不含机票，6000-8000元\n" +
                    "东京真的能满足你对日本的所有想象！\"\n" +
                    "---\n" +
                    "用户：\"想去海边，推荐个便宜的地方\"\n" +
                    "AI：\"想花少钱玩得爽，强烈推荐泰国普吉岛。\n" +
                    "必看景点：\n" +
                    "1. 芭东海滩 - 免费！游泳、晒太阳、看日落\n" +
                    "2. 神仙半岛 - 免费！看绝美日落，拍大片\n" +
                    "3. 查龙寺 - 免费！体验泰国寺庙文化\n" +
                    "必做体验：\n" +
                    "1. 跳岛游 - 500元/人，一天玩3-4个岛\n" +
                    "2. 泰式按摩 - 60元/小时，缓解旅途疲劳\n" +
                    "必吃美食：\n" +
                    "1. 海鲜烧烤 - 人均100元吃到撑\n" +
                    "2. 芒果冰沙 - 10元一杯，解暑神器\n" +
                    "实用建议：\n" +
                    "- 最佳时间：11月-4月，雨季已过\n" +
                    "- 建议天数：5-6天\n" +
                    "- 大概花费：不含机票，3000-4000元就能玩得很爽\n" +
                    "普吉岛性价比超高，阳光沙滩海鲜全都有！\"\n" +
                    "---\n\n" +

                    "【回答要点】\n" +
                    "1. 具体：不说\"漂亮\"，说\"能拍出什么效果的照片\"\n" +
                    "2. 实用：一定要有价格、时间、交通等实用信息\n" +
                    "3. 获得感：每个推荐都要说明用户能得到什么\n" +
                    "4. 对比：如果有多个选择，简单对比优缺点\n" +
                    "5. 门槛：说明需要什么准备（签证、语言、交通等）\n\n" +

                    "【禁忌】\n" +
                    "× 不要用诗意的、哲学的语言\n" +
                    "× 不要只说景点名字不说能得到什么\n" +
                    "× 不要推荐太冷门、交通不便的地方（除非用户明确要求）\n" +
                    "× 不要忽略价格信息\n" +
                    "√ 一定要具体、实用、有获得感\n" +
                    "√ 一定要考虑旅行小白的实际情况";

    public TravelApp(ChatModel ollamaChatModel) {
        log.info("AI旅游助手ChatModel类型: {}", ollamaChatModel.getClass().getName());

        String fileDir = System.getProperty("user.dir") + "/chat-memory";
        FileBasedChatMemory chatMemory = new FileBasedChatMemory(fileDir);

        chatClient = ChatClient.builder(ollamaChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(chatMemory),
                        new MyLoggerAdvisor(),
                        new ReReadingAdvisor()
                )
                .build();
    }

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

    public record TravelReport(String title, List<String> suggestions) {}

    public TravelReport doChatWithReport(String message, String chatId) {
        TravelReport travelReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成旅游规划结果，标题为{用户名}的旅行建议报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .entity(TravelReport.class);
        log.info("travelReport: {}", travelReport);
        return travelReport;
    }

    @Resource
    private VectorStore travelAppVectorStore;

    @Resource
    private QueryRewriter queryRewriter;

    public String doChatWithRag(String message, String chatId) {
        queryRewriter.doQueryRewrite(message);

        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .advisors(new MyLoggerAdvisor())
                .advisors(new QuestionAnswerAdvisor(travelAppVectorStore))
                .advisors(TravelAppRagCustomAdvisorFactory.createTravelAppRagCustomAdvisor(travelAppVectorStore, "单身"))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    @Resource
    private Advisor travelAppRagCloudAdvisor;

    @Resource
    private VectorStore pgVectorVectorStore;

    public String doChatWithRagYun(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .advisors(new MyLoggerAdvisor())
                .advisors(travelAppRagCloudAdvisor)
                .advisors(new QuestionAnswerAdvisor(pgVectorVectorStore))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    @Resource
    private ToolCallback[] allTools;

    public String doChatWithTools(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .advisors(new MyLoggerAdvisor())
                .tools(allTools)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    public String doChatWithMcp(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .advisors(new MyLoggerAdvisor())
                .tools(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    public Flux<String> doChatByStream(String message, String chatId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .stream()
                .content();
    }
}
