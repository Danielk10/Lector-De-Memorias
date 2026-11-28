# Notas de versión — v2.3.6 (versionCode 20)

## Español (es-419 / es-ES)

- Corregida la detección automática de los programadores XGecu T48 y T56, que comparten el mismo USB que el TL866II+.
- La app ahora identifica el modelo real del programador conectado y actualiza el selector automáticamente.
- Mejorada la autodetección de chips SPI en T48/T56.
- Correcciones menores de estabilidad.

## English (en-US)

- Fixed automatic detection of XGecu T48 and T56 programmers, which share the same USB ID as the TL866II+.
- The app now identifies the actual connected programmer model and updates the selector automatically.
- Improved SPI chip autodetection on T48/T56.
- Minor stability fixes.

## Detalle técnico

- `UsbController`: `a466:0a53` ya no se etiqueta siempre como TL866II+; se trata como familia XGecu compartida (TL866II+/T48/T56).
- `MainActivity`: tras conceder el permiso USB se ejecuta `minipro -k` para leer el byte de tipo de dispositivo del firmware y seleccionar TL866II+, T48, T56 o T76.
- `ProgrammerModel`: nuevo parser del resultado de `minipro -k`, con pruebas unitarias.
- Emulador local: añadido `--programmer t48|t56|t76|tl866ii`, respuesta de autodetección SPI y lectura/escritura T48 por EP2 contiguo.
