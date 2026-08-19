# Ficha Técnica para Google Play Store - EEPROM Flasher

## Información General

* **Nombre de la aplicación:** EEPROM Flasher
* **Versión:** 1.0.0
* **Categoría:** Herramientas / Productividad
* **Clasificación de contenido:** Para todos (PEGI 3 / Everyone)
* **Idiomas:** Español, Inglés (Soporte multilingüe general)

---

## Descripción Corta

Programador USB OTG para memorias EEPROM, Flash SPI y microcontroladores.

---

## Descripción Larga

EEPROM Flasher es una herramienta de código abierto (open source) diseñada para la lectura, escritura, verificación y borrado de memorias EEPROM, Flash SPI y microcontroladores directamente desde dispositivos Android compatibles mediante conexión USB OTG.

Esta aplicación permite a profesionales y aficionados de la electrónica interactuar con integrados de memoria sin necesidad de permisos de superusuario (root) en el dispositivo, operando de manera completamente portátil.

### Funciones Principales

* **Detección Automática:** Identificación del chip conectado, en aquellos programadores compatibles que lo soportan.
* **Lectura y Escritura:** Extracción de datos en formato binario (.bin) y grabación a partir de archivos .bin o .hex.
* **Verificación:** Comprobación de que los datos de la memoria coinciden exactamente con el archivo.
* **Comparador de Binarios:** Herramienta de análisis visual que resalta discrepancias y porcentaje de variación entre dos volcados.
* **Visor Hexadecimal:** Inspección detallada de archivos con visualización de direcciones, bytes hexadecimales y caracteres ASCII.
* **Consulta de Dispositivos:** Base de datos de componentes soportados accesible rápidamente a través del botón CATALOGO.
* **Consola de Operaciones:** Registro de la comunicación en tiempo real para supervisar el progreso de cada transferencia.

### Familias de Integrados Compatibles (Resumen)

* Memorias Flash SPI (Serie 25)
* Memorias EEPROM I2C (Serie 24)
* Memorias EEPROM Microwire (Serie 93)
* Memorias EEPROM SPI (Serie 95)
* Memorias Paralelas (Series 27, 28, 29, 39)
* Selecciones de Microchip PIC, Atmel AVR y 8051

*(Consulte la lista completa dentro de la aplicación en la sección CATALOGO para confirmar el soporte exacto de su integrado).*

### Hardware Programador Compatible

La aplicación requiere la conexión a través de USB OTG de uno de los siguientes programadores de hardware soportados (basado en la compatibilidad de minipro):

* TL866II+
* TL866A / TL866CS
* XGecu T48
* XGecu T56
* XGecu T76

*Nota: Los programadores genéricos basados en chips CH341A o CH347 no son compatibles actualmente.*

### Requisitos del Sistema

* Dispositivo Android versión 6.0 (Marshmallow) o superior.
* Soporte nativo para USB Host (OTG) en el teléfono o tableta.
* Adaptador o cable USB OTG para conectar el programador.
* Hardware programador compatible de la lista anterior.

### Privacidad y Seguridad

EEPROM Flasher procesa los archivos de volcado y los datos de memoria de manera estrictamente local en su dispositivo. Al ser un proyecto de código abierto, la aplicación no contiene anuncios, no recolecta información personal ni transmite datos de telemetría a servidores de terceros. Únicamente requiere los permisos técnicos necesarios para la gestión local de archivos y la comunicación directa por puerto USB con su programador de hardware.
