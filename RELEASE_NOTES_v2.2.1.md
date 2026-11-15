# EEPROM Flasher v2.2.1 (versionCode 9) — Corrección del consentimiento en el pase Pro / Pro pass consent fix

Fecha de lanzamiento / Release Date: 15 de noviembre de 2026 / November 15, 2026

---

## 🇪🇸 Español

### 📌 Resumen
Esta versión corrige un aviso incorrecto en el **pase Pro de 15 minutos**. En países donde el consentimiento de anuncios **no es obligatorio**, al pulsar «Ver video» podía aparecer el mensaje «Los videos recompensados requieren consentimiento», aunque UMP no exigiera ninguno.

El problema era una condición de carrera: la app consultaba `canRequestAds()` antes de que UMP terminara de resolver la geografía del usuario. UMP devuelve `false` mientras está consultando, y el código lo interpretaba como un rechazo.

### 🛠️ Cambios
• El pase Pro ahora **espera** a que UMP termine antes de decidir si el video recompensado puede reproducirse.
• En países sin consentimiento obligatorio, el video se carga y se muestra **sin formulario ni aviso**.
• El aviso «se requiere consentimiento» solo aparece si UMP confirma que el consentimiento es obligatorio y el usuario no lo otorgó.
• Si UMP falla temporalmente (por ejemplo, sin red), el flujo ya no queda bloqueado para siempre: se reintenta en la siguiente petición.
• Añadido log del estado real de UMP (`status`) para facilitar el diagnóstico.
• El script `upload_play_store.py` incorpora `--dry-run` para preparar una subida y descartarla sin publicar.

### ✅ Validación
• `:app:compileDebugJavaWithJavac` y `:app:testDebugUnitTest` correctos.
• `assembleRelease` y `bundleRelease` correctos.
• APK firmado y verificado: `versionCode 9`, `versionName 2.2.1`, ABIs `arm64-v8a` y `armeabi-v7a`.
• AAB firmado y subido a la pista de producción de Google Play.

### 📦 Artefactos
• **APK Release:** `app-release.apk`
  **SHA-256:** `3b342618c7a18582522bc6c39b319fd0a0e142b3e8e506e3cb4fea0b307e8ea8`
• **AAB Release (Google Play Bundle):** `app-release.aab`
  **SHA-256:** `5fb675fc2e360d2679686d9b6ff6278391dad4614afeb4b31081dcd6408084f3`

---

## 🇺🇸 English

### 📌 Overview
This release fixes an incorrect message in the **15-minute Pro pass**. In countries where ad consent is **not required**, tapping “Watch video” could show “Rewarded videos require ad consent” even though UMP did not require any consent.

The cause was a race condition: the app checked `canRequestAds()` before UMP finished resolving the user's geography. UMP returns `false` while it is still checking, and the code treated that as a refusal.

### 🛠️ Changes
• The Pro pass now **waits** for UMP to finish before deciding whether the rewarded video can play.
• In countries where consent is not required, the video loads and plays **without a form or message**.
• The “consent required” message only appears when UMP confirms that consent is required and was not granted.
• If UMP temporarily fails (for example, no network), the flow is no longer blocked permanently: it retries on the next request.
• Added logging of UMP's real status (`status`) to simplify diagnosis.
• `upload_play_store.py` now supports `--dry-run` to prepare an upload and discard it without publishing.

### ✅ Validation
• `:app:compileDebugJavaWithJavac` and `:app:testDebugUnitTest` succeeded.
• `assembleRelease` and `bundleRelease` succeeded.
• APK signed and verified: `versionCode 9`, `versionName 2.2.1`, `arm64-v8a` and `armeabi-v7a` ABIs.
• AAB signed and uploaded to the Google Play production track.

### 📦 Artifacts
• **APK Release:** `app-release.apk`
  **SHA-256:** `3b342618c7a18582522bc6c39b319fd0a0e142b3e8e506e3cb4fea0b307e8ea8`
• **AAB Release (Google Play Bundle):** `app-release.aab`
  **SHA-256:** `5fb675fc2e360d2679686d9b6ff6278391dad4614afeb4b31081dcd6408084f3`

---

## 🔗 Enlaces / Links
• **Repositorio / Repository:** https://github.com/Danielk10/Lector-De-Memorias
• **Release:** https://github.com/Danielk10/Lector-De-Memorias/releases/tag/v2.2.1
