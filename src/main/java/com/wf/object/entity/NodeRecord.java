package com.wf.object.entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeRecord {
    private String nodeName;
    private String input;
    private String output;
    private LocalDateTime timestamp;
}
