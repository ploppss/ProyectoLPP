package vault;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** Orquesta autenticación, cifrado, persistencia y CRUD de secretos. */
public class GestorBoveda {

    public enum Tipo { CREDENCIAL, NOTA }

    private static final Path DIR_BASE = Paths.get("vault_data");
    private static final Path DIR_SECRETOS = Paths.get("vault_data", "secretos");
    private static final Path ARCHIVO_MAESTRO = Paths.get("vault_data", "maestro.hash");

    private final Cifrable cifrador;
    private String claveSesion; // solo en memoria; nunca se escribe en disco

    public GestorBoveda() throws IOException {
        this.cifrador = new CifradorXor();
        Files.createDirectories(DIR_BASE);
        Files.createDirectories(DIR_SECRETOS);
    }

    // ---------------------------------------------------------------- clave maestra

    public boolean existeBoveda() {
        return Files.exists(ARCHIVO_MAESTRO);
    }

    public void inicializarBoveda(String claveMaestra) throws IOException {
        if (existeBoveda()) {
            throw new IllegalStateException("La bóveda ya está inicializada.");
        }
        String hash = Hasheador.sha256Hex(claveMaestra);
        Files.writeString(ARCHIVO_MAESTRO, hash, StandardCharsets.UTF_8);
        claveSesion = claveMaestra;
    }

    public boolean login(String claveMaestra) throws IOException {
        if (coincideConMaestro(claveMaestra)) {
            claveSesion = claveMaestra;
            return true;
        }
        return false;
    }

    private boolean coincideConMaestro(String clave) throws IOException {
        String guardado = Files.readString(ARCHIVO_MAESTRO, StandardCharsets.UTF_8).trim();
        String calculado = Hasheador.sha256Hex(clave);
        return MessageDigest.isEqual(calculado.getBytes(StandardCharsets.UTF_8),
                                     guardado.getBytes(StandardCharsets.UTF_8));
    }

    private void exigirSesion() {
        if (claveSesion == null) {
            throw new IllegalStateException("Debe autenticarse antes de operar la bóveda.");
        }
    }

    // ---------------------------------------------------------------- CREATE

    public Credencial crearCredencial(String nombre, String usuario, String servicio, String contrasena)
            throws IOException {
        exigirSesion();
        Credencial c = new Credencial(generarId(), nombre, hashDeNombre(nombre),
                cifrador.cifrar(contrasena, claveSesion), usuario, servicio);
        guardar(c);
        return c;
    }

    public NotaTexto crearNota(String nombre, String categoria, String cuerpo) throws IOException {
        exigirSesion();
        NotaTexto n = new NotaTexto(generarId(), nombre, hashDeNombre(nombre),
                cifrador.cifrar(cuerpo, claveSesion), categoria);
        guardar(n);
        return n;
    }

    private String generarId() {
        long t = System.currentTimeMillis();
        while (Files.exists(DIR_SECRETOS.resolve("S-" + t + ".txt"))) {
            t++; // evita colisiones si se crean dos secretos en el mismo milisegundo
        }
        return "S-" + t;
    }

    private static String hashDeNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El campo 'nombre' no puede ser nulo ni estar vacío.");
        }
        return Hasheador.sha256Hex(nombre.trim().toLowerCase());
    }

    // ---------------------------------------------------------------- READ

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
        String hash = hashDeNombre(nombre);
        List<Secreto> res = new ArrayList<>();
        for (Secreto s : listar()) {
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
            if (tipoDe(s) == tipo) {
                res.add(s);
            }
        }
        return res;
    }

    public String descifrarContenido(Secreto s) {
        exigirSesion();
        return cifrador.descifrar(s.getContenidoCifrado(), claveSesion);
    }

    public static Tipo tipoDe(Secreto s) {
        return (s instanceof Credencial) ? Tipo.CREDENCIAL : Tipo.NOTA;
    }

    // ---------------------------------------------------------------- UPDATE (null = conservar valor actual)

    public void modificarCredencial(String id, String nombre, String usuario, String servicio, String contrasena)
            throws IOException {
        exigirSesion();
        Secreto s = obtener(id);
        if (!(s instanceof Credencial c)) {
            throw new IllegalArgumentException("El secreto " + id + " no es una credencial.");
        }
        if (nombre != null) { c.setNombre(nombre); c.setHashNombre(hashDeNombre(nombre)); }
        if (usuario != null) { c.setUsuario(usuario); }
        if (servicio != null) { c.setServicio(servicio); }
        if (contrasena != null) { c.setContenidoCifrado(cifrador.cifrar(contrasena, claveSesion)); }
        guardar(c);
    }

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

    public boolean eliminar(String id) throws IOException {
        obtener(id); // valida formato y existencia
        return Files.deleteIfExists(DIR_SECRETOS.resolve(id + ".txt"));
    }

    // ---------------------------------------------------------------- Cambio de clave maestra + recifrado

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
        for (Secreto s : todos) {
            guardar(s);
        }
        Files.writeString(ARCHIVO_MAESTRO, nuevoHash, StandardCharsets.UTF_8);
        claveSesion = nueva;
    }

    // ---------------------------------------------------------------- Persistencia (clave=valor, UTF-8)

    private void guardar(Secreto s) throws IOException {
        List<String> lineas = new ArrayList<>();
        lineas.add("id=" + s.getId());
        lineas.add("tipo=" + tipoDe(s));
        lineas.add("nombre=" + s.getNombre());
        lineas.add("hashNombre=" + s.getHashNombre());
        lineas.add("contenidoCifrado=" + s.getContenidoCifrado());
        if (s instanceof Credencial c) {
            lineas.add("usuario=" + c.getUsuario());
            lineas.add("servicio=" + c.getServicio());
        } else if (s instanceof NotaTexto n) {
            lineas.add("categoria=" + n.getCategoria());
        }
        Files.write(DIR_SECRETOS.resolve(s.getId() + ".txt"), lineas, StandardCharsets.UTF_8);
    }

    private Secreto leerArchivo(Path p) throws IOException {
        Map<String, String> d = new HashMap<>();
        for (String linea : Files.readAllLines(p, StandardCharsets.UTF_8)) {
            int i = linea.indexOf('=');
            if (i > 0) {
                d.put(linea.substring(0, i).trim(), linea.substring(i + 1));
            }
        }
        String tipo = d.get("tipo");
        if ("CREDENCIAL".equals(tipo)) {
            return new Credencial(d.get("id"), d.get("nombre"), d.get("hashNombre"),
                    d.get("contenidoCifrado"), d.get("usuario"), d.get("servicio"));
        } else if ("NOTA".equals(tipo)) {
            return new NotaTexto(d.get("id"), d.get("nombre"), d.get("hashNombre"),
                    d.get("contenidoCifrado"), d.get("categoria"));
        }
        throw new IllegalArgumentException("Tipo desconocido en " + p.getFileName());
    }

    private List<Secreto> cargarTodos(List<String> errores) throws IOException {
        List<Path> archivos;
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
                errores.add(p.getFileName() + ": " + e.getMessage());
            }
        }
        return lista;
    }
}
