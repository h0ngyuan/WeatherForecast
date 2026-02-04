package com.wf.config;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Getter
@Configuration
public class MilvusConfig {

    @Value("${milvus.uri:}")
    private String uri;

    @Value("${milvus.token:}")
    private String token;

    @Value("${milvus.collection.name:}")
    private String collectionName;

    @Value("${milvus.collection.dimension:1536}")
    private int dimension;

    @Bean
    public MilvusClientV2 milvusClient() {
        ConnectConfig connectConfig = ConnectConfig.builder()
                .uri(uri)
                .token(token)
                .build();
        return new MilvusClientV2(connectConfig);
    }
}
