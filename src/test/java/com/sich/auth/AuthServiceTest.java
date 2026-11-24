package com.sich.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

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

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private CustomerService customerService;

    @Mock
    private ProviderService providerService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_shouldCreateCustomerProfile_whenUserTypeIsCustomer() {
        RegisterRequest request = new RegisterRequest(
                "John Doe", "john@doe.com", "11999999999", "12345678900", "password123", UserType.CUSTOMER,
                "São Paulo", "SP", "Rua A", "Apto 1", "Centro", "01310-100", "123");
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setEmail("john@doe.com");
        user.setUserType(UserType.CUSTOMER);
        when(userService.create(request)).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("token");
        when(jwtProperties.getExpirationMs()).thenReturn(3600000L);

        AuthResponse response = authService.register(request);

        verify(customerService).create(user, request);
        verify(providerService, never()).create(any(), any());
        assertThat(response.token()).isEqualTo("token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInMs()).isEqualTo(3600000L);
        assertThat(response.userType()).isEqualTo(UserType.CUSTOMER);
    }

    @Test
    void register_shouldCreateProviderProfile_whenUserTypeIsProvider() {
        RegisterRequest request = new RegisterRequest(
                "Jane Doe", "jane@doe.com", "11999999999", null, "password123", UserType.PROVIDER,
                null, null, null, null, null, null, null);
        UserEntity user = new UserEntity();
        user.setId(2L);
        user.setEmail("jane@doe.com");
        user.setUserType(UserType.PROVIDER);
        when(userService.create(request)).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("token");
        when(jwtProperties.getExpirationMs()).thenReturn(3600000L);

        AuthResponse response = authService.register(request);

        verify(providerService).create(user, request);
        verify(customerService, never()).create(any(), any());
        assertThat(response.userType()).isEqualTo(UserType.PROVIDER);
    }

    @Test
    void register_shouldThrow_whenUserTypeIsAdmin() {
        RegisterRequest request = new RegisterRequest(
                "Admin", "admin@sich.com", "11999999999", null, "password123", UserType.ADMIN,
                null, null, null, null, null, null, null);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Não é permitido cadastrar administradores");
        verify(userService, never()).create(any());
        verify(customerService, never()).create(any(), any());
        verify(providerService, never()).create(any(), any());
    }

    @Test
    void login_shouldAuthenticateAndIssueToken() {
        LoginRequest request = new LoginRequest("john@doe.com", "password123");
        UserEntity user = new UserEntity();
        user.setEmail("john@doe.com");
        user.setUserType(UserType.CUSTOMER);
        when(userService.findByEmail("john@doe.com")).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("token");
        when(jwtProperties.getExpirationMs()).thenReturn(3600000L);

        AuthResponse response = authService.login(request);

        verify(authenticationManager).authenticate(
                eq(new UsernamePasswordAuthenticationToken("john@doe.com", "password123")));
        assertThat(response.token()).isEqualTo("token");
        assertThat(response.userType()).isEqualTo(UserType.CUSTOMER);
    }
}
