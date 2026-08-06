#!/bin/bash
set -euo pipefail

echo "Instalando dependencias necesarias en Termux para minipro..."
pkg install -y git pkg-config clang make binutils

cd "$HOME" || exit 1

# Exportar las variables con el paquete correcto: com.diamon.mini
export APP_PREFIX="/data/data/com.diamon.mini/files/usr"
export DESTDIR="$HOME/fake_root"
export FAKE_USR="$DESTDIR$APP_PREFIX"
export TMX_PREFIX="/data/data/com.termux/files/usr"
export CC="clang"

# Configurar CFLAGS e Include paths (incluyendo explícitamente libusb-1.0)
export COMMON_CFLAGS="-fPIC -Oz -flto -fstack-protector-strong -D_FORTIFY_SOURCE=2 -I$FAKE_USR/include -I$FAKE_USR/include/libusb-1.0 -I$TMX_PREFIX/include -ffile-prefix-map=$DESTDIR="

# Configurar LDFLAGS con RPATH, hardening, alineación a 16KB y dependencia de Android log (-llog)
export COMMON_LDFLAGS="-flto -Wl,-z,max-page-size=16384 -Wl,-z,relro,-z,now -L$FAKE_USR/lib -L$TMX_PREFIX/lib -Wl,-rpath,$APP_PREFIX/lib -Wl,-rpath,$TMX_PREFIX/lib -llog"

# Hacer que pkg-config encuentre primero la libusb parcheada en fake_root
export PKG_CONFIG_PATH="$FAKE_USR/lib/pkgconfig:$TMX_PREFIX/lib/pkgconfig${PKG_CONFIG_PATH:+:$PKG_CONFIG_PATH}"

# Forzar a pkg-config a usar el directorio destino como raíz para inyectar el prefijo correcto en las rutas -I y -L
export PKG_CONFIG_SYSROOT_DIR="$DESTDIR"

# Exportar variables globales para que make las respete sin anular los flags internos
export CFLAGS="$COMMON_CFLAGS"
export LDFLAGS="$COMMON_LDFLAGS"

echo "Limpiando directorio previo y descargando código fuente de minipro..."
# Esto SOLO borra el código fuente descargado en $HOME, NO toca tu fake_root
rm -rf "$HOME/minipro"
git clone https://gitlab.com/DavidGriffith/minipro.git --depth 1
cd "$HOME/minipro" || exit 1

echo "Compilando minipro..."
make -j"$(nproc)" \
    PREFIX="$APP_PREFIX" \
    CC="$CC"

echo "Instalando minipro en fake_root..."
# Sobrescribimos las variables exactas del Makefile para evitar que pkg-config genere rutas de Termux
make install \
    PREFIX="$APP_PREFIX" \
    DESTDIR="$DESTDIR" \
    COMPLETIONS_DIR="$APP_PREFIX/share/bash-completion/completions" \
    UDEV_DIR="$APP_PREFIX/lib/udev/rules.d"

echo "========================================"
echo "Compilación de minipro Exitosa"
echo "========================================"
ls -lh "$FAKE_USR/bin/minipro" || true

echo ""
echo "Dependencias dinámicas de minipro:"
readelf -d "$FAKE_USR/bin/minipro" | grep NEEDED || true

echo ""
echo "Alineación 16KB en minipro:"
readelf -l "$FAKE_USR/bin/minipro" | grep LOAD || true
