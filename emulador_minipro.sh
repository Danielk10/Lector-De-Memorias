#!/bin/bash
set -euo pipefail

# ==========================================
# Script para emular la conexión USB en local
# usando el parche libusb de Lector De Memorias
# ==========================================

export NATIVE_PREFIX="$HOME/native_test_root"
mkdir -p "$NATIVE_PREFIX"

echo "=== 1. Compilando libusb parcheada de forma nativa ==="
cd "$HOME"
rm -rf libusb_native
git clone https://github.com/libusb/libusb.git libusb_native --depth 1
cd libusb_native/libusb

cat > patch_libusb.py << 'EOF'
import os
def run_patch():
    core_path = 'core.c'
    if os.path.exists(core_path):
        with open(core_path, 'r') as f: lines = f.readlines()
        for i, line in enumerate(lines):
            if '#include <stdio.h>' in line:
                lines.insert(i + 1, '#include <stdlib.h>\n#include <stdint.h>\n')
                break
        for i, line in enumerate(lines):
            if 'int API_EXPORTED libusb_init(' in line:
                for j in range(i, i + 15):
                    if '{' in lines[j]:
                        lines[j+1:j+1] = [
                            '\n\tchar *f3 = getenv("ANDROID_USB_FD");\n',
                            '\tif (f3) {\n',
                            '\t\tfprintf(stderr, "[LIBUSB-HACK] libusb_init interceptado\\n");\n',
                            '\t\tif (ctx) *ctx = NULL;\n',
                            '\t\treturn 0;\n',
                            '\t}\n'
                        ]
                        break
                break
        for i, line in enumerate(lines):
            if 'ssize_t API_EXPORTED libusb_get_device_list' in line:
                for j in range(i, i + 20):
                    if 'ssize_t i, len' in lines[j]:
                        lines[j+1:j+1] = [
                            '\n\tchar *f1 = getenv("ANDROID_USB_FD");\n',
                            '\tif (f1) {\n',
                            '\t\tfprintf(stderr, "[LIBUSB-HACK] Creando dispositivo emulado en la lista\\n");\n',
                            '\t\tret = calloc(2, sizeof(void*));\n',
                            '\t\tstruct libusb_device *d = usbi_alloc_device(usbi_get_context(ctx), 0);\n',
                            '\t\tret[0] = d; ret[1] = NULL; *list = ret;\n',
                            '\t\tif (discdevs) discovered_devs_free(discdevs);\n',
                            '\t\treturn 1;\n',
                            '\t}\n'
                        ]
                        break
                break
        for i, line in enumerate(lines):
            if 'int API_EXPORTED libusb_open(' in line:
                for j in range(i, i + 20):
                    if 'int r;' in lines[j]:
                        lines[j+1:j+1] = [
                            '\n\tchar *f2 = getenv("ANDROID_USB_FD");\n',
                            '\tif (f2) {\n',
                            '\t\tfprintf(stderr, "[LIBUSB-HACK] libusb_open llamado! Envolviendo FD %s\\n", f2);\n',
                            '\t\tint fd = atoi(f2);\n',
                            '\t\treturn libusb_wrap_sys_device(ctx, (intptr_t)fd, dev_handle);\n',
                            '\t}\n'
                        ]
                        break
                break
        with open(core_path, 'w') as f: f.writelines(lines)
    desc_path = 'descriptor.c'
    if os.path.exists(desc_path):
        with open(desc_path, 'r') as f: lines = f.readlines()
        for i, line in enumerate(lines):
            if '#include <stdio.h>' in line or '#include <string.h>' in line:
                lines.insert(i + 1, '#include <stdlib.h>\n#include <unistd.h>\n')
                break
        for i, line in enumerate(lines):
            if 'int API_EXPORTED libusb_get_device_descriptor(' in line:
                for j in range(i, i + 15):
                    if '{' in lines[j]:
                        lines[j+1:j+1] = [
                            '\n\tchar *f_fd = getenv("ANDROID_USB_FD");\n',
                            '\tif (f_fd) {\n',
                            '\t\tint fd = atoi(f_fd);\n',
                            '\t\tunsigned char buf[18];\n',
                            '\t\tfprintf(stderr, "[LIBUSB-HACK] Leyendo Descriptor del FD %d... ", fd);\n',
                            '\t\tssize_t res = pread(fd, buf, 18, 0);\n',
                            '\t\tif (res == 18) {\n',
                            '\t\t\tdesc->bLength = buf[0];\n',
                            '\t\t\tdesc->bDescriptorType = buf[1];\n',
                            '\t\t\tdesc->bcdUSB = (buf[3] << 8) | buf[2];\n',
                            '\t\t\tdesc->bDeviceClass = buf[4];\n',
                            '\t\t\tdesc->bDeviceSubClass = buf[5];\n',
                            '\t\t\tdesc->bDeviceProtocol = buf[6];\n',
                            '\t\t\tdesc->bMaxPacketSize0 = buf[7];\n',
                            '\t\t\tdesc->idVendor = (buf[9] << 8) | buf[8];\n',
                            '\t\t\tdesc->idProduct = (buf[11] << 8) | buf[10];\n',
                            '\t\t\tdesc->bcdDevice = (buf[13] << 8) | buf[12];\n',
                            '\t\t\tdesc->iManufacturer = buf[14];\n',
                            '\t\t\tdesc->iProduct = buf[15];\n',
                            '\t\t\tdesc->iSerialNumber = buf[16];\n',
                            '\t\t\tdesc->bNumConfigurations = buf[17];\n',
                            '\t\t\tfprintf(stderr, "EXITO! VID=%04x PID=%04x\\n", desc->idVendor, desc->idProduct);\n',
                            '\t\t\treturn 0;\n',
                            '\t\t} else {\n',
                            '\t\t\tfprintf(stderr, "FALLO! (res=%ld)\\n", (long)res);\n',
                            '\t\t}\n',
                            '\t}\n'
                        ]
                        break
                break
        with open(desc_path, 'w') as f: f.writelines(lines)
