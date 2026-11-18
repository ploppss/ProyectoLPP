package vault;

public class NotaTexto extends Secreto {
    private String categoria;

    public NotaTexto(String id, String nombre, String hashNombre, String contenidoCifrado, String categoria) {
        super(id, nombre, hashNombre, contenidoCifrado);
        setCategoria(categoria);
    }

    public String getCategoria() { return categoria; }

    public final void setCategoria(String categoria) { this.categoria = validar(categoria, "categoria"); }

    @Override
    public void mostrarDetalle(String contenidoPlano) {
        System.out.println("============= NOTA =============");
        System.out.println("ID       : " + getId());
        System.out.println("Nombre   : " + getNombre());
        System.out.println("Categoría: " + categoria);
        System.out.println("Contenido:");
        System.out.println(contenidoPlano);
        System.out.println("================================");
    }
}
