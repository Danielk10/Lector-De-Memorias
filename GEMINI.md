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

## 5. Publicación Automática en Google Play

El proyecto incluye el script `upload_play_store.py` para automatizar la subida y publicación del Android App Bundle (`.aab`) a la pista de producción (o pruebas) de Google Play Store utilizando la Service Account oficial:

```bash
python3 upload_play_store.py \
  --track production \
  --release_notes "<Notas en español (- Viñetas)>" \
  --release_notes_en "<Notas en inglés (- Bullets)>"
```

## 6. Emulador Local de PC y Pruebas de Hardware Virtual (TL866II+)

El proyecto cuenta con un entorno de simulación dinámico sobre `socketpair` que permite compilar y probar **minipro (v0.7.4)** y **libusb** directamente en una PC Linux x86_64 sin necesidad de conectar hardware físico ni requerir un dispositivo Android real.

### A. Ejecución del ciclo automatizado completo
El script [`emulador_minipro.sh`](emulador_minipro/emulador_minipro.sh) compila libusb con el parche de sockets, compila minipro con `RPATH` incrustado, compila el emulador en C++ y ejecuta pruebas de lectura de EEPROM y SPI Flash:

```bash
cd ~/emulador_minipro
./emulador_minipro.sh
```

### B. Ejecución directa de pruebas individuales
Gracias a que el binario de minipro en [`~/native_test_root/bin/minipro`](file:///home/danielpdiamon/native_test_root/bin/minipro) está enlazado con `RPATH` hacia [`~/native_test_root/lib`](file:///home/danielpdiamon/native_test_root/lib), **no se requiere configurar `LD_LIBRARY_PATH`**.

* **Lectura de chip EEPROM (`AT24C02C`):**
  ```bash
  ~/native_test_root/bin/emulador_minipro --fill count ~/native_test_root/bin/minipro -p "AT24C02C" -r read_eeprom.bin
  ```

* **Escritura y verificación de EEPROM (`Verification OK`):**
  ```bash
  ~/native_test_root/bin/emulador_minipro --save memory_out.bin ~/native_test_root/bin/minipro -p "AT24C02C" -w file_to_write.bin
  ```

* **Lectura de chip SPI Flash (`W25Q80BV` - 1MB):**
  ```bash
  ~/native_test_root/bin/emulador_minipro --flash mock_spi.bin ~/native_test_root/bin/minipro -p "W25Q80BV" -r read_spi.bin
  ```


