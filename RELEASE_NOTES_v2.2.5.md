# EEPROM Flasher v2.2.5 (versionCode 13) — Corrección de directorio de trabajo y guardado de rom.bin en TL866II+

Fecha / Date: 20 de noviembre de 2026 / November 20, 2026

---

## 🇪🇸 Español

### 📌 Resumen
Esta actualización corrige un error crítico de ejecución en el programador TL866II+ donde las operaciones de lectura y escritura fallaban con el mensaje "Could not open file rom.bin for writing. Read-only file system". Se configuró el directorio de trabajo del subproceso nativo de minipro apuntando al almacenamiento interno de la aplicación y se añadió la versión al inicio del registro de la app para agilizar el soporte técnico.

### 🛠️ Cambios
• **Corrección de ruta de trabajo nativa:** el proceso hijo de `minipro` ahora establece su directorio de trabajo (`chdir`) en el almacenamiento interno de la app (`filesDir`), permitiendo crear y escribir archivos temporales como `rom.bin` sin errores de sistema de archivos de solo lectura.
• **Registro de versión al iniciar:** se imprime la versión exacta de EEPROM Flasher (`v2.2.5 (13)`) en la consola de depuración desde el arranque para validar instantáneamente el estado de la aplicación.
• **Mejoras acumuladas:** incluye todas las optimizaciones de catálogo y compatibilidad de TL866II+ introducidas en v2.2.4.

### 📋 Notas de lanzamiento para Google Play
```text
- Corregido el error "Read-only file system" al leer o escribir chips en TL866II+: rom.bin ahora se guarda en el almacenamiento interno de la app.
- Añadida la versión al inicio del registro para facilitar el soporte técnico.
- Incluye las mejoras de catálogo, autodetección y modo bootloader de la v2.2.4.
```

### 📦 Artefactos
- `EEPROM-Flasher-v2.2.5-release.apk` — 11655723 bytes
  - SHA-256: `9073ae0f0ed15ebe6d4637de927dedb28ff51199197ec6f471354b5821bff948`
- `EEPROM-Flasher-v2.2.5-release.aab` — 10634618 bytes
  - SHA-256: `b559539ed930eb7e9cde887c52ea2af912f04bdc935f518b2b645b776587d467`
- `versionCode 13`, `versionName 2.2.5`, `minSdk 24`, `targetSdk 37`.

---

## 🇺🇸 English

### 📌 Overview
This update resolves a critical execution error on the TL866II+ programmer where read and write operations failed with "Could not open file rom.bin for writing. Read-only file system". The working directory for the native minipro sub-process is now set to internal app storage, and version details are printed at application startup for faster technical support.

### 🛠️ Changes
• **Native Process Working Directory Fix:** minipro's child process now sets its working directory (`chdir`) to internal app storage (`filesDir`), allowing temporary files like `rom.bin` to be created without read-only filesystem errors.
• **Startup Version Logging:** logs the exact app version (`v2.2.5 (13)`) upon startup in the debug console to immediately confirm the installed release.
• **Cumulative Improvements:** includes all TL866II+ chip catalog and compatibility enhancements introduced in v2.2.4.

### 📋 Google Play Release Notes
```text
- Fixed "Read-only file system" when reading/writing chips on TL866II+: rom.bin is now saved in app storage.
- Added version info at startup for easier support.
- Includes the v2.2.4 catalog, autodetect, and bootloader improvements.
```

### 📦 Artifacts
- `EEPROM-Flasher-v2.2.5-release.apk` — 11655723 bytes
  - SHA-256: `9073ae0f0ed15ebe6d4637de927dedb28ff51199197ec6f471354b5821bff948`
- `EEPROM-Flasher-v2.2.5-release.aab` — 10634618 bytes
  - SHA-256: `b559539ed930eb7e9cde887c52ea2af912f04bdc935f518b2b645b776587d467`
- `versionCode 13`, `versionName 2.2.5`, `minSdk 24`, `targetSdk 37`.
