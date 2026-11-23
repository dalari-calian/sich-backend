package com.sich.profile;

/** Foto de perfil de um prestador ou cliente, pelo id dele (resultado de consulta em lote). */
public record OwnerPhoto(Long ownerId, String image) {
}
