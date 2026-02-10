package com.wf.service;

import com.wf.object.entity.MilvusData;

import java.util.List;
import java.util.Map;

public interface MilvusService {
    
    List<MilvusData> acquireDataFromExcel();

    List<MilvusData> acquireEmbedVector(List<MilvusData> dataList);

    String milvusBatchInsert(List<MilvusData> dataList);

    List<Map<String, Object>> milvusSearch(String content);
}
