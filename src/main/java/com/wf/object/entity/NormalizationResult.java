package com.wf.object.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NormalizationResult {
    //其实这个类完全不需要，懒得改了
    private String normalizedQuestion;
    private String requestInfo;
}
