package com.wf.object.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeExecutionRecordEntity {
    private String nodeName;
    private String output;
    private Long timestamp;

    public NodeExecutionRecordEntity(String nodeName, String output) {
        this.nodeName = nodeName;
        this.output = output;
        this.timestamp = System.currentTimeMillis();
    }
}
