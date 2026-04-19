package com.wf.agent.map.graph.node;

import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.fastjson.JSON;
import com.wf.agent.map.constants.MapGraphConstants;
import com.wf.agent.map.entity.HistoricalCase;
import com.wf.mapper.HistoricalCaseMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 评测图 - 案例加载节点
 * 职责：从数据库加载历史案例
 */
@Component
@Slf4j
public class CaseLoaderNode implements NodeAction {

    @Autowired
    private HistoricalCaseMapper historicalCaseMapper;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String caseId = state.value("caseId", "");
        int caseLimit = state.value("caseLimit", 10);
        
        List<HistoricalCase> cases;
        try {
            if (caseId != null && !caseId.isEmpty()) {
                log.info("[CaseLoaderNode] 加载指定案例：{}", caseId);
                HistoricalCase c = historicalCaseMapper.selectByCaseId(caseId);
                cases = c != null ? Collections.singletonList(c) : Collections.emptyList();
            } else {
                log.info("[CaseLoaderNode] 加载最近 {} 个历史案例", caseLimit);
                cases = historicalCaseMapper.selectRecentCases(caseLimit);
            }
        } catch (Exception e) {
            log.warn("[CaseLoaderNode] 查询历史案例失败（表可能不存在），使用空数据: {}", e.getMessage());
            cases = Collections.emptyList();
        }

        List<Map<String, Object>> caseList = new ArrayList<>();
        for (HistoricalCase c : cases) {
            Map<String, Object> caseData = new HashMap<>();
            caseData.put("caseId", c.getCaseId());
            caseData.put("eventType", c.getEventType());
            caseData.put("inputData", c.getInputData());
            caseData.put("finalResult", c.getFinalResult());
            caseData.put("startTime", c.getStartTime());
            caseData.put("endTime", c.getEndTime());
            caseData.put("accuracyScore", c.getAccuracyScore());
            caseList.add(caseData);
        }

        log.info("[CaseLoaderNode] 加载 {} 个案例", caseList.size());
        
        return Map.of(
            MapGraphConstants.KEY_CASE_LIST, JSON.toJSONString(caseList),
            MapGraphConstants.KEY_CURRENT_CASE, caseList.isEmpty() ? "" : JSON.toJSONString(caseList.get(0))
        );
    }
}
