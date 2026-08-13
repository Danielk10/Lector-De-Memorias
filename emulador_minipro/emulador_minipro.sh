#!/bin/bash
set -euo pipefail

# ========================================================
# Script para emular la conexión USB del programador TL866II+
# de forma dinámica sobre socketpair utilizando el emulador
# en C++ y parcheando libusb localmente.
# ========================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export NATIVE_PREFIX="$HOME/native_test_root"
mkdir -p "$NATIVE_PREFIX"
mkdir -p "$NATIVE_PREFIX/bin"

echo "=== 1. Compilando libusb parcheada de forma nativa ==="
cd "$HOME"
rm -rf libusb_native
git clone https://github.com/libusb/libusb.git libusb_native --depth 1
cd libusb_native/libusb

# Copiar el script de parchado dinámico local
cp "$SCRIPT_DIR/patch_libusb_local.py" ./
python3 patch_libusb_local.py

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

echo "=== 3. Compilando el emulador TL866II+ en C++ ==="
cd "$SCRIPT_DIR"
make clean
make
cp emulador_minipro "$NATIVE_PREFIX/bin/"

echo "=== 4. Creando imágenes de flash de prueba (SPI y EEPROM) ==="
# Generar archivos de prueba con patrones específicos
python3 -c '
# 1MB para simular chip flash SPI grande
with open("mock_spi.bin", "wb") as f:
    f.write(bytes([i % 256 for i in range(1048576)]))

# 256 bytes para simular EEPROM pequeña (ej. AT24C02C)
with open("mock_eeprom.bin", "wb") as f:
    f.write(bytes([255 - (i % 256) for i in range(256)]))
'

echo "=== 5. Ejecutando la simulación dinámica en socketpair (Lectura de EEPROM) ==="
export LD_LIBRARY_PATH="$NATIVE_PREFIX/lib"
export MINIPRO_DATA="$NATIVE_PREFIX/share/minipro"

echo "Simulando lectura de chip EEPROM AT24C02C..."
rm -f read_eeprom.bin
"$NATIVE_PREFIX/bin/emulador_minipro" --flash mock_eeprom.bin "$NATIVE_PREFIX/bin/minipro" -p "AT24C02C" -r read_eeprom.bin || true

# Comparar resultados EEPROM
eeprom_ok=0
if [ -f read_eeprom.bin ]; then
    echo "Comparando archivo leído con la eeprom simulada..."
    if cmp -s mock_eeprom.bin read_eeprom.bin; then
        echo "¡ÉXITO EEPROM! Los archivos coinciden perfectamente."
        eeprom_ok=1
    else
        echo "¡FALLO EEPROM! El archivo leído no coincide con el simulado."
    fi
else
    echo "¡FALLO EEPROM! No se generó read_eeprom.bin."
fi

echo "=== 6. Ejecutando la simulación de lectura de chip SPI Flash ==="
echo "Simulando lectura de chip SPI Flash W25Q80BV..."
rm -f read_spi.bin
"$NATIVE_PREFIX/bin/emulador_minipro" --flash mock_spi.bin "$NATIVE_PREFIX/bin/minipro" -p "W25Q80BV" -r read_spi.bin || true

# Comparar resultados SPI
spi_ok=0
if [ -f read_spi.bin ]; then
    echo "Comparando archivo leído con el SPI Flash simulado..."
    if cmp -s mock_spi.bin read_spi.bin; then
        echo "¡ÉXITO SPI! Los archivos coinciden perfectamente."
        spi_ok=1
    else
        echo "¡FALLO SPI! El archivo leído no coincide con el simulado."
    fi
else
    echo "¡FALLO SPI! No se generó read_spi.bin."
fi

echo "========================================================="
if [ $eeprom_ok -eq 1 ] && [ $spi_ok -eq 1 ]; then
    echo "¡Simulación Finalizada con ÉXITO completo para SPI y EEPROM!"
else
    echo "¡Simulación Finalizada con ERRORES!"
fi
echo "========================================================="
