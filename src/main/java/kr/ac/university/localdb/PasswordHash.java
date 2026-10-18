package kr.ac.university.localdb;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** 비밀번호 원문 대신 개인별 salt와 PBKDF2 해시를 저장합니다. */
public final class PasswordHash {
    private static final int ITERATIONS = 600_000;
    private PasswordHash() {}

    public static String create(char[] password) throws Exception {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return "pbkdf2-sha256$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(derive(password, salt, ITERATIONS));
    }

    public static boolean verify(char[] password, String encoded) throws Exception {
        if (encoded == null || !encoded.startsWith("pbkdf2-sha256$")) return false;
        String[] fields = encoded.split("\\$");
        byte[] salt = Base64.getDecoder().decode(fields[2]);
        byte[] expected = Base64.getDecoder().decode(fields[3]);
        return MessageDigest.isEqual(expected, derive(password, salt, Integer.parseInt(fields[1])));
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        finally { spec.clearPassword(); }
    }
}
