# Emulador Local de MiniPro para Android (Pruebas sin Root)

Este documento explica cómo replicar en un entorno Linux local (x86_64 o cualquier arquitectura) el mismo mecanismo de **File Descriptor Injection (ANDROID_USB_FD)** que utiliza la aplicación en Android. Esto permite depurar y probar parches sin necesidad de compilar la app completa en el teléfono.

## 🛠️ ¿Qué hace este entorno de pruebas?

En Android, no tenemos permisos root para que `libusb` acceda a `/dev/bus/usb/`. La app sortea este obstáculo usando el API de Android para pedir permisos, obteniendo un File Descriptor y pasándolo por una variable de entorno (`ANDROID_USB_FD`). 

Para replicar esto localmente:
1. Se compila `libusb` **nativamente** con nuestro parche interceptor de Python (`patch_libusb.py`).
2. Se compila `minipro` vinculándolo contra esa `libusb` parcheada.
3. Se crea un archivo binario (`mock_usb.bin`) con exactamente 18 bytes que simulan ser el Descriptor de Dispositivo USB del programador (ej. TL866II+).
4. Se inyecta ese archivo como un File Descriptor (FD) directamente en la entrada de `minipro`, emulando a Android.

---

## 🚀 Pasos para la Prueba

### 1. Ejecutar el Script Todo-en-Uno

Para facilitar las cosas, se ha creado el script `emulador_minipro.sh` que descarga, parchea, compila y ejecuta todo el entorno de forma aislada sin ensuciar tu sistema.

```bash
chmod +x emulador_minipro.sh
./emulador_minipro.sh
```

### 2. ¿Qué ocurre internamente durante el script?

1. **Parche de libusb**: El script descarga `libusb` y ejecuta el script Python que modifica `core.c` y `descriptor.c`. Esto inyecta condicionales `getenv("ANDROID_USB_FD")` en funciones críticas como `libusb_init` y `libusb_get_device_descriptor`.
2. **Compilación Aislada**: Se instala en un directorio prefijado (`~/native_test_root`) para no afectar a las librerías del sistema Linux base.
3. **Generación del Descriptor Falso**: Usando un simple script en Python, genera los 18 bytes del descriptor de un programador TL866II+ (VID `0x04d8`, PID `0x00e0`).
4. **Ejecución y Resultados**: Configura `ANDROID_USB_FD=99` y ejecuta minipro así:
   ```bash
   minipro -p "AT24C02C" -r dump.bin 99<mock_usb.bin
   ```

### 3. Salida Esperada

Si todo funciona, `minipro` caerá en la trampa del parche de `libusb`, leerá tu `mock_usb.bin` en lugar del USB físico, y reportará que encontró el dispositivo:

```text
=== Probando minipro con emulación USB ===
[LIBUSB-HACK] libusb_init interceptado
[LIBUSB-HACK] libusb_init interceptado
[LIBUSB-HACK] libusb_init interceptado
[LIBUSB-HACK] Creando dispositivo emulado en la lista
[LIBUSB-HACK] Leyendo Descriptor del FD 99... EXITO! VID=04d8 PID=00e0
[LIBUSB-HACK] libusb_open llamado! Envolviendo FD 99
Found TL866II+ 04.2.143 (0x28f)
```

*(Es normal que luego dé algún error sobre la lectura o sobre el firmware, ya que no hay un hardware real conectado que responda a comandos posteriores, el objetivo es validar la etapa de **Enumeración e Identificación USB**)*.

---

## 📝 Utilidad

- Validar rápidamente cambios en la lógica de intercepción de `libusb` sin enviar comandos al teléfono.
- Extender el parche si algún programador nuevo (`CH341A`, `T56`) se comporta de manera extraña durante la apertura del File Descriptor.
