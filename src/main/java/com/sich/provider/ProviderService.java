package com.sich.provider;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
