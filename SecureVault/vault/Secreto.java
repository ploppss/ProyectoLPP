package vault;

/**
 * Clase abstracta base de todos los secretos de la bóveda.
 * Los setters son public final y validan que el valor no sea null ni esté en blanco.
 */
public abstract class Secreto {
    private String id;
    private String nombre;
    private String hashNombre;
    private String contenidoCifrado;

    public Secreto(String id, String nombre, String hashNombre, String contenidoCifrado) {
        setId(id);
        setNombre(nombre);
        setHashNombre(hashNombre);
        setContenidoCifrado(contenidoCifrado);
    }

    /** Validación compartida con las subclases. */
    protected static String validar(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El campo '" + campo + "' no puede ser nulo ni estar vacío.");
        }
        return valor;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getHashNombre() { return hashNombre; }
    public String getContenidoCifrado() { return contenidoCifrado; }

    public final void setId(String id) { this.id = validar(id, "id"); }
    public final void setNombre(String nombre) { this.nombre = validar(nombre, "nombre"); }
    public final void setHashNombre(String hashNombre) { this.hashNombre = validar(hashNombre, "hashNombre"); }
    public final void setContenidoCifrado(String contenidoCifrado) {
        this.contenidoCifrado = validar(contenidoCifrado, "contenidoCifrado");
    }

    /** Cada tipo de secreto muestra su detalle de forma distinta (polimorfismo). */
    public abstract void mostrarDetalle(String contenidoPlano);
}
