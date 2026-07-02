package it.smartmall.service;

import it.smartmall.model.AvailabilityRule;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AvailabilityUtils {

    private static final LocalTime DEFAULT_MORNING_END = LocalTime.of(13, 0);
    private static final LocalTime DEFAULT_AFTERNOON_START = LocalTime.of(14, 0);

    private AvailabilityUtils() {
    }

    public static List<OpeningWindow> getOpeningWindows(AvailabilityRule rule) {
        if (isClosed(rule)) {
            return List.of();
        }

        List<OpeningWindow> windows = new ArrayList<>();
        addWindow(windows, getMorningStartTime(rule), getMorningEndTime(rule));
        addWindow(windows, getAfternoonStartTime(rule), getAfternoonEndTime(rule));
        return windows;
    }

    public static Optional<OpeningWindow> findWindowContaining(AvailabilityRule rule, LocalTime time) {
        return getOpeningWindows(rule)
                .stream()
                .filter(window -> isTimeInsideWindow(time, window.start(), window.end()))
                .findFirst();
    }

    public static boolean isTimeInsideWindow(LocalTime time, LocalTime start, LocalTime end) {
        if (start == null || end == null) {
            return false;
        }

        return (time.equals(start) || time.isAfter(start)) && time.isBefore(end);
    }

    public static LocalTime getMorningStartTime(AvailabilityRule rule) {
        if (isClosed(rule)) {
            return null;
        }

        if (rule.getMorningStartTime() != null) {
            return rule.getMorningStartTime();
        }

        if (rule.getStartTime() == null || !rule.getStartTime().isBefore(DEFAULT_MORNING_END)) {
            return null;
        }

        return rule.getStartTime();
    }

    public static LocalTime getMorningEndTime(AvailabilityRule rule) {
        if (isClosed(rule)) {
            return null;
        }

        if (rule.getMorningEndTime() != null) {
            return rule.getMorningEndTime();
        }

        if (rule.getStartTime() == null || rule.getEndTime() == null ||
                !rule.getStartTime().isBefore(DEFAULT_MORNING_END)) {
            return null;
        }

        return rule.getEndTime().isAfter(DEFAULT_MORNING_END) ? DEFAULT_MORNING_END : rule.getEndTime();
    }

    public static LocalTime getAfternoonStartTime(AvailabilityRule rule) {
        if (isClosed(rule)) {
            return null;
        }

        if (rule.getAfternoonStartTime() != null) {
            return rule.getAfternoonStartTime();
        }

        if (rule.getStartTime() == null || rule.getEndTime() == null ||
                !rule.getEndTime().isAfter(DEFAULT_AFTERNOON_START)) {
            return null;
        }

        return rule.getStartTime().isAfter(DEFAULT_AFTERNOON_START)
                ? rule.getStartTime()
                : DEFAULT_AFTERNOON_START;
    }

    public static LocalTime getAfternoonEndTime(AvailabilityRule rule) {
        if (isClosed(rule)) {
            return null;
        }

        if (rule.getAfternoonEndTime() != null) {
            return rule.getAfternoonEndTime();
        }

        if (rule.getEndTime() == null || !rule.getEndTime().isAfter(DEFAULT_AFTERNOON_START)) {
            return null;
        }

        return rule.getEndTime();
    }

    public static boolean isClosed(AvailabilityRule rule) {
        return Boolean.TRUE.equals(rule.getClosed());
    }

    private static void addWindow(List<OpeningWindow> windows, LocalTime start, LocalTime end) {
        if (start != null && end != null) {
            windows.add(new OpeningWindow(start, end));
        }
    }

    public record OpeningWindow(LocalTime start, LocalTime end) {
    }
}
