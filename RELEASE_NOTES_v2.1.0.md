# EEPROM Flasher v2.1.0 (versionCode 7) — Modo Pro, pase recompensado y bóveda antibrick / Pro Mode, Rewarded Pass and Antibrick Vault

Fecha de lanzamiento / Release Date: 13 de noviembre de 2026 / November 13, 2026

---

## 🇪🇸 Español

### 📌 Resumen
Esta versión introduce **EEPROM Flasher Pro** sin retirar ninguna función gratuita. Pro añade una suite técnica avanzada para trabajar con volcados de EEPROM y Flash: bóveda antibrick, verificador criptográfico, editor HEX con escritura, diff avanzado e informe técnico, además de eliminar los anuncios.

Hay dos formas de acceder a Pro:
1. **Pase Pro temporal de 15 minutos** viendo un video recompensado real de AdMob.
2. **Compra permanente** de pago único `eeprom_flasher_pro` por **4,99 USD** en Google Play.

### ⭐ Qué hace el pase Pro
• Desbloquea **todas las funciones Pro durante 15 minutos**.
• Se activa al terminar el video recompensado, no al abrirlo.
• Si se ve otro video con el pase activo, el pase se **restablece a 15 minutos** desde ese momento; no se acumulan minutos.
• Con el pase activo, el botón **Ver video** sigue disponible y reinicia el pase a 15 minutos. Solo la compra permanente oculta la opción.
• El pase se guarda localmente con firma SHA-256 y sobrevive al reinicio de la app.
• Es personal del dispositivo: **no se transfiere** a otro móvil ni se restaura desde una copia de seguridad.
• Al terminar el contador, la app vuelve al modo gratuito y los anuncios reaparecen.
• Los cambios ya realizados en el editor HEX **no se pierden**: se pueden guardar o aplicar a `rom.bin` aunque el pase haya vencido.

### 👑 Qué pasa cuando el usuario es Pro
• **Sin anuncios:** no se solicitan ni muestran banners ni intersticiales.
• **Bóveda Antibrick:** copias automáticas de `rom.bin` antes de leer, escribir o borrar el chip, más copias manuales. Restauración segura a `rom.bin` (nunca escribe directamente al chip) y límite de 8 instantáneas.
• **Verificador de integridad:** MD5, SHA-1, SHA-256 y CRC32 en una sola pasada, con comparación contra hashes del portapapeles.
• **Editor HEX con escritura:** edición byte a byte, deshacer/rehacer, guardar copia y aplicar los cambios a `rom.bin`.
• **Diff avanzado:** análisis por sectores de 4 KB, primera y última diferencia e informe exportable.
• **Informe técnico:** hashes, comprobación de blanco, histograma de bytes y huella SHA-256, exportable a `.txt`.
• **Compra permanente:** se reconoce automáticamente, se restaura con la misma cuenta de Google y no caduca.

Las funciones gratuitas siguen intactas: detección USB, selección de programador, catálogo y chips personalizados, autodetección, lectura, escritura, verificación, borrado, importación `.bin`/`.hex`, exportación del volcado, visor HEX, diff básico, pinouts, comando `minipro` personalizado y copia del registro.

### 🛠️ Cambios
• Integrado Google Play Billing 8.3.0 con el producto permanente `eeprom_flasher_pro` (4,99 USD).
• Añadido el pase Pro de 15 minutos con el anuncio recompensado real `ca-app-pub-5141499161332805/3489519872`.
• Añadida la bóveda antibrick con copias automáticas antes de operaciones peligrosas.
• Añadido el verificador de integridad MD5/SHA-1/SHA-256/CRC32.
• Añadido el editor HEX con escritura, deshacer/rehacer y aplicación a la ROM.
• Añadido el diff avanzado por sectores y el informe técnico exportable.
• Suprimidos banner e intersticiales mientras Pro está activo.
• Excluidas las preferencias de compra y pase de la copia de seguridad y transferencia entre dispositivos.
• Cadenas completas en inglés y español.
• Compatibilidad mínima: Android 7.0 (API 24).

### ✅ Validación
• `assembleDebug`, `assembleRelease` y `bundleRelease` correctos.
• `testDebugUnitTest`: **15 pruebas superadas**, incluidas las de integridad, diff por sectores y deshacer/rehacer.
• `lintDebug` sin errores.
• APK y AAB firmados con la clave de producción; certificado verificado y coincidente con v2.0.3 (`SHA-256 0a289008…e626`).
• **Emulador local de minipro:** lectura EEPROM `AT24C02C` y SPI `W25Q80BV` con coincidencia byte a byte; escritura `AT24C02C` con `Verification OK` y contenido final correcto.
• La compra real, la restauración y el video recompensado requieren una instalación desde Google Play y la unidad de AdMob activa; quedan pendientes de prueba en dispositivo.

