package com.sich.appointment.dto;

import java.math.BigDecimal;

import com.sich.appointment.AppointmentItemEntity;
import com.sich.common.util.DurationFormat;

public record AppointmentItemResponse(
        Long productId,
        String name,
        BigDecimal price,
        String duration,
        Integer durationMinutes) {

    public static AppointmentItemResponse of(AppointmentItemEntity item) {
        return new AppointmentItemResponse(
                item.getProduct() == null ? null : item.getProduct().getId(),
                item.getProductName(),
                item.getPrice(),
                DurationFormat.format(item.getDurationMinutes()),
                item.getDurationMinutes());
    }
}
