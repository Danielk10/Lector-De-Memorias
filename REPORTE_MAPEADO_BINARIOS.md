# Nombres Nuevos vs Antiguos (Binarios Ejecutables)

De acuerdo a la verificación del archivo `REPORTE_ANALISIS_DEPENDENCIAS.md` y a la separación estricta entre binarios (ejecutables) y librerías compartidas (`.so`), a continuación se presenta la relación de los binarios reales de la aplicación. 

Google Play requiere que todo archivo dentro de `jniLibs/arm64-v8a/` tenga el formato `lib<nombre>.so`. Por ello, los binarios han sido renombrados en la carpeta, y el código de la app (`AssetHelper.java`) se encarga de extraerlos/enlazarlos en las rutas exactas requeridas en el *fake_root* de la aplicación en tiempo de ejecución.

## 1. Mapeo de Binarios Ejecutables

El único binario ejecutable PIE descrito en `REPORTE_ANALISIS_DEPENDENCIAS.md` es `minipro`. Ha sido renombrado en `arm64-v8a` para cumplir con las políticas de Google Play, y es reconstruido en la ruta exacta del `fake_root` por `AssetHelper.java`.

| Nombre Antiguo (Original) | Nombre Nuevo (Google Play) en `arm64-v8a` | Ruta exacta reconstruida en App (`fake_root`) |
|---|---|---|
| `minipro` | `libminipro_bin.so` | `usr/bin/minipro` |

---

## 2. Dependencias de los Binarios

Todas las dependencias listadas en el reporte han sido verificadas dentro de `jniLibs/arm64-v8a/` o se resuelven como librerías del sistema de Android.

| Binario | Dependencias Exigidas (DT_NEEDED) | Estado en `arm64-v8a` / App |
|---|---|---|
| **`minipro`** | `liblog.so`, `libusb-1.0.so` (ahora `libusb_1_0.so`), `libz.so.1` (ahora `libz_1.so`), `libdl.so`, `libc.so` | ✔ Todas las dependencias presentes. `libusb_1_0.so` y `libz_1.so` se proveen en `jniLibs/arm64-v8a/` y el resto son librerías del sistema Android. |

---

## 3. Estado de Archivos en `assets` vs `fake_root`

Al revisar y comparar la carpeta `fake_root` contra los archivos incluidos en `assets`, se confirmó que:

1. **Binarios y Librerías (.so):** No están en `assets`. Están correctamente ubicados en `app/src/main/jniLibs/arm64-v8a/` (`libminipro_bin.so` y `libusb_1_0.so`). Durante la ejecución de la app, se enlazan (symlinks) o copian a la jerarquía de directorios esperada (`usr/bin/minipro`, `usr/lib/libusb-1.0.so`).
2. **Scripts Shell y Otros Recursos:** Herramientas como `dump-alg-minipro.bash` no son binarios ELF (son scripts shell). Éstos están localizados correctamente en `assets/` y se extraen directamente a `usr/bin/` al iniciarse la app.
3. **Resto de archivos en assets:** Se incluyen correctamente `infoic.xml` y `logicic.xml` bajo `usr/share/minipro/` sin faltar ningún archivo de sus respectivas rutas indispensables.
