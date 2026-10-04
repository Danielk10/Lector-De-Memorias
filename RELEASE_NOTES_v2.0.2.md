# Notas de Lanzamiento - Lector-De-Memorias v2.0.2 (versionCode 5)

## 🌐 Notas para Google Play Console (Bilingüe)

### Español (`es-419` / `es-ES`)
- Visor Hexadecimal con mapeo de memoria virtual mmap para explorar hasta 32MB sin carga en heap.
- Exportación zero-copy ultrarrápida con FileChannel.transferTo a nivel de kernel.
- WakeLock continuo durante operaciones USB evitando suspensión de CPU y caídas de energía.
- Blindaje en manifiesto contra cierre inesperado (NPE) en segundo plano.
- Compatibilidad completa con Android API 37.

### English (`en-US`)
- Hex Viewer with virtual memory mapping (mmap) for smooth navigation of large dumps up to 32MB.
- Kernel-level high-speed zero-copy export via FileChannel.transferTo.
- Continuous WakeLock protection preventing CPU sleep and power cuts during USB flashing.
- Manifest crash protection against background NPEs.
- Full Android API 37 support and compatibility.

---

## 🛠️ Detalle Técnico de Cambios

1. **Visor Hexadecimal Zero-Copy (`HexViewerActivity.java`)**:
   - `MappedFileSource` mapea archivos con `FileChannel.map()` paginando en memoria virtual a demanda (0 MB de impacto en el heap).
2. **Exportación Zero-Copy (`MainActivity.java`)**:
   - Uso de `FileChannel.transferTo()` para volcar lecturas directamente al almacenamiento.
3. **Protección WakeLock (`MiniproExecutor.java`)**:
   - Mantiene despierta la CPU durante toda la ejecución de operaciones de lectura/escritura nativas con `minipro`.
4. **Blindaje de Manifiesto (`AndroidManifest.xml`)**:
   - Exclusión de `AlarmManagerSchedulerBroadcastReceiver` con `tools:node="remove"`.
5. **Configuración de SDK (`app/build.gradle`)**:
   - `compileSdk release(37)`, `targetSdk 37`, `versionCode 5`, `versionName "2.0.2"`.
