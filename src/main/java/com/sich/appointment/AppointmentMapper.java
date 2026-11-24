package com.sich.appointment;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.sich.appointment.dto.AppointmentResponse;
import com.sich.common.dto.PageResponse;
import com.sich.customer.CustomerEntity;
import com.sich.profile.UserPhotoService;

import lombok.RequiredArgsConstructor;

/**
 * Monta as respostas de agendamento com as fotos de perfil do prestador e do cliente.
 * As fotos de uma lista inteira vêm em duas consultas (prestadores e clientes), nunca
 * uma por agendamento.
 */
@Component
@RequiredArgsConstructor
public class AppointmentMapper {

    private final UserPhotoService userPhotoService;

    public AppointmentResponse toResponse(AppointmentEntity appointment) {
        return toResponses(List.of(appointment)).get(0);
    }

    public List<AppointmentResponse> toResponses(List<AppointmentEntity> appointments) {
        if (appointments.isEmpty()) {
            return List.of();
        }
        Set<Long> providerIds = appointments.stream()
                .map(appointment -> appointment.getProvider().getId())
                .collect(Collectors.toSet());
        Set<Long> customerIds = appointments.stream()
                .map(AppointmentEntity::getCustomer)
                .filter(Objects::nonNull)
                .map(CustomerEntity::getId)
                .collect(Collectors.toSet());
        Map<Long, String> providerPhotos = userPhotoService.findByProviderIds(providerIds);
        Map<Long, String> customerPhotos = userPhotoService.findByCustomerIds(customerIds);
        return appointments.stream()
                .map(appointment -> AppointmentResponse.of(appointment,
                        providerPhotos.get(appointment.getProvider().getId()),
                        appointment.getCustomer() == null ? null : customerPhotos.get(appointment.getCustomer().getId())))
                .toList();
    }

    public PageResponse<AppointmentResponse> toPage(Page<AppointmentEntity> page) {
        return new PageResponse<>(
                toResponses(page.getContent()),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
