// El gestor y sus modelos pertenecen al mismo paquete.
package vault;

// IOException representa problemas al leer o escribir archivos.
import java.io.IOException;
// Permite especificar UTF-8 al convertir texto y leer o escribir archivos.
import java.nio.charset.StandardCharsets;
// Operaciones de archivo: existencia, creación, lectura, escritura y eliminación.
import java.nio.file.Files;
// Representa una ruta dentro del sistema operativo.
import java.nio.file.Path;
// Facilita crear rutas como vault_data/secretos.
import java.nio.file.Paths;
// Permite comparar bytes de forma segura para evitar diferencias por tiempo.
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Coordina toda la lógica de la bóveda:
 * autenticación, cifrado, creación, consulta, modificación, eliminación
 * y lectura o escritura de archivos.
 */
public class GestorBoveda {

    // Enumera los dos tipos que puede contener la aplicación.
    public enum Tipo { CREDENCIAL, NOTA }

    // Rutas comunes usadas por todos los metodos de persistencia.
    private static final Path DIR_BASE = Paths.get("vault_data");
    private static final Path DIR_SECRETOS = Paths.get("vault_data", "secretos");
    private static final Path ARCHIVO_MAESTRO = Paths.get("vault_data", "maestro.hash");

    // El gestor delega la transformación real del texto en esta implementación.
    private final Cifrable cifrador;
    // Solo se conserva mientras esta ejecutandose la sesion; no se guarda.
    private String claveSesion;

    // Se ejecuta al crear el gestor y prepara los directorios necesarios.
    public GestorBoveda() throws IOException {
        this.cifrador = new CifradorXor();
        Files.createDirectories(DIR_BASE);
        Files.createDirectories(DIR_SECRETOS);
    }

    // ---------------------------------------------------------------- clave maestra

    // Comprueba si ya existe el archivo que contiene el hash maestro.
    public boolean existeBoveda() {
        return Files.exists(ARCHIVO_MAESTRO);
    }

    // Crea una bóveda nueva y guarda solo el hash de la clave, no la clave.
    public void inicializarBoveda(String claveMaestra) throws IOException {
        if (existeBoveda()) {
            throw new IllegalStateException("La bóveda ya está inicializada.");
        }
        String hash = Hasheador.sha256Hex(claveMaestra);
        Files.writeString(ARCHIVO_MAESTRO, hash, StandardCharsets.UTF_8);
        claveSesion = claveMaestra;
    }

    // Comprueba la clave proporcionada y activa la sesion si coincide.
    public boolean login(String claveMaestra) throws IOException {
        if (coincideConMaestro(claveMaestra)) {
            claveSesion = claveMaestra;
            return true;
        }
        return false;
    }

    // Compara el hash calculado con el hash almacenado sin comparar texto plano.
    private boolean coincideConMaestro(String clave) throws IOException {
        String guardado = Files.readString(ARCHIVO_MAESTRO, StandardCharsets.UTF_8).trim();
        String calculado = Hasheador.sha256Hex(clave);
        // isEqual reduce el riesgo de comparaciones que pueden depender del tiempo.
        return MessageDigest.isEqual(calculado.getBytes(StandardCharsets.UTF_8),
                                     guardado.getBytes(StandardCharsets.UTF_8));
    }

    // Impide operaciones sensibles si todavía no hubo acceso valido.
    private void exigirSesion() {
        if (claveSesion == null) {
            throw new IllegalStateException("Debe autenticarse antes de operar la bóveda.");
        }
    }

    // ---------------------------------------------------------------- CREATE

    // Crea una credencial, cifra su contraseña y la guarda en disco.
    public Credencial crearCredencial(String nombre, String usuario, String servicio, String contrasena)
            throws IOException {
        exigirSesion();
        Credencial c = new Credencial(generarId(), nombre, hashDeNombre(nombre),
                cifrador.cifrar(contrasena, claveSesion), usuario, servicio);
        guardar(c);
        return c;
    }

    // Crea una nota, cifra su cuerpo y conserva sus datos en un archivo.
    public NotaTexto crearNota(String nombre, String categoria, String cuerpo) throws IOException {
        exigirSesion();
        NotaTexto n = new NotaTexto(generarId(), nombre, hashDeNombre(nombre),
                cifrador.cifrar(cuerpo, claveSesion), categoria);
        guardar(n);
        return n;
    }

    // Obtiene el reloj actual para formar un identificador unico del secreto.
    private String generarId() {
        long t = System.currentTimeMillis();
        while (Files.exists(DIR_SECRETOS.resolve("S-" + t + ".txt"))) {
            t++; // evita colisiones si se crean dos secretos en el mismo milisegundo
        }
        return "S-" + t;
    }

