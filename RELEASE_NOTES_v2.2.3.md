# EEPROM Flasher v2.2.3 (versionCode 11) — Banner adaptativo, pase Pro directo e intersticial fiable

Fecha / Date: 15 de noviembre de 2026 / November 15, 2026

> **Publicado / Published:** compilado y firmado con el SDK de Android. Esta
> versión es la primera que llega a Google Play con el rediseño de publicidad
> (la 2.2.2 no se publicó en Play).

---

## 🇪🇸 Español

### 📌 Resumen
Esta versión renueva toda la publicidad de la app: el banner ocupa el ancho
completo de la pantalla, el pase Pro se abre directamente desde el menú y el
intersticial del visor HEX ya no se pierde cuando el usuario cierra el visor en
pocos segundos. Los anuncios siguen respetando el consentimiento UMP y el modo
Pro.

### 🛠️ Cambios
• **Banner adaptativo:** el banner usa `AnchoredAdaptiveBannerAdSize` con el
  ancho real de la ventana, por lo que elimina las franjas laterales del banner
  fijo de 320×50.
• **Banner oculto en el flujo Pro:** mientras el diálogo del pase Pro tapa la
  pantalla el banner se oculta (evita impresiones y clics accidentales) y se
  restaura al cerrarlo.
• **Acceso directo al pase Pro:** el menú muestra el estado del pase (tiempo
  restante o compra permanente) y abre el diálogo Pro sin pasos intermedios.
• **Intersticial compartido:** el anuncio ya no se crea dentro de
  `HexViewerActivity`. Un gestor estático lo precarga al abrir el visor desde
  `MainActivity`, lo conserva entre Activities y lo muestra al cerrar el visor.
  Si el usuario sale antes de que termine la descarga, la petición continúa y
  el anuncio queda listo para la siguiente apertura en vez de descartarse.
• **Caducidad y cooldown:** un anuncio cargado se descarta a la hora y se exige
  un mínimo de 3 minutos entre dos intersticiales.
• **Sin reinicializar UMP/MobileAds en el visor:** el consentimiento ya se
  resolvió en `MainActivity`; el visor solo reutiliza ese estado.
• **Modo Pro limpia el anuncio:** al activar el pase Pro se descarta cualquier
  intersticial cargado para no mostrarlo por error.

### ✅ Validación
• `:app:compileDebugJavaWithJavac` y `:app:bundleRelease` correctos con
  `compileSdk 37`, `buildTools 37.0.0` y `targetSdk 37`.
• APK y AAB firmados con `firma_eeprom_flasher.jks` (alias `minipro`).
• `versionCode 11` y `versionName 2.2.3`.

### 📦 Artefactos
- `EEPROM-Flasher-v2.2.3-release.apk` — 11646003 bytes
  - SHA-256: `3d06c37bb5bdf8ed0835e746f4ef06caeb91514de4bccfa00a259d1a0dc82806`
- `EEPROM-Flasher-v2.2.3-release.aab` — 10631212 bytes
  - SHA-256: `e00d7a0d2ca5d3113dd536c9188eaafbad92092df0c25bf5b4b8810d5d49f9be`
- `versionCode 11`, `versionName 2.2.3`, `minSdk 24`, `targetSdk 37`.

---

## 🇺🇸 English

### 📌 Overview
This release renews all the app’s advertising: the banner spans the full screen
width, the Pro pass opens directly from the menu, and the HEX viewer interstitial
is no longer lost when the viewer is closed within a few seconds. Ads still
respect UMP consent and Pro mode.

### 🛠️ Changes
• **Adaptive banner:** the banner uses `AnchoredAdaptiveBannerAdSize` with the
  real window width, removing the side gaps of the fixed 320×50 banner.
• **Banner hidden during the Pro flow:** the banner is hidden while the Pro pass
  dialog covers the screen (no accidental impressions or clicks) and restored
  when it closes.
• **Direct Pro pass access:** the menu shows the pass status (remaining time or
  permanent purchase) and opens the Pro dialog without extra steps.
• **Shared interstitial:** the ad is no longer created inside
  `HexViewerActivity`. A static manager preloads it when the viewer is opened
  from `MainActivity`, keeps it across Activities and shows it when the viewer
  closes. If the user leaves before the download finishes, the request continues
  and the ad is ready for the next opening instead of being discarded.
• **Expiry and cooldown:** a loaded ad is dropped after one hour and at least
  3 minutes are required between two interstitials.
• **No UMP/MobileAds re-initialisation in the viewer:** consent was already
  resolved in `MainActivity`; the viewer only reuses that state.
• **Pro mode clears the ad:** enabling the Pro pass discards any loaded
  interstitial so it is never shown by mistake.

### ✅ Validation
• `:app:compileDebugJavaWithJavac` and `:app:bundleRelease` succeeded with
  `compileSdk 37`, `buildTools 37.0.0` and `targetSdk 37`.
• APK and AAB signed with `firma_eeprom_flasher.jks` (alias `minipro`).
• `versionCode 11` and `versionName 2.2.3`.

### 📦 Artifacts
- `EEPROM-Flasher-v2.2.3-release.apk` — 11646003 bytes
  - SHA-256: `3d06c37bb5bdf8ed0835e746f4ef06caeb91514de4bccfa00a259d1a0dc82806`
- `EEPROM-Flasher-v2.2.3-release.aab` — 10631212 bytes
  - SHA-256: `e00d7a0d2ca5d3113dd536c9188eaafbad92092df0c25bf5b4b8810d5d49f9be`
- `versionCode 11`, `versionName 2.2.3`, `minSdk 24`, `targetSdk 37`.
