package com.sich.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sich.common.exception.ResourceNotFoundException;
import com.sich.user.UserEntity;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private AdminRepository adminRepository;

    @InjectMocks
    private AdminService adminService;

    @Test
    void findByUser_shouldReturnAdmin_whenFound() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        AdminEntity admin = new AdminEntity();
        admin.setUser(user);
        when(adminRepository.findByUserId(1L)).thenReturn(Optional.of(admin));

        AdminEntity result = adminService.findByUser(user);

        assertThat(result).isSameAs(admin);
    }

    @Test
    void findByUser_shouldThrow_whenNotFound() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        when(adminRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.findByUser(user))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Administrador não encontrado");
    }
}
