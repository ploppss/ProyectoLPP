// Ubica la clase de entrada dentro del mismo paquete de la aplicación.
package vault;

// Permite informar errores producidos al trabajar con archivos.
import java.io.IOException;
// Permite guardar y recorrer listas de objetos Secreto.
import java.util.List;
// Permite leer texto escrito por el usuario en la consola.
import java.util.Scanner;

/**
 * Punto de entrada de la aplicación.
 * Presenta un menú y traduce las opciones del usuario a operaciones del gestor.
 */
public class Main {

    // Scanner compartido permite leer todas las respuestas desde System.in.
    private static final Scanner sc = new Scanner(System.in);

    // main es el método ejecutado al iniciar el programa.
    public static void main(String[] args) {
        GestorBoveda gestor;
        try {
            // Prepara vault_data y vault_data/secretos y crea el gestor.
            gestor = new GestorBoveda();
        } catch (IOException e) {
            System.out.println("No se pudo preparar vault_data/: " + e.getMessage());
            return;
        }
        System.out.println("=== SECURE VAULT ===");
        // Si la autenticación falla o el usuario abandona, termina el programa.
        if (!autenticar(gestor)) {
            return;
        }
        // salir cambia a true solo cuando el usuario elige la opción cero.
        boolean salir = false;
        while (!salir) {
            mostrarMenu();
            String op = leer("Opción: ");
            try {
                // switch con expresiones modernas ejecuta una operación por opción.
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
            // Captura errores de datos o estado y muestra un mensaje al usuario.
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("Error: " + e.getMessage());
            // Captura errores específicos de lectura o escritura de archivos.
            } catch (IOException e) {
                System.out.println("Error de archivo: " + e.getMessage());
            // Captura cualquier RuntimeException no contemplada anteriormente.
            } catch (RuntimeException e) {
                System.out.println("Error inesperado: " + e);
            }
        }
    }

    // ------------------------------------------------------------------ autenticación

    // Si no existe bóveda permite crearla; si existe concede tres intentos.
    private static boolean autenticar(GestorBoveda g) {
        if (!g.existeBoveda()) {
            System.out.println("No existe una bóveda. Vamos a crearla.");
            while (true) {
                String c1 = leer("Nueva contraseña maestra: ");
                String c2 = leer("Confirma la contraseña: ");
                // Comparar cadenas requiere que coincidan exactamente.
                if (!c1.equals(c2)) {
                    System.out.println("Las contraseñas no coinciden.");
                    continue;
                }
                try {
                    // inicializarBoveda genera el hash y activa la sesión.
                    g.inicializarBoveda(c1);
                    System.out.println("Bóveda creada correctamente.");
                    return true;
                } catch (IllegalArgumentException | IllegalStateException | IOException e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
        }
        // Repite como máximo tres veces el intento de iniciar sesión.
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

    // Muestra en consola todas las operaciones disponibles.
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
        // Cada llamada lee una línea y elimina los espacios al inicio y al final.
        String nombre = leer("Nombre del secreto: ");
        String servicio = leer("Servicio: ");
        String usuario = leer("Usuario: ");
        String pass = leer("Contraseña: ");
        // El gestor crea, cifra y persiste la credencial.
        Credencial c = g.crearCredencial(nombre, usuario, servicio, pass);
        System.out.println("Credencial guardada con ID " + c.getId());
    }

    private static void altaNota(GestorBoveda g) throws IOException {
        String nombre = leer("Nombre de la nota: ");
        String categoria = leer("Categoría: ");
        // leerCuerpo permite introducir varias líneas hasta encontrar una vacía.
        String cuerpo = leerCuerpo();
        NotaTexto n = g.crearNota(nombre, categoria, cuerpo);
        System.out.println("Nota guardada con ID " + n.getId());
    }

    private static void buscarPorTipo(GestorBoveda g) throws IOException {
        String t = leer("Tipo (1 = Credencial, 2 = Nota): ");
        GestorBoveda.Tipo tipo;
        // Convierte el número seleccionado al valor del enum correspondiente.
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
        // Obtiene el objeto cargado desde el archivo.
        Secreto s = g.obtener(leer("ID del secreto: "));
        // mostrarDetalle se resuelve según el tipo real de s (polimorfismo).
        s.mostrarDetalle(g.descifrarContenido(s)); // polimorfismo
    }

    // Obtiene el secreto antes de pedir únicamente los campos que pueden cambiar.
    private static void modificar(GestorBoveda g) throws IOException {
        String id = leer("ID del secreto a modificar: ");
        Secreto s = g.obtener(id);
        System.out.println("(Deje en blanco cualquier campo para conservar su valor actual)");
        String nombre = opcional("Nombre [" + s.getNombre() + "]: ");
        // Patron instanceof diferencia una credencial de una nota.
        if (s instanceof Credencial c) {
            String usuario = opcional("Usuario [" + c.getUsuario() + "]: ");
            String servicio = opcional("Servicio [" + c.getServicio() + "]: ");
            String pass = opcional("Nueva contraseña: ");
            g.modificarCredencial(id, nombre, usuario, servicio, pass);
        } else if (s instanceof NotaTexto n) {
            String categoria = opcional("Categoría [" + n.getCategoria() + "]: ");
            System.out.println("¿Cambiar el cuerpo? (s/n)");
            // equalsIgnoreCase acepta respuestas s o S sin distinguir mayúsculas.
            String cuerpo = leer("> ").equalsIgnoreCase("s") ? leerCuerpo() : null;
            g.modificarNota(id, nombre, categoria, cuerpo);
        }
        System.out.println("Secreto " + id + " actualizado.");
    }

    // Pide confirmación antes de eliminar para evitar borrados accidentales.
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
        // La nueva clave se rechaza si su confirmacion no es identica.
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
        // %-16s reserva ancho para columnas alineadas en la consola.
        System.out.printf("%-16s %-11s %s%n", "ID", "TIPO", "NOMBRE");
        for (Secreto s : lista) {
            System.out.printf("%-16s %-11s %s%n", s.getId(), GestorBoveda.tipoDe(s), s.getNombre());
        }
    }

    // lee valida que todavía exista entrada y devuelve una cadena limpia.
    private static String leer(String prompt) {
        System.out.print(prompt);
        if (!sc.hasNextLine()) {
            System.out.println("\nFin de la entrada. Saliendo.");
            System.exit(0);
        }
        // trim elimina espacios sobrantes al inicio y al final.
        return sc.nextLine().trim();
    }

    /** Devuelve null si el usuario deja el campo en blanco (conservar valor). */
    private static String opcional(String prompt) {
        String v = leer(prompt);
        // null representa conservar el valor actual durante una modificación.
        return v.isEmpty() ? null : v;
    }

    // Construye el cuerpo de una nota leyendo líneas hasta encontrar una vacía.
    private static String leerCuerpo() {
        System.out.println("Escriba el contenido (línea vacía para terminar):");
        StringBuilder sb = new StringBuilder();
        while (true) {
            String linea = leer("");
            if (linea.isEmpty()) {
                break;
            }
            // Se inserta un salto de línea entre párrafos, no al final ni al inicio.
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(linea);
        }
        return sb.toString();
    }
}
