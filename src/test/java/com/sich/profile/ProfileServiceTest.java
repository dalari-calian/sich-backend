package com.sich.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sich.admin.AdminEntity;
import com.sich.admin.AdminService;
import com.sich.auth.dto.UserProfileResponse;
import com.sich.common.enums.UserType;
import com.sich.common.exception.BusinessRuleException;
import com.sich.customer.CustomerEntity;
import com.sich.customer.CustomerService;
import com.sich.profile.dto.AddressRequest;
import com.sich.profile.dto.PasswordChangeRequest;
import com.sich.profile.dto.PhotoRequest;
import com.sich.profile.dto.ProfileRequest;
import com.sich.provider.ProviderEntity;
import com.sich.provider.ProviderService;
import com.sich.user.UserEntity;
import com.sich.user.UserService;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    private static final String PHOTO = "data:image/png;base64,AQI=";

    @Mock
    private UserService userService;

    @Mock
    private CustomerService customerService;

    @Mock
    private ProviderService providerService;

    @Mock
    private AdminService adminService;

    @Mock
    private UserPhotoService userPhotoService;

    @InjectMocks
    private ProfileService profileService;

    private static UserEntity user(Long id, String email, UserType userType) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setEmail(email);
        user.setUserType(userType);
        return user;
    }

    private static CustomerEntity customer(UserEntity user) {
        CustomerEntity customer = new CustomerEntity();
        customer.setUser(user);
        customer.setName("John Doe");
        customer.setPhone("11999999999");
        customer.setCnpjCpf("12345678900");
        customer.setCity("São Paulo");
        customer.setState("SP");
        customer.setStreet("Rua A");
        customer.setComplementAdress("Apto 1");
        customer.setNeighborhood("Centro");
        customer.setCep("01310100");
        customer.setNumberAdress("123");
        return customer;
    }

    private CustomerEntity authenticatedAsCustomer() {
        UserEntity user = user(1L, "john@doe.com", UserType.CUSTOMER);
        CustomerEntity customer = customer(user);
        when(userService.getCurrentlyAuthenticatedUser()).thenReturn(user);
        when(customerService.findByUser(user)).thenReturn(customer);
        return customer;
    }

    private AdminEntity authenticatedAsAdmin() {
        UserEntity user = user(3L, "admin@sich.com", UserType.ADMIN);
        AdminEntity admin = new AdminEntity();
        admin.setUser(user);
        admin.setName("Admin Sich");
        when(userService.getCurrentlyAuthenticatedUser()).thenReturn(user);
        when(adminService.findByUser(user)).thenReturn(admin);
        return admin;
    }

    // --- leitura ---------------------------------------------------------------------

    @Test
    void getMe_shouldReturnCustomerProfileWithPhoto() {
        CustomerEntity customer = authenticatedAsCustomer();
        when(userPhotoService.findByUser(customer.getUser())).thenReturn(PHOTO);

        UserProfileResponse response = profileService.getMe();

        verify(providerService, never()).findByUser(any());
        assertThat(response).isEqualTo(new UserProfileResponse(
                "john@doe.com", UserType.CUSTOMER,
                "John Doe", "11999999999", "12345678900",
                "São Paulo", "SP", "Rua A", "Apto 1",
                "Centro", "01310100", "123", PHOTO, null));
    }

    @Test
    void getMe_shouldReturnProviderProfile() {
        UserEntity user = user(2L, "jane@doe.com", UserType.PROVIDER);
        ProviderEntity provider = new ProviderEntity();
        provider.setUser(user);
        provider.setName("Jane Doe");
        provider.setPhone("11988887777");
        provider.setCity("Campinas");
        when(userService.getCurrentlyAuthenticatedUser()).thenReturn(user);
        when(providerService.findByUser(user)).thenReturn(provider);

        UserProfileResponse response = profileService.getMe();

        verify(customerService, never()).findByUser(any());
        assertThat(response.userType()).isEqualTo(UserType.PROVIDER);
        assertThat(response.name()).isEqualTo("Jane Doe");
        assertThat(response.city()).isEqualTo("Campinas");
        assertThat(response.photo()).isNull();
    }

    @Test
    void getMe_shouldReturnAdminProfileWithOnlyName() {
        authenticatedAsAdmin();

        UserProfileResponse response = profileService.getMe();

        verify(customerService, never()).findByUser(any());
        verify(providerService, never()).findByUser(any());
        assertThat(response).isEqualTo(new UserProfileResponse(
                "admin@sich.com", UserType.ADMIN, "Admin Sich",
                null, null, null, null, null, null, null, null, null, null, null));
    }

    // --- meus dados ------------------------------------------------------------------

    @Test
    void updateData_shouldTrimAndSaveInformedFields() {
        CustomerEntity customer = authenticatedAsCustomer();

        UserProfileResponse response = profileService.updateData(new ProfileRequest("  Júlia Prado ", " 11977776666 ", "  "));

        assertThat(customer.getName()).isEqualTo("Júlia Prado");
        assertThat(customer.getPhone()).isEqualTo("11977776666");
        assertThat(customer.getCnpjCpf()).isNull();
        assertThat(response.name()).isEqualTo("Júlia Prado");
    }

    @Test
    void updateData_shouldKeepFieldsThatWereNotInformed() {
        CustomerEntity customer = authenticatedAsCustomer();

        profileService.updateData(new ProfileRequest(null, null, "98765432100"));

        assertThat(customer.getName()).isEqualTo("John Doe");
        assertThat(customer.getPhone()).isEqualTo("11999999999");
        assertThat(customer.getCnpjCpf()).isEqualTo("98765432100");
    }

    @Test
    void updateData_shouldUpdateProvider() {
        UserEntity user = user(2L, "jane@doe.com", UserType.PROVIDER);
        ProviderEntity provider = new ProviderEntity();
        provider.setName("Jane Doe");
        when(userService.getCurrentlyAuthenticatedUser()).thenReturn(user);
        when(providerService.findByUser(user)).thenReturn(provider);

        profileService.updateData(new ProfileRequest("Jane Studio", null, null));

        assertThat(provider.getName()).isEqualTo("Jane Studio");
    }

    @Test
    void updateData_shouldReject_whenNameIsBlank() {
        UserEntity user = user(1L, "john@doe.com", UserType.CUSTOMER);
        when(userService.getCurrentlyAuthenticatedUser()).thenReturn(user);

        assertThatThrownBy(() -> profileService.updateData(new ProfileRequest("   ", null, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("O nome não pode ficar vazio");
    }

    @Test
    void updateData_shouldReject_whenPhoneIsBlank() {
        authenticatedAsCustomer();

        assertThatThrownBy(() -> profileService.updateData(new ProfileRequest(null, "", null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("O telefone não pode ficar vazio");
    }

    @Test
    void updateData_shouldOnlyChangeNameOfAdmin() {
        AdminEntity admin = authenticatedAsAdmin();

        UserProfileResponse response = profileService.updateData(new ProfileRequest("Admin Novo", "119", "123"));

        assertThat(admin.getName()).isEqualTo("Admin Novo");
        assertThat(response.phone()).isNull();
        verify(customerService, never()).findByUser(any());
    }

    @Test
    void updateData_shouldKeepAdminName_whenNameIsNotInformed() {
        AdminEntity admin = authenticatedAsAdmin();

        profileService.updateData(new ProfileRequest(null, "119", null));

        assertThat(admin.getName()).isEqualTo("Admin Sich");
    }

    // --- endereço --------------------------------------------------------------------

    @Test
    void updateAddress_shouldSaveInformedFieldsAndKeepTheOthers() {
        CustomerEntity customer = authenticatedAsCustomer();

        UserProfileResponse response = profileService.updateAddress(
                new AddressRequest("04538-133", " Rio de Janeiro ", "RJ", "Av. Atlântica", "s/n", null, null));

        assertThat(customer.getCep()).isEqualTo("04538133");
        assertThat(customer.getCity()).isEqualTo("Rio de Janeiro");
        assertThat(customer.getState()).isEqualTo("RJ");
        assertThat(customer.getStreet()).isEqualTo("Av. Atlântica");
        assertThat(customer.getNumberAdress()).isEqualTo("s/n");
        assertThat(customer.getComplementAdress()).isEqualTo("Apto 1");
        assertThat(customer.getNeighborhood()).isEqualTo("Centro");
        assertThat(response.cep()).isEqualTo("04538133");
    }

    @Test
    void updateAddress_shouldClearFieldsSentEmpty() {
        CustomerEntity customer = authenticatedAsCustomer();

        profileService.updateAddress(new AddressRequest("", null, null, null, null, " ", ""));

        assertThat(customer.getCep()).isNull();
        assertThat(customer.getComplementAdress()).isNull();
        assertThat(customer.getNeighborhood()).isNull();
        assertThat(customer.getCity()).isEqualTo("São Paulo");
    }

    @Test
    void updateAddress_shouldReject_whenCepDoesNotHaveEightDigits() {
        authenticatedAsCustomer();

        assertThatThrownBy(() -> profileService.updateAddress(new AddressRequest("0131-01", null, null, null, null, null, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("O CEP deve ter 8 dígitos");
    }

    @Test
    void updateAddress_shouldReject_whenUserIsAdmin() {
        when(userService.getCurrentlyAuthenticatedUser()).thenReturn(user(3L, "admin@sich.com", UserType.ADMIN));

        assertThatThrownBy(() -> profileService.updateAddress(new AddressRequest(null, "SP", null, null, null, null, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("O administrador não possui endereço");
    }

    // --- senha e foto ----------------------------------------------------------------

    @Test
    void changePassword_shouldDelegateToUserServiceWithAuthenticatedUser() {
        UserEntity user = user(1L, "john@doe.com", UserType.CUSTOMER);
        when(userService.getCurrentlyAuthenticatedUser()).thenReturn(user);
        PasswordChangeRequest request = new PasswordChangeRequest("senhaAtual123", "novaSenha456");

        profileService.changePassword(request);

        verify(userService).changePassword(user, request);
    }

    @Test
    void updatePhoto_shouldSavePhotoAndReturnProfile() {
        CustomerEntity customer = authenticatedAsCustomer();
        when(userPhotoService.findByUser(customer.getUser())).thenReturn(PHOTO);

        UserProfileResponse response = profileService.updatePhoto(new PhotoRequest(PHOTO));

        verify(userPhotoService).save(customer.getUser(), PHOTO);
        assertThat(response.photo()).isEqualTo(PHOTO);
    }

    @Test
    void deletePhoto_shouldRemovePhotoOfAuthenticatedUser() {
        UserEntity user = user(1L, "john@doe.com", UserType.CUSTOMER);
        when(userService.getCurrentlyAuthenticatedUser()).thenReturn(user);

        profileService.deletePhoto();

        verify(userPhotoService).delete(user);
    }
}
