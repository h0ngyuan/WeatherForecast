package com.wf.object.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessRecord {
    private List<NodeRecord> nodeRecords;
    private Integer totalLoopCount;
    private Integer totalNodeEnterCount;

    public ProcessRecord(Integer totalLoopCount) {
        this.nodeRecords = new ArrayList<>();
        this.totalLoopCount = totalLoopCount;
        this.totalNodeEnterCount = 0;
    }

    public void addNodeRecord(NodeRecord nodeRecord) {
        this.nodeRecords.add(nodeRecord);
        this.totalNodeEnterCount++;
    }

    public void incrementLoopCount() {
        this.totalLoopCount++;
    }
}
