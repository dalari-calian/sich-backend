package com.sich.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.sich.common.enums.UserType;
import com.sich.common.exception.ResourceAlreadyExistsException;
import com.sich.common.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_shouldPersistUserWithEncodedPassword_whenEmailNotInUse() {
        when(userRepository.existsByEmail("john@doe.com")).thenReturn(false);
        when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserEntity result = userService.create("john@doe.com", "plain-password", UserType.CUSTOMER);

        assertThat(result.getEmail()).isEqualTo("john@doe.com");
        assertThat(result.getPassword()).isEqualTo("encoded-password");
        assertThat(result.getUserType()).isEqualTo(UserType.CUSTOMER);
        assertThat(result.isActive()).isTrue();
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void create_shouldThrow_whenEmailAlreadyInUse() {
        when(userRepository.existsByEmail("john@doe.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.create("john@doe.com", "plain-password", UserType.CUSTOMER))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessage("Já existe um usuário com esse email");

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void findByEmail_shouldReturnUser_whenFound() {
        UserEntity user = new UserEntity();
        user.setEmail("john@doe.com");
        when(userRepository.findByEmail("john@doe.com")).thenReturn(Optional.of(user));

        UserEntity result = userService.findByEmail("john@doe.com");

        assertThat(result).isSameAs(user);
    }

    @Test
    void findByEmail_shouldThrow_whenNotFound() {
        when(userRepository.findByEmail("missing@doe.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByEmail("missing@doe.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuário não encontrado");
    }

    @Test
    void findById_shouldReturnUser_whenFound() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserEntity result = userService.findById(1L);

        assertThat(result).isSameAs(user);
    }

    @Test
    void findById_shouldThrow_whenNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuário não encontrado");
    }

    @Test
    void getCurrentlyAuthenticatedUser_shouldResolveByUserIdClaim_whenAuthenticated() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setEmail("john@doe.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserDetails delegate = User.withUsername("john@doe.com").password("hash").authorities(List.of()).build();
        AuthenticatedUser principal = new AuthenticatedUser(1L, delegate);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));

        UserEntity result = userService.getCurrentlyAuthenticatedUser();

        assertThat(result).isSameAs(user);
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void getCurrentlyAuthenticatedUser_shouldThrow_whenNoAuthenticationPresent() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> userService.getCurrentlyAuthenticatedUser())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuário não autenticado");
    }

    @Test
    void getCurrentlyAuthenticatedUser_shouldThrow_whenAuthenticationIsNotAuthenticated() {
        UsernamePasswordAuthenticationToken unauthenticated =
                new UsernamePasswordAuthenticationToken("john@doe.com", "password");
        unauthenticated.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(unauthenticated);

        assertThatThrownBy(() -> userService.getCurrentlyAuthenticatedUser())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuário não autenticado");
    }

    @Test
    void getCurrentlyAuthenticatedUser_shouldThrow_whenPrincipalIsNotAuthenticatedUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("john@doe.com", null, List.of()));

        assertThatThrownBy(() -> userService.getCurrentlyAuthenticatedUser())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuário não autenticado");
    }
}
