# Reporte de Auditoría y Corrección: Rutas, Portabilidad y Ejecución de EEPROM Flasher
ESTADO: CORREGIDO (Última actualización: Agosto 2026)

Este documento detalla los hallazgos críticos, parches aplicados y soluciones de ingeniería implementadas para garantizar la portabilidad y ejecución nativa de herramientas como `minipro` y `libusb` dentro de Android, sin permisos root, en el paquete `com.diamon.mini`.

---

### 1. Eliminación de Rutas Hardcoded en Binarios (RUNPATH)
* **Problema:** Los binarios compilados originalmente bajo Termux contenían referencias absolutas a la ruta `/data/data/com.termux/files/usr/lib` incrustadas en sus cabeceras ELF (`RUNPATH`/`RPATH`). Esto provocaba fallos de carga en dispositivos sin Termux instalado.
* **Acción:** Se utilizó la herramienta `patchelf --remove-rpath` en todos los archivos binarios (`.so`) para limpiar los directorios de búsqueda de librerías.
* **Resultado:** El cargador dinámico de Android ahora busca dependencias exclusivamente en las rutas seguras del sistema y en el directorio nativo del APK (`getApplicationInfo().nativeLibraryDir`), o en su defecto a través de `LD_LIBRARY_PATH`.

### 2. Limpieza de Assets y Scripts de Configuración
* **Acción:** Se auditaron las carpetas `assets` y `fake_root` para eliminar archivos redundantes de desarrollo (`include/`, `pkgconfig/`, `udev/`) que inflaban el tamaño del APK.
* **Resultado:** Reducción de espacio y eliminación de scripts contaminados con rutas absolutas de desarrollo, dejando solo los binarios y archivos XML necesarios en runtime.

### 3. Parche de Strings de Directorio en Minipro (Share Dir)
* **Problema:** El ejecutable `minipro` venía compilado de origen con la ruta de datos hardcodeada apuntando a directorios del compilador en Termux.
* **Acción:** Se parchó directamente la firma del string del ejecutable en binario (`libminipro_bin.so`), sustituyéndola por la ruta del espacio privado del app en Android: `/data/data/com.diamon.mini/files/usr/share/minipro`, rellenando el resto con bytes nulos (`\0`).
* **Resultado:** `minipro` localiza dinámicamente y sin errores la base de datos XML de chips (`infoic.xml` y `logicic.xml`).

### 4. Portabilidad del Ejecutable Nivel OS (Google Play Compliance)
* **Problema:** Android y Gradle prohíben empaquetar ejecutables nativos puros dentro del directorio `jniLibs` si no tienen el prefijo `lib` y extensión `.so`.
* **Acción:** Se renombró el ejecutable `minipro` a `libminipro_bin.so`. En tiempo de ejecución, la aplicación extrae y hace referencia al binario usando la ruta nativa del APK.

### 5. Bypass de Cierre de File Descriptor en Android 10+ (Exec / Fork JNI)
* **Problema Crítico:** A partir de Android 10 (API 29), las llamadas del sistema a través de la API estándar de Java (`ProcessBuilder` y `Runtime.exec`) realizan una limpieza interna en la que cierran sistemáticamente todos los descriptores de archivo (FD) abiertos mayores a 2 en el proceso hijo antes de llamar a `execve`. Esto invalidaba el FD inyectado para la comunicación USB sin Root (`ANDROID_USB_FD`).
* **Acción:** Se eliminó la dependencia de `ProcessBuilder` y se reescribió el módulo de ejecución en C++ mediante JNI en [native-lib.cpp](app/src/main/cpp/native-lib.cpp).
  * **Ejecución Nativa:** El proceso se bifurca (`fork()`) y se ejecuta (`execv()`) a nivel nativo en C++, preservando el FD USB abierto sin interferencia del recolector de FDs de Java.
  * **Captura de Salida en Tiempo Real:** Se creó un pipe POSIX en C++. En Java se adopta el FD del pipe mediante `ParcelFileDescriptor.adoptFd(readFd)`, pasándolo a un `BufferedReader` para actualizar la consola en la interfaz gráfica de usuario en tiempo real.
  * **Interrupción / Aborto:** El hilo de cancelación detiene la ejecución inmediatamente enviando una señal `SIGKILL` al PID del proceso hijo mediante JNI.

### 6. Optimización de Consulta de Descriptor en Libusb
* **Problema:** En el código original de intercepción del FD USB, se leía el descriptor de dispositivo mediante la llamada secuencial `read(fd, buf, 18)`. Si otro proceso o lectura previa ya había avanzado el puntero del FD, la lectura fallaba o devolvía descriptores corruptos.
* **Acción:** Se actualizó [build_libusb_custom_mini.sh](build_libusb_custom_mini.sh) para consultar el descriptor de dispositivo enviando la llamada de control directamente a la controladora USB mediante `ioctl(fd, USBDEVFS_CONTROL, &ctrl)` (ioctl `0xC0185500`).
* **Resultado:** La lectura del descriptor es ahora atómica, concurrente y completamente independiente del estado del cursor del File Descriptor.
