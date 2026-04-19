package com.wf.agent.map.service;

import com.alibaba.fastjson.JSON;
import com.wf.agent.map.entity.HistoricalCase;
import com.wf.mapper.HistoricalCaseMapper;
import com.wf.agent.map.memory.EventMemory;
import com.wf.agent.map.memory.Evidence;
import com.wf.agent.map.memory.SharedMemoryService;
import com.wf.agent.map.memory.Suggestion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 历史案例归档服务
 */
@Service
@Slf4j
public class CaseArchiveService {

    @Autowired
    private SharedMemoryService memoryService;

    @Autowired
    private HistoricalCaseMapper historicalCaseMapper;

    /**
     * 事件关闭时归档
     */
    public void archiveOnClose(String eventId) {
        log.info("[CaseArchive] 开始归档事件: {}", eventId);

        try {
            // 1. 从共享记忆读取完整决策链
            EventMemory event = memoryService.getEvent(eventId);
            if (event == null) {
                log.warn("[CaseArchive] 事件 {} 不存在", eventId);
                return;
            }

            List<Evidence> evidences = memoryService.getEvidences(eventId);
            List<Suggestion> suggestions = memoryService.getSuggestions(eventId);

            // 2. 保存到historical_case
            HistoricalCase historicalCase = new HistoricalCase();
            historicalCase.setCaseId(eventId);
            historicalCase.setEventType(event.getEventType());
            historicalCase.setStartTime(event.getStartTime());
            historicalCase.setEndTime(LocalDateTime.now());

            // 构建输入数据
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("centerCity", event.getCenterCity());
            inputData.put("affectedCities", event.getAffectedCities());
            inputData.put("metadata", event.getMetadata());
            historicalCase.setInputData(JSON.toJSONString(inputData));

            // 构建决策链
            Map<String, Object> decisionChain = new HashMap<>();
            decisionChain.put("evidences", evidences);
            decisionChain.put("suggestions", suggestions);
            historicalCase.setDecisionChain(JSON.toJSONString(decisionChain));

            // 构建最终结果
            Map<String, Object> finalResult = new HashMap<>();
            finalResult.put("status", event.getStatus());
            finalResult.put("maxDisasterLevel", event.getMaxDisasterLevel());
            historicalCase.setFinalResult(JSON.toJSONString(finalResult));

            // 设置配置版本
            historicalCase.setAgentConfigVersion(getCurrentConfigVersion());

            historicalCaseMapper.insert(historicalCase);

            // 3. 清理Redis共享记忆
            memoryService.cleanup(eventId);

            log.info("[CaseArchive] 事件 {} 归档完成", eventId);

        } catch (Exception e) {
            log.error("[CaseArchive] 归档事件 {} 失败", eventId, e);
        }
    }

    /**
     * 获取当前Agent配置版本
     */
    private String getCurrentConfigVersion() {
        // 可以从配置文件或版本控制获取
        return "v1.0.0";
    }
}
