package com.sich.provider;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sich.common.exception.ResourceNotFoundException;
import com.sich.user.UserEntity;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProviderService {

    private final ProviderRepository providerRepository;

    @Transactional
    public ProviderEntity create(UserEntity user, String name, String phone) {
        ProviderEntity provider = new ProviderEntity();
        provider.setUser(user);
        provider.setName(name);
        provider.setPhone(phone);
        return providerRepository.save(provider);
    }

    @Transactional(readOnly = true)
    public ProviderEntity findByUser(UserEntity user) {
        return providerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Prestador não encontrado"));
    }
}
