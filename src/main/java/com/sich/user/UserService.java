package com.sich.user;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sich.auth.dto.RegisterRequest;
import com.sich.common.exception.BusinessRuleException;
import com.sich.common.exception.ResourceAlreadyExistsException;
import com.sich.common.exception.ResourceNotFoundException;
import com.sich.profile.dto.PasswordChangeRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserEntity create(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ResourceAlreadyExistsException("Já existe um usuário com esse email");
        }
        UserEntity user = new UserEntity();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setUserType(request.userType());
        user.setActive(true);
        return userRepository.save(user);
    }

    /** Troca a senha depois de conferir a atual; a nova precisa ser diferente da atual. */
    @Transactional
    public void changePassword(UserEntity user, PasswordChangeRequest request) {
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessRuleException("A senha atual está incorreta");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BusinessRuleException("A nova senha deve ser diferente da atual");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserEntity findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }

    @Transactional(readOnly = true)
    public UserEntity findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }

    @Transactional(readOnly = true)
    public UserEntity getCurrentlyAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            throw new ResourceNotFoundException("Usuário não autenticado");
        }
        return findById(principal.userId());
    }
}
