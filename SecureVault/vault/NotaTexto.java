// Incluye esta clase concreta dentro del paquete vault.
package vault;

/**
 * Modelo especializado para almacenar una nota de texto.
 * Solo añade la categoria porque el resto de datos proviene de Secreto.
 */
public class NotaTexto extends Secreto {
    // Variable exclusiva de las notas.
    private String categoria;

    /**
     * Construye la nota reutilizando los datos comunes mediante super().
     */
    public NotaTexto(String id, String nombre, String hashNombre, String contenidoCifrado, String categoria) {
        super(id, nombre, hashNombre, contenidoCifrado);
        setCategoria(categoria);
    }

    // Getter para consultar la categoria desde otra clase.
    public String getCategoria() { return categoria; }

    // Setter final que valida la categoria antes de asignarla.
    public final void setCategoria(String categoria) { this.categoria = validar(categoria, "categoria"); }

    // Implementacion propia del metodo abstracto heredado.
    @Override
    public void mostrarDetalle(String contenidoPlano) {
        // Imprime los datos generales y el contenido plano descifrado.
        System.out.println("============= NOTA =============");
        System.out.println("ID       : " + getId());
        System.out.println("Nombre   : " + getNombre());
        System.out.println("Categoría: " + categoria);
        System.out.println("Contenido:");
        System.out.println(contenidoPlano);
        System.out.println("================================");
    }
}
