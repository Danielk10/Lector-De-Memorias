# Ficha Técnica para Google Play Store - EEPROM Flasher

## Descripción Corta (Límite: 80 caracteres)
Programador profesional de memorias EEPROM y Flash mediante USB OTG (minipro).

---

## Descripción Larga (Límite: 4000 caracteres)

EEPROM Flasher es una herramienta técnica profesional diseñada para la interacción directa con memorias EEPROM, Flash SPI, EPROM paralelas y microcontroladores a través de dispositivos Android. Mediante una conexión USB OTG, la aplicación permite realizar operaciones de lectura, escritura, verificación y borrado sin necesidad de acceso root.

Esta solución está orientada a técnicos, ingenieros y entusiastas de la electrónica que requieren una interfaz móvil para programar dispositivos utilizando hardware compatible. La aplicación integra el motor nativo minipro compilado para arquitectura arm64-v8a, garantizando un rendimiento óptimo en dispositivos móviles.

### Funcionalidades Principales

* Autodetección de Chips: Identificación automática de una amplia gama de circuitos integrados.
* Gestión de Datos: Lectura y respaldo íntegro de memorias en archivos .bin e Intel .hex.
* Procesos de Grabación: Escritura y flasheo de firmware directamente desde el almacenamiento local.
* Herramientas de Análisis: Visor hexadecimal integrado y comparador de binarios (Diff) con resaltado de discrepancias.
* Soporte de Hardware: Visualización de diagramas de pinouts para diversos encapsulados (ZIF40, SOIC8, DIP32, ICSP).
* Exportación Segura: Gestión de archivos mediante Storage Access Framework para compatibilidad con versiones recientes de Android.

### Programadores Compatibles

La aplicación es compatible con una variedad de programadores populares, incluyendo:
* Serie TL866 (TL866II+, TL866A, TL866CS).
* Serie Xgecu T48, T56 y T76 (Soporte experimental).
* Programadores basados en CH341A y CH347.

### Especificaciones Técnicas y Licencias

EEPROM Flasher es un proyecto de código abierto distribuido bajo la licencia GNU GPLv3. El desarrollo se fundamenta en la integración de bibliotecas de software libre reconocidas en la industria:

* Motor de programación: Basado en minipro (GNU GPLv3) por David Griffith y colaboradores.
* Comunicación USB: Implementada mediante libusb (GNU LGPLv2.1).
* Controladores serie: usb-serial-for-android (MIT).

El código fuente completo, las instrucciones de compilación y la documentación técnica adicional están disponibles en el repositorio oficial del proyecto.

Repositorio oficial: https://github.com/Danielk10/Lector-De-Memorias

### Información de Privacidad y Seguridad

Esta aplicación requiere permisos de acceso USB y almacenamiento para operar con el hardware del programador y los archivos de firmware. No se recopilan datos personales de forma directa, y la publicidad integrada se gestiona bajo las políticas de seguridad de Google AdMob.

---

## Instrucciones para el Desarrollador
- Asegúrese de que el enlace al repositorio sea funcional.
- Verifique que la descripción corta no exceda los 80 caracteres en la consola de Google Play.
- Esta ficha cumple con las normativas de Google Play al ser descriptiva, formal y evitar el uso de caracteres especiales o emoticones que puedan interferir con la legibilidad profesional.