    // Normaliza el nombre antes de hashearlo para que las búsquedas sean consistentes.
    private static String hashDeNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El campo 'nombre' no puede ser nulo ni estar vacío.");
        }
        return Hasheador.sha256Hex(nombre.trim().toLowerCase());
    }

    // ---------------------------------------------------------------- READ

    // Valida el formato de un ID y carga su secreto desde el archivo correspondiente.
    public Secreto obtener(String id) throws IOException {
        if (id == null || !id.matches("S-\\d+")) {
            throw new IllegalArgumentException("ID inválido. Formato esperado: S-<número>.");
        }
        Path p = DIR_SECRETOS.resolve(id + ".txt");
        if (!Files.exists(p)) {
            throw new IllegalArgumentException("No existe un secreto con ID " + id + ".");
        }
        return leerArchivo(p);
    }

    // Carga todos los archivos válidos y informa por consola los archivos dañados.
    public List<Secreto> listar() throws IOException {
        List<String> errores = new ArrayList<>();
        List<Secreto> lista = cargarTodos(errores);
        for (String e : errores) {
            System.err.println("Aviso: " + e);
        }
        return lista;
    }

    /** Sobrecarga 1: búsqueda por nombre, comparando hashes (no se descifra nada). */
    public List<Secreto> buscar(String nombre) throws IOException {
        // Calcula el mismo hash normalizado que se guardo al crear el secreto.
        String hash = hashDeNombre(nombre);
        List<Secreto> res = new ArrayList<>();
        for (Secreto s : listar()) {
            // Solo coincide el hash; los nombres reales nunca se buscan dentro del cifrado.
            if (s.getHashNombre().equals(hash)) {
                res.add(s);
            }
        }
        return res;
    }

    /** Sobrecarga 2: búsqueda por tipo de secreto. */
    public List<Secreto> buscar(Tipo tipo) throws IOException {
        if (tipo == null) {
            throw new IllegalArgumentException("Debe indicar un tipo.");
        }
        List<Secreto> res = new ArrayList<>();
        for (Secreto s : listar()) {
            // tipoDe identifica la clase concreta y la compara con el tipo solicitado.
            if (tipoDe(s) == tipo) {
                res.add(s);
            }
        }
        return res;
    }

    // Descifra temporalmente el contenido únicamente después de comprobar la sesion.
    public String descifrarContenido(Secreto s) {
        exigirSesion();
        return cifrador.descifrar(s.getContenidoCifrado(), claveSesion);
    }

    // Usa pattern matching con instanceof para reconocer la clase concreta.
    public static Tipo tipoDe(Secreto s) {
        return (s instanceof Credencial) ? Tipo.CREDENCIAL : Tipo.NOTA;
    }

    // ---------------------------------------------------------------- UPDATE (null = conservar valor actual)

    // Modifica solo los campos que reciban un valor distinto de null.
    public void modificarCredencial(String id, String nombre, String usuario, String servicio, String contrasena)
            throws IOException {
        exigirSesion();
        Secreto s = obtener(id);
        // Descarta la operación si el ID pertenece a otro tipo de secreto.
        if (!(s instanceof Credencial c)) {
            throw new IllegalArgumentException("El secreto " + id + " no es una credencial.");
        }
        // Un cambio de nombre también requiere recalcular su hash de búsqueda.
        if (nombre != null) { c.setNombre(nombre); c.setHashNombre(hashDeNombre(nombre)); }
        if (usuario != null) { c.setUsuario(usuario); }
        if (servicio != null) { c.setServicio(servicio); }
        // La nueva contraseña se cifra antes de reemplazar el contenido almacenado.
        if (contrasena != null) { c.setContenidoCifrado(cifrador.cifrar(contrasena, claveSesion)); }
        guardar(c);
    }

    // Modifica una nota respetando los campos que el usuario deja sin cambio.
    public void modificarNota(String id, String nombre, String categoria, String cuerpo) throws IOException {
        exigirSesion();
        Secreto s = obtener(id);
        if (!(s instanceof NotaTexto n)) {
            throw new IllegalArgumentException("El secreto " + id + " no es una nota.");
        }
        if (nombre != null) { n.setNombre(nombre); n.setHashNombre(hashDeNombre(nombre)); }
        if (categoria != null) { n.setCategoria(categoria); }
        if (cuerpo != null) { n.setContenidoCifrado(cifrador.cifrar(cuerpo, claveSesion)); }
        guardar(n);
    }

    // ---------------------------------------------------------------- DELETE

    // obtener valida primero el formato y la existencia del secreto.
    public boolean eliminar(String id) throws IOException {
        obtener(id); // valida formato y existencia
        return Files.deleteIfExists(DIR_SECRETOS.resolve(id + ".txt"));
    }

    // ---------------------------------------------------------------- Cambio de clave maestra + recifrado

    // Cambia la clave maestra y vuelve a cifrar todos los contenidos secretos.
    public void cambiarClaveMaestra(String actual, String nueva) throws IOException {
        exigirSesion();
        if (!coincideConMaestro(actual)) {
            throw new IllegalArgumentException("La contraseña maestra actual no es correcta.");
        }
        String nuevoHash = Hasheador.sha256Hex(nueva); // valida la nueva clave

        List<String> errores = new ArrayList<>();
        List<Secreto> todos = cargarTodos(errores);
        if (!errores.isEmpty()) {
            throw new IllegalArgumentException("No se puede recifrar: hay archivos dañados. " + errores.get(0));
        }
        // Primero se recifra todo en memoria; si algo falla, no se toca el disco.
        for (Secreto s : todos) {
            String plano = cifrador.descifrar(s.getContenidoCifrado(), claveSesion);
            s.setContenidoCifrado(cifrador.cifrar(plano, nueva));
        }
        // Solo después de terminar el recifrado se sobrescriben los archivos.
        for (Secreto s : todos) {
            guardar(s);
        }
        Files.writeString(ARCHIVO_MAESTRO, nuevoHash, StandardCharsets.UTF_8);
        claveSesion = nueva;
    }

    // ---------------------------------------------------------------- Persistencia (clave=valor, UTF-8)

    // Convierte un objeto Secreto a líneas clave=valor y las escribe en UTF-8.
    private void guardar(Secreto s) throws IOException {
        List<String> lineas = new ArrayList<>();
        lineas.add("id=" + s.getId());
        lineas.add("tipo=" + tipoDe(s));
        lineas.add("nombre=" + s.getNombre());
        lineas.add("hashNombre=" + s.getHashNombre());
        lineas.add("contenidoCifrado=" + s.getContenidoCifrado());
        // instanceof reconoce qué datos adicionales debe escribir.
        if (s instanceof Credencial c) {
            lineas.add("usuario=" + c.getUsuario());
            lineas.add("servicio=" + c.getServicio());
        } else if (s instanceof NotaTexto n) {
            lineas.add("categoria=" + n.getCategoria());
        }
        // write recibe la ruta, las líneas y la codificación para guardar el archivo.
        Files.write(DIR_SECRETOS.resolve(s.getId() + ".txt"), lineas, StandardCharsets.UTF_8);
    }

    // Lee un archivo clave=valor y reconstruye el objeto Credencial o NotaTexto.
    private Secreto leerArchivo(Path p) throws IOException {
        Map<String, String> d = new HashMap<>();
        // Lee todas las líneas del archivo manteniendo caracteres UTF-8.
        for (String linea : Files.readAllLines(p, StandardCharsets.UTF_8)) {
            // Busca la primera aparición del signo igual en cada línea.
            int i = linea.indexOf('=');
            if (i > 0) {
                // La parte anterior es la clave y la posterior es el valor.
                d.put(linea.substring(0, i).trim(), linea.substring(i + 1));
            }
        }
        // El tipo determina qué clase concreta debe reconstruirse.
        String tipo = d.get("tipo");
        if ("CREDENCIAL".equals(tipo)) {
            return new Credencial(d.get("id"), d.get("nombre"), d.get("hashNombre"),
                    d.get("contenidoCifrado"), d.get("usuario"), d.get("servicio"));
        } else if ("NOTA".equals(tipo)) {
            return new NotaTexto(d.get("id"), d.get("nombre"), d.get("hashNombre"),
                    d.get("contenidoCifrado"), d.get("categoria"));
        }
        // Se rechaza cualquier formato cuyo tipo no sea reconocido.
        throw new IllegalArgumentException("Tipo desconocido en " + p.getFileName());
    }

    // Recorre los archivos válidos y conserva los errores encontrados por separado.
    private List<Secreto> cargarTodos(List<String> errores) throws IOException {
        List<Path> archivos;
        // try-with-resources cierra automáticamente el Stream al terminar.
        try (Stream<Path> stream = Files.list(DIR_SECRETOS)) {
            archivos = stream.filter(f -> f.getFileName().toString().matches("S-\\d+\\.txt"))
                             .sorted()
                             .toList();
        }
        List<Secreto> lista = new ArrayList<>();
        for (Path p : archivos) {
            try {
                lista.add(leerArchivo(p));
            } catch (IllegalArgumentException e) {
                // Un archivo inválido no detiene la carga de los demás archivos.
                errores.add(p.getFileName() + ": " + e.getMessage());
            }
        }
        return lista;
    }
}
