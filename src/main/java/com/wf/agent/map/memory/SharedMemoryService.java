package com.wf.agent.map.memory;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 共享记忆服务（Redis实现）
 */
@Service
@Slf4j
public class SharedMemoryService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_EVENT = "map:event:%s";
    private static final String KEY_EVIDENCE = "map:evidence:%s";
    private static final String KEY_SUGGESTION = "map:suggestions:%s";
    private static final String KEY_AGENT_STATE = "map:agent:%s:state";

    /**
     * 创建新事件
     */
    public String createEvent(String eventType, String centerCity) {
        String eventId = generateEventId();
        EventMemory event = new EventMemory();
        event.setEventId(eventId);
        event.setEventType(eventType);
        event.setStatus("DETECTED");
        event.setStartTime(LocalDateTime.now());
        event.setCenterCity(centerCity);
        event.setAffectedCities(new ArrayList<>());
        event.setMetadata(new HashMap<>());

        String key = String.format(KEY_EVENT, eventId);
        Map<String, String> eventMap = new HashMap<>();
        eventMap.put("eventId", eventId);
        eventMap.put("eventType", eventType);
        eventMap.put("status", "DETECTED");
        eventMap.put("startTime", LocalDateTime.now().toString());
        eventMap.put("centerCity", centerCity);
        eventMap.put("affectedCities", "[]");
        eventMap.put("metadata", "{}");

        redisTemplate.opsForHash().putAll(key, eventMap);
        redisTemplate.expire(key, 7, TimeUnit.DAYS);

        log.info("[SharedMemory] 创建事件: {}", eventId);
        return eventId;
    }

    /**
     * 获取事件
     */
    public EventMemory getEvent(String eventId) {
        String key = String.format(KEY_EVENT, eventId);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

        if (entries.isEmpty()) {
            return null;
        }

        EventMemory event = new EventMemory();
        event.setEventId((String) entries.get("eventId"));
        event.setEventType((String) entries.get("eventType"));
        event.setStatus((String) entries.get("status"));
        event.setCenterCity((String) entries.get("centerCity"));

        String startTimeStr = (String) entries.get("startTime");
        if (startTimeStr != null) {
            event.setStartTime(LocalDateTime.parse(startTimeStr));
        }

        String citiesStr = (String) entries.get("affectedCities");
        if (citiesStr != null) {
            event.setAffectedCities(JSON.parseArray(citiesStr, String.class));
        }

        String metadataStr = (String) entries.get("metadata");
        if (metadataStr != null) {
            event.setMetadata(JSON.parseObject(metadataStr, Map.class));
        }

        return event;
    }

    /**
     * 更新事件状态
     */
    public void updateEventStatus(String eventId, String status) {
        String key = String.format(KEY_EVENT, eventId);
        redisTemplate.opsForHash().put(key, "status", status);
        log.info("[SharedMemory] 事件 {} 状态更新为: {}", eventId, status);
    }

    /**
     * 添加证据（LPUSH，最新在前）
     */
    public void addEvidence(String eventId, Evidence evidence) {
        String key = String.format(KEY_EVIDENCE, eventId);
        evidence.setTimestamp(LocalDateTime.now());
        if (evidence.getEvidenceId() == null) {
            evidence.setEvidenceId(UUID.randomUUID().toString());
        }

        String evidenceJson = JSON.toJSONString(evidence);
        redisTemplate.opsForList().leftPush(key, evidenceJson);
        redisTemplate.opsForList().trim(key, 0, 49);

        log.debug("[SharedMemory] 事件 {} 添加证据: {}", eventId, evidence.getAgentName());
    }

    /**
     * 获取所有证据（从新到旧）
     */
    public List<Evidence> getEvidences(String eventId) {
        String key = String.format(KEY_EVIDENCE, eventId);
        List<String> evidenceJsons = redisTemplate.opsForList().range(key, 0, -1);

        if (evidenceJsons == null || evidenceJsons.isEmpty()) {
            return new ArrayList<>();
        }

        return evidenceJsons.stream()
                .map(json -> JSON.parseObject(json, Evidence.class))
                .collect(Collectors.toList());
    }

    /**
     * 添加建议
     */
    public void addSuggestion(String eventId, Suggestion suggestion) {
        String key = String.format(KEY_SUGGESTION, eventId);
        suggestion.setTimestamp(LocalDateTime.now());
        if (suggestion.getSuggestionId() == null) {
            suggestion.setSuggestionId(UUID.randomUUID().toString());
        }
        if (suggestion.getStatus() == null) {
            suggestion.setStatus("PENDING");
        }

        String suggestionJson = JSON.toJSONString(suggestion);
        redisTemplate.opsForList().leftPush(key, suggestionJson);

        log.debug("[SharedMemory] 事件 {} 添加建议: {}", eventId, suggestion.getAgentName());
    }

    /**
     * 获取所有建议
     */
    public List<Suggestion> getSuggestions(String eventId) {
        String key = String.format(KEY_SUGGESTION, eventId);
        List<String> suggestionJsons = redisTemplate.opsForList().range(key, 0, -1);

        if (suggestionJsons == null || suggestionJsons.isEmpty()) {
            return new ArrayList<>();
        }

        return suggestionJsons.stream()
                .map(json -> JSON.parseObject(json, Suggestion.class))
                .collect(Collectors.toList());
    }

    /**
     * 获取进行中的事件
     */
    public List<EventMemory> getActiveEvents() {
        Set<String> keys = redisTemplate.keys("map:event:*");
        if (keys == null || keys.isEmpty()) {
            return new ArrayList<>();
        }

        return keys.stream()
                .map(key -> {
                    String eventId = key.replace("map:event:", "");
                    return getEvent(eventId);
                })
                .filter(event -> event != null && !"CLOSED".equals(event.getStatus()))
                .collect(Collectors.toList());
    }

    /**
     * 清理事件数据
     */
    public void cleanup(String eventId) {
        redisTemplate.delete(String.format(KEY_EVENT, eventId));
        redisTemplate.delete(String.format(KEY_EVIDENCE, eventId));
        redisTemplate.delete(String.format(KEY_SUGGESTION, eventId));
        log.info("[SharedMemory] 清理事件 {} 数据", eventId);
    }

    /**
     * 生成事件ID
     */
    private String generateEventId() {
        return "EVT" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4);
    }
}
