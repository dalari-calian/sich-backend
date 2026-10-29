package com.sich.workinghours.dto;

import com.sich.common.util.TimeGrid;
import com.sich.workinghours.WorkingHoursEntity;

public record WorkingIntervalResponse(
        Integer weekday,
        String start,
        String end) {

    public static WorkingIntervalResponse of(WorkingHoursEntity interval) {
        return new WorkingIntervalResponse(
                interval.getWeekday(),
                TimeGrid.formatTime(interval.getStartTime()),
                TimeGrid.formatTime(interval.getEndTime()));
    }
}
