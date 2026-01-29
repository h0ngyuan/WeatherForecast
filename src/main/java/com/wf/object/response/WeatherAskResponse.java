package com.wf.object.response;

public record WeatherAskResponse(
    String answer,
    boolean relevant,
    Double relevanceScore,
    Double finalQualityScore,
    Integer loopCount
) {}