# Reporte de análisis de dependencias — 32 y 64 bits

Generado por `analizar_dependencias.py` el 2026-11-14.

Analiza los binarios de `minipro` para las dos arquitecturas soportadas, verifica que cada uno tenga la clase ELF correcta y comprueba que todas las dependencias `DT_NEEDED` estén presentes en el árbol o las provea Android.

## Resumen

| Arquitectura | ABI | Fake root | Libs | Estado |
|---|---|---|---|---|
| 32-bit | `armeabi-v7a` | `fake_root_32_bit` | `libs_32` | ✔ Completo |
| 64-bit | `arm64-v8a` | `fake_root_64_bit` | `libs_64` | ✔ Completo |

## 1. 32-bit (`armeabi-v7a`)

### Binarios analizados

| Binario | Ruta | ELF detectado | ELF esperado | Estado | Tamaño |
|---|---|---|---|---|---|
| `minipro` | `fake_root_32_bit/data/data/com.diamon.mini/files/usr/bin/minipro` | ELF32 / ARM | ELF32 / ARM | ✔ Correcto | 187 KB |
| `libusb-1.0.so` | `fake_root_32_bit/data/data/com.diamon.mini/files/usr/lib/libusb-1.0.so` | ELF32 / ARM | ELF32 / ARM | ✔ Correcto | 109 KB |
| `libz.so.1` | `libs_32/libz.so.1` | ELF32 / ARM | ELF32 / ARM | ✔ Correcto | 55 KB |

### Dependencias `DT_NEEDED`

| Binario | Dependencia | Tipo | Disponible | Dónde se resuelve |
|---|---|---|---|---|
| `minipro` | `liblog.so` | Sistema (Android) | ✔ sistema | /system/lib/ |
| `minipro` | `libusb-1.0.so` | Externa (empaquetada) | ✔ árbol | `fake_root_32_bit/data/data/com.diamon.mini/files/usr/lib/libusb-1.0.so` |
| `minipro` | `libz.so.1` | Externa (empaquetada) | ✔ árbol | `libs_32/libz.so.1` |
| `minipro` | `libdl.so` | Sistema (Android) | ✔ sistema | /system/lib/ |
| `minipro` | `libc.so` | Sistema (Android) | ✔ sistema | /system/lib/ |
| `libusb-1.0.so` | `libdl.so` | Sistema (Android) | ✔ sistema | /system/lib/ |
| `libusb-1.0.so` | `libc.so` | Sistema (Android) | ✔ sistema | /system/lib/ |
| `libz.so.1` | `libc.so` | Sistema (Android) | ✔ sistema | /system/lib/ |

### Empaquetado en `jniLibs/armeabi-v7a/`

| Archivo en el APK | Ruta | Estado |
|---|---|---|
| `libminipro_bin.so` | `app/src/main/jniLibs/armeabi-v7a/libminipro_bin.so` | ✔ Presente |
| `libusb_1_0.so` | `app/src/main/jniLibs/armeabi-v7a/libusb_1_0.so` | ✔ Presente |
| `libz_1.so` | `app/src/main/jniLibs/armeabi-v7a/libz_1.so` | ✔ Presente |

## 2. 64-bit (`arm64-v8a`)

### Binarios analizados

| Binario | Ruta | ELF detectado | ELF esperado | Estado | Tamaño |
|---|---|---|---|---|---|
| `minipro` | `fake_root_64_bit/data/data/com.diamon.mini/files/usr/bin/minipro` | ELF64 / AArch64 | ELF64 / AArch64 | ✔ Correcto | 219 KB |
| `libusb-1.0.so` | `fake_root_64_bit/data/data/com.diamon.mini/files/usr/lib/libusb-1.0.so` | ELF64 / AArch64 | ELF64 / AArch64 | ✔ Correcto | 133 KB |
| `libz.so.1` | `libs_64/libz.so.1` | ELF64 / AArch64 | ELF64 / AArch64 | ✔ Correcto | 71 KB |

### Dependencias `DT_NEEDED`

| Binario | Dependencia | Tipo | Disponible | Dónde se resuelve |
|---|---|---|---|---|
| `minipro` | `liblog.so` | Sistema (Android) | ✔ sistema | /system/lib64/ |
| `minipro` | `libusb-1.0.so` | Externa (empaquetada) | ✔ árbol | `fake_root_64_bit/data/data/com.diamon.mini/files/usr/lib/libusb-1.0.so` |
| `minipro` | `libz.so.1` | Externa (empaquetada) | ✔ árbol | `libs_64/libz.so.1` |
| `minipro` | `libdl.so` | Sistema (Android) | ✔ sistema | /system/lib64/ |
| `minipro` | `libc.so` | Sistema (Android) | ✔ sistema | /system/lib64/ |
| `libusb-1.0.so` | `libdl.so` | Sistema (Android) | ✔ sistema | /system/lib64/ |
| `libusb-1.0.so` | `libc.so` | Sistema (Android) | ✔ sistema | /system/lib64/ |
| `libz.so.1` | `libc.so` | Sistema (Android) | ✔ sistema | /system/lib64/ |

### Empaquetado en `jniLibs/arm64-v8a/`

| Archivo en el APK | Ruta | Estado |
|---|---|---|
| `libminipro_bin.so` | `app/src/main/jniLibs/arm64-v8a/libminipro_bin.so` | ✔ Presente |
| `libusb_1_0.so` | `app/src/main/jniLibs/arm64-v8a/libusb_1_0.so` | ✔ Presente |
| `libz_1.so` | `app/src/main/jniLibs/arm64-v8a/libz_1.so` | ✔ Presente |

## Notas

* `libc.so`, `libdl.so`, `liblog.so` y `libm.so` las provee Android; no se empaquetan en el APK.
* `libusb-1.0.so` se empaqueta como `libusb_1_0.so` y `libz.so.1` como `libz_1.so`; `AssetHelper.java` crea los symlinks `usr/lib/libusb-1.0.so` y `usr/lib/libz.so.1` en tiempo de ejecución.
* `minipro` se empaqueta como `libminipro_bin.so` y se enlaza como `usr/bin/minipro`.
* Los assets (`infoic.xml`, `logicic.xml`, `dump-alg-minipro.bash`) son comunes a las dos arquitecturas y viven en `app/src/main/assets/`.

