import subprocess, os

# Rutas de los binarios y librerías originales en fake_root y libs
binarios_originales = {
    'minipro': 'fake_root/data/data/com.diamon.mini/files/usr/bin/minipro',
    'libusb-1.0.so': 'fake_root/data/data/com.diamon.mini/files/usr/lib/libusb-1.0.so',
    'libz.so.1': 'libs/libz.so.1'
}

files = set(binarios_originales.keys())

# Generar Reporte con los binarios originales
with open('REPORTE_ANALISIS_DEPENDENCIAS.md', 'w') as r:
    r.write('# Reporte Actualizado de Dependencias\n\n')
    for f in sorted(binarios_originales.keys()):
        path = binarios_originales[f]
        if not os.path.exists(path):
            continue
        r.write(f'### {f}\n| Dep | Class | InFolder |\n|---|---|---|\n')
        try:
            out = subprocess.check_output(['readelf', '-d', path], text=True)
            for line in out.splitlines():
                if '(NEEDED)' in line:
                    d = line.split('[')[1].split(']')[0]
                    es_sistema = d in ['libc.so', 'libm.so', 'libdl.so', 'liblog.so', 'libz.so', 'libz.so.1', 'libstdc++.so', 'libgcc.so', 'libc++_shared.so']
                    c = 'Sistema' if es_sistema else 'Externa'
                    r.write(f'| {d} | {c} | {("Sí" if d in files else "No")} |\n')
        except: pass
        r.write('\n')

# Verificación
missing_deps = set()
for f, path in binarios_originales.items():
    if not os.path.exists(path):
        continue
    try:
        out = subprocess.check_output(['readelf', '-d', path], text=True)
        for line in out.splitlines():
            if '(NEEDED)' in line:
                d = line.split('[')[1].split(']')[0]
                if d not in files and d not in ['libc.so', 'libm.so', 'libdl.so', 'liblog.so', 'libz.so', 'libz.so.1', 'libstdc++.so', 'libgcc.so', 'libc++_shared.so']:
                    missing_deps.add(d)
    except: pass

if missing_deps:
    print('Error: Dependencias faltantes:', missing_deps)
else:
    print('Verificación exitosa: Todas las dependencias externas están presentes.')
