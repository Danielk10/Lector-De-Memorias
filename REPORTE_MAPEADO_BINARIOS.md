# Nombres Nuevos vs Antiguos (Binarios Ejecutables)

De acuerdo a la verificación de `REPORTE_ANALISIS_DEPENDENCIAS.md` (válido para 32 y 64 bits) y a la separación estricta entre binarios (ejecutables) y librerías compartidas (`.so`), a continuación se presenta la relación de los binarios reales de la aplicación.

Google Play requiere que todo archivo dentro de `jniLibs/<abi>/` tenga el formato `lib<nombre>.so`. Por ello, los binarios han sido renombrados en la carpeta de cada ABI, y el código de la app (`AssetHelper.java`) se encarga de extraerlos/enlazarlos en las rutas exactas requeridas en el *fake_root* de la aplicación en tiempo de ejecución.

## 1. Estructura por arquitectura

| Arquitectura | ABI | Binarios fuente (`fake_root`) | Librerías (`libs`) | Empaquetado (`jniLibs`) |
|---|---|---|---|---|
| 32-bit | `armeabi-v7a` | `fake_root_32_bit/data/data/com.diamon.mini/files/usr/` | `libs_32/` | `app/src/main/jniLibs/armeabi-v7a/` |
| 64-bit | `arm64-v8a` | `fake_root_64_bit/data/data/com.diamon.mini/files/usr/` | `libs_64/` | `app/src/main/jniLibs/arm64-v8a/` |

Los *assets* (`infoic.xml`, `logicic.xml`, `dump-alg-minipro.bash`) son **comunes** a las dos arquitecturas: una sola copia en `app/src/main/assets/` sirve para ambas, porque son datos y no código máquina.

## 2. Mapeo de Binarios Ejecutables

El único binario ejecutable PIE descrito en `REPORTE_ANALISIS_DEPENDENCIAS.md` es `minipro`. Ha sido renombrado en `jniLibs/<abi>/` para cumplir con las políticas de Google Play, y es reconstruido en la ruta exacta del `fake_root` por `AssetHelper.java`.

| Nombre Antiguo (Original) | Nombre Nuevo (Google Play) en `jniLibs/<abi>/` | Ruta exacta reconstruida en App (`fake_root`) |
|---|---|---|
| `minipro` | `libminipro_bin.so` | `usr/bin/minipro` |

## 3. Dependencias de los Binarios

Todas las dependencias listadas en el reporte han sido verificadas dentro de `jniLibs/<abi>/` o se resuelven como librerías del sistema de Android. El conjunto de dependencias es **idéntico** para 32 y 64 bits.

| Binario | Dependencias Exigidas (DT_NEEDED) | Estado en `jniLibs/<abi>/` / App |
|---|---|---|
| **`minipro`** | `liblog.so`, `libusb-1.0.so` (ahora `libusb_1_0.so`), `libz.so.1` (ahora `libz_1.so`), `libdl.so`, `libc.so` | ✔ Todas las dependencias presentes. `libusb_1_0.so` y `libz_1.so` se proveen en `jniLibs/<abi>/` y el resto son librerías del sistema Android. |

## 4. Estado de Archivos en `assets` vs `fake_root`

Al revisar y comparar las carpetas `fake_root_32_bit` y `fake_root_64_bit` contra los archivos incluidos en `assets`, se confirmó que:

1. **Binarios y Librerías (.so):** No están en `assets`. Están correctamente ubicados en `app/src/main/jniLibs/<abi>/` (`libminipro_bin.so`, `libusb_1_0.so` y `libz_1.so`). Durante la ejecución de la app, se enlazan (symlinks) o copian a la jerarquía de directorios esperada (`usr/bin/minipro`, `usr/lib/libusb-1.0.so`, `usr/lib/libz.so.1`).
2. **Scripts Shell y Otros Recursos:** Herramientas como `dump-alg-minipro.bash` no son binarios ELF (son scripts shell). Éstos están localizados correctamente en `assets/` y se extraen directamente a `usr/bin/` al iniciarse la app.
3. **Resto de archivos en assets:** Se incluyen correctamente `infoic.xml` y `logicic.xml` bajo `usr/share/minipro/` sin faltar ningún archivo de sus respectivas rutas indispensables. Son idénticos (`md5`) en `assets`, `fake_root_32_bit` y `fake_root_64_bit`.

## 5. Rutas antiguas eliminadas

Las siguientes rutas ya **no existen** y no deben volver a usarse en scripts ni documentación:

| Ruta antigua | Ruta actual |
|---|---|
| `fake_root/…` | `fake_root_32_bit/…` o `fake_root_64_bit/…` |
| `libs/libz.so.1` | `libs_32/libz.so.1` o `libs_64/libz.so.1` |

El script `analizar_dependencias.py` valida automáticamente las dos arquitecturas y regenera `REPORTE_ANALISIS_DEPENDENCIAS.md`.
