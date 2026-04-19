package com.wf.agent.map.graph.node;

import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.wf.agent.map.constants.MapGraphConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Month;
import java.util.HashMap;
import java.util.Map;

/**
 * 季节评估子图 - 季节信息节点
 * 职责：根据日期获取当前季节信息
 */
@Component
@Slf4j
public class SeasonInfoNode implements NodeAction {

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String dateStr = state.value(MapGraphConstants.KEY_DATE, LocalDate.now().toString());
        LocalDate date = LocalDate.parse(dateStr);
        int month = date.getMonthValue();
        String season = getSeason(month);
        
        log.info("[SeasonInfoNode] 日期：{}，月份：{}，季节：{}", dateStr, month, season);
        
        return Map.of(
            MapGraphConstants.KEY_SEASON, season,
            MapGraphConstants.KEY_MONTH, month
        );
    }

    /**
     * 根据月份判断季节
     * 春3-5/夏6-8/秋9-11/冬12-2
     */
    private String getSeason(int month) {
        if (month >= 3 && month <= 5) return "春季";
        if (month >= 6 && month <= 8) return "夏季";
        if (month >= 9 && month <= 11) return "秋季";
        return "冬季";
    }
}
