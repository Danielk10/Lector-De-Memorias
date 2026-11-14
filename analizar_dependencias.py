#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Analiza las dependencias (DT_NEEDED) de los binarios de minipro para las
arquitecturas de 32 y 64 bits y regenera REPORTE_ANALISIS_DEPENDENCIAS.md.

Estructura esperada (las rutas antiguas 'fake_root/' y 'libs/' ya no existen):

    fake_root_32_bit/<BASE>/bin/minipro
    fake_root_32_bit/<BASE>/lib/libusb-1.0.so
    libs_32/libz.so.1
    fake_root_64_bit/<BASE>/bin/minipro
    fake_root_64_bit/<BASE>/lib/libusb-1.0.so
    libs_64/libz.so.1

Uso:
    python3 analizar_dependencias.py
"""

import datetime
import os
import subprocess
import sys

BASE = "data/data/com.diamon.mini/files/usr"
REPORTE = "REPORTE_ANALISIS_DEPENDENCIAS.md"
JNI_LIBS = os.path.join("app", "src", "main", "jniLibs")

# Librerías que provee Android en /system/lib (32-bit) o /system/lib64 (64-bit).
# No se empaquetan en el APK; el linker las resuelve del sistema.
LIBRERIAS_SISTEMA = {
    "libc.so",
    "libm.so",
    "libdl.so",
    "liblog.so",
    "libz.so",
    "libstdc++.so",
    "libgcc.so",
    "libc++_shared.so",
    "libandroid.so",
}

# Nombre con el que cada binario viaja dentro del APK (jniLibs/<abi>/).
NOMBRE_EN_APK = {
    "minipro": "libminipro_bin.so",
    "libusb-1.0.so": "libusb_1_0.so",
    "libz.so.1": "libz_1.so",
}

ARQUITECTURAS = [
    {
        "titulo": "32-bit",
        "abi": "armeabi-v7a",
        "clase": "ELF32",
        "maquina": "ARM",
        "fake_root": "fake_root_32_bit",
        "libs": "libs_32",
    },
    {
        "titulo": "64-bit",
        "abi": "arm64-v8a",
        "clase": "ELF64",
        "maquina": "AArch64",
        "fake_root": "fake_root_64_bit",
        "libs": "libs_64",
    },
]


def ejecutar_readelf(args, ruta):
    """Devuelve la salida de readelf o None si no se puede leer el archivo."""
    try:
        return subprocess.check_output(
            ["readelf"] + args + [ruta],
            text=True,
            stderr=subprocess.DEVNULL,
        )
    except (subprocess.CalledProcessError, FileNotFoundError, OSError):
        return None


def leer_cabecera(ruta):
    """Devuelve (clase, maquina) del header ELF, o (None, None)."""
    salida = ejecutar_readelf(["-h"], ruta)
    if salida is None:
        return None, None
    clase = maquina = None
    for linea in salida.splitlines():
        if "Class:" in linea and clase is None:
            clase = linea.split("Class:", 1)[1].strip()
        elif "Machine:" in linea and maquina is None:
            maquina = linea.split("Machine:", 1)[1].strip()
    return clase, maquina


def leer_needed(ruta):
    """Devuelve la lista de DT_NEEDED, o None si el archivo no se puede leer."""
    salida = ejecutar_readelf(["-d"], ruta)
    if salida is None:
        return None
    dependencias = []
    for linea in salida.splitlines():
        if "(NEEDED)" in linea and "[" in linea:
            dependencias.append(linea.split("[", 1)[1].split("]", 1)[0])
    return dependencias


def leer_soname(ruta):
    salida = ejecutar_readelf(["-d"], ruta)
    if salida is None:
        return None
    for linea in salida.splitlines():
        if "(SONAME)" in linea and "[" in linea:
            return linea.split("[", 1)[1].split("]", 1)[0]
    return None


def binarios_de(arq):
    """Lista de (nombre, ruta) de los binarios de una arquitectura."""
    fake_root = arq["fake_root"]
    libs = arq["libs"]

    ruta_libz = os.path.join(libs, "libz.so.1")
    if not os.path.exists(ruta_libz):
        # Respaldo: la copia incluida en el fake_root de esa arquitectura.
        ruta_libz = os.path.join(fake_root, BASE, "lib", "libz.so.1")

    return [
        ("minipro", os.path.join(fake_root, BASE, "bin", "minipro")),
        ("libusb-1.0.so", os.path.join(fake_root, BASE, "lib", "libusb-1.0.so")),
        ("libz.so.1", ruta_libz),
    ]


def tamano_legible(ruta):
    if not os.path.exists(ruta):
        return "-"
    tam = os.path.getsize(ruta)
    if tam >= 1024 * 1024:
        return f"{tam / (1024 * 1024):.1f} MB"
    if tam >= 1024:
        return f"{tam / 1024:.0f} KB"
    return f"{tam} B"


def analizar_arquitectura(arq):
    """Analiza una arquitectura y devuelve un dict con los resultados."""
    filas_binarios = []
    filas_dependencias = []
    filas_jni = []
    faltantes = []

    binarios = binarios_de(arq)
    nombres_presentes = {nombre for nombre, ruta in binarios if os.path.exists(ruta)}
    dir_sistema = "/system/lib/" if arq["clase"] == "ELF32" else "/system/lib64/"

    # Rutas donde se puede resolver cada dependencia dentro de esa arquitectura.
    rutas_busqueda = {
        "libusb-1.0.so": os.path.join(arq["fake_root"], BASE, "lib", "libusb-1.0.so"),
        "libz.so.1": os.path.join(arq["libs"], "libz.so.1"),
    }

    for nombre, ruta in binarios:
        existe = os.path.exists(ruta)
        clase, maquina = leer_cabecera(ruta) if existe else (None, None)
        correcta = clase == arq["clase"] and maquina == arq["maquina"]

        if not existe:
            estado = "❌ No existe"
        elif correcta:
            estado = "✔ Correcto"
        else:
            estado = "⚠️ Arquitectura distinta"

        filas_binarios.append(
            [
                f"`{nombre}`",
                f"`{ruta}`",
                f"{clase or '?'} / {maquina or '?'}",
                f"{arq['clase']} / {arq['maquina']}",
                estado,
                tamano_legible(ruta),
            ]
        )

        # Empaquetado en jniLibs (solo aplica a binarios que van al APK).
        nombre_apk = NOMBRE_EN_APK[nombre]
        ruta_apk = os.path.join(JNI_LIBS, arq["abi"], nombre_apk)
        if os.path.exists(ruta_apk):
            estado_apk = "✔ Presente"
        else:
            estado_apk = "❌ Falta"
            faltantes.append(f"{arq['abi']}/{nombre_apk}")
        filas_jni.append([f"`{nombre_apk}`", f"`{ruta_apk}`", estado_apk])

        # Dependencias reales del binario.
        dependencias = leer_needed(ruta) if existe else None
        if dependencias is None:
            continue

        for dep in dependencias:
            if dep in LIBRERIAS_SISTEMA:
                tipo = "Sistema (Android)"
                disponible = "✔ sistema"
                donde = dir_sistema
            elif dep in nombres_presentes:
                tipo = "Externa (empaquetada)"
                disponible = "✔ árbol"
                donde = f"`{rutas_busqueda.get(dep, ruta)}`"
            else:
                tipo = "Externa"
                disponible = "❌ falta"
                donde = "-"
                faltantes.append(f"{arq['abi']}: {dep} (requerida por {nombre})")

            filas_dependencias.append(
                [f"`{nombre}`", f"`{dep}`", tipo, disponible, donde]
            )

    return {
        "filas_binarios": filas_binarios,
        "filas_dependencias": filas_dependencias,
        "filas_jni": filas_jni,
        "faltantes": faltantes,
    }


def tabla(cabeceras, filas):
    lineas = [
        "| " + " | ".join(cabeceras) + " |",
        "|" + "|".join(["---"] * len(cabeceras)) + "|",
    ]
    for fila in filas:
        lineas.append("| " + " | ".join(str(c) for c in fila) + " |")
    return "\n".join(lineas)


def main():
    resultados = [(arq, analizar_arquitectura(arq)) for arq in ARQUITECTURAS]

    todas_faltantes = [f for _, res in resultados for f in res["faltantes"]]

    with open(REPORTE, "w", encoding="utf-8") as r:
        r.write("# Reporte de análisis de dependencias — 32 y 64 bits\n\n")
        r.write(
            "Generado por `analizar_dependencias.py` el "
            f"{datetime.date.today().isoformat()}.\n\n"
        )
        r.write(
            "Analiza los binarios de `minipro` para las dos arquitecturas soportadas, "
            "verifica que cada uno tenga la clase ELF correcta y comprueba que todas "
            "las dependencias `DT_NEEDED` estén presentes en el árbol o las provea Android.\n\n"
        )

        r.write("## Resumen\n\n")
        resumen = []
        for arq, res in resultados:
            estado = "✔ Completo" if not res["faltantes"] else "⚠️ Incompleto"
            resumen.append(
                [
                    arq["titulo"],
                    f"`{arq['abi']}`",
                    f"`{arq['fake_root']}`",
                    f"`{arq['libs']}`",
                    estado,
                ]
            )
        r.write(tabla(["Arquitectura", "ABI", "Fake root", "Libs", "Estado"], resumen))
        r.write("\n\n")

        for indice, (arq, res) in enumerate(resultados, start=1):
            r.write(f"## {indice}. {arq['titulo']} (`{arq['abi']}`)\n\n")

            r.write("### Binarios analizados\n\n")
            r.write(
                tabla(
                    [
                        "Binario",
                        "Ruta",
                        "ELF detectado",
                        "ELF esperado",
                        "Estado",
                        "Tamaño",
                    ],
                    res["filas_binarios"],
                )
            )
            r.write("\n\n")

            r.write("### Dependencias `DT_NEEDED`\n\n")
            r.write(
                tabla(
                    ["Binario", "Dependencia", "Tipo", "Disponible", "Dónde se resuelve"],
                    res["filas_dependencias"],
                )
            )
            r.write("\n\n")

            r.write(f"### Empaquetado en `jniLibs/{arq['abi']}/`\n\n")
            r.write(tabla(["Archivo en el APK", "Ruta", "Estado"], res["filas_jni"]))
            r.write("\n\n")

        r.write("## Notas\n\n")
        r.write(
            "* `libc.so`, `libdl.so`, `liblog.so` y `libm.so` las provee Android; "
            "no se empaquetan en el APK.\n"
        )
        r.write(
            "* `libusb-1.0.so` se empaqueta como `libusb_1_0.so` y `libz.so.1` como "
            "`libz_1.so`; `AssetHelper.java` crea los symlinks "
            "`usr/lib/libusb-1.0.so` y `usr/lib/libz.so.1` en tiempo de ejecución.\n"
        )
        r.write(
            "* `minipro` se empaqueta como `libminipro_bin.so` y se enlaza como "
            "`usr/bin/minipro`.\n"
        )
        r.write(
            "* Los assets (`infoic.xml`, `logicic.xml`, `dump-alg-minipro.bash`) son "
            "comunes a las dos arquitecturas y viven en `app/src/main/assets/`.\n"
        )
        r.write("\n")

        if todas_faltantes:
            r.write("## ⚠️ Pendientes\n\n")
            for pendiente in todas_faltantes:
                r.write(f"* {pendiente}\n")
            r.write("\n")

    if todas_faltantes:
        print("Advertencia: elementos pendientes:")
        for pendiente in todas_faltantes:
            print(f"  - {pendiente}")
        print(f"\nReporte regenerado en {REPORTE}")
        return 1

    print("Verificación exitosa: 32 y 64 bits con todas las dependencias presentes.")
    print(f"Reporte regenerado en {REPORTE}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
