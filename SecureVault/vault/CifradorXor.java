package vault;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Cifrado XOR byte a byte con clave circular, codificado en Base64. */
public class CifradorXor implements Cifrable {

    @Override
    public String cifrar(String textoPlano, String claveMaestra) {
        validar(textoPlano, "texto");
        validar(claveMaestra, "clave maestra");
        byte[] resultado = xor(textoPlano.getBytes(StandardCharsets.UTF_8),
                               claveMaestra.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(resultado);
    }

    @Override
    public String descifrar(String textoCifradoBase64, String claveMaestra) {
        validar(textoCifradoBase64, "texto cifrado");
        validar(claveMaestra, "clave maestra");
        byte[] bytesCifrados;
        try {
            bytesCifrados = Base64.getDecoder().decode(textoCifradoBase64);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("El contenido cifrado no es Base64 válido.", e);
        }
        byte[] resultado = xor(bytesCifrados, claveMaestra.getBytes(StandardCharsets.UTF_8));
        return new String(resultado, StandardCharsets.UTF_8);
    }

    private static byte[] xor(byte[] datos, byte[] clave) {
        byte[] salida = new byte[datos.length];
        for (int i = 0; i < datos.length; i++) {
            salida[i] = (byte) (datos[i] ^ clave[i % clave.length]);
        }
        return salida;
    }

    private static void validar(String valor, String campo) {
        if (valor == null || valor.isEmpty()) {
            throw new IllegalArgumentException("El " + campo + " no puede ser nulo ni vacío.");
        }
    }
}
