package com.sich.profile;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sich.auth.dto.UserProfileResponse;
import com.sich.profile.dto.AddressRequest;
import com.sich.profile.dto.PasswordChangeRequest;
import com.sich.profile.dto.PhotoRequest;
import com.sich.profile.dto.ProfileRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Edição do perfil do usuário autenticado (cliente ou prestador; o admin altera nome,
 * senha e foto). A leitura continua em {@code GET /auth/me}.
 */
@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
@Tag(name = "Meu perfil", description = "Edição de dados pessoais, endereço, senha e foto do usuário autenticado (a leitura é GET /auth/me)")
public class ProfileController {

    private final ProfileService profileService;

    @PatchMapping
    @Operation(summary = "Altera nome, telefone e CPF/CNPJ (\"Meus dados\"); campo ausente mantém o valor atual. "
            + "Nome ou telefone vazio retorna 422. O admin só altera o nome. Retorna o perfil atualizado")
    public ResponseEntity<UserProfileResponse> updateData(@Valid @RequestBody ProfileRequest request) {
        return ResponseEntity.ok(profileService.updateData(request));
    }

    @PatchMapping("/address")
    @Operation(summary = "Altera o endereço do cliente ou prestador; campo ausente mantém o valor atual e campo vazio apaga. "
            + "CEP com tamanho diferente de 8 dígitos ou usuário admin retornam 422. Retorna o perfil atualizado")
    public ResponseEntity<UserProfileResponse> updateAddress(@Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(profileService.updateAddress(request));
    }

    @PutMapping("/password")
    @Operation(summary = "Troca a senha conferindo a atual (422 se a atual estiver errada ou a nova for igual à atual). "
            + "O token atual continua válido")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        profileService.changePassword(request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/photo")
    @Operation(summary = "Envia ou troca a foto de perfil (data URL png/jpeg/webp, até 1 MB). Retorna o perfil atualizado; "
            + "a foto passa a aparecer em /auth/me, nos cards de prestador e nos agendamentos")
    public ResponseEntity<UserProfileResponse> updatePhoto(@Valid @RequestBody PhotoRequest request) {
        return ResponseEntity.ok(profileService.updatePhoto(request));
    }

    @DeleteMapping("/photo")
    @Operation(summary = "Remove a foto de perfil (sem foto, não faz nada); o front volta a exibir as iniciais")
    public ResponseEntity<Void> deletePhoto() {
        profileService.deletePhoto();
        return ResponseEntity.noContent().build();
    }
}
