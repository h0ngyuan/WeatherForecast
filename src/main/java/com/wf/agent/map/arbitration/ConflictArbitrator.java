package com.wf.agent.map.arbitration;

import com.wf.agent.base.AIClient;
import com.wf.agent.map.memory.SharedMemoryService;
import com.wf.agent.map.memory.Suggestion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 冲突仲裁器
 */
@Component
@Slf4j
public class ConflictArbitrator {

    @Autowired
    private SharedMemoryService memoryService;

    @Autowired
    private AIClient aiClient;

    /**
     * 仲裁冲突建议
     */
    public ArbitrationResult arbitrate(String eventId) {
        log.info("[ConflictArbitrator] 开始仲裁事件: {}", eventId);

        List<Suggestion> suggestions = memoryService.getSuggestions(eventId);

        if (suggestions.isEmpty()) {
            log.warn("[ConflictArbitrator] 事件 {} 没有建议", eventId);
            return new ArbitrationResult(eventId, new ArrayList<>(), "NO_SUGGESTIONS", "没有建议需要仲裁");
        }

        // 按类型分组
        Map<String, List<Suggestion>> grouped = suggestions.stream()
                .filter(s -> "PENDING".equals(s.getStatus()))
                .collect(Collectors.groupingBy(Suggestion::getType));

        List<ResolvedSuggestion> resolved = new ArrayList<>();

        for (Map.Entry<String, List<Suggestion>> entry : grouped.entrySet()) {
            if (entry.getValue().size() > 1) {
                // 有冲突，需要仲裁
                Suggestion winner = resolveConflict(entry.getValue());
                ResolvedSuggestion rs = new ResolvedSuggestion();
                rs.setAccepted(winner);
                rs.setRejected(entry.getValue().stream()
                        .filter(s -> !s.getSuggestionId().equals(winner.getSuggestionId()))
                        .collect(Collectors.toList()));
                rs.setResolutionReason("置信度优先");
                resolved.add(rs);
            } else {
                // 无冲突
                ResolvedSuggestion rs = new ResolvedSuggestion();
                rs.setAccepted(entry.getValue().get(0));
                rs.setRejected(new ArrayList<>());
                rs.setResolutionReason("唯一建议");
                resolved.add(rs);
            }
        }

        String method = resolved.size() > 1 ? "VOTE" : "CONFIDENCE";
        String reasoning = generateReasoning(resolved);

        log.info("[ConflictArbitrator] 仲裁完成，方法: {}", method);
        return new ArbitrationResult(eventId, resolved, method, reasoning);
    }

    /**
     * 解决冲突
     */
    private Suggestion resolveConflict(List<Suggestion> conflicting) {
        // 策略1：置信度差距大，直接选高的
        double maxConfidence = conflicting.stream()
                .mapToDouble(Suggestion::getConfidence)
                .max()
                .orElse(0);
        double minConfidence = conflicting.stream()
                .mapToDouble(Suggestion::getConfidence)
                .min()
                .orElse(0);

        if (maxConfidence - minConfidence > 0.2) {
            return conflicting.stream()
                    .max(Comparator.comparingDouble(Suggestion::getConfidence))
                    .orElse(conflicting.get(0));
        }

        // 策略2：置信度接近，选优先级高的
        return conflicting.stream()
                .max(Comparator.comparingInt(Suggestion::getPriority)
                        .thenComparingDouble(Suggestion::getConfidence))
                .orElse(conflicting.get(0));
    }

    private String generateReasoning(List<ResolvedSuggestion> resolved) {
        StringBuilder sb = new StringBuilder();
        sb.append("仲裁结果：\n");
        for (ResolvedSuggestion rs : resolved) {
            sb.append("- 采纳 ").append(rs.getAccepted().getAgentName())
                    .append(" 的建议（置信度").append(rs.getAccepted().getConfidence()).append("）\n");
            if (!rs.getRejected().isEmpty()) {
                sb.append("  拒绝：");
                for (Suggestion rejected : rs.getRejected()) {
                    sb.append(rejected.getAgentName()).append(" ");
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * 仲裁结果
     */
    public static class ArbitrationResult {
        private String eventId;
        private LocalDateTime arbitrationTime;
        private List<ResolvedSuggestion> resolvedSuggestions;
        private String arbitrationMethod;
        private String reasoning;

        public ArbitrationResult(String eventId, List<ResolvedSuggestion> resolvedSuggestions,
                                 String arbitrationMethod, String reasoning) {
            this.eventId = eventId;
            this.arbitrationTime = LocalDateTime.now();
            this.resolvedSuggestions = resolvedSuggestions;
            this.arbitrationMethod = arbitrationMethod;
            this.reasoning = reasoning;
        }

        // Getters
        public String getEventId() { return eventId; }
        public LocalDateTime getArbitrationTime() { return arbitrationTime; }
        public List<ResolvedSuggestion> getResolvedSuggestions() { return resolvedSuggestions; }
        public String getArbitrationMethod() { return arbitrationMethod; }
        public String getReasoning() { return reasoning; }
    }

    /**
     * 解决的建议
     */
    public static class ResolvedSuggestion {
        private Suggestion accepted;
        private List<Suggestion> rejected;
        private String resolutionReason;

        // Getters and Setters
        public Suggestion getAccepted() { return accepted; }
        public void setAccepted(Suggestion accepted) { this.accepted = accepted; }
        public List<Suggestion> getRejected() { return rejected; }
        public void setRejected(List<Suggestion> rejected) { this.rejected = rejected; }
        public String getResolutionReason() { return resolutionReason; }
        public void setResolutionReason(String resolutionReason) { this.resolutionReason = resolutionReason; }
    }
}
