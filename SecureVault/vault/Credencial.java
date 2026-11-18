package vault;

public class Credencial extends Secreto {
    private String usuario;
    private String servicio;

    public Credencial(String id, String nombre, String hashNombre, String contenidoCifrado,
                      String usuario, String servicio) {
        super(id, nombre, hashNombre, contenidoCifrado);
        setUsuario(usuario);
        setServicio(servicio);
    }

    public String getUsuario() { return usuario; }
    public String getServicio() { return servicio; }

    public final void setUsuario(String usuario) { this.usuario = validar(usuario, "usuario"); }
    public final void setServicio(String servicio) { this.servicio = validar(servicio, "servicio"); }

    @Override
    public void mostrarDetalle(String contenidoPlano) {
        System.out.println("========== CREDENCIAL ==========");
        System.out.println("ID        : " + getId());
        System.out.println("Nombre    : " + getNombre());
        System.out.println("Servicio  : " + servicio);
        System.out.println("Usuario   : " + usuario);
        System.out.println("Contraseña: " + contenidoPlano);
        System.out.println("================================");
    }
}
