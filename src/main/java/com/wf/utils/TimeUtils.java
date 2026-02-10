package com.wf.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

public class TimeUtils {

    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     *  时间校齐工具
     */
    private static LocalDateTime alignToHour(LocalDateTime dt) {
        return dt.withMinute(0).withSecond(0).withNano(0);
    }

    /**
     * 获取当前时间
     */
    public static String getCurrentFormatTime() {
        return alignToHour(LocalDateTime.now()).format(FORMATTER);
    }

    /**
     * 获取当前时间(Hour)
     */
    public static String getCurrentFormatHourTime() {
        return alignToHour(LocalDateTime.now()).format(FORMATTER).substring(0,10);
    }

    /**
     * 获取之前时间
     */
    public static String acquirePastFormatTime(long amount, TimeUnit unit) {
        LocalDateTime now = alignToHour(LocalDateTime.now());
        LocalDateTime past = switch (unit) {
            case HOURS -> now.minusHours(amount);
            case MINUTES -> now.minusMinutes(amount);
            case DAYS -> now.minusDays(amount);
            default -> throw new IllegalArgumentException("不支持这种时间单位: " + unit);
        };
        return past.format(FORMATTER);
    }

    /**
     * 获取之后时间
     */
    public static String acquireFutureFormatTime(long amount, TimeUnit unit) {
        LocalDateTime now = alignToHour(LocalDateTime.now());
        LocalDateTime past = switch (unit) {
            case HOURS -> now.plusHours(amount);
            case MINUTES -> now.plusMinutes(amount);
            case DAYS -> now.plusDays(amount);
            default -> throw new IllegalArgumentException("不支持这种时间单位: " + unit);
        };
        return past.format(FORMATTER);
    }

    /**
     * 获取之前时间(Hour)
     */
    public static String acquirePastFormatHourTime(long amount, TimeUnit unit) {
        String s = acquirePastFormatTime(amount, unit);
        return s.substring(0,10);
    }


    /**
     * 获取之后时间(Hour)
     */
    public static String acquireFutureFormatHourTime(long amount, TimeUnit unit) {
        String s = acquireFutureFormatTime(amount, unit);
        return s.substring(0,10);
    }

    /**
     * 获取特定时间
     */
    public static String getHistoryWindowStart(String predictionStartTime, int encoderLengthInHours) {
        LocalDateTime start = alignToHour(LocalDateTime.parse(predictionStartTime, FORMATTER));
        return start.minusHours(encoderLengthInHours).format(FORMATTER);
    }



    public static void main(String[] args) {
        System.out.println(acquirePastFormatTime(2,TimeUnit.DAYS));
        System.out.println("下面的不用管");
        System.out.println(getCurrentFormatTime());
        System.out.println(getCurrentFormatHourTime());
        System.out.println(acquirePastFormatTime(168,TimeUnit.HOURS));
        System.out.println(getHistoryWindowStart(acquirePastFormatTime(168,TimeUnit.HOURS),5));
        System.out.println(acquirePastFormatHourTime(1,TimeUnit.DAYS));
    }

}