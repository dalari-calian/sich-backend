package com.sich.common.util;

import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sich.common.exception.BusinessRuleException;

/**
 * Validação das imagens recebidas como data URL em base64
 * ({@code data:image/png|jpeg|webp;base64,...}), usadas nas fotos dos produtos e
 * na foto de perfil.
 */
public final class ImageDataUrl {

    private static final Pattern PREFIX = Pattern.compile("^data:image/(png|jpeg|webp);base64,");

    private static final int ONE_MB = 1024 * 1024;

    private ImageDataUrl() {
    }

    /** Aceita apenas png/jpeg/webp com base64 válido, não vazio e com no máximo {@code maxBytes} decodificados. */
    public static void validate(String image, int maxBytes) {
        Matcher prefix = PREFIX.matcher(image);
        if (!prefix.lookingAt()) {
            throw new BusinessRuleException(
                    "A imagem deve estar no formato data:image/(png|jpeg|webp);base64,...");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(image.substring(prefix.end()));
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("O conteúdo base64 da imagem é inválido");
        }
        if (bytes.length == 0) {
            throw new BusinessRuleException("A imagem está vazia");
        }
        if (bytes.length > maxBytes) {
            throw new BusinessRuleException("A imagem deve ter no máximo " + describe(maxBytes));
        }
    }

    private static String describe(int maxBytes) {
        return maxBytes % ONE_MB == 0 ? (maxBytes / ONE_MB) + " MB" : (maxBytes / 1024) + " KB";
    }
}
