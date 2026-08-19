# Ficha Técnica para Google Play Store - EEPROM Flasher

## Descripción Corta (Límite: 80 caracteres)
Programador de memorias EEPROM y Flash por USB OTG. Lectura y escritura rápida.

---

## Descripción Larga (Límite: 4000 caracteres)

EEPROM Flasher es una herramienta técnica profesional diseñada para la lectura, escritura, verificación y borrado de memorias EEPROM, Flash SPI, memorias paralelas y microcontroladores directamente desde dispositivos Android compatibles mediante conexión USB OTG.

La aplicación permite a técnicos de reparación, ingenieros y desarrolladores electrónicos gestionar volcados de firmware y configuraciones de circuitos integrados con total portabilidad, sin necesidad de permisos de superusuario (root) en el teléfono o tableta.

### Características Principales

• Auto-detección de Chip: Identifica automáticamente el modelo de chip conectado en los programadores compatibles.
• Lectura Completa: Extrae y almacena respaldos íntegros de la memoria en formato binario estándar (.bin).
• Grabación de Firmware: Escribe archivos binarios (.bin) e Intel HEX (.hex) directamente en el integrado.
• Verificación de Datos: Compara la memoria física contra el archivo cargado para garantizar integridad total.
• Borrado de Memoria: Limpieza completa del circuito integrado con verificación de seguridad.
• Visor Hexadecimal Integrado: Inspección detallada de archivos con visualización de direcciones, bytes hexadecimales y caracteres ASCII.
• Comparador de Archivos (Diff): Análisis visual side-by-side que resalta discrepancias exactas y porcentaje de variación entre dos volcados.
• Diagramas de Pinout: Guías gráficas esquemáticas de conexiones y zócalos para ZIF40, SOIC8, DIP32, conectores ICSP y adaptadores PLCC32.
• Consola de Diagnóstico: Terminal en tiempo real para supervisar el estado de cada comando y transferencia.

### Familias de Circuitos Integrados Compatibles

• Memorias Flash SPI serie 25xxx (25Q, 25L, 25VF, etc.)
• Memorias EEPROM I2C serie 24Cxx (24C01 hasta 24C1024)
• Memorias EEPROM Microwire serie 93Cxx (93C46 hasta 93C86 en 8 y 16 bits)
• Memorias EEPROM SPI serie 25Cxx y 95xxx
• Memorias Flash y EPROM Paralelas serie 27C, 28C, 29F y 39SF
• Microcontroladores Microchip PIC seleccionados
• Microcontroladores Atmel AVR y 8051 seleccionados

### Programadores de Hardware Compatibles

La aplicación interactúa mediante USB Host con los siguientes programadores:
• TL866II+
• TL866A
• TL866CS
• T48
• T56
• T76
• Programadores basados en serie CH341A y CH347

### Requisitos del Sistema

• Dispositivo Android con versión 6.0 (Marshmallow) o superior.
• Compatibilidad con USB Host / OTG en el dispositivo Android.
• Cable o adaptador USB OTG de buena calidad para conectar el programador.
• Programador de hardware compatible y chip soportado.

### Privacidad y Seguridad

EEPROM Flasher procesa los archivos de volcado y datos de memoria de manera estrictamente local en su dispositivo. La aplicación integra anuncios mediante Google AdMob para sostener su desarrollo continuo y requiere únicamente los permisos necesarios para la comunicación USB, gestión de almacenamiento y conectividad de red.
