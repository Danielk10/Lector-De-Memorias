# Reporte de Auditoría y Corrección: Rutas y Portabilidad de EepromFlasher
ESTADO: CORREGIDO (Última actualización: Agosto 2026)

Este documento detalla los hallazgos críticos y las acciones correctivas aplicadas para garantizar la portabilidad de las herramientas nativas (`minipro`, `libusb`, etc.) en Android para el paquete `com.diamon.mini`.

### 1. Problema: Rutas Hardcoded en Binarios (RUNPATH) - SOLUCIONADO
**Acción:** Se utilizó `patchelf --remove-rpath` en todas las librerías compartidas y binarios dentro de `app/src/main/jniLibs/arm64-v8a/`. Durante la auditoría se encontraron referencias a rutas obsoletas de compilación (ej. `/data/data/com.termux/files/usr/lib`) incrustadas en el header ELF `RUNPATH`.
**Resultado:** El sistema Android ahora cargará las librerías dinámicas utilizando estrictamente las rutas estándar del sistema y el directorio nativo del APK, o en su defecto a través de la variable `LD_LIBRARY_PATH` asignada en runtime. Se eliminó la dependencia total del entorno de Termux.

### 2. Contaminación en Assets y Scripts de Configuración - SOLUCIONADO
**Acción:** Se auditaron las carpetas `assets` y `fake_root` en busca de rutas absolutas al entorno de desarrollo (`/data/data/com.termux`). Estas rutas se encontraban mayoritariamente en configuraciones de CMake y de desarrollo de `libusb`.
**Resultado:** Al no ser necesarias estas herramientas de compilación en el entorno de ejecución de Android, se procedió a eliminar completamente las carpetas `include/`, `pkgconfig/`, `udev/` y archivos redundantes. Los scripts que sí se necesitan en tiempo de ejecución (como `dump-alg-minipro.bash`) se validaron y no contienen rutas absolutas hardcodeadas hacia Termux.

### 3. Optimización de Espacio en el APK - SOLUCIONADO
**Acción:** Se excluyeron de la carpeta de `assets` los recursos que no son necesarios en runtime (librerías estáticas `.a` y headers C/C++), reubicándolos conceptualmente para el desarrollo con NDK en el proyecto Android, mientras que los archivos originales se conservan intactos en la carpeta de referencia `fake_root` para tomar cualquier cosa faltante. También se limpió documentación innecesaria en runtime.
**Resultado:** Se redujo el tamaño del paquete, liberando espacio redundante en los assets. Con esto, se evita la extracción innecesaria de archivos de desarrollo en el dispositivo, acelerando los procesos de `AssetHelper.java` durante el primer arranque, garantizando al mismo tiempo que los includes y archivos de desarrollo NDK estén en su lugar correcto para el entorno.

### 4. Carga Dinámica de Dependencias JNI y Renombramiento - SOLUCIONADO
**Acción:** Se implementó una resolución inteligente de dependencias nativas en la capa Java, junto con el renombramiento de los binarios PIE (Position-Independent Executable). Google Play y Android requieren que todo lo contenido en `jniLibs` lleve el prefijo `lib` y extensión `.so`.
**Resultado:** El ejecutable `minipro` fue renombrado exitosamente a `libminipro_bin.so`. La aplicación recuperará la versión renombrada usando `getApplicationInfo().nativeLibraryDir` y creará symlinks o referencias adecuadas en runtime para ejecutar la suite con el cargador dinámico de Android.

### 5. Resolución de Dependencia Versionada `libz.so.1` - SOLUCIONADO
**Acción:** El ejecutable `minipro` compilado originalmente dependía de la versión versionada de Zlib (`libz.so.1`). Para cumplir con las reglas de empaquetado de Android (Google Play/Gradle no empaquetan archivos que no terminen estrictamente en `.so`), el archivo se renombró a `libz_1.so` dentro del directorio `jniLibs/arm64-v8a/` y `libs/`. En el código fuente (`AssetHelper.java`), se añadió una instrucción para crear un enlace simbólico (`symlink`) en tiempo de ejecución desde `libz_1.so` hacia la ruta `usr/lib/libz.so.1`.
**Resultado:** El ejecutable original sin parchear ahora encuentra la librería con su nombre versionado real en tiempo de ejecución mediante el enlace simbólico, respetando los estándares de Google Play.
