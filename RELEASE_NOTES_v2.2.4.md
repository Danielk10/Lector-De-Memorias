# EEPROM Flasher v2.2.4 (versionCode 12) — Corrección de compatibilidad de catálogo y soporte TL866II+

Fecha / Date: 19 de noviembre de 2026 / November 19, 2026

---

## 🇪🇸 Español

### 📌 Resumen
Esta actualización corrige un problema crítico de compatibilidad en la selección y autodetección de chips con el programador TL866II+. Se normalizó el catálogo completo contra la base de datos interna de minipro, se agregaron diálogos orientativos para autodetección y se añadió protección ante dispositivos en modo bootloader.

### 🛠️ Cambios
• **Catálogo de chips normalizado:** corrección de 238 nombres de componentes (series 24xxx, 93xxx, 25xxx, 27xxx) incluyendo variantes de encapsulado (`@SOIC8`, `@DIP28`) y bus (`x8`/`x16`) para garantizar coincidencia exacta con `infoic.xml`.
• **Claridad en autodetección:** el botón "Auto-detectar" ahora advierte que la lectura por hardware aplica únicamente a SPI Flash (25xxx) vía JEDEC ID y ofrece acceso directo al catálogo para memorias EEPROM (24xxx/93xxx).
• **Detección de modo Bootloader:** advertencia proactiva si el TL866II+ se encuentra en modo bootloader (`VID:PID 04d8:00e0`) con instrucciones claras para restaurar el firmware.
• **Validación de integridad de assets:** verificación de tamaño mínimo de `infoic.xml` y `logicic.xml` al inicio para auto-recuperar archivos truncados.
• **Mensajes y sugerencias contextuales:** detección de errores habituales de minipro en el log terminal con recomendaciones accionables.

### 📋 Notas de lanzamiento para Google Play
```text
- Corrección de compatibilidad en catálogo de chips para TL866II+ (sufijos de encapsulado y organización en series 24xxx, 93xxx, 25xxx y 27xxx).
- Nuevo diálogo informativo en Auto-detectar: aclara el uso exclusivo en SPI Flash y guía hacia el catálogo en memorias EEPROM.
- Detección y advertencia de programadores en modo bootloader con instrucciones de recuperación.
- Verificación automática de integridad de la base de datos de chips (infoic.xml) para evitar fallos de lectura.
```

### 📦 Artefactos
- `EEPROM-Flasher-v2.2.4-release.apk` — 11655027 bytes
  - SHA-256: `352ae956f37772c48e7f72a26dee80684a5a15b7dcb6934c143c8e8c15bc03e5`
- `EEPROM-Flasher-v2.2.4-release.aab` — 10633875 bytes
  - SHA-256: `a7665f911e0c056070154d27b08d86749f3aa9f1d915360d3fe8061562a53b12`
- `versionCode 12`, `versionName 2.2.4`, `minSdk 24`, `targetSdk 37`.

---

## 🇺🇸 English

### 📌 Overview
This update resolves a critical compatibility issue with chip selection and autodetection on the TL866II+ programmer. The entire chip catalog has been normalized against the internal minipro database, explanatory dialogs have been added for autodetection, and bootloader mode protection has been introduced.

### 🛠️ Changes
• **Normalized Chip Catalog:** corrected 238 chip identifiers (24xxx, 93xxx, 25xxx, 27xxx series) including package suffixes (`@SOIC8`, `@DIP28`) and bus organization (`x8`/`x16`) to match `infoic.xml` exactly.
• **Clear Autodetection Flow:** the "Autodetect" button now clarifies that hardware identification only applies to SPI Flash (25xxx) via JEDEC ID, offering direct catalog access for EEPROMs (24xxx/93xxx).
• **Bootloader Mode Protection:** proactive alert when TL866II+ is connected in bootloader mode (`VID:PID 04d8:00e0`) with recovery guidance.
• **Asset Integrity Check:** automatic verification of `infoic.xml` and `logicic.xml` size on launch to recover from truncated extractions.
• **Contextual Terminal Hints:** common minipro error codes now provide clear, actionable troubleshooting hints in the output log.

### 📋 Google Play Release Notes
```text
- Fixed chip catalog naming compatibility for TL866II+ (package suffixes and organization in 24xxx, 93xxx, 25xxx, and 27xxx series).
- Added explanatory dialog for Autodetect: clarifies SPI Flash scope and directs to the catalog for EEPROMs.
- Added bootloader mode detection and alert with recovery hints for TL866II+.
- Added automatic integrity validation for the chip database (infoic.xml) to prevent false detection errors.
```

### 📦 Artifacts
- `EEPROM-Flasher-v2.2.4-release.apk` — 11655027 bytes
  - SHA-256: `352ae956f37772c48e7f72a26dee80684a5a15b7dcb6934c143c8e8c15bc03e5`
- `EEPROM-Flasher-v2.2.4-release.aab` — 10633875 bytes
  - SHA-256: `a7665f911e0c056070154d27b08d86749f3aa9f1d915360d3fe8061562a53b12`
- `versionCode 12`, `versionName 2.2.4`, `minSdk 24`, `targetSdk 37`.
