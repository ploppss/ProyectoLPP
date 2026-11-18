package vault;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hasheador {

    private Hasheador() { }

    /** SHA-256 en hexadecimal minúsculas (64 caracteres). */
    public static String sha256Hex(String entrada) {
        if (entrada == null || entrada.isBlank()) {
            throw new IllegalArgumentException("La entrada a hashear no puede ser nula ni vacía.");
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(entrada.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible en este JDK.", e);
        }
    }
}
