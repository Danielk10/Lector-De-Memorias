<div align="center">

# EEPROM Flasher

### Programador de Memorias EEPROM / Flash / MCU para Android

[![Android](https://img.shields.io/badge/Android-23%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Architecture](https://img.shields.io/badge/Arch-arm64--v8a-FF6F00?logo=arm&logoColor=white)](https://developer.android.com/ndk/guides/abis)
[![License](https://img.shields.io/badge/License-GPLv3-blue.svg?logo=gnu&logoColor=white)](LICENSE)
[![NDK](https://img.shields.io/badge/NDK-30-4285F4?logo=android&logoColor=white)](https://developer.android.com/ndk)
[![CMake](https://img.shields.io/badge/CMake-4.1.2-064F8C?logo=cmake&logoColor=white)](https://cmake.org)
[![USB](https://img.shields.io/badge/USB-Host%20API-FF4081?logo=usb&logoColor=white)](https://developer.android.com/guide/topics/connectivity/usb/host)
[![minipro](https://img.shields.io/badge/Basado%20en-minipro-orange?logo=gnubash&logoColor=white)](https://gitlab.com/DavidGriffith/minipro)

---

*Herramienta Android nativa para leer, escribir, verificar, comparar y borrar memorias EEPROM/Flash*
*usando programadores TL866II+, TL866A/CS, T48, T56, CH341A y compatibles.*

</div>

---

## 📋 Descripción

**EEPROM Flasher** es una aplicación Android profesional que permite programar e interactuar con memorias EEPROM, Flash SPI, EPROM paralelas, microcontroladores PIC/AVR y dispositivos lógicos directamente desde tu móvil o tablet Android mediante conexión USB OTG directa (sin root).

Utiliza el motor [minipro](https://gitlab.com/DavidGriffith/minipro) compilado para `arm64-v8a` junto con un puente [libusb](https://github.com/libusb/libusb) que intercepta los descriptores de archivo del USB Host API de Android.

---

## ✨ Características Principales

| Característica | Descripción |
|---|---|
| 🔍 **Auto-detectar Chip** | Identifica automáticamente el chip insertado en el programador (`-a 8`, `-a 16`, `-d`) |
| 📚 **Catálogo de Chips** | Selector con búsqueda en tiempo real categorizado por familias (SPI 25xxx, I2C 24xxx, Microwire 93xxx, Paralelas 27/28/29/39/49, PIC, AVR, GAL/PLD) |
| ➕ **Agregar Chips Propios** | Agrega y guarda modelos de chips personalizados en una lista rápida persistente |
| 📖 **Lectura / Backup** | Lee el contenido íntegro de la memoria a un archivo `.bin` |
| ✏️ **Escritura y Flasheo** | Graba archivos `.bin` o Intel `.hex` directamente en la memoria |
| ✅ **Verificación** | Compara el contenido de la memoria física contra el archivo cargado |
| 🗑️ **Borrado Seguro** | Borra completamente el contenido del chip con diálogo de confirmación |
| 🔢 **Visor Hexadecimal** | Visualizador de archivos `.bin` e Intel `.hex` con direcciones, volcado hexadecimal y caracteres ASCII |
| ⚖️ **Comparador de Binarios** | Herramienta de Diff side-by-side que resalta discrepancias (`02X→02X`), porcentaje y conteo de bytes distintos |
| 🔌 **Diagramas de Pinouts** | Diagramas esquemáticos renderizados en Canvas para ZIF 40 pines, SOIC8 SPI, 24Cxx, 93Cxx, DIP32, ICSP 6 pines, AVR ISP y adaptadores PLCC32 |
| 💾 **Exportar a Descargas / SAF** | Exporta volcados a la carpeta `Descargas/EEPROM Flasher` o a cualquier almacenamiento con Storage Access Framework |
| 💻 **Terminal Integrada** | Consola con soporte para retornos de carro (`\r`), buffer optimizado y scroll estable (`LogScrollView`) |
| 📋 **Portapapeles Rápido** | Copia la salida completa de la consola manteniendo presionado el log |
| 🛡️ **Acerca de y Licencias** | Información detallada del software y licencias de código abierto con enlaces interactivos |

---

## 🎯 Programadores Compatibles

| Programador | VID:PID | Estado |
|---|---|---|
| 🟢 **TL866II+** | `04D8:00E0` / `A466:0A53` | ✅ Soportado |
| 🟢 **TL866A** | `04D8:00DE` | ✅ Soportado |
| 🟢 **TL866CS** | `04D8:00DF` | ✅ Soportado |
| 🟡 **Xgecu T48** | `2E8A:000A` | ⚠️ Experimental |
| 🟡 **Xgecu T56** | `2E8A:0005` | ⚠️ Experimental |
| 🟡 **Xgecu T76** | — | ⚠️ Experimental |
| 🔵 **CH341A / CH347** | `1A86:5512` / `1A86:5523` / `1A86:55DB` | ✅ Soportado |

---

## 📐 Diagramas de Conexión y Pinouts Incluidos

1. **Socket ZIF 40 Pines**: Regla de alineación universal para TL866/T48 (alineación inferior, muesca arriba).
2. **SPI Flash SOIC8 / SOP8 / DIP8 (25xxx)**: Pinout estándar, CS#, DO, WP#, GND, DI, CLK, HOLD#, VCC.
3. **I2C EEPROM SOIC8 / DIP8 (24Cxx)**: A0, A1, A2, GND, SDA, SCL, WP, VCC.
4. **Microwire EEPROM (93Cxx)**: CS, SK, DI, DO, GND, ORG (8/16 bits), NC, VCC.
5. **Memorias Paralelas Flash / EPROM (DIP28 / DIP32)**: Buses de direcciones A0-A18, datos D0-D7, control /CE, /OE, /WE, VPP.
6. **Puerto ICSP 6 Pines (TL866 / T48)**: VPP/MCLR, VCC, GND, PGD/MOSI, PGC/SCK, AUX/MISO para PIC/AVR in-circuit.
7. **Conectores AVR ISP (6 y 10 Pines)**: MISO, SCK, RESET, VCC, MOSI, GND.
8. **Adaptador PLCC32 a DIP32**: Mapeo y orientación para chips BIOS.

---

## 🏗️ Arquitectura del Sistema

```
┌─────────────────────────────────────────────────────────────┐
│                    Android App (Java)                       │
│  ┌──────────────┐ ┌─────────────┐ ┌─────────────┐ ┌──────┐  │
│  │ MainActivity │ │  HexViewer  │ │   HexDiff   │ │Policy│  │
│  └──────┬───────┘ └─────────────┘ └─────────────┘ └──────┘  │
│         │                                                   │
│  ┌──────▼───────┐    ┌─────────────────┐ ┌───────────────┐  │
│  │ UsbController│───▶│ MiniproExecutor │ │  PinoutView   │  │
│  └──────┬───────┘    └────────┬────────┘ └───────────────┘  │
│         │ FD                  │                             │
│  ┌──────▼───────┐    ┌────────▼────────┐ ┌───────────────┐  │
│  │   JNI Bridge │    │libminipro_bin.so│ │  FileManager  │  │
│  │ (native-lib) │    │  (ELF arm64)    │ │ (MediaStore)  │  │
│  └──────┬───────┘    └────────┬────────┘ └───────────────┘  │
│         │                     │                             │
│  ┌──────▼─────────────────────▼────────┐                    │
│  │      libusb_1_0.so (parcheada)      │                    │
│  │      ANDROID_USB_FD Interception    │                    │
│  └─────────────────────────────────────┘                    │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔧 Compilación y Ejecución

### Requisitos

- Android SDK 37 (Build Tools 37.0.0)
- NDK 30.0.14904198 rc1
- CMake 4.1.2
- JDK 11 o superior

### Compilación desde terminal

```bash
# Configurar SDK (si no está disponible)
chmod +x setup-sdk.sh
./setup-sdk.sh

# Compilar APK Debug
./gradlew assembleDebug
```

---

## 📄 Licencias y Atribuciones

- **EEPROM Flasher**: [GNU GPLv3](LICENSE)
- **minipro**: [GNU GPLv3](https://gitlab.com/DavidGriffith/minipro) — David Griffith y colaboradores
- **libusb**: [GNU LGPLv2.1](https://github.com/libusb/libusb) — libusb contributors
- **usb-serial-for-android**: [MIT](https://github.com/mik3y/usb-serial-for-android) — mik3y
- **AndroidX & Material Design**: [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0)

---

<div align="center">

**Desarrollado por Daniel Diamon** ([@Danielk10](https://github.com/Danielk10))  
*Tinaquillo, Cojedes, Venezuela*

</div>
