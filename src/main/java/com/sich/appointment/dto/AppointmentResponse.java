package com.sich.appointment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import com.sich.appointment.AppointmentEntity;
import com.sich.appointment.AppointmentOrigin;
import com.sich.appointment.AppointmentStatus;
import com.sich.common.util.DurationFormat;
import com.sich.customer.CustomerEntity;

public record AppointmentResponse(
        Long id,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String duration,
        Integer durationMinutes,
        BigDecimal totalPrice,
        AppointmentStatus status,
        AppointmentOrigin origin,
        String notes,
        Long providerId,
        String providerName,
        Long customerId,
        String customerName,
        List<AppointmentItemResponse> items,
        Instant canceledAt,
        AppointmentOrigin canceledBy,
        Instant createdAt) {

    /** {@code customerName} é o nome do cliente cadastrado ou, sem cadastro, o nome digitado pelo prestador. */
    public static AppointmentResponse of(AppointmentEntity appointment) {
        CustomerEntity customer = appointment.getCustomer();
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getStartAt(),
                appointment.getEndAt(),
                DurationFormat.format(appointment.getTotalMinutes()),
                appointment.getTotalMinutes(),
                appointment.getTotalPrice(),
                appointment.getStatus(),
                appointment.getOrigin(),
                appointment.getNotes(),
                appointment.getProvider().getId(),
                appointment.getProvider().getName(),
                customer == null ? null : customer.getId(),
                customer == null ? appointment.getCustomerName() : customer.getName(),
                appointment.getItems().stream().map(AppointmentItemResponse::of).toList(),
                appointment.getCanceledAt(),
                appointment.getCanceledBy(),
                appointment.getCreatedAt());
    }
}
