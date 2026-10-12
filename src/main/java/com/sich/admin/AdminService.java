package com.sich.admin;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sich.common.exception.ResourceNotFoundException;
import com.sich.user.UserEntity;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;

    @Transactional(readOnly = true)
    public AdminEntity findByUser(UserEntity user) {
        return adminRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Administrador não encontrado"));
    }
}
