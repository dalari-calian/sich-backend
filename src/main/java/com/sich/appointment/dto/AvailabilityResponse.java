package com.sich.appointment.dto;

import java.time.LocalDate;
import java.util.List;

public record AvailabilityResponse(
        LocalDate date,
        String duration,
        Integer durationMinutes,
        List<String> startTimes) {
}
