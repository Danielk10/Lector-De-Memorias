<div align="center">

# EEPROM Flasher

### Programador de Memorias EEPROM, Flash y Microcontroladores para Android

[![Android](https://img.shields.io/badge/Android-23%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Architecture](https://img.shields.io/badge/Arch-arm64--v8a-FF6F00?logo=arm&logoColor=white)](https://developer.android.com/ndk/guides/abis)
[![License](https://img.shields.io/badge/License-GPLv3-blue.svg?logo=gnu&logoColor=white)](LICENSE)
[![NDK](https://img.shields.io/badge/NDK-30-4285F4?logo=android&logoColor=white)](https://developer.android.com/ndk)
[![CMake](https://img.shields.io/badge/CMake-4.1.2-064F8C?logo=cmake&logoColor=white)](https://cmake.org)
[![USB](https://img.shields.io/badge/USB-Host%20API-FF4081?logo=usb&logoColor=white)](https://developer.android.com/guide/topics/connectivity/usb/host)
[![minipro](https://img.shields.io/badge/Motor-minipro-orange?logo=gnubash&logoColor=white)](https://gitlab.com/DavidGriffith/minipro)

---

*Herramienta Android nativa para leer, escribir, verificar, comparar y borrar memorias EEPROM y Flash*
*mediante programadores TL866II+, TL866A, TL866CS, T48, T56 y T76 por USB OTG sin root.*

</div>

---

## 📋 Descripción

**EEPROM Flasher** es una aplicación Android profesional de código abierto diseñada para la interacción directa con memorias EEPROM, Flash SPI, EPROM paralelas, microcontroladores y dispositivos lógicos desde teléfonos y tabletas Android mediante conexión USB OTG directa, sin requerir permisos de superusuario (root).

El proyecto integra el motor nativo [minipro](https://gitlab.com/DavidGriffith/minipro) compilado para `arm64-v8a` junto con un puente [libusb](https://github.com/libusb/libusb) personalizado que intercepta descriptores de archivo del USB Host API de Android.

---

## 📥 Descarga

- **Última versión:** [v1.0.0-pre](https://github.com/Danielk10/Lector-De-Memorias/releases/tag/v1.0.0-pre)
- **Compilaciones disponibles:** `app-release.apk` (Producción firmada) y `app-debug.apk` (Depuración)

---

## ✨ Características Principales

| Característica | Descripción |
|---|---|
| 🔍 **Auto-detectar Chip** | Identifica automáticamente el chip insertado en el programador |
| 📚 **Catálogo de Chips (CATALOGO)** | Selector rápido con búsqueda en tiempo real categorizado por familias de circuitos integrados |
| ➕ **Agregar Chips Propios** | Agrega y guarda modelos de chips personalizados en la lista persistente |
| 📖 **Lectura y Respaldo** | Lee el contenido íntegro de la memoria física y genera respaldos binarios |
| ✏️ **Escritura y Flasheo** | Graba archivos binarios `.bin` o Intel `.hex` directamente en la memoria |
| ✅ **Verificación** | Compara el contenido de la memoria física contra el archivo cargado |
| 🗑️ **Borrado Seguro** | Borra completamente la memoria del integrado con confirmación de seguridad |
| 🔢 **Visor Hexadecimal** | Inspección de volcados con visualización de direcciones, bytes hexadecimales y ASCII |
| ⚖️ **Comparador de Binarios** | Comparador Diff con resaltado de discrepancias, porcentaje de cambio y conteo de bytes |
| 🔌 **Diagramas de Pinouts** | Diagramas esquemáticos renderizados en Canvas para ZIF40, SOIC8, 24Cxx, 93Cxx, DIP32, ICSP, AVR ISP y PLCC32 |
| 💾 **Exportación Segura** | Exportación de volcados leídos del chip mediante el Storage Access Framework |
| 💻 **Terminal Integrada** | Consola en tiempo real con soporte de secuencias de escape, buffer optimizado y scroll fluido |
| 📋 **Portapapeles Rápido** | Copia la salida completa de la consola con pulsación prolongada |
| 🛡️ **Acerca de y Licencias** | Información técnica y reconocimiento de licencias de código abierto |

---

## 🎯 Programadores Compatibles y Base de Datos

| Programador | VID:PID | Dispositivos Soportados | Estado |
|---|---|---|---|
| 🟢 **TL866II+** | `04D8:00E0` / `A466:0A53` | 29,774 dispositivos (+47 custom) | ✅ Soportado |
| 🟢 **TL866A** | `04D8:00DE` / `04D8:E11C` | 14,162 dispositivos (+46 custom) | ✅ Soportado |
| 🟢 **TL866CS** | `04D8:00DF` / `04D8:E11C` | 14,162 dispositivos (+46 custom) | ✅ Soportado |
| 🟢 **XGecu T48** | `A466:0A53` / `2E8A:000A` | 29,739 dispositivos | ✅ Soportado |
| 🟢 **XGecu T56** | `A466:0A53` / `2E8A:0005` | 32,513 dispositivos | ✅ Soportado |
| 🟢 **XGecu T76** | `A466:1A86` | 34,607 dispositivos | ✅ Soportado |
| 🟢 **Logic IC Tester** | Modo de prueba lógica | 283 circuitos integrados (+6 custom) | ✅ Soportado |
| 🔴 **Serie CH341A / CH347** | `1A86:5512` / `1A86:5523` / `1A86:55DB` | — | ❌ No soportado por minipro |

> [!NOTE]
> El motor **minipro** está diseñado exclusivamente para la familia de programadores Autoelectric / XGecu (TL866II+, TL866A, TL866CS, T48, T56, T76) y el probador de circuitos lógicos. **No cuenta con soporte** para hardware de la serie CH341A ni CH347.

---

## 📐 Diagramas de Conexión y Pinouts Incluidos

1. **Socket ZIF 40 Pines**: Regla de alineación universal para programadores TL866 y T48.
2. **SPI Flash SOIC8 / SOP8 / DIP8 serie 25xxx**: Pinout estándar, CS#, DO, WP#, GND, DI, CLK, HOLD#, VCC.
3. **I2C EEPROM SOIC8 / DIP8 serie 24Cxx**: A0, A1, A2, GND, SDA, SCL, WP, VCC.
4. **Microwire EEPROM serie 93Cxx**: CS, SK, DI, DO, GND, ORG en 8 y 16 bits, NC, VCC.
5. **Memorias Paralelas Flash y EPROM DIP28 / DIP32**: Buses de direcciones A0 a A18, datos D0 a D7, control /CE, /OE, /WE, VPP.
6. **Puerto ICSP 6 Pines**: VPP/MCLR, VCC, GND, PGD/MOSI, PGC/SCK, AUX/MISO para programación en circuito.
7. **Conectores AVR ISP de 6 y 10 Pines**: MISO, SCK, RESET, VCC, MOSI, GND.
8. **Adaptador PLCC32 a DIP32**: Mapeo, orientación y marcas de alineación para chips BIOS y Flash.

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

## 🔧 Compilación y Construcción

### Requisitos

- Android SDK 37 (Build Tools 37.0.0)
- NDK 30.0.14904198 rc1
- CMake 4.1.2
- JDK 17 o superior

### Compilación desde terminal

```bash
# Compilar APK Release
./gradlew assembleRelease
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