### 📦 Artefactos
• **APK Release:** `app-release.apk`  
  **SHA-256:** `77552aa4290e64fb2669112107a4291acad280aea18472f449a15aa7ac5c8e07`
• **AAB Release (Google Play Bundle):** `app-release.aab`  
  **SHA-256:** `d949d7bea5c35b6fcaf35002e81d8a6eb353347c5c80d6065d562e940b0fcd69`

---

## 🇺🇸 English

### 📌 Overview
This release introduces **EEPROM Flasher Pro** without removing any free feature. Pro adds an advanced technical suite for EEPROM and Flash dumps: antibrick vault, cryptographic integrity checker, writable HEX editor, advanced diff and technical report, plus ad-free use.

There are two ways to access Pro:
1. **15-minute temporary Pro pass** by watching a real AdMob rewarded video.
2. **Permanent one-time purchase** of `eeprom_flasher_pro` for **USD 4.99** on Google Play.

### ⭐ What the Pro pass does
• Unlocks **every Pro feature for 15 minutes**.
• It activates when the rewarded video finishes, not when it opens.
• Watching another video while the pass is active **resets the pass to 15 minutes** from that moment; minutes do not stack.
• With an active pass, the **Watch video** button stays available and restarts the 15-minute pass. Only the permanent purchase hides the option.
• The pass is stored locally with a SHA-256 signature and survives app restarts.
• It is bound to the device: it is **not transferred** to another phone and is not restored from backups.
• When the countdown ends, the app returns to free mode and ads reappear.
• Edits already made in the HEX editor are **not lost**: they can still be saved or applied to `rom.bin` after the pass expires.

### 👑 What happens when the user is Pro
• **No ads:** banners and interstitials are neither requested nor shown.
• **Antibrick Vault:** automatic `rom.bin` snapshots before reading, writing or erasing the chip, plus manual snapshots. Safe restore into `rom.bin` (never writes directly to the chip) with an 8-snapshot limit.
• **Integrity checker:** MD5, SHA-1, SHA-256 and CRC32 in a single pass, with clipboard hash comparison.
• **Writable HEX editor:** byte-level editing, undo/redo, save a copy and apply changes to `rom.bin`.
• **Advanced diff:** 4 KB sector analysis, first/last difference and exportable report.
• **Technical report:** hashes, blank check, byte histogram and SHA-256 fingerprint, exportable to `.txt`.
• **Permanent purchase:** recognized automatically, restored with the same Google account and never expires.

All free features remain available: USB detection, programmer selection, chip catalog and custom chips, autodetect, read, write, verify, erase, `.bin`/`.hex` import, dump export, HEX viewer, basic diff, pinouts, custom `minipro` command and log copy.

### 🛠️ Changes
• Integrated Google Play Billing 8.3.0 with the permanent `eeprom_flasher_pro` product (USD 4.99).
• Added the 15-minute Pro pass using the real rewarded unit `ca-app-pub-5141499161332805/3489519872`.
• Added the antibrick vault with automatic snapshots before risky operations.
• Added the MD5/SHA-1/SHA-256/CRC32 integrity checker.
• Added the writable HEX editor with undo/redo and apply-to-ROM.
• Added the sector-based advanced diff and the exportable technical report.
• Suppressed banners and interstitials while Pro is active.
• Excluded purchase and pass preferences from backup and device transfer.
• Complete English and Spanish strings.
• Minimum compatibility: Android 7.0 (API 24).

### ✅ Validation
• `assembleDebug`, `assembleRelease` and `bundleRelease` succeeded.
• `testDebugUnitTest`: **15 tests passed**, including integrity, sector diff and undo/redo.
• `lintDebug` with no errors.
• APK and AAB signed with the production key; certificate verified and matching v2.0.3 (`SHA-256 0a289008…e626`).
• **Local minipro emulator:** EEPROM `AT24C02C` and SPI `W25Q80BV` reads matched byte for byte; `AT24C02C` write returned `Verification OK` with the correct final contents.
• The real purchase, restore and rewarded video require a Google Play installation with the AdMob unit active, so they remain pending on-device testing.

### 📦 Artifacts
• **APK Release:** `app-release.apk`  
  **SHA-256:** `77552aa4290e64fb2669112107a4291acad280aea18472f449a15aa7ac5c8e07`
• **AAB Release (Google Play Bundle):** `app-release.aab`  
  **SHA-256:** `d949d7bea5c35b6fcaf35002e81d8a6eb353347c5c80d6065d562e940b0fcd69`

---

## 🔗 Enlaces / Links
• **Repositorio / Repository:** https://github.com/Danielk10/Lector-De-Memorias
• **Release:** https://github.com/Danielk10/Lector-De-Memorias/releases/tag/v2.1.0
