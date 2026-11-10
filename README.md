# Proyecto Final: [Nombre del Sistema / Aplicación]

Proyecto final para la experiencia educativa de **Lenguajes y Paradigmas de Programación**.

## Descripción

Resumen del proyecto

## Requisitos previos

* **Java JDK**: Versión 17 o 21 (LTS recomendada).
* **Gestor de dependencias** *(si aplica)*: Maven, Gradle o ejecución directa con `javac`.

Verifica tu versión instalada:
\`\`\`bash
java -version
javac -version
\`\`\`

## Estructura del proyecto

\`\`\`text
ProyectoLPP/
├── src/                # Código fuente Java
│   ├── models/         # Clases base
│   ├── utils/          
│   └── Main.java
├── docs/               # Documentación y diagramas
├── .gitignore
└── README.md
\`\`\`

## Compilación y Ejecución

Si no utilizan herramientas como Maven o Gradle y compilan directamente con el JDK:

1. **Compilar el código fuente:**
   \`\`\`bash
   javac -d bin src/**/*.java
   \`\`\`

2. **Ejecutar la clase principal:**
   \`\`\`bash
   java -cp bin com.proyecto.Main
   \`\`\`

*(Si usan Maven, reemplaza por `mvn clean package` y `java -jar target/...`).*

## Integrantes del equipo

* **Gómez García José Alberto** - *GitHub:* [@usuario1](https://github.com/usuario1)
* **Gutiérrez Benavides Roberto de Jesús** - *GitHub:* [@usuario2](https://github.com/usuario2)
* **Olmos Tlapa Christian** - *GitHub:* [@usuario3](https://github.com/usuario3)
