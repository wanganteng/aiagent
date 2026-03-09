package com.wang.aiagent.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class LoveAppVectorStoreConfig {

    @Resource
    private LoveAppDocumentLoader loveAppDocumentLoader;

    @Resource
    private MyTokenTextSplitter myTokenTextSplitter;

    @Resource
    private MyKeywordEnricher myKeywordEnricher;


    //参数可以使用百炼的向量转换模型dashscopeEmbeddingModel或者ollama的向量转换模型ollamaEmbeddingModel
    @Bean
    VectorStore loveAppVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
        //SimpleVectorStore实现了SimpleVectorStore接口，所以有写入文档的能力
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel)
                .build();
        // 加载文档
        List<Document> documents = loveAppDocumentLoader.loadMarkdowns();



        //切词器切分(切分效果其实不好)
        List<Document> documents1 = myTokenTextSplitter.splitDocuments(documents);
        // 自动补充关键词元信息
        List<Document> enrichedDocuments = myKeywordEnricher.enrichDocuments(documents);




        //其中会先调用EmbeddingModel模型，将数据变成变量然后再存储变量数据到数据库
        simpleVectorStore.add(documents);
        return simpleVectorStore;
    }
}
