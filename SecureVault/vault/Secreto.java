// Agrupa la jerarquia de modelos dentro del mismo paquete de la aplicacion.
package vault;

/**
 * Representa los datos comunes que comparten todas las clases de secreto.
 * Es abstracta porque no existe un secreto generico: debe ser una credencial
 * o una nota concreta.
 */
public abstract class Secreto {
    // private impide que estas variables se accedan directamente desde otras clases.
    private String id;
    private String nombre;
    private String hashNombre;
    private String contenidoCifrado;

    /**
     * Construye un secreto con sus datos comunes.
     * Cada setter valida y asigna el valor correspondiente.
     */
    public Secreto(String id, String nombre, String hashNombre, String contenidoCifrado) {
        setId(id);
        setNombre(nombre);
        setHashNombre(hashNombre);
        setContenidoCifrado(contenidoCifrado);
    }

    /**
     * Metodo protegido: las subclases pueden usarlo y las demas clases no.
     * isBlank rechaza cadenas vacias y cadenas formadas solo por espacios.
     */
    protected static String validar(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            // IllegalArgumentException indica que un argumento recibido no es valido.
            throw new IllegalArgumentException("El campo '" + campo + "' no puede ser nulo ni estar vacío.");
        }
        // Solo devuelve el valor si supero la validacion.
        return valor;
    }

    // Los getters permiten leer cada atributo conservando su encapsulacion.
    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getHashNombre() { return hashNombre; }
    public String getContenidoCifrado() { return contenidoCifrado; }

    // public final permite modificar los datos, pero impide sobrescribir el metodo.
    public final void setId(String id) { this.id = validar(id, "id"); }
    public final void setNombre(String nombre) { this.nombre = validar(nombre, "nombre"); }
    public final void setHashNombre(String hashNombre) { this.hashNombre = validar(hashNombre, "hashNombre"); }
    public final void setContenidoCifrado(String contenidoCifrado) {
        this.contenidoCifrado = validar(contenidoCifrado, "contenidoCifrado");
    }

    /**
     * Declaracion abstracta: obliga a cada subclase a mostrar su propio detalle.
     * El polimorfismo hace que se execute la version apropiada segun el tipo real.
     */
    public abstract void mostrarDetalle(String contenidoPlano);
}
