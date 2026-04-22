package com.wang.aiagent.app;

import com.wang.aiagent.demo.rag.MultiQueryExpanderDemo;
import com.wang.aiagent.rag.local.TravelAppDocumentLoader;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
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
class TravelAppTest {

    @Resource
    private TravelApp travelApp;

    @Resource
    private TravelAppDocumentLoader travelAppDocumentLoader;

    @Test
    void doChat() {
        String chatId = UUID.randomUUID().toString();
        String message = "你好，我是程序员鱼皮";
        String answer = travelApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);

        message = "我想做一份上海三日游攻略，预算 3000 元";
        answer = travelApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);

        message = "帮我回忆一下你刚刚建议的关键景点";
        answer = travelApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithReport() {
        String chatId = UUID.randomUUID().toString();
        String message = "你好，我是程序员鱼皮，想做一份上海周末两日游计划，预算 2000 元，请帮我安排";
        TravelApp.TravelReport travelReport = travelApp.doChatWithReport(message, chatId);
        Assertions.assertNotNull(travelReport);
    }

    @Test
    void loadMarkdowns() {
        travelAppDocumentLoader.loadMarkdowns();
    }

    @Test
    void doChatWithRag() {
        String chatId = UUID.randomUUID().toString();
        String message = "我计划国庆去上海旅游，想避开人太多的景点，怎么安排？";
        String answer = travelApp.doChatWithRag(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithRagYun() {
        String chatId = UUID.randomUUID().toString();
        String message = "我计划国庆去上海旅游，想避开人太多的景点，怎么安排？";
        String answer = travelApp.doChatWithRagYun(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Resource
    VectorStore pgVectorVectorStore;

    @Test
    void testPgVectorVectorStore() {
        List<Document> documents = List.of(
                new Document("Spring AI rocks!! Spring AI rocks!! Spring AI rocks!! Spring AI rocks!! Spring AI rocks!!", Map.of("meta1", "meta1")),
                new Document("The World is Big and Salvation Lurks Around the Corner"),
                new Document("You walk forward facing the past and you turn back toward the future.", Map.of("meta2", "meta2")));
        pgVectorVectorStore.add(documents);
        List<Document> results = pgVectorVectorStore.similaritySearch(SearchRequest.builder().query("Spring").topK(5).build());
        Assertions.assertNotNull(results);
    }

    @Resource
    private MultiQueryExpanderDemo multiQueryExpanderDemo;

    @Test
    void expand() {
        List<Query> expand = multiQueryExpanderDemo.expand("啥是程序员鱼皮啊啊啊啊啊啊啊？！请回答我哈哈哈");
        Assertions.assertNotNull(expand);
    }

    @Test
    void doChatWithTools() {
        testMessage("直接下载一张适合做手机壁纸的星空旅行图片为文件");
    }

    private void testMessage(String message) {
        String chatId = UUID.randomUUID().toString();
        String answer = travelApp.doChatWithTools(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithMcp() {
        String chatId = UUID.randomUUID().toString();
        String message = "帮我搜索一些计算机的图片";
        String answer = travelApp.doChatWithMcp(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    public void testConnection() {
        WebClient client = WebClient.create("http://localhost:8127");
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
