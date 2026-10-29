package com.sich.common.dto;

import com.sich.customer.CustomerEntity;
import com.sich.provider.ProviderEntity;

/** Endereço de um cliente ou prestador (ex.: local do atendimento na agenda). */
public record AddressResponse(
        String street,
        String numberAdress,
        String complementAdress,
        String neighborhood,
        String city,
        String state,
        String cep) {

    public static AddressResponse of(CustomerEntity customer) {
        return new AddressResponse(customer.getStreet(), customer.getNumberAdress(), customer.getComplementAdress(),
                customer.getNeighborhood(), customer.getCity(), customer.getState(), customer.getCep());
    }

    public static AddressResponse of(ProviderEntity provider) {
        return new AddressResponse(provider.getStreet(), provider.getNumberAdress(), provider.getComplementAdress(),
                provider.getNeighborhood(), provider.getCity(), provider.getState(), provider.getCep());
    }
}
