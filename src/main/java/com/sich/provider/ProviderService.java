package com.sich.provider;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sich.auth.dto.RegisterRequest;
import com.sich.common.exception.ResourceNotFoundException;
import com.sich.provider.dto.ProviderSummaryResponse;
import com.sich.user.UserEntity;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProviderService {

    private final ProviderRepository providerRepository;

    @Transactional
    public ProviderEntity create(UserEntity user, RegisterRequest request) {
        ProviderEntity provider = new ProviderEntity();
        provider.setUser(user);
        provider.setName(request.name());
        provider.setPhone(request.phone());
        provider.setCnpjCpf(request.cnpjCpf());
        provider.setCity(request.city());
        provider.setState(request.state());
        provider.setStreet(request.street());
        provider.setComplementAdress(request.complementAdress());
        provider.setNeighborhood(request.neighborhood());
        provider.setCep(request.cep());
        provider.setNumberAdress(request.numberAdress());
        return providerRepository.save(provider);
    }

    @Transactional(readOnly = true)
    public ProviderEntity findByUser(UserEntity user) {
        return providerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Prestador não encontrado"));
    }

    @Transactional(readOnly = true)
    public ProviderEntity findById(Long id) {
        return providerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prestador não encontrado"));
    }

    /** Trava o prestador até o fim da transação corrente (deve ser chamado dentro de uma transação de escrita). */
    @Transactional
    public ProviderEntity lockById(Long id) {
        return providerRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prestador não encontrado"));
    }

    /** Busca prestadores pelo nome (parcial, sem diferenciar maiúsculas) e, opcionalmente, pela categoria. */
    @Transactional(readOnly = true)
    public List<ProviderSummaryResponse> search(String name, Long categoryId) {
        String term = name == null ? "" : name.trim();
        List<ProviderEntity> providers = categoryId == null
                ? providerRepository.findByNameContainingIgnoreCaseOrderByNameAsc(term)
                : providerRepository.searchByNameAndCategory(term, categoryId);
        return providers.stream().map(ProviderSummaryResponse::of).toList();
    }
}
