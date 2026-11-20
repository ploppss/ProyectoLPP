// Agrupa la clase Hasheador en el paquete de la aplicacion.
package vault;

// Convierte cadenas a bytes usando codificacion UTF-8.
import java.nio.charset.StandardCharsets;
// Clase Java que calcula resúmenes o hashes criptograficos.
import java.security.MessageDigest;
// Excepcion que aparece si el algoritmo solicitado no existe.
import java.security.NoSuchAlgorithmException;

/**
 * Utilidad estatica para convertir texto en su hash SHA-256.
 * Un hash no es reversible: permite compararlo, pero no recuperar el texto original.
 */
public class Hasheador {

    /**
     * Constructor privado: evita crear objetos porque todos los metodos son static.
     * Las utilidades se usan escribiendo directamente Hasheador.sha256Hex(...).
     */
    private Hasheador() { }

    /** Calcula SHA-256 y devuelve 64 caracteres hexadecimales en minusculas. */
    public static String sha256Hex(String entrada) {
        // Rechaza valores que no pueden convertirse a un hash util.
        if (entrada == null || entrada.isBlank()) {
            throw new IllegalArgumentException("La entrada a hashear no puede ser nula ni vacía.");
        }
        try {
            // Solicita al JDK una instancia del algoritmo SHA-256.
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            // Convierte el texto a bytes UTF-8 y calcula el resumen.
            byte[] digest = md.digest(entrada.getBytes(StandardCharsets.UTF_8));

            // StringBuilder construye eficientemente una cadena de caracteres.
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) {
                // %02x convierte cada byte a dos dígitos hexadecimales minusculas.
                sb.append(String.format("%02x", b));
            }
            // Devuelve los 64 caracteres hexadecimales generados.
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // Convierte un problema del JDK en una excepcion clara.
            throw new IllegalStateException("SHA-256 no está disponible en este JDK.", e);
        }
    }
}
