// Agrupa esta clase en el paquete vault, igual que las demas clases del proyecto.
package vault;

// Permite codificar textos como UTF-8 y obtener el juego de caracteres estandar.
import java.nio.charset.StandardCharsets;
// Proporciona los metodos para convertir bytes a Base64 y viceversa.
import java.util.Base64;

/**
 * Aplica una operación XOR a los bytes del texto y los bytes de una clave.
 *
 * La misma operación sirve para cifrar y descifrar. Después de cifrar, el
 * resultado se convierte a texto Base64 para que sea fácil de almacenar.
 *
 * IMPORTANTE: XOR no es seguro para proteger secretos reales por sí solo.
 * Base64 solo cambia la representación de los datos; no cifra ni protege nada.
 */
public class CifradorXor implements Cifrable {

    /**
     * Convierte el texto plano y la clave maestra a bytes y les aplica XOR.
     * Finalmente, codifica los bytes resultantes en Base64.
     */
    // Indica que este metodo implementa la operación declarada por Cifrable.
    @Override
    public String cifrar(String textoPlano, String claveMaestra) {
        // No se permite cifrar un texto vacío ni usar una clave vacía.
        validar(textoPlano, "texto");
        validar(claveMaestra, "clave maestra");

        // UTF-8 permite trabajar correctamente con caracteres como tildes o ñ.
        byte[] resultado = xor(textoPlano.getBytes(StandardCharsets.UTF_8),
                               claveMaestra.getBytes(StandardCharsets.UTF_8));

        // Base64 convierte los bytes cifrados en caracteres ASCII legibles.
        return Base64.getEncoder().encodeToString(resultado);
    }

    // Indica que este metodo implementa la operación de descifrado de Cifrable.
    /**
     * Decodifica el Base64 y vuelve a aplicar XOR con la clave maestra.
     * Al aplicar XOR dos veces con los mismos datos se recupera el texto original.
     */
    @Override
    public String descifrar(String textoCifradoBase64, String claveMaestra) {
        // Comprueba que existan los dos datos necesarios antes de continuar.
        validar(textoCifradoBase64, "texto cifrado");
        validar(claveMaestra, "clave maestra");

        // Reservamos la variable fuera del try para usarla después de decodificar.
        byte[] bytesCifrados;
        try {
            // Primero se convierte el texto Base64 a los bytes cifrados originales.
            bytesCifrados = Base64.getDecoder().decode(textoCifradoBase64);
        } catch (IllegalArgumentException e) {
            // Si los caracteres no forman Base64 valido, se explica el problema.
            throw new IllegalArgumentException("El contenido cifrado no es Base64 válido.", e);
        }

        // Aplicar XOR otra vez invierte el cifrado realizado por cifrar().
        byte[] resultado = xor(bytesCifrados, claveMaestra.getBytes(StandardCharsets.UTF_8));

        // Los bytes recuperados representan texto codificado con UTF-8.
        return new String(resultado, StandardCharsets.UTF_8);
    }

    /**
     * Combina cada byte de datos con un byte de la clave.
     *
     * El operador XOR (^), aplicado dos veces con el mismo byte, devuelve
     * el valor original. Cuando los datos son más largos que la clave,
     * i % clave.length hace que la clave se reutilice de forma circular.
     */
    private static byte[] xor(byte[] datos, byte[] clave) {
        // Crea una salida del mismo tamaño que los datos de entrada.
        byte[] salida = new byte[datos.length];
        // Recorre cada posicion: i comienza en cero y avanza una unidad por vuelta.
        for (int i = 0; i < datos.length; i++) {
            // El resto permite elegir clave[0], clave[1]... y volver a empezar.
            salida[i] = (byte) (datos[i] ^ clave[i % clave.length]);
        }
        // Devuelve el arreglo con los bytes transformados.
        return salida;
    }

    /**
     * Comprueba que un valor recibido no sea null ni una cadena vacía.
     * El parametro campo permite personalizar el mensaje de error.
     */
    private static void validar(String valor, String campo) {
        if (valor == null || valor.isEmpty()) {
            // Lanza una excepcion porque no es valido continuar sin ese dato.
            throw new IllegalArgumentException("El " + campo + " no puede ser nulo ni vacío.");
        }
    }
}
