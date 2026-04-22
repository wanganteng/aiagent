package com.wang.aiagent.rag.database;

import com.wang.aiagent.rag.local.MyKeywordEnricher;
import com.wang.aiagent.rag.local.MyTokenTextSplitter;
import com.wang.aiagent.rag.local.TravelAppDocumentLoader;
import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class TravelAppVectorStoreConfig {

    @Resource
    private TravelAppDocumentLoader travelAppDocumentLoader;

    @Resource
    private MyTokenTextSplitter myTokenTextSplitter;

    @Resource
    private MyKeywordEnricher myKeywordEnricher;

    @Bean
    VectorStore travelAppVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel)
                .build();
        List<Document> documents = travelAppDocumentLoader.loadMarkdowns();

        List<Document> splitDocs = myTokenTextSplitter.splitDocuments(documents);
        List<Document> enrichedDocs = myKeywordEnricher.enrichDocuments(splitDocs);

        simpleVectorStore.add(enrichedDocs);
        return simpleVectorStore;
    }
}
