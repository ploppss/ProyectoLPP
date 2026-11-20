// Todas las clases de la aplicación pertenecen al paquete vault.
package vault;

/**
 * Contrato común para las clases que pueden cifrar y descifrar texto.
 *
 * Una interfaz declara qué operaciones se pueden realizar, pero no define
 * la forma concreta de implementarlas. Así, GestorBoveda puede usar
 * CifradorXor sin depender directamente de todos los detalles internos.
 */
public interface Cifrable {

    /**
     * Recibe texto plano y una clave maestra.
     * Devuelve el texto transformado, normalmente preparado para guardarse.
     */
    String cifrar(String textoPlano, String claveMaestra);

    /**
     * Recibe texto cifrado y la misma clave maestra usada originalmente.
     * Devuelve el texto plano recuperado.
     */
    String descifrar(String textoCifradoBase64, String claveMaestra);
}
