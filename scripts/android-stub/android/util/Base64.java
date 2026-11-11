package android.util;

import java.nio.charset.StandardCharsets;

/**
 * Minimal desktop stub for android.util.Base64.
 *
 * The flag constants and the URL-safe/no-wrap/no-padding behaviour used by the
 * scrapers and MediaVideo are reproduced with java.util.Base64.
 */
public final class Base64 {
    public static final int DEFAULT = 0;
    public static final int NO_PADDING = 1;
    public static final int NO_WRAP = 2;
    public static final int CRLF = 4;
    public static final int URL_SAFE = 8;
    public static final int NO_CLOSE = 16;

    private Base64() {
    }

    public static byte[] decode(String str, int flags) {
        String value = str.replace('-', '+').replace('_', '/');
        while (value.length() % 4 != 0) value += "=";
        return java.util.Base64.getDecoder().decode(value);
    }

    public static byte[] decode(byte[] input, int flags) {
        return decode(new String(input, StandardCharsets.US_ASCII), flags);
    }

    public static String encodeToString(byte[] input, int flags) {
        java.util.Base64.Encoder encoder = (flags & URL_SAFE) != 0
                ? java.util.Base64.getUrlEncoder()
                : java.util.Base64.getEncoder();
        if ((flags & NO_PADDING) != 0) encoder = encoder.withoutPadding();
        return encoder.encodeToString(input);
    }

    public static String encode(byte[] input, int flags) {
        return encodeToString(input, flags);
    }
}
