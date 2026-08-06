<div align="center">

# 🔌 EepromFlasher

### Programador de Memorias EEPROM/Flash para Android

[![Android](https://img.shields.io/badge/Android-23%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Architecture](https://img.shields.io/badge/Arch-arm64--v8a-FF6F00?logo=arm&logoColor=white)](https://developer.android.com/ndk/guides/abis)
[![License](https://img.shields.io/badge/License-GPLv3-blue.svg?logo=gnu&logoColor=white)](LICENSE)
[![NDK](https://img.shields.io/badge/NDK-30-4285F4?logo=android&logoColor=white)](https://developer.android.com/ndk)
[![CMake](https://img.shields.io/badge/CMake-4.1.2-064F8C?logo=cmake&logoColor=white)](https://cmake.org)
[![USB](https://img.shields.io/badge/USB-Host%20API-FF4081?logo=usb&logoColor=white)](https://developer.android.com/guide/topics/connectivity/usb/host)
[![minipro](https://img.shields.io/badge/Basado%20en-minipro-orange?logo=gnubash&logoColor=white)](https://gitlab.com/DavidGriffith/minipro)

---

*Herramienta Android nativa para leer, escribir, verificar y borrar memorias EEPROM/Flash*
*usando programadores TL866II+, TL866A/CS, T48, T56 y compatibles.*

</div>

---

## 📋 Descripción

**EepromFlasher** es una aplicación Android que permite interactuar con programadores de memorias universales directamente desde tu dispositivo móvil, sin necesidad de PC. Utiliza el binario [minipro](https://gitlab.com/DavidGriffith/minipro) compilado nativamente para `arm64-v8a` con una versión parcheada de [libusb](https://github.com/libusb/libusb) que intercepta los file descriptors del USB Host API de Android.

## ✨ Características

| Característica | Descripción |
|---|---|
| 🔍 **Identificar Chip** | Detecta automáticamente el chip conectado al programador |
| 📖 **Leer Chip** | Lee el contenido completo de la memoria a un archivo `.bin` |
| ✏️ **Escribir Chip** | Flashea un archivo `.bin` o `.hex` al chip |
| ✅ **Verificar** | Compara el contenido del chip con un archivo local |
| 🗑️ **Borrar Chip** | Borra completamente el contenido del chip |
| 📂 **Importar/Exportar** | Carga archivos `.bin`/`.hex` y exporta dumps |
| 🔢 **Visor Hexadecimal** | Visualiza archivos binarios e Intel HEX con dirección, hex y ASCII |
| 💻 **Terminal** | Consola integrada para comandos minipro personalizados |
| 📋 **Copiar Log** | Copia la salida de la terminal al portapapeles (long-press) |
| 🔌 **Auto-detección USB** | Reconoce automáticamente programadores compatibles |

## 🎯 Dispositivos Compatibles

| Programador | VID:PID | Estado |
|---|---|---|
| 🟢 TL866II+ | `04D8:00E0` | ✅ Soportado |
| 🟢 TL866A | `04D8:00DE` | ✅ Soportado |
| 🟢 TL866CS | `04D8:00DF` | ✅ Soportado |
| 🟡 T48 | `2E8A:000A` | ⚠️ Experimental |
| 🟡 T56 | `2E8A:0005` | ⚠️ Experimental |
| 🟡 T76 | — | ⚠️ Experimental |
| 🔵 CH341A | `1A86:5523` | ✅ Soportado |

> **Nota:** La base de datos de chips soportados proviene del proyecto minipro e incluye más de **15,000 dispositivos** (EEPROM, Flash, MCU, PLD, etc.)

## 🏗️ Arquitectura

```
┌──────────────────────────────────────────┐
│              Android App (Java)          │
│  ┌──────────┐ ┌────────────┐ ┌────────┐ │
│  │ MainActivity │ HexViewer │ │ Policy │ │
│  └─────┬────┘ └────────────┘ └────────┘ │
│        │                                 │
│  ┌─────▼────┐    ┌──────────────────┐    │
│  │UsbControl│    │ MiniproExecutor  │    │
│  │  ler     │───▶│  (ProcessBuilder)│    │
│  └─────┬────┘    └────────┬─────────┘    │
│        │ FD               │              │
│  ┌─────▼────┐    ┌────────▼─────────┐    │
│  │ JNI/NDK  │    │ libminipro_bin.so│    │
│  │setUsbFd()│    │   (ELF arm64)    │    │
│  └─────┬────┘    └────────┬─────────┘    │
│        │                  │              │
│  ┌─────▼──────────────────▼─────────┐    │
│  │    libusb_1_0.so (parcheada)     │    │
│  │    ANDROID_USB_FD interception   │    │
│  └──────────────────────────────────┘    │
└──────────────────────────────────────────┘
```

## 🔧 Compilación

### Requisitos

- Android SDK 37 / Build Tools 37.0.0
- NDK 30.0.14904198
- CMake 4.1.2
- JDK 11+

### Setup rápido

```bash
# 1. Clonar el repositorio
git clone https://github.com/Danielk10/Lector-De-Memorias.git
cd Lector-De-Memorias

# 2. Configurar SDK (opcional, si no tienes Android Studio)
chmod +x setup-sdk.sh
./setup-sdk.sh

# 3. Compilar la app
./gradlew assembleDebug
```

### Compilar binarios nativos (desde Termux)

```bash
# Compilar libusb parcheada para Android
chmod +x build_libusb_custom_mini.sh
./build_libusb_custom_mini.sh

# Compilar minipro
chmod +x build_minipro_custom.sh
./build_minipro_custom.sh
```

## 📁 Estructura del Proyecto

```
Lector-De-Memorias/
├── app/src/main/
│   ├── java/com/diamon/mini/
│   │   ├── MainActivity.java          # Activity principal
│   │   ├── HexViewerActivity.java     # Visor hexadecimal
│   │   ├── PolicyActivity.java        # Políticas de privacidad
│   │   ├── core/
│   │   │   ├── UsbController.java     # Control USB + auto-detección
│   │   │   └── MiniproExecutor.java   # Ejecutor de comandos minipro
│   │   └── utils/
│   │       └── AssetHelper.java       # Extracción de assets + symlinks
│   ├── cpp/
│   │   ├── native-lib.cpp             # JNI bridge (setUsbFd/clearUsbFd)
│   │   ├── CMakeLists.txt             # Config CMake
│   │   └── include/libusb-1.0/        # Headers libusb
│   ├── jniLibs/arm64-v8a/
│   │   ├── libminipro_bin.so          # Binario minipro (PIE arm64)
│   │   └── libusb_1_0.so             # libusb parcheada
│   ├── assets/.../usr/share/minipro/
│   │   ├── infoic.xml                 # Base de datos de chips (19 MB)
│   │   └── logicic.xml               # Definiciones de lógica IC
│   └── res/
│       ├── layout/                    # Layouts XML
│       ├── menu/                      # Menú de opciones
│       ├── values/                    # Strings, colors, themes
│       └── xml/                       # device_filter, backup rules
├── fake_root/                         # Staging de binarios compilados
├── build_libusb_custom_mini.sh        # Script: compilar libusb parcheada
├── build_minipro_custom.sh            # Script: compilar minipro
├── setup-sdk.sh                       # Script: configurar Android SDK
└── LICENSE                            # GNU GPLv3
```

## 🔐 Mecanismo USB (sin root)

La app funciona **sin root** gracias a un parche en libusb que intercepta las funciones clave:

1. `libusb_init()` → detecta `ANDROID_USB_FD` y omite escaneo de `/dev/bus/usb/`
2. `libusb_get_device_list()` → crea dispositivo virtual desde el FD
3. `libusb_open()` → llama `libusb_wrap_sys_device()` con el FD de Android
4. `libusb_get_device_descriptor()` → lee descriptor directo del FD con `pread()`

El FD se obtiene vía Android USB Host API (`UsbDeviceConnection.getFileDescriptor()`) y se pasa al entorno nativo mediante JNI.

## 📄 Licencias

| Componente | Licencia |
|---|---|
| **EepromFlasher** | [GNU GPLv3](LICENSE) |
| **minipro** | [GNU GPLv3](https://gitlab.com/DavidGriffith/minipro/-/blob/master/COPYING) |
| **libusb** | [GNU LGPLv2.1](https://github.com/libusb/libusb/blob/master/COPYING) |
| **usb-serial-for-android** | [MIT](https://github.com/mik3y/usb-serial-for-android/blob/master/LICENSE) |
| **AndroidX / Material** | [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) |

## 🤝 Créditos

- [minipro](https://gitlab.com/DavidGriffith/minipro) — David Griffith y colaboradores
- [libusb](https://github.com/libusb/libusb) — libusb contributors
- [usb-serial-for-android](https://github.com/mik3y/usb-serial-for-android) — mik3y

---

<div align="center">

**Desarrollado por Daniel Diamon** ([@Danielk10](https://github.com/Danielk10))
*Tinaquillo, Cojedes, Venezuela*

*Si este proyecto te resulta útil, ¡dale una ⭐!*

</div>
