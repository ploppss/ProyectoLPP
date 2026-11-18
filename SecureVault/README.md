# Secure Vault

Baúl digital de consola en Java (POO) que guarda credenciales y notas cifradas bajo una contraseña maestra.
Proyecto final – Lenguajes y Paradigmas de Programación.

## Requisitos
- JDK 17 o superior (`javac -version`). Sin Maven/Gradle ni librerías externas.

## Estructura
```
SecureVault/
├── vault/            Secreto, Credencial, NotaTexto, Cifrable, CifradorXor,
│                     Hasheador, GestorBoveda, Main   (paquete `vault`)
├── vault_data/       (se genera al ejecutar)
│   ├── maestro.hash  solo el SHA-256 de la clave maestra
│   └── secretos/     S-<timestamp>.txt  (formato clave=valor, UTF-8)
└── README.md
```

## Compilación y ejecución
Desde la carpeta `SecureVault/` (la que contiene `vault/`):
```
javac -encoding UTF-8 -d out vault/*.java
java -cp out vault.Main
```
`vault_data/` se crea en el directorio desde el que se ejecuta el programa.
En Windows, si los acentos se ven mal, ejecutar antes `chcp 65001`.

## Funcionamiento
1. Primera ejecución: pide crear la contraseña maestra (se guarda solo su SHA-256).
2. Siguientes ejecuciones: login comparando hashes (3 intentos).
3. Menú: crear credencial / nota, listar, buscar por nombre, buscar por tipo, ver por ID (descifra),
   modificar por ID (campo en blanco = conservar), eliminar por ID, cambiar contraseña maestra (recifra toda la bóveda), salir.

## Decisiones de diseño
- **POO:** `Secreto` abstracta con setters `public final` que validan (null/blank → `IllegalArgumentException`);
  `Credencial` y `NotaTexto` sobrescriben `mostrarDetalle`.
- **Interfaz:** `GestorBoveda` depende de `Cifrable`; `CifradorXor` hace XOR circular sobre bytes UTF-8 + Base64.
- **Hashing:** `Hasheador.sha256Hex`. `buscar(String)` hashea `nombre.trim().toLowerCase()` y compara con `hashNombre`;
  `buscar(Tipo)` es la sobrecarga por tipo.
- **Seguridad:** la clave maestra solo vive en memoria durante la sesión; en disco solo está su hash.
  El contenido sensible se guarda cifrado.
- **Aviso académico:** XOR con clave repetida no es un cifrado seguro para uso real; se usa con fines didácticos.

## Evidencia de ejecución (extracto)
```
Contraseña maestra: Contraseña incorrecta (1/3).
Contraseña maestra: Acceso concedido.

# ALTA
Credencial guardada con ID S-1795012618635
Nota guardada con ID S-1795012618655

# LISTAR / BUSCAR (sobrecarga)
ID               TIPO        NOMBRE
S-1795012618635  CREDENCIAL  Gmail
S-1795012618655  NOTA        Ideas
Nombre a buscar: " GMAIL "   -> S-1795012618635  CREDENCIAL  Gmail
Tipo 2 (Nota)                -> S-1795012618655  NOTA        Ideas

# VER (polimorfismo)
========== CREDENCIAL ==========        ============= NOTA =============
ID        : S-1795012618635             ID       : S-1795012618655
Nombre    : Gmail                       Nombre   : Ideas
Servicio  : Google                      Categoría: Personal
Usuario   : ana@correo.com              Contenido:
Contraseña: P@ss ñ✓ 123                 linea uno / línea dos con acentos

# MODIFICAR
Secreto S-1795012618635 actualizado.   (nueva contraseña: NuevaPass#9)

# ELIMINAR
¿Eliminar definitivamente 'Ideas v2'? (s/n): Secreto eliminado.
Error: No existe un secreto con ID S-1.

# CAMBIO DE CLAVE MAESTRA (recifrado)
Contraseña maestra cambiada y bóveda recifrada.
contenidoCifrado en disco: MywSBUXyg9H//0FHVwI=  ->  IQEXFyI8AAUWFww=
Reinicio: la clave anterior falla; la nueva abre la bóveda y la contraseña sigue siendo NuevaPass#9.
```
