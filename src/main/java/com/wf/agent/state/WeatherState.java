package com.wf.agent.state;

import com.wf.object.entity.NodeExecutionRecordEntity;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class WeatherState {
    private String question;
    private Double relevanceScore;
    private String answer;
    private Double qualityScore;
    private Integer loopCount = 0;
    private String nextAction = "next";
    private String transformedQuestion;
    private String locationInfo;
    private List<NodeExecutionRecordEntity> executionRecords = new ArrayList<>();

    public WeatherState() {
    }

    public WeatherState(String question) {
        this.question = question;
    }

    public void recordNodeExecution(String nodeName, String output) {
        this.executionRecords.add(new NodeExecutionRecordEntity(nodeName, output));
    }

    public void incrementLoopCount() {
        this.loopCount++;
    }

    public boolean isRelevant() {
        return relevanceScore != null && relevanceScore >= 0.6;
    }

    public boolean isQualityAcceptable() {
        return qualityScore != null && qualityScore >= 0.9;
    }

    public boolean shouldLoop() {
        return "loop".equals(nextAction) && loopCount < 3;
    }
}
