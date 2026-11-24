package com.sich.auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sich.auth.dto.AuthResponse;
import com.sich.auth.dto.LoginRequest;
import com.sich.auth.dto.RegisterRequest;
import com.sich.auth.jwt.JwtProperties;
import com.sich.auth.jwt.JwtService;
import com.sich.common.enums.UserType;
import com.sich.common.exception.BusinessRuleException;
import com.sich.customer.CustomerService;
import com.sich.provider.ProviderService;
import com.sich.user.UserEntity;
import com.sich.user.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final CustomerService customerService;
    private final ProviderService providerService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.userType() == UserType.ADMIN) {
            throw new BusinessRuleException("Não é permitido cadastrar administradores");
        }
        UserEntity user = userService.create(request);
        createProfile(user, request);
        return issueToken(user);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        UserEntity user = userService.findByEmail(request.email());
        return issueToken(user);
    }

    private void createProfile(UserEntity user, RegisterRequest request) {
        if (request.userType() == UserType.CUSTOMER) {
            customerService.create(user, request);
        } else {
            providerService.create(user, request);
        }
    }

    private AuthResponse issueToken(UserEntity user) {
        String token = jwtService.generateToken(user);
        return AuthResponse.bearer(token, jwtProperties.getExpirationMs(), user);
    }
}
