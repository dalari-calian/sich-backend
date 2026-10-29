package com.sich.customer.dto;

import com.sich.customer.CustomerEntity;

public record CustomerSummaryResponse(
        Long id,
        String name) {

    public static CustomerSummaryResponse of(CustomerEntity customer) {
        return new CustomerSummaryResponse(customer.getId(), customer.getName());
    }
}
