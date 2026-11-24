package com.sich.common.util;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;

import org.junit.jupiter.api.Test;

import com.sich.common.exception.BusinessRuleException;

class ImageDataUrlTest {

    private static String dataUrl(String type, int bytes) {
        return "data:image/" + type + ";base64," + Base64.getEncoder().encodeToString(new byte[bytes]);
    }

    @Test
    void validate_shouldAcceptPngJpegAndWebpWithinLimit() {
        assertThatCode(() -> ImageDataUrl.validate(dataUrl("png", 10), 10)).doesNotThrowAnyException();
        assertThatCode(() -> ImageDataUrl.validate(dataUrl("jpeg", 1), 10)).doesNotThrowAnyException();
        assertThatCode(() -> ImageDataUrl.validate(dataUrl("webp", 1), 10)).doesNotThrowAnyException();
    }

    @Test
    void validate_shouldReject_whenFormatIsNotSupported() {
        assertThatThrownBy(() -> ImageDataUrl.validate(dataUrl("gif", 1), 10))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("A imagem deve estar no formato data:image/(png|jpeg|webp);base64,...");
    }

    @Test
    void validate_shouldReject_whenBase64IsInvalid() {
        assertThatThrownBy(() -> ImageDataUrl.validate("data:image/png;base64,@@@", 10))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("O conteúdo base64 da imagem é inválido");
    }

    @Test
    void validate_shouldReject_whenImageIsEmpty() {
        assertThatThrownBy(() -> ImageDataUrl.validate("data:image/png;base64,", 10))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("A imagem está vazia");
    }

    @Test
    void validate_shouldDescribeLimitInMegabytes_whenLimitIsWholeMegabytes() {
        int oneMb = 1024 * 1024;
        assertThatThrownBy(() -> ImageDataUrl.validate(dataUrl("png", oneMb + 1), oneMb))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("A imagem deve ter no máximo 1 MB");
    }

    @Test
    void validate_shouldDescribeLimitInKilobytes_otherwise() {
        assertThatThrownBy(() -> ImageDataUrl.validate(dataUrl("png", 2049), 2048))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("A imagem deve ter no máximo 2 KB");
    }
}
