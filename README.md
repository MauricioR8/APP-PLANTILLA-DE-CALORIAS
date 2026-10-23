# Calorías, macros y gym

App Android (Kotlin, Jetpack Compose, Room) para llevar el control diario de calorías, macros,
agua, suplementos, peso, medidas y tu rutina de gym. Todo funciona sin internet y los datos
se quedan en el teléfono.

## Pestañas

- **Hoy**: tira de la semana con el color de la rutina de cada día, anillo de calorías con
  macros, entrenamiento de hoy, resumen por comida, agua rápida (+250 ml), suplementos,
  copiar el día anterior y "detalle del día" (de dónde salió cada número).
- **Gym**: la sesión del día (bloque diario + la rutina que toca). Marca series, descanso con
  cuenta atrás y vibración, cronómetro por intervalos para ejercicios por tiempo, peso de trabajo
  por ejercicio con historial y sugerencia de subida. Las rutinas y ejercicios se editan en la app.
- **Progreso**: *Nutrición* (resumen, racha y gasto energético real estimado), *Cuerpo* (peso con
  media móvil y medidas) y *Fuerza* (cumplimiento, series por grupo muscular, progreso de cargas).
- **Notas** y **Más** (calculadora, tablas de alimentos, calcular objetivos y ajustes).

## Ajustes

Tema (sistema/claro/oscuro), inicio de semana, historial de comidas (siempre, semanal o 30 días:
archiva el detalle y conserva los totales por día), bloqueo con huella/PIN, recordatorios
(espirómetro 2 veces al día, actualizar pesos cada mes, agua), respaldo JSON y exportación CSV.
Hay además un widget de pantalla de inicio con calorías y proteína restantes y botón de agua.

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
la versión y escribir su migración en `Migraciones.kt` (copiando los `CREATE` del JSON exportado);
`MigracionesTest` la prueba contra SQLite real comparando con el esquema que Room espera y verifica
la cadena completa desde la versión 6. Sin migración la app perdería los datos del usuario.

## Releases

Un push a `main` compila, corre las pruebas y publica una release `v<versionName>` (la versión de
`app/build.gradle.kts`). Las ramas distintas de `main` solo generan el APK como artefacto del run.
