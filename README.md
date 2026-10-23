# Plantilla de calorías

App Android (Kotlin, Jetpack Compose, Room) para llevar el control diario de calorías,
macros, agua y peso corporal.

## Qué hace

- **Mi día**: métricas con meta diaria (calorías, proteínas, carbohidratos, grasas, fibra, azúcar,
  sodio, agua o las que crees), historial editable y resumen por comida.
- **Progreso**: gráfica de 7 o 30 días por métrica con su meta, y peso corporal con tendencia.
- **Agregar alimento**: manual, desde tu historial o buscando en
  [Open Food Facts](https://openfoodfacts.org) (solo se envía el texto buscado).
- **Calcular objetivos**: calorías y macros con Mifflin-St Jeor según tu meta (definición,
  mantener, hipertrofia); se aplican a tus métricas con un toque.
- **Respaldo**: *Guardar respaldo* / *Restaurar respaldo* (JSON) desde el menú ⋮.
- Notas, calculadora con historial y tablas de alimentos tipo hoja de cálculo.
- Recordatorio opcional de agua (8:00 a 22:00).

## Compilar

Requiere JDK 17 y el Android SDK 34.

```
./gradlew testDebugUnitTest   # pruebas
./gradlew assembleDebug       # APK de depuración
./gradlew assembleRelease     # APK de lanzamiento (firmado si hay llave, ver abajo)
```

## Firma de la app

Para poder instalar una versión sobre otra sin perder datos, todas las versiones deben ir
firmadas con **la misma llave**. La llave NO está en el repositorio: se pasa por variables de
entorno (`CALORIAS_KEYSTORE`, `CALORIAS_STORE_PASSWORD`, `CALORIAS_KEY_ALIAS`,
`CALORIAS_KEY_PASSWORD`). En GitHub Actions se leen de estos *Secrets* del repositorio:

| Secret              | Contenido                              |
|---------------------|----------------------------------------|
| `KEYSTORE_BASE64`   | el archivo `.jks` codificado en base64 |
| `KEYSTORE_PASSWORD` | contraseña del almacén                 |
| `KEY_ALIAS`         | alias de la llave                      |
| `KEY_PASSWORD`      | contraseña de la llave                 |

**Respalda la llave en un lugar seguro**: si se pierde no se podrá actualizar la app instalada.

## Base de datos y migraciones

Room exporta el esquema de cada versión a `app/schemas`. Cada cambio de entidades requiere subir
la versión y escribir su migración en `Migraciones.kt`; `MigracionesTest` la prueba contra SQLite
real comparando con el esquema que Room espera. Sin migración la app perdería los datos del usuario.

## Releases

Un push a `main` compila, corre las pruebas y publica una release `v<versionName>` (la versión de
`app/build.gradle.kts`). Las ramas distintas de `main` solo generan el APK como artefacto del run.
