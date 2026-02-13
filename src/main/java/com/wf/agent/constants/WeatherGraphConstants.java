package com.wf.agent.constants;

public class WeatherGraphConstants {
    public static final String KEY_QUESTION = "question";
    public static final String KEY_RELEVANCE_SCORE = "relevanceScore";
    public static final String KEY_ANSWER = "answer";
    public static final String KEY_QUALITY_SCORE = "qualityScore";
    public static final String KEY_LOOP_COUNT = "loopCount";
    public static final String KEY_NEXT_ACTION = "nextAction";
    public static final String KEY_TRANSFORMED_QUESTION = "transformedQuestion";
    public static final String KEY_WEATHER_CODE_QUERY = "weatherCodeQuery";
    public static final String KEY_FORECAST_RESULT = "forecastResult";
    public static final String KEY_ALERT_CHECK_RESULT = "alertCheckResult";
    public static final String KEY_GENERATE_RESULT = "generateResult";
    public static final String KEY_NEED_INTERVENTION = "needIntervention";
    public static final String KEY_HAS_PERMISSION = "hasPermission";
    public static final String KEY_HUMAN_FEEDBACK = "humanFeedback";
    public static final String KEY_EXECUTION_RECORDS = "executionRecords";
    public static final String KEY_ACTIVITY_TYPE = "activityType";
    public static final String KEY_CONCERN_CONDITION = "concernCondition";

    public static final String ACTION_NEXT = "next";
    public static final String ACTION_END = "end";
    public static final String ACTION_LOOP = "loop";
    public static final String ACTION_BREAK = "break";

    public static final double THRESHOLD_RELEVANCE = 0.6;
    public static final double THRESHOLD_QUALITY = 0.9;
    public static final int MAX_LOOP_COUNT = 3;
}