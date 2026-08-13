# 🔌 Emulador Local de USB FD (TL866II+) para Minipro (Android / Local)

Este documento explica cómo utilizar el entorno de emulación local y dinámica de USB para **minipro** (`EepromFlasher`). Este entorno emula el mecanismo de inyección de File Descriptor (`ANDROID_USB_FD`) usado en la aplicación Android.

---

## 🛠️ Estructura del Entorno

El emulador está organizado en los siguientes directorios para mayor comodidad:
1. **En el repositorio**: [`Lector-De-Memorias/emulador_minipro/`](file:///home/danielpdiamon/Lector-De-Memorias/emulador_minipro/)
2. **En el home del usuario**: `/home/danielpdiamon/emulador_minipro/`

### Archivos clave:
* **[`emulador_minipro.cpp`](file:///home/danielpdiamon/Lector-De-Memorias/emulador_minipro/emulador_minipro.cpp)**: Código fuente en C++ del emulador de hardware del programador TL866II+ con una memoria virtual de 1MB.
* **[`patch_libusb_local.py`](file:///home/danielpdiamon/Lector-De-Memorias/emulador_minipro/patch_libusb_local.py)**: Script Python que realiza el parchado de red/sockets en el código de `libusb`.
* **[`Makefile`](file:///home/danielpdiamon/Lector-De-Memorias/emulador_minipro/Makefile)**: Archivo para compilar el emulador con un simple `make`.
* **[`emulador_minipro.sh`](file:///home/danielpdiamon/Lector-De-Memorias/emulador_minipro/emulador_minipro.sh)**: Script automatizado "todo en uno" que compila y corre una simulación completa de lectura.

---

## 💡 Mecanismo de Emulación y Simulación de Memorias (SPI & EEPROM)

El emulador implementa una simulación dinámica a nivel de socket UNIX/socketpair, permitiendo transferencias asíncronas bidireccionales transparentes.

### 1. Simulación de Memorias:
- Posee una **memoria virtual de 1MB** en RAM.
- **Lectura/Escritura Entrelazada**: Las transferencias de datos en los endpoints `0x02`/`0x82` (EP2) y `0x03`/`0x83` (EP3) son multiplexadas y mapeadas al búfer físico de forma entrelazada (bloques de 64 bytes pares e impares), tal como ocurre en el programador real TL866II+.
- **Decodificación de Direcciones**: El emulador intercepta los comandos de control enviados a la interfaz de control (`EP 0x01`). Si detecta un comando de lectura/escritura (`TL866IIPLUS_READ_CODE`, `TL866IIPLUS_READ_DATA`, etc.), decodifica la dirección física en memoria (bytes 4..7 de la trama) y reposiciona dinámicamente los índices de lectura/escritura. Esto permite volcados y escrituras en cualquier dirección sin perder la sincronía.

### 2. Flujo del Protocolo de Red:
* **Cliente -> Servidor**: Envía `[4 bytes signed size] [1 byte endpoint] [payload]`.
  - Si `size > 0`: Representa una transferencia `OUT` (escritura).
  - Si `size < 0`: Representa una solicitud de transferencia `IN` (lectura) por `abs(size)` bytes.
* **Servidor -> Cliente**: Responde con la trama del descriptor (18 bytes al inicio) o el payload solicitado en transferencias `IN`.

---

## 🚀 Cómo Ejecutar la Simulación

Puedes correr todo el entorno ejecutando el script integrado:

```bash
cd ~/emulador_minipro
chmod +x emulador_minipro.sh
./emulador_minipro.sh
```

### Comandos manuales:

Puedes ejecutar el emulador de forma aislada para cargar y salvar memorias simuladas en archivos `.bin`:

```bash
# Rellenar memoria simulada, cargar un mock, correr minipro y guardar el estado final
./emulador_minipro --fill pattern --flash mock_eeprom.bin minipro -p "AT24C02C" -r read_eeprom.bin
```

Opciones admitidas:
* `--flash ARCHIVO.bin`: Inicializa la memoria del chip simulado desde un archivo.
* `--save ARCHIVO.bin`: Guarda los datos finales del chip simulado tras correr las operaciones.
* `--fill empty|count|random`: Rellena la memoria inicial con `0xFF`, contadores secuenciales o valores aleatorios.
