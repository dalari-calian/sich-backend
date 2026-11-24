package com.sich.profile;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.sich.admin.AdminEntity;
import com.sich.admin.AdminService;
import com.sich.auth.dto.UserProfileResponse;
import com.sich.common.base.PersonalData;
import com.sich.common.enums.UserType;
import com.sich.common.exception.BusinessRuleException;
import com.sich.customer.CustomerService;
import com.sich.profile.dto.AddressRequest;
import com.sich.profile.dto.PasswordChangeRequest;
import com.sich.profile.dto.PhotoRequest;
import com.sich.profile.dto.ProfileRequest;
import com.sich.provider.ProviderService;
import com.sich.user.UserEntity;
import com.sich.user.UserService;

import lombok.RequiredArgsConstructor;

/**
 * Perfil do usuário autenticado: leitura ("Meu perfil") e edição de dados pessoais,
 * endereço, senha e foto. O usuário é sempre o do token. Nas edições, campo
 * {@code null} mantém o valor atual.
 */
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserService userService;
    private final CustomerService customerService;
    private final ProviderService providerService;
    private final AdminService adminService;
    private final UserPhotoService userPhotoService;

    @Transactional(readOnly = true)
    public UserProfileResponse getMe() {
        return profileOf(userService.getCurrentlyAuthenticatedUser());
    }

    /** Nome, telefone e CPF/CNPJ. Nome e telefone não podem ficar vazios; o admin só altera o nome. */
    @Transactional
    public UserProfileResponse updateData(ProfileRequest request) {
        UserEntity user = userService.getCurrentlyAuthenticatedUser();
        String name = request.name() == null ? null : required(request.name(), "O nome não pode ficar vazio");
        if (user.getUserType() == UserType.ADMIN) {
            if (name != null) {
                adminService.findByUser(user).setName(name);
            }
            return profileOf(user);
        }
        PersonalData person = personOf(user);
        if (name != null) {
            person.setName(name);
        }
        if (request.phone() != null) {
            person.setPhone(required(request.phone(), "O telefone não pode ficar vazio"));
        }
        if (request.cnpjCpf() != null) {
            person.setCnpjCpf(optional(request.cnpjCpf()));
        }
        return profileOf(user);
    }

    /** Endereço do cliente ou prestador; o administrador não tem endereço. */
    @Transactional
    public UserProfileResponse updateAddress(AddressRequest request) {
        UserEntity user = userService.getCurrentlyAuthenticatedUser();
        if (user.getUserType() == UserType.ADMIN) {
            throw new BusinessRuleException("O administrador não possui endereço");
        }
        PersonalData person = personOf(user);
        if (request.cep() != null) {
            person.setCep(cep(request.cep()));
        }
        if (request.city() != null) {
            person.setCity(optional(request.city()));
        }
        if (request.state() != null) {
            person.setState(optional(request.state()));
        }
        if (request.street() != null) {
            person.setStreet(optional(request.street()));
        }
        if (request.numberAdress() != null) {
            person.setNumberAdress(optional(request.numberAdress()));
        }
        if (request.complementAdress() != null) {
            person.setComplementAdress(optional(request.complementAdress()));
        }
        if (request.neighborhood() != null) {
            person.setNeighborhood(optional(request.neighborhood()));
        }
        return profileOf(user);
    }

    @Transactional
    public void changePassword(PasswordChangeRequest request) {
        userService.changePassword(userService.getCurrentlyAuthenticatedUser(), request);
    }

    /** Cria ou troca a foto de perfil. */
    @Transactional
    public UserProfileResponse updatePhoto(PhotoRequest request) {
        UserEntity user = userService.getCurrentlyAuthenticatedUser();
        userPhotoService.save(user, request.image());
        return profileOf(user);
    }

    @Transactional
    public void deletePhoto() {
        userPhotoService.delete(userService.getCurrentlyAuthenticatedUser());
    }

    private UserProfileResponse profileOf(UserEntity user) {
        String photo = userPhotoService.findByUser(user);
        if (user.getUserType() == UserType.ADMIN) {
            AdminEntity admin = adminService.findByUser(user);
            return UserProfileResponse.of(user, admin, photo);
        }
        return UserProfileResponse.of(user, personOf(user), photo);
    }

    private PersonalData personOf(UserEntity user) {
        return user.getUserType() == UserType.CUSTOMER
                ? customerService.findByUser(user)
                : providerService.findByUser(user);
    }

    private static String required(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessRuleException(message);
        }
        return value.trim();
    }

    private static String optional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /** CEP só com números; vazio apaga e, quando informado, precisa ter 8 dígitos. */
    private static String cep(String value) {
        String digits = value.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return null;
        }
        if (digits.length() != 8) {
            throw new BusinessRuleException("O CEP deve ter 8 dígitos");
        }
        return digits;
    }
}
