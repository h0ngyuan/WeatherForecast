package com.wf.agent.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
public class NormalizationResult {
    private String normalizedQuestion;
    private String locationInfo;
}
