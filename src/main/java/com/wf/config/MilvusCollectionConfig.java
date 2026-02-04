package com.wf.config;

import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.collection.request.LoadCollectionReq;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@DependsOn("milvusClient")
public class MilvusCollectionConfig {

    @Autowired
    private MilvusClientV2 milvusClient;

    @Value("${milvus.collection.name:}")
    private String collectionName;

    @PostConstruct
    public void loadCollection() {
        try {
            LoadCollectionReq loadReq = LoadCollectionReq.builder()
                    .collectionName(collectionName)
                    .build();
            milvusClient.loadCollection(loadReq);
            log.info("Collection {} 加载成功", collectionName);
        } catch (Exception e) {
            log.error("Collection 加载失败: {}", e.getMessage(), e);
        }
    }
}
