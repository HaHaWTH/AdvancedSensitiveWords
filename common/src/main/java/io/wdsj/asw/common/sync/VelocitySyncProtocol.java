package io.wdsj.asw.common.sync;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

public final class VelocitySyncProtocol {
    public static final int VERSION = 1;
    public static final String PATH = "/asw";

    public static final String TYPE_HELLO = "hello";
    public static final String TYPE_HELLO_OK = "hello-ok";
    public static final String TYPE_VL_INCREMENT = "vl-increment";
    public static final String TYPE_VL_SYNC = "vl-sync";
    public static final String TYPE_VL_QUERY = "vl-query";
    public static final String TYPE_VL_RESET_REQUEST = "vl-reset-request";
    public static final String TYPE_VL_RESET = "vl-reset";
    public static final String TYPE_VL_RESET_ALL = "vl-reset-all";
    public static final String TYPE_PING = "ping";
    public static final String TYPE_PONG = "pong";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private VelocitySyncProtocol() {
    }

    public static String nonce() {
        byte[] bytes = new byte[16];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public static String signature(String secret, String serverId, String nonce, long timestamp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal((serverId + nonce + timestamp).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to calculate Velocity sync signature", exception);
        }
    }

    public static boolean signatureMatches(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8)
        );
    }
}
