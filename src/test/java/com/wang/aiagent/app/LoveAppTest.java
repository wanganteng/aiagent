package com.wang.aiagent.app;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetriever;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetrieverOptions;
import com.wang.aiagent.demo.rag.MultiQueryExpanderDemo;
import com.wang.aiagent.rag.LoveAppDocumentLoader;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SpringBootTest
class LoveAppTest {

    @Resource
    private LoveApp loveApp;

    @Resource
    private LoveAppDocumentLoader loveAppDocumentLoader;


        @Test
        void doChat() {
            String chatId = UUID.randomUUID().toString();
            // 第一轮
            String message = "你好，我是程序员鱼皮";
            String answer = loveApp.doChat(message, chatId);
            Assertions.assertNotNull(answer);
            // 第二轮
            message = "我想让另一半（编程导航）更爱我";
            answer = loveApp.doChat(message, chatId);
            Assertions.assertNotNull(answer);
            // 第三轮
            message = "我的另一半叫什么来着？刚跟你说过，帮我回忆一下";
            answer = loveApp.doChat(message, chatId);
            Assertions.assertNotNull(answer);
        }

    /*测试导出结构化恋爱功能*/
    @Test
    void doChatWithReport() {
        String chatId = UUID.randomUUID().toString();
        // 第一轮
        String message = "你好，我是程序员鱼皮，我想让另一半（编程导航）更爱我，但我不知道该怎么做";
        LoveApp.LoveReport loveReport = loveApp.doChatWithReport(message, chatId);
        Assertions.assertNotNull(loveReport);
    }


    /**
     * 测试本地知识库文档导入到内存向量数据库
     */
    @Test
    void loadMarkdowns() {
        loveAppDocumentLoader.loadMarkdowns();
    }


    /**
     * 测试rag
     */
    @Test
    void doChatWithRag() {
        String chatId = UUID.randomUUID().toString();
        String message = "我已经结婚了，但是婚后关系不太亲密，怎么办？";
        String answer =  loveApp.doChatWithRag(message, chatId);
        Assertions.assertNotNull(answer);
    }


    /**
     * 测试rag云端知识库
     */
    @Test
    void doChatWithRagYun() {
        String chatId = UUID.randomUUID().toString();
        String message = "我已经结婚了，但是婚后关系不太亲密，怎么办？";
        String answer =  loveApp.doChatWithRagYun(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Resource
    VectorStore pgVectorVectorStore;

    /**
     * 测试pgVector向量数据库
     */
    @Test
    void testPgVectorVectorStore() {
        List<Document> documents = List.of(
                new Document("Spring AI rocks!! Spring AI rocks!! Spring AI rocks!! Spring AI rocks!! Spring AI rocks!!", Map.of("meta1", "meta1")),
                new Document("The World is Big and Salvation Lurks Around the Corner"),
                new Document("You walk forward facing the past and you turn back toward the future.", Map.of("meta2", "meta2")));
        // 添加文档
        pgVectorVectorStore.add(documents);
        // 相似度查询
        List<Document> results = pgVectorVectorStore.similaritySearch(SearchRequest.builder().query("Spring").topK(5).build());
        Assertions.assertNotNull(results);
    }



    @Resource
    private MultiQueryExpanderDemo multiQueryExpanderDemo;

    /**
     * 测试扩展词
     */
    @Test
    void expand() {
        List<Query> expand = multiQueryExpanderDemo.expand("啥是程序员鱼皮啊啊啊啊啊啊啊？！请回答我哈哈哈");
        Assertions.assertNotNull(expand);
    }



    @Test
    void doChatWithTools() {
        // 测试联网搜索问题的答案
        //testMessage("周末想带女朋友去上海约会，推荐几个适合情侣的小众打卡地？");

        // 测试网页抓取：恋爱案例分析
       // testMessage("最近和对象吵架了，看看编程导航网站（codefather.cn）的其他情侣是怎么解决矛盾的？");

        // 测试资源下载：图片下载
        testMessage("直接下载一张适合做手机壁纸的星空情侣图片为文件");

        // 测试终端操作：执行代码
        //testMessage("执行 Python3 脚本来生成数据分析报告");

        // 测试文件操作：保存用户档案
        //testMessage("保存我的恋爱档案为文件");

        // 测试 PDF 生成
        //testMessage("生成一份‘七夕约会计划’PDF，包含餐厅预订、活动流程和礼物清单");
    }

    private void testMessage(String message) {
        String chatId = UUID.randomUUID().toString();
        String answer = loveApp.doChatWithTools(message, chatId);
        Assertions.assertNotNull(answer);
    }




    @Test
    void doChatWithMcp() {
        String chatId = UUID.randomUUID().toString();
        // 测试地图 MCP
        //String message = "我的另一半居住在上海静安区，请帮我找到 5 公里内合适的约会地点。无需要看到约会地点的图片";
        //String answer =  loveApp.doChatWithMcp(message, chatId);

        // 测试图片搜索 MCP
        String message = "帮我搜索一些计算机的图片";
        String answer =  loveApp.doChatWithMcp(message, chatId);
        Assertions.assertNotNull(answer);

    }

    @Test
    public void testConnection() {
        WebClient client = WebClient.create("http://localhost:8127");

        // 测试 SSE 连接
        client.get()
                .uri("/sse")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofSeconds(30))
                .doOnSubscribe(s -> System.out.println("开始连接..."))
                .doOnNext(event -> System.out.println("收到事件: " + event))
                .doOnError(e -> System.err.println("连接错误: " + e.getMessage()))
                .doOnComplete(() -> System.out.println("连接完成"))
                .blockFirst(Duration.ofSeconds(10));
    }
}