package com.wf.object.request;

public record WeatherAskRequest(String question) {
    public WeatherAskRequest {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("Question cannot be blank");
        }
    }
}