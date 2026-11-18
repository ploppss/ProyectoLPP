package vault;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        GestorBoveda gestor;
        try {
            gestor = new GestorBoveda();
        } catch (IOException e) {
            System.out.println("No se pudo preparar vault_data/: " + e.getMessage());
            return;
        }
        System.out.println("=== SECURE VAULT ===");
        if (!autenticar(gestor)) {
            return;
        }
        boolean salir = false;
        while (!salir) {
            mostrarMenu();
            String op = leer("Opción: ");
            try {
                switch (op) {
                    case "1" -> altaCredencial(gestor);
                    case "2" -> altaNota(gestor);
                    case "3" -> imprimirLista(gestor.listar());
                    case "4" -> imprimirLista(gestor.buscar(leer("Nombre a buscar: ")));
                    case "5" -> buscarPorTipo(gestor);
                    case "6" -> verPorId(gestor);
                    case "7" -> modificar(gestor);
                    case "8" -> eliminar(gestor);
                    case "9" -> cambiarClave(gestor);
                    case "0" -> { salir = true; System.out.println("Bóveda cerrada. ¡Hasta pronto!"); }
                    default -> System.out.println("Opción no válida.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("Error: " + e.getMessage());
            } catch (IOException e) {
                System.out.println("Error de archivo: " + e.getMessage());
            } catch (RuntimeException e) {
                System.out.println("Error inesperado: " + e);
            }
        }
    }

    // ------------------------------------------------------------------ autenticación

    private static boolean autenticar(GestorBoveda g) {
        if (!g.existeBoveda()) {
            System.out.println("No existe una bóveda. Vamos a crearla.");
            while (true) {
                String c1 = leer("Nueva contraseña maestra: ");
                String c2 = leer("Confirma la contraseña: ");
                if (!c1.equals(c2)) {
                    System.out.println("Las contraseñas no coinciden.");
                    continue;
                }
                try {
                    g.inicializarBoveda(c1);
                    System.out.println("Bóveda creada correctamente.");
                    return true;
                } catch (IllegalArgumentException | IllegalStateException | IOException e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
        }
        for (int intento = 1; intento <= 3; intento++) {
            try {
                if (g.login(leer("Contraseña maestra: "))) {
                    System.out.println("Acceso concedido.");
                    return true;
                }
                System.out.println("Contraseña incorrecta (" + intento + "/3).");
            } catch (IllegalArgumentException | IOException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
        System.out.println("Demasiados intentos fallidos. Saliendo.");
        return false;
    }

    // ------------------------------------------------------------------ opciones

    private static void mostrarMenu() {
        System.out.println("\n--- MENÚ ---");
        System.out.println("1. Crear credencial");
        System.out.println("2. Crear nota");
        System.out.println("3. Listar secretos");
        System.out.println("4. Buscar por nombre");
        System.out.println("5. Buscar por tipo");
        System.out.println("6. Ver por ID");
        System.out.println("7. Modificar por ID");
        System.out.println("8. Eliminar por ID");
        System.out.println("9. Cambiar contraseña maestra");
        System.out.println("0. Salir");
    }

    private static void altaCredencial(GestorBoveda g) throws IOException {
        String nombre = leer("Nombre del secreto: ");
        String servicio = leer("Servicio: ");
        String usuario = leer("Usuario: ");
        String pass = leer("Contraseña: ");
        Credencial c = g.crearCredencial(nombre, usuario, servicio, pass);
        System.out.println("Credencial guardada con ID " + c.getId());
    }

    private static void altaNota(GestorBoveda g) throws IOException {
        String nombre = leer("Nombre de la nota: ");
        String categoria = leer("Categoría: ");
        String cuerpo = leerCuerpo();
        NotaTexto n = g.crearNota(nombre, categoria, cuerpo);
        System.out.println("Nota guardada con ID " + n.getId());
    }

    private static void buscarPorTipo(GestorBoveda g) throws IOException {
        String t = leer("Tipo (1 = Credencial, 2 = Nota): ");
        GestorBoveda.Tipo tipo;
        if (t.equals("1")) {
            tipo = GestorBoveda.Tipo.CREDENCIAL;
        } else if (t.equals("2")) {
            tipo = GestorBoveda.Tipo.NOTA;
        } else {
            throw new IllegalArgumentException("Tipo no válido; elija 1 o 2.");
        }
        imprimirLista(g.buscar(tipo));
    }

    private static void verPorId(GestorBoveda g) throws IOException {
        Secreto s = g.obtener(leer("ID del secreto: "));
        s.mostrarDetalle(g.descifrarContenido(s)); // polimorfismo
    }

    private static void modificar(GestorBoveda g) throws IOException {
        String id = leer("ID del secreto a modificar: ");
        Secreto s = g.obtener(id);
        System.out.println("(Deje en blanco cualquier campo para conservar su valor actual)");
        String nombre = opcional("Nombre [" + s.getNombre() + "]: ");
        if (s instanceof Credencial c) {
            String usuario = opcional("Usuario [" + c.getUsuario() + "]: ");
            String servicio = opcional("Servicio [" + c.getServicio() + "]: ");
            String pass = opcional("Nueva contraseña: ");
            g.modificarCredencial(id, nombre, usuario, servicio, pass);
        } else if (s instanceof NotaTexto n) {
            String categoria = opcional("Categoría [" + n.getCategoria() + "]: ");
            System.out.println("¿Cambiar el cuerpo? (s/n)");
            String cuerpo = leer("> ").equalsIgnoreCase("s") ? leerCuerpo() : null;
            g.modificarNota(id, nombre, categoria, cuerpo);
        }
        System.out.println("Secreto " + id + " actualizado.");
    }

    private static void eliminar(GestorBoveda g) throws IOException {
        String id = leer("ID del secreto a eliminar: ");
        Secreto s = g.obtener(id);
        String r = leer("¿Eliminar definitivamente '" + s.getNombre() + "'? (s/n): ");
        if (r.equalsIgnoreCase("s")) {
            System.out.println(g.eliminar(id) ? "Secreto eliminado." : "No se pudo eliminar.");
        } else {
            System.out.println("Operación cancelada.");
        }
    }

    private static void cambiarClave(GestorBoveda g) throws IOException {
        String actual = leer("Contraseña maestra actual: ");
        String nueva = leer("Nueva contraseña maestra: ");
        String conf = leer("Confirma la nueva contraseña: ");
        if (!nueva.equals(conf)) {
            throw new IllegalArgumentException("La confirmación no coincide.");
        }
        g.cambiarClaveMaestra(actual, nueva);
        System.out.println("Contraseña maestra cambiada y bóveda recifrada.");
    }

    // ------------------------------------------------------------------ utilidades de consola

    private static void imprimirLista(List<Secreto> lista) {
        if (lista.isEmpty()) {
            System.out.println("(sin resultados)");
            return;
        }
        System.out.printf("%-16s %-11s %s%n", "ID", "TIPO", "NOMBRE");
        for (Secreto s : lista) {
            System.out.printf("%-16s %-11s %s%n", s.getId(), GestorBoveda.tipoDe(s), s.getNombre());
        }
    }

    private static String leer(String prompt) {
        System.out.print(prompt);
        if (!sc.hasNextLine()) {
            System.out.println("\nFin de la entrada. Saliendo.");
            System.exit(0);
        }
        return sc.nextLine().trim();
    }

    /** Devuelve null si el usuario deja el campo en blanco (conservar valor). */
    private static String opcional(String prompt) {
        String v = leer(prompt);
        return v.isEmpty() ? null : v;
    }

    private static String leerCuerpo() {
        System.out.println("Escriba el contenido (línea vacía para terminar):");
        StringBuilder sb = new StringBuilder();
        while (true) {
            String linea = leer("");
            if (linea.isEmpty()) {
                break;
            }
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(linea);
        }
        return sb.toString();
    }
}
