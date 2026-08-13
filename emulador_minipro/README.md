# 🔌 Emulador Local de TL866II+ para Minipro (Simulación de Chips)

Este subdirectorio contiene el entorno para simular de forma local y dinámica un programador de memorias **TL866II+** conectado a través de la interfaz USB. 

A diferencia de la simulación básica previa que utilizaba descriptores de archivos estáticos y causaba errores `LIBUSB_ERROR_IO` al leer la memoria, esta versión implementa una **emulación de hardware completa por Socket** en tiempo real.

---

## 🛠️ ¿Cómo funciona?

El sistema se compone de dos partes críticas inspiradas en la arquitectura de pruebas de `flashrom`:

1. **Parcheo dinámico de `libusb`** (`patch_libusb_local.py`):
   - Modifica las funciones críticas de envío de tramas (`libusb_submit_transfer` en `io.c`) de modo que cuando detecta la variable `ANDROID_USB_FD`, redirige las escrituras (`OUT`) y lecturas (`IN`) de tramas USB directamente sobre un descriptor de socket UNIX o socketpair.
   - Envía tramas estructuradas de forma bidireccional: `[4 bytes signed size] [1 byte endpoint] [payload]`.
   
2. **Emulador de Hardware en C++** (`emulador_minipro.cpp`):
   - Levanta el otro extremo del socket y simula los descriptores de dispositivo del programador TL866II+ (`VID=a466 PID=0a53`).
   - Mantiene una **memoria virtual de 1MB** en RAM para actuar como chip SPI Flash o EEPROM física.
   - Traduce y procesa las lecturas/escrituras en los endpoints de datos `EP 0x82` / `EP 0x83` y `EP 0x02` / `EP 0x03` de manera entrelazada respetando el protocolo interno de multiplexado de `minipro`.
   - Soporta cargar la memoria inicial desde un archivo (`--flash`) y guardarla al salir (`--save`).

---

## 🚀 Uso Rápido

Para compilar las librerías locales, compilar el emulador y ejecutar una prueba de lectura de un chip EEPROM `AT24C02C`:

```bash
chmod +x emulador_minipro.sh
./emulador_minipro.sh
```

El script creará automáticamente un archivo de memoria de prueba `mock_eeprom.bin` relleno con un patrón secuencial invertido, lanzará la simulación, y luego comparará el volcado (`read_eeprom.bin`) obtenido por `minipro` con el original.

---

## 💻 Comandos del Emulador

Puedes ejecutar el emulador de forma manual con las siguientes opciones:

* `--flash ARCHIVO`: Carga el contenido inicial del chip simulado desde un binario de hasta 1MB.
* `--save ARCHIVO`: Guarda el estado final de la memoria del chip en un archivo al finalizar el proceso de `minipro`.
* `--fill PATRON`: Inicializa la memoria con un patrón específico: `empty` (todo en `0xFF`), `count` (contador secuencial `0..255`), o `random` (valores aleatorios).
* `minipro [ARGS]`: Ejecuta el subproceso de `minipro` en el mismo `socketpair` de forma auto-contenida.

### Ejemplo de Lectura y Escritura:

```bash
# 1. Ejecutar lectura cargando un mock
./emulador_minipro --flash mock_eeprom.bin minipro -p "AT24C02C" -r output.bin

# 2. Ejecutar escritura de un archivo y guardar el estado de la memoria
./emulador_minipro --save memory_final.bin minipro -p "AT24C02C" -w new_data.bin
```
