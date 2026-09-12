package com.sich.auth;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.sich.auth.dto.UserProfileResponse;
import com.sich.auth.jwt.JwtProperties;
import com.sich.auth.jwt.JwtService;
import com.sich.common.enums.UserType;
import com.sich.customer.CustomerEntity;
import com.sich.customer.CustomerService;
import com.sich.provider.ProviderEntity;
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
                "São Paulo", "SP", "Rua A", "Apto 1");
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setEmail("john@doe.com");
        user.setUserType(UserType.CUSTOMER);
        when(userService.create("john@doe.com", "password123", UserType.CUSTOMER)).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("token");
        when(jwtProperties.getExpirationMs()).thenReturn(3600000L);

        AuthResponse response = authService.register(request);

        verify(customerService).create(user, "John Doe", "11999999999", "12345678900", "São Paulo", "SP", "Rua A", "Apto 1");
        verify(providerService, never()).create(any(), any(), any());
        assertThat(response.token()).isEqualTo("token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInMs()).isEqualTo(3600000L);
        assertThat(response.userType()).isEqualTo(UserType.CUSTOMER);
    }

    @Test
    void register_shouldCreateProviderProfile_whenUserTypeIsProvider() {
        RegisterRequest request = new RegisterRequest(
                "Jane Doe", "jane@doe.com", "11999999999", null, "password123", UserType.PROVIDER,
                null, null, null, null);
        UserEntity user = new UserEntity();
        user.setId(2L);
        user.setEmail("jane@doe.com");
        user.setUserType(UserType.PROVIDER);
        when(userService.create("jane@doe.com", "password123", UserType.PROVIDER)).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("token");
        when(jwtProperties.getExpirationMs()).thenReturn(3600000L);

        AuthResponse response = authService.register(request);

        verify(providerService).create(user, "Jane Doe", "11999999999");
        verify(customerService, never()).create(any(), any(), any(), any(), any(), any(), any(), any());
        assertThat(response.userType()).isEqualTo(UserType.PROVIDER);
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

    @Test
    void getMe_shouldReturnCustomerProfile_whenUserIsCustomer() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setEmail("john@doe.com");
        user.setUserType(UserType.CUSTOMER);
        user.setActive(true);

        CustomerEntity customer = new CustomerEntity();
        customer.setUser(user);
        customer.setName("John Doe");
        customer.setPhone("11999999999");
        customer.setCnpjCpf("12345678900");
        customer.setCity("São Paulo");
        customer.setState("SP");
        customer.setStreet("Rua A");
        customer.setComplementAdress("Apto 1");

        when(userService.getCurrentlyAuthenticatedUser()).thenReturn(user);
        when(customerService.findByUser(user)).thenReturn(customer);

        UserProfileResponse response = authService.getMe();

        verify(providerService, never()).findByUser(any());
        assertThat(response).isEqualTo(new UserProfileResponse(
                "john@doe.com", UserType.CUSTOMER,
                "John Doe", "11999999999", "12345678900",
                "São Paulo", "SP", "Rua A", "Apto 1"));
    }

    @Test
    void getMe_shouldReturnProviderProfile_whenUserIsProvider() {
        UserEntity user = new UserEntity();
        user.setId(2L);
        user.setEmail("jane@doe.com");
        user.setUserType(UserType.PROVIDER);
        user.setActive(true);

        ProviderEntity provider = new ProviderEntity();
        provider.setUser(user);
        provider.setName("Jane Doe");
        provider.setPhone("11999999999");

        when(userService.getCurrentlyAuthenticatedUser()).thenReturn(user);
        when(providerService.findByUser(user)).thenReturn(provider);

        UserProfileResponse response = authService.getMe();

        verify(customerService, never()).findByUser(any());
        assertThat(response).isEqualTo(new UserProfileResponse(
                "jane@doe.com", UserType.PROVIDER,
                "Jane Doe", "11999999999", null, null, null, null, null));
    }
}
