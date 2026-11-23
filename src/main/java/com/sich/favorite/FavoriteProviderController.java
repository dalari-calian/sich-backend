package com.sich.favorite;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sich.provider.dto.ProviderSummaryResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/customers/me/favorites")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "Favoritos do cliente", description = "Prestadores favoritados pelo cliente autenticado. "
        + "Os cards de prestador (busca, em alta, detalhe) também trazem o campo favorite")
public class FavoriteProviderController {

    private final FavoriteProviderService favoriteProviderService;

    @GetMapping
    @Operation(summary = "Lista os prestadores favoritos (cards com foto, menor preço e categorias), do mais recente para o mais antigo")
    public ResponseEntity<List<ProviderSummaryResponse>> findAll() {
        return ResponseEntity.ok(favoriteProviderService.findAll());
    }

    @PutMapping("/{providerId}")
    @Operation(summary = "Favorita o prestador (idempotente: repetir não duplica) e retorna o card dele; 404 se o prestador não existir")
    public ResponseEntity<ProviderSummaryResponse> add(@Parameter(description = "Id do prestador") @PathVariable Long providerId) {
        return ResponseEntity.ok(favoriteProviderService.add(providerId));
    }

    @DeleteMapping("/{providerId}")
    @Operation(summary = "Remove o prestador dos favoritos (idempotente: se não era favorito, também retorna 204)")
    public ResponseEntity<Void> remove(@Parameter(description = "Id do prestador") @PathVariable Long providerId) {
        favoriteProviderService.remove(providerId);
        return ResponseEntity.noContent().build();
    }
}
