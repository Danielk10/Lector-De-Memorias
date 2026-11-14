# EEPROM Flasher v2.2.0 (versionCode 8) — Soporte 32-bit (armeabi-v7a) / 32-bit support

Fecha de lanzamiento / Release Date: 14 de noviembre de 2026 / November 14, 2026

---

## 🇪🇸 Español

### 📌 Resumen
Esta versión añade **soporte nativo para dispositivos de 32 bits** (`armeabi-v7a`). Hasta ahora la aplicación solo incluía los binarios para `arm64-v8a`, por lo que en equipos o instalaciones de 32 bits el motor `minipro` no podía ejecutarse. Ahora el APK incorpora las dos arquitecturas y Android elige la correcta en cada dispositivo.

No hay cambios en la interfaz ni en el modo Pro: todas las funciones del 2.1.0 siguen igual.

### 🛠️ Cambios
• Añadida la ABI `armeabi-v7a` a `abiFilters`, junto a `arm64-v8a`.
• Nuevos binarios de 32 bits en `jniLibs/armeabi-v7a/`:
  – `libminipro_bin.so` (motor minipro 0.7.4, ARMv7, PIE)
  – `libusb_1_0.so` (libusb 1.0.30 parcheada para USB Host API)
  – `libz_1.so` (zlib 1.2.12)
• `libz.so.1` añadido a `fake_root_32_bit` y `fake_root_64_bit`, de modo que cada árbol queda autosuficiente.
• Los assets (`infoic.xml`, `logicic.xml`, `dump-alg-minipro.bash`) siguen siendo **una sola copia compartida** por las dos arquitecturas: son datos, no código máquina.
• Los scripts `build_minipro_custom.sh` y `build_libusb_custom_mini.sh` aceptan ahora `ABI=32` o `ABI=64`.
• `analizar_dependencias.py` valida las dos arquitecturas (clase ELF, `DT_NEEDED`, empaquetado en `jniLibs`) y regenera su reporte.
• `README.md` y `REPORTE_MAPEADO_BINARIOS.md` actualizados a las dos ABIs.

### ✅ Validación
• `assembleRelease` y `bundleRelease` correctos.
• `analizar_dependencias.py`: 32 y 64 bits con todas las dependencias presentes.
• Verificado que el APK incluye `lib/arm64-v8a/` y `lib/armeabi-v7a/`.
• Alineación de páginas de 16 KB en los binarios de 64 bits.
• La ejecución real en un dispositivo ARMv7 queda pendiente de prueba física.

### 📦 Artefactos
• **APK Release:** `app-release.apk`
  **SHA-256:** `38465139362fd8e909c8a0ebfa07c740629e6cb52daf10e1813bec10d92ffa29`
• **AAB Release (Google Play Bundle):** `app-release.aab`
  **SHA-256:** `2b05a075ea68709193677ef57963843b7f9f5609b1c79928374425855d8783ed`

---

## 🇺🇸 English

### 📌 Overview
This release adds **native support for 32-bit devices** (`armeabi-v7a`). Until now the app only bundled `arm64-v8a` binaries, so on 32-bit devices or installations the `minipro` engine could not run. The APK now ships both architectures and Android picks the right one per device.

There are no UI or Pro-mode changes: every 2.1.0 feature behaves exactly the same.

### 🛠️ Changes
• Added the `armeabi-v7a` ABI to `abiFilters`, alongside `arm64-v8a`.
• New 32-bit binaries under `jniLibs/armeabi-v7a/`:
  – `libminipro_bin.so` (minipro 0.7.4 engine, ARMv7, PIE)
  – `libusb_1_0.so` (libusb 1.0.30 patched for the USB Host API)
  – `libz_1.so` (zlib 1.2.12)
• Added `libz.so.1` to `fake_root_32_bit` and `fake_root_64_bit`, so each tree is self-contained.
• Assets (`infoic.xml`, `logicic.xml`, `dump-alg-minipro.bash`) remain a **single shared copy** for both architectures: they are data, not machine code.
• `build_minipro_custom.sh` and `build_libusb_custom_mini.sh` now accept `ABI=32` or `ABI=64`.
• `analizar_dependencias.py` validates both architectures (ELF class, `DT_NEEDED`, `jniLibs` packaging) and regenerates its report.
• `README.md` and `REPORTE_MAPEADO_BINARIOS.md` updated for both ABIs.

### ✅ Validation
• `assembleRelease` and `bundleRelease` succeeded.
• `analizar_dependencias.py`: 32-bit and 64-bit with all dependencies present.
• Confirmed the APK ships both `lib/arm64-v8a/` and `lib/armeabi-v7a/`.
• 16 KB page alignment on the 64-bit binaries.
• Actual execution on an ARMv7 device is still pending physical testing.

### 📦 Artifacts
• **APK Release:** `app-release.apk`
  **SHA-256:** `38465139362fd8e909c8a0ebfa07c740629e6cb52daf10e1813bec10d92ffa29`
• **AAB Release (Google Play Bundle):** `app-release.aab`
  **SHA-256:** `2b05a075ea68709193677ef57963843b7f9f5609b1c79928374425855d8783ed`

---

## 🔗 Enlaces / Links
• **Repositorio / Repository:** https://github.com/Danielk10/Lector-De-Memorias
• **Release:** https://github.com/Danielk10/Lector-De-Memorias/releases/tag/v2.2.0
