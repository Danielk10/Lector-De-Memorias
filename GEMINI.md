# Instrucciones de Compilación y Reglas del Proyecto

Este documento describe cómo instalar el SDK de Android, compilar el proyecto **Lector-De-Memorias** (EEPROM Flasher - `com.diamon.mini`) y las normas para lanzamientos de versión.

## 1. Instalación del SDK

El SDK de Android necesario para compilar este proyecto se instala automáticamente ejecutando el script proporcionado:

```bash
bash setup-sdk.sh
```

- **Ubicación del SDK:** Todas las descargas y herramientas del SDK (incluyendo NDK 30, CMake 4.1.2, Build-Tools 37 y plataformas) se instalan temporalmente en el directorio `/tmp/android-sdk`.
- **Configuración de Gradle:** El script genera automáticamente el archivo `local.properties` apuntando a `sdk.dir=/tmp/android-sdk`.

## 2. Compilación y Firma

El proyecto utiliza Gradle para compilar tanto versiones de depuración como de producción (firmadas mediante `keystore.properties` o variables de entorno con `mini.jks`):

* **Compilar APK de Depuración:**
  ```bash
  ./gradlew assembleDebug
  ```
* **Compilar APK de Producción (Release):**
  ```bash
  ./gradlew assembleRelease
  ```
* **Compilar Android App Bundle (AAB para Google Play):**
  ```bash
  ./gradlew bundleRelease
  ```

## 3. Ubicación de Archivos de Salida

Para mantener limpio el árbol de trabajo del proyecto y no saturar el almacenamiento de `/home`, la carpeta de construcción (`buildDirectory`) está redirigida en `app/build.gradle` a `/tmp/mini`:

* **APK Release:** `/tmp/mini/outputs/apk/release/app-release.apk`
* **Bundle Release (AAB):** `/tmp/mini/outputs/bundle/release/app-release.aab`

## 4. Notas de Versión para Google Play (Bilingüe Obligatorio)

Siempre que se prepare un lanzamiento o se suba una actualización a **Google Play**, es obligatorio generar y proporcionar al usuario las notas de versión estructuradas tanto en **Inglés** (`en-US`) como en **Español** (`es-419` / `es-ES`), usando viñetas concisas (`- Elemento`) compatibles con el límite de caracteres de Google Play Console para validar la nota antes de su publicación.
