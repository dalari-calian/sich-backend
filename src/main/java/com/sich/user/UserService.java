package com.sich.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sich.common.enums.UserType;
import com.sich.common.exception.ResourceAlreadyExistsException;
import com.sich.common.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserEntity create(String email, String rawPassword, UserType type) {
        if (userRepository.existsByEmail(email)) {
            throw new ResourceAlreadyExistsException("Já existe um usuário com esse email");
        }
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setUserType(type);
        user.setActive(true);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserEntity findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }
}
