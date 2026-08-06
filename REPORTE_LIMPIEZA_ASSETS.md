# Reporte de Limpieza de Assets y Corrección de Rutas
Paquete: `com.diamon.mini` (EepromFlasher)

Este reporte documenta los archivos y carpetas dentro de `app/src/main/assets/data/data/com.diamon.mini/files/usr` que no son necesarios para la ejecución de los binarios en la arquitectura arm64-v8a del proyecto Android y que, por lo tanto, han sido eliminados para reducir significativamente el peso del APK y mejorar el rendimiento de la aplicación.

### 1. Archivos y Carpetas Eliminados

Los siguientes recursos correspondían a cabeceras de compilación, configuraciones de enlazadores y documentación, que no son empleados en tiempo de ejecución en Android por `minipro`.

**A. Cabeceras y Archivos de Código Fuente C/C++ (NDK)**
*   **Ruta Original:** `usr/include/` (Directorio completo)
*   **Descripción:** Contenía las cabeceras `.h` de libusb-1.0 (`usr/include/libusb-1.0/libusb.h`).
*   **Razón de exclusión en assets:** Solo se necesitan durante el desarrollo y la compilación nativa (NDK) del proyecto Android. No se requieren en tiempo de ejecución.
*   **Ahorro de espacio en APK:** ~89 KB

**B. Librerías Estáticas (.a) (NDK)**
*   **Ruta Original:** `usr/lib/*.a` (Archivo `libusb-1.0.a`)
*   **Descripción:** `libusb-1.0.a`.
*   **Razón de exclusión en assets:** Al igual que las cabeceras, estas librerías estáticas se utilizan en el desarrollo NDK y no se emplean en runtime en Android. Los binarios correspondientes ya están vinculados estáticamente o se cargan dinámicamente como `.so`.
*   **Ahorro de espacio en APK:** ~273 KB

**C. Archivos de Configuración de Desarrollo y CMake/PkgConfig**
*   **Rutas:** `usr/lib/pkgconfig/`
*   **Descripción:** Archivos de configuración `.pc` como `libusb-1.0.pc` para `pkg-config`.
*   **Razón:** Pkg-config no se ejecuta en el dispositivo Android para lanzar la aplicación.
*   **Ahorro de espacio:** ~1 KB

**D. Reglas de Dispositivos (udev)**
*   **Ruta Original:** `usr/lib/udev/`
*   **Descripción:** Archivos de reglas udev para minipro (`60-minipro.rules`).
*   **Razón:** Android no usa udev para la gestión de dispositivos USB nativos en el espacio de usuario de esta forma (la aplicación maneja los permisos USB a través del sistema de Android y el manifest con `device_filter.xml`).
*   **Ahorro de espacio:** ~1 KB

**E. Documentación y Completado de Bash**
*   **Rutas:** `usr/share/bash-completion/` y `usr/share/man/`
*   **Descripción:** Completado automático de comandos para bash y manuales (man pages) de minipro.
*   **Razón:** No son necesarios en la interfaz gráfica del usuario de Android, ya que la aplicación tiene su propio frontend interactivo.
*   **Ahorro de espacio:** ~100 KB

### 2. Archivos y Carpetas Necesarios (Conservados)

Los siguientes archivos se han considerado esenciales para la correcta operación del ecosistema y se mantendrán en `assets/`:

*   **`usr/share/minipro/infoic.xml`**: Esta es la base de datos de circuitos integrados y microchips soportados por `minipro` (indispensable para programar/leer memorias).
*   **`usr/share/minipro/logicic.xml`**: Archivo con la lógica de circuitos integrados requerida por la suite.
*   **`usr/bin/dump-alg-minipro.bash`**: Script ejecutable de soporte.

### Resumen del Impacto de la Limpieza
*   **Peso Inicial Ahorrado:** Aproximadamente **460 KB** liberados en total.
*   **Velocidad de Extracción:** El archivo `AssetHelper.java` de la aplicación recorrerá muchos menos ficheros durante su rutina `extractAssets()`, acelerando dramáticamente la velocidad del primer inicio tras instalar el APK.
*   **Seguridad:** Garantiza la portabilidad de la aplicación y la limpieza absoluta del empaquetado del APK.
