package com.wf.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.wf.entity.MilvusData;
import com.wf.service.MilvusService;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MilvusServiceImpl implements MilvusService {

    @Autowired
    private MilvusClientV2 milvusClient;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Value("${milvus.collection.name:wf_general_acknowledge}")
    private String collectionName;

    @Value("${milvus.collection.dimension:1024}")
    private int dimension;

    @Override
    public List<MilvusData> acquireDataFromExcel() {
        List<MilvusData> dataList = new ArrayList<>();
        String filePath = "weather_knowledge.xlsx";

        try {
            EasyExcel.read(filePath, ExcelData.class, new AnalysisEventListener<ExcelData>() {
                @Override
                public void invoke(ExcelData data, AnalysisContext context) {
                    MilvusData milvusData = new MilvusData();
                    milvusData.setContent(data.getContent());
                    milvusData.setCategory(data.getCategory());
                    dataList.add(milvusData);
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    log.info("Excel read completed, total rows: {}", dataList.size());
                }
            }).sheet().doRead();
        } catch (Exception e) {
            log.error("Error reading Excel file: {}", e.getMessage(), e);
        }

        return dataList;
    }

    @Override
    public List<MilvusData> acquireEmbedVector(List<MilvusData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return dataList;
        }

        try {
            int successCount = 0;
            for (MilvusData data : dataList) {
                if (data.getContent() != null && !data.getContent().isEmpty()) {
                    EmbeddingResponse response = embeddingModel.embedForResponse(Collections.singletonList(data.getContent()));
                    float[] vectorArray = response.getResults().get(0).getOutput();
                    List<Float> vector = new ArrayList<>();
                    for (float value : vectorArray) {
                        vector.add(value);
                    }
                    data.setVector(vector);
                    successCount++;
                }
            }
            log.info("Generated embeddings for {} items", successCount);
        } catch (Exception e) {
            log.error("Error generating embeddings: {}", e.getMessage(), e);
        }

        return dataList;
    }

    @Override
    @Transactional
    public String milvusBatchInsert(List<MilvusData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return "Data list is empty";
        }

        try {
            List<JsonObject> jsonDataList = new ArrayList<>();

            for (MilvusData data : dataList) {
                if (data.getVector() == null || data.getVector().isEmpty()) {
                    log.warn("Skipping item with empty vector: {}", data.getContent());
                    continue;
                }

                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("content", data.getContent());
                jsonObject.addProperty("category", data.getCategory());
                
                JsonArray vectorArray = new JsonArray();
                for (Float value : data.getVector()) {
                    vectorArray.add(value);
                }
                jsonObject.add("vector", vectorArray);

                jsonDataList.add(jsonObject);
            }

            if (jsonDataList.isEmpty()) {
                return "No valid data to insert";
            }

            InsertReq insertReq = InsertReq.builder()
                    .collectionName(collectionName)
                    .data(jsonDataList)
                    .build();

            milvusClient.insert(insertReq);

            log.info("Successfully inserted {} items into collection {}", jsonDataList.size(), collectionName);
            return "Success: Inserted " + jsonDataList.size() + " items";
        } catch (Exception e) {
            log.error("Error inserting data into Milvus: {}", e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
    }

    @Override
    public List<Map<String, Object>> milvusSearch(String content) {
        if (content == null || content.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            EmbeddingResponse response = embeddingModel.embedForResponse(Collections.singletonList(content));
            float[] vectorArray = response.getResults().get(0).getOutput();
            List<Float> queryVector = new ArrayList<>();
            for (float value : vectorArray) {
                queryVector.add(value);
            }

            SearchReq searchReq = SearchReq.builder()
                    .collectionName(collectionName)
                    .data(Collections.singletonList(new FloatVec(queryVector)))
                    .topK(3)
                    .outputFields(Arrays.asList("content", "category"))
                    .build();

            SearchResp searchResp = milvusClient.search(searchReq);

            List<List<SearchResp.SearchResult>> results = searchResp.getSearchResults();

            List<Map<String, Object>> searchResults = new ArrayList<>();

            for (List<SearchResp.SearchResult> perQueryResults : results) {
                for (SearchResp.SearchResult result : perQueryResults) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("score", result.getScore());
                    item.put("id", result.getId());
                    
                    Map<String, Object> entity = result.getEntity();
                    item.put("content", entity.get("content"));
                    item.put("category", entity.get("category"));
                    
                    searchResults.add(item);
                }
            }

            log.info("Search completed, found {} results", searchResults.size());
            return searchResults;
        } catch (Exception e) {
            log.error("Error searching in Milvus: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Data
    public static class ExcelData {
        @ExcelProperty("content")
        private String content;

        @ExcelProperty("category")
        private String category;
    }

    

    public static void main(String[] args) {
        MilvusServiceImpl service = new MilvusServiceImpl();
        service.test();
    }
    
    public void test() {
        System.out.println("========== Milvus 测试开始 ==========");

        System.out.println("\n1. 从 Excel 读取数据...");
        List<MilvusData> allData = acquireDataFromExcel();
        System.out.println("读取到 " + allData.size() + " 条数据");

        if (allData.isEmpty()) {
            System.out.println("数据为空，请先准备 Excel 文件");
            return;
        }

        System.out.println("\n2. 分批处理数据...");
        int batchSize = 1000;
        int totalBatches = (int) Math.ceil((double) allData.size() / batchSize);
        
        for (int i = 0; i < totalBatches; i++) {
            int fromIndex = i * batchSize;
            int toIndex = Math.min(fromIndex + batchSize, allData.size());
            List<MilvusData> batch = allData.subList(fromIndex, toIndex);
            
            System.out.println("处理批次 " + (i + 1) + "/" + totalBatches + "，大小: " + batch.size());

            System.out.println("  生成向量...");
            List<MilvusData> dataWithVectors = acquireEmbedVector(batch);
            System.out.println("  向量生成完成");

            System.out.println("  批量插入 Milvus...");
            String insertResult = milvusBatchInsert(dataWithVectors);
            System.out.println("  插入结果: " + insertResult);
        }

        System.out.println("\n3. 测试搜索...");

        System.out.println("\n搜索 1: \"我爱段佳慧\"");
        List<Map<String, Object>> results1 = milvusSearch("我爱段佳慧");
        System.out.println("找到 " + results1.size() + " 条结果:");
        for (int i = 0; i < results1.size(); i++) {
            Map<String, Object> result = results1.get(i);
            System.out.println("  结果 " + (i + 1) + ":");
            System.out.println("    相似度: " + result.get("score"));
            System.out.println("    内容: " + result.get("content"));
            System.out.println("    分类: " + result.get("category"));
        }

        System.out.println("\n搜索 2: \"南通今天天气怎么样，适合晒被子吗\"");
        List<Map<String, Object>> results2 = milvusSearch("南通今天天气怎么样，适合晒被子吗");
        System.out.println("找到 " + results2.size() + " 条结果:");
        for (int i = 0; i < results2.size(); i++) {
            Map<String, Object> result = results2.get(i);
            System.out.println("  结果 " + (i + 1) + ":");
            System.out.println("    相似度: " + result.get("score"));
            System.out.println("    内容: " + result.get("content"));
            System.out.println("    分类: " + result.get("category"));
        }

        System.out.println("\n========== Milvus 测试完成 ==========");
    }
}