EOF

python3 patch_libusb.py
cd ..
NOCONFIGURE=1 ./autogen.sh
./configure --prefix="$NATIVE_PREFIX" --disable-udev --enable-shared
make -j"$(nproc)"
make install

echo "=== 2. Compilando minipro con la libusb parcheada ==="
cd "$HOME"
rm -rf minipro_native
git clone https://gitlab.com/DavidGriffith/minipro.git minipro_native --depth 1
cd minipro_native
export PKG_CONFIG_PATH="$NATIVE_PREFIX/lib/pkgconfig"
make -j"$(nproc)" PREFIX="$NATIVE_PREFIX"
# Ignoramos error en make install (udev) porque no somos root localmente
make install PREFIX="$NATIVE_PREFIX" || true

echo "=== 3. Generando archivo de descriptor falso (mock_usb.bin) ==="
cd "$HOME"
# Usamos el VID:PID del TL866II+ (0x04D8:0x00E0)
python3 -c '
desc = bytearray([
    18, 1, 0x00, 0x02, 0, 0, 0, 64, 
    0xd8, 0x04, 0xe0, 0x00, 0x01, 0x00, 
    1, 2, 0, 1
])
with open("mock_usb.bin", "wb") as f:
    f.write(desc)
'

echo "=== 4. Ejecutando la simulación ==="
export LD_LIBRARY_PATH="$NATIVE_PREFIX/lib"
export ANDROID_USB_FD="99"
export MINIPRO_DATA="$NATIVE_PREFIX/share/minipro"

# Alimentamos a minipro pasando el mock_usb por el FD 99
"$NATIVE_PREFIX/bin/minipro" -p "AT24C02C" -r dump.bin 99<mock_usb.bin || true

echo "========================================================="
echo "¡Simulación Finalizada! Si dice 'Found TL866II+', el parche funciona."
echo "========================================================="
