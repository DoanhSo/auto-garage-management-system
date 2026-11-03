package com.Group13_65CNTT.AutoGarage.reception.entity;

import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum AttachmentFormat {
    JPEG("image/jpeg", ".jpg"), PNG("image/png", ".png"),
    WEBP("image/webp", ".webp"), HEIC("image/heic", ".heic"), PDF("application/pdf", ".pdf");

    private final String contentType;
    private final String extension;

    public static AttachmentFormat forContentType(String value) {
        return Arrays.stream(values()).filter(format -> format.contentType.equalsIgnoreCase(value))
                .findFirst().orElseThrow(() -> new BusinessException(ResponseCode.UNSUPPORTED_MEDIA_TYPE));
    }

    public boolean matches(byte[] bytes) {
        return switch (this) {
            case JPEG -> bytes.length >= 3 && (bytes[0] & 0xff) == 0xff
                    && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff;
            case PNG -> bytes.length >= 8 && Arrays.equals(Arrays.copyOf(bytes, 8),
                    new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});
            case WEBP -> at(bytes, 0, "RIFF") && at(bytes, 8, "WEBP");
            case HEIC -> at(bytes, 4, "ftyp") && (at(bytes, 8, "heic") || at(bytes, 8, "heix"));
            case PDF -> at(bytes, 0, "%PDF-");
        };
    }

    private static boolean at(byte[] bytes, int offset, String signature) {
        byte[] expected = signature.getBytes(StandardCharsets.US_ASCII);
        return bytes.length >= offset + expected.length
                && Arrays.equals(bytes, offset, offset + expected.length, expected, 0, expected.length);
    }
}
