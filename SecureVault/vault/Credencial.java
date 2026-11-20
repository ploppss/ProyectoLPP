// Incluye esta clase concreta dentro del paquete vault.
package vault;

/**
 * Modelo especializado para guardar una cuenta o credencial.
 * Hereda los datos generales de Secreto y añade usuario y servicio.
 */
public class Credencial extends Secreto {
    // Datos exclusivos de una credencial.
    private String usuario;
    private String servicio;

    /**
     * Construye la credencial.
     * super() envia los datos comunes al constructor heredado de Secreto.
     */
    public Credencial(String id, String nombre, String hashNombre, String contenidoCifrado,
                      String usuario, String servicio) {
        super(id, nombre, hashNombre, contenidoCifrado);
        setUsuario(usuario);
        setServicio(servicio);
    }

    // Getters para consultar usuario y servicio sin acceder directamente al atributo.
    public String getUsuario() { return usuario; }
    public String getServicio() { return servicio; }

    // Los setters finalizan cada dato mediante el metodo validar heredado.
    public final void setUsuario(String usuario) { this.usuario = validar(usuario, "usuario"); }
    public final void setServicio(String servicio) { this.servicio = validar(servicio, "servicio"); }

    // @Override confirma que se esta implementando el metodo abstracto de Secreto.
    @Override
    public void mostrarDetalle(String contenidoPlano) {
        // println escribe una linea completa y salta a la siguiente.
        System.out.println("========== CREDENCIAL ==========");
        System.out.println("ID        : " + getId());
        System.out.println("Nombre    : " + getNombre());
        System.out.println("Servicio  : " + servicio);
        System.out.println("Usuario   : " + usuario);
        System.out.println("Contraseña: " + contenidoPlano);
        System.out.println("================================");
    }
}
