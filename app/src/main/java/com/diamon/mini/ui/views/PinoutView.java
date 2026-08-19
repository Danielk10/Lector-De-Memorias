package com.diamon.mini.ui.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.widget.ImageView;

import com.diamon.mini.R;

/**
 * PinoutView: Dibuja diagramas de conexión y pinouts de hardware de alta precisión
 * para programadores MiniPro (TL866II+, TL866A/CS, T48, T56, T76) y chips soportados.
 */
public class PinoutView {

    // Paleta de colores Dark Theme
    private static final int COL_BG = 0xFF12141D;
    private static final int COL_HEADER = 0xFF0D47A1;
    private static final int COL_PANEL = 0xFF1C2234;
    private static final int COL_CARD = 0xFF161A28;
    private static final int COL_BORDE = 0xFF3E4A61;
    private static final int COL_LINEA = 0xFF546E7A;
    private static final int COL_TITULO = 0xFFECEFF1;
    private static final int COL_LABEL = 0xFFB0BEC5;
    private static final int COL_PIN_NUM = 0xFF80CBC4;
    private static final int COL_AVISO = 0xFFFF7043;
    private static final int COL_VCC = 0xFFEF5350;
    private static final int COL_GND = 0xFF90A4AE;
    private static final int COL_SIGNAL = 0xFF66BB6A;
    private static final int COL_CHIP_BODY = 0xFF21252B;
    private static final int COL_ACCENT = 0xFF29B6F6;
    private static final int COL_YELLOW = 0xFFFFCA28;
    private static final int COL_PURPLE = 0xFFCE93D8;

    private static final int W = 800;
    private static final int H = 560;

    private static final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final RectF rectF = new RectF();

    // ────────── 1. ZIF Socket 40 Pines TL866 / T48 / T56 / T76 ───────────────

    public static void dibujarZIF40(Context ctx, ImageView target) {
        Bitmap bmp = crearBitmap();
        Canvas canvas = new Canvas(bmp);
        dibujarHeader(canvas, ctx.getString(R.string.str_pinout_zif40_header));

        // Dibujar Socket ZIF 40 pines esquemático
        float zx = 45, zy = 52, zw = 175, zh = 450;
        dibujarRectanguloRedondeado(canvas, zx, zy, zw, zh, 8, COL_CHIP_BODY);
        bordesRedondeados(canvas, zx, zy, zw, zh, 8, COL_BORDE);

        // Palanca ZIF en la parte superior izquierda
        dibujarRectangulo(canvas, zx - 14, zy + 8, 12, 48, 0xFF78909C);
        dibujarRectangulo(canvas, zx - 22, zy + 4, 10, 18, 0xFFFFB74D); // Pomo palanca

        // Muesca superior
        paint.setColor(0xFF37474F);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(zx + zw / 2f, zy + 2, 12, paint);

        configurarTexto(11f, true);
        dibujarTexto(canvas, "ZIF 40", zx + 60, zy + 24, COL_TITULO);

        // Pines del ZIF (20 a cada lado)
        float pinH = 14, pinW = 16, gap = 6.5f;
        for (int i = 0; i < 20; i++) {
            float py = zy + 36 + i * (pinH + gap);
            // Pines izquierdos: 1 a 20
            int pIzq = i + 1;
            dibujarRectangulo(canvas, zx - pinW, py, pinW, pinH, COL_PANEL);
            bordes(canvas, zx - pinW, py, pinW, pinH, COL_BORDE);
            configurarTexto(8f, false);
            dibujarTexto(canvas, String.valueOf(pIzq), zx - pinW + 2, py + pinH - 3, COL_PIN_NUM);

            // Pines derechos: 40 a 21
            int pDer = 40 - i;
            dibujarRectangulo(canvas, zx + zw, py, pinW, pinH, COL_PANEL);
            bordes(canvas, zx + zw, py, pinW, pinH, COL_BORDE);
            dibujarTexto(canvas, String.valueOf(pDer), zx + zw + 2, py + pinH - 3, COL_PIN_NUM);
        }

        // Panel de Alineación a la derecha
        float tx = 250, ty = 52, tw = 515, th = 450;
        dibujarRectanguloRedondeado(canvas, tx, ty, tw, th, 8, COL_PANEL);
        bordesRedondeados(canvas, tx, ty, tw, th, 8, COL_BORDE);

        configurarTexto(12.5f, true);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_universal_alignment), tx + 14, ty + 24, COL_YELLOW);

        configurarTexto(10f, false);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_dip_notch_up), tx + 14, ty + 50, COL_TITULO);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_dip_align_bottom), tx + 14, ty + 68, COL_TITULO);

        // Tabla de colocación
        float tby = ty + 90;
        dibujarFilaInfo(canvas, tx + 14, tby, "DIP8 — Series 24xx, 25xx, 93xx:", "Pines 17-20 (Izq) y 21-24 (Der) [4 filas inferiores]", COL_SIGNAL);
        dibujarFilaInfo(canvas, tx + 14, tby + 36, "DIP14/16 — Series 28xx, GAL16V8, PIC16F:", "Pines 13-20 (Izq) y 21-28 (Der)", COL_SIGNAL);
        dibujarFilaInfo(canvas, tx + 14, tby + 72, "DIP18/20 — PIC16F628A, GAL20V8, ATTiny:", "Pines 11-20 (Izq) y 21-30 (Der)", COL_SIGNAL);
        dibujarFilaInfo(canvas, tx + 14, tby + 108, "DIP24 — Series 27C64, 27C128:", "Pines 9-20 (Izq) y 21-32 (Der)", COL_SIGNAL);
        dibujarFilaInfo(canvas, tx + 14, tby + 144, "DIP28 — Series 27C256, 28C256, ATmega328P:", "Pines 7-20 (Izq) y 21-34 (Der)", COL_SIGNAL);
        dibujarFilaInfo(canvas, tx + 14, tby + 180, "DIP32 — Series 27C512, 29F010, SST39SF040:", "Pines 5-20 (Izq) y 21-36 (Der)", COL_SIGNAL);
        dibujarFilaInfo(canvas, tx + 14, tby + 216, "DIP40 — Series PIC16F877A, 27C400, AVR DIP40:", "Pines 1-20 (Izq) y 21-40 (Der) [Zócalo Completo]", COL_ACCENT);

        configurarTexto(9.5f, true);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_pin1_corner), tx + 14, ty + 360, COL_PIN_NUM);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_lock_lever), tx + 14, ty + 382, COL_LABEL);

        dibujarNota(canvas, ctx.getString(R.string.str_pinout_zif40_note));
        aplicar(bmp, target);
    }

    // ────────── 2. SPI Flash SOIC8 / SOP8 / DIP8 25xxx ───────────────────────

    public static void dibujarSPI25(Context ctx, ImageView target) {
        Bitmap bmp = crearBitmap();
        Canvas canvas = new Canvas(bmp);
        dibujarHeader(canvas, ctx.getString(R.string.str_pinout_spi25_header));

        dibujarChipSOIC8(canvas, 60, 100,
                new String[] { "CS#", "DO", "WP#", "GND", "DI", "CLK", "HOLD#", "VCC" },
                true, "25xxx SPI");

        dibujarFlecha(canvas, 310, 220);

        dibujarTablaConexion(canvas, 360, 75,
                "Flash 25xxx", "Función / MiniPro", "Pin",
                new String[] { "1 - CS#", "2 - DO / SO", "3 - WP# / IO2", "4 - GND", "5 - DI / SI", "6 - CLK", "7 - HOLD# / IO3", "8 - VCC" },
                new String[] { "Chip Select (Activo Bajo)", "Data Out / MISO", "Write Protect (a VCC)", "Tierra / Masa 0V", "Data In / MOSI", "Clock / Reloj SPI", "Hold / Reset (a VCC)", "Alimentación 3.3V / 1.8V" });

        dibujarNota(canvas, ctx.getString(R.string.str_pinout_spi25_note));
        aplicar(bmp, target);
    }

    // ────────── 3. I2C EEPROM 24Cxx ──────────────────────────────────────────

    public static void dibujarI2C24(Context ctx, ImageView target) {
        Bitmap bmp = crearBitmap();
        Canvas canvas = new Canvas(bmp);
        dibujarHeader(canvas, ctx.getString(R.string.str_pinout_i2c24_header));

        dibujarChipSOIC8(canvas, 60, 100,
                new String[] { "A0", "A1", "A2", "GND", "SDA", "SCL", "WP", "VCC" },
                true, "24Cxx I2C");

        dibujarFlecha(canvas, 310, 220);

        dibujarTablaConexion(canvas, 360, 75,
                "EEPROM 24Cxx", "Función / Conexión", "Pin",
                new String[] { "1 - A0", "2 - A1", "3 - A2", "4 - GND", "5 - SDA", "6 - SCL", "7 - WP", "8 - VCC" },
                new String[] { "Dirección Chip Bit 0", "Dirección Chip Bit 1", "Dirección Chip Bit 2", "Tierra / Masa 0V", "Línea de Datos Serie (Pull-up)", "Línea de Reloj Serie (Pull-up)", "Write Protect (GND = Escribir)", "Alimentación 1.8V / 3.3V / 5V" });

        dibujarNota(canvas, ctx.getString(R.string.str_pinout_i2c24_note));
        aplicar(bmp, target);
    }

    // ────────── 4. Microwire EEPROM 93Cxx ────────────────────────────────────

    public static void dibujarMicrowire93(Context ctx, ImageView target) {
        Bitmap bmp = crearBitmap();
        Canvas canvas = new Canvas(bmp);
        dibujarHeader(canvas, ctx.getString(R.string.str_pinout_mw93_header));

        dibujarChipSOIC8(canvas, 60, 100,
                new String[] { "CS", "SK", "DI", "DO", "GND", "ORG", "NC", "VCC" },
                true, "93Cxx uWire");

        dibujarFlecha(canvas, 310, 220);

        dibujarTablaConexion(canvas, 360, 75,
                "EEPROM 93Cxx", "Función / Microwire", "Pin",
                new String[] { "1 - CS", "2 - SK", "3 - DI", "4 - DO", "5 - GND", "6 - ORG", "7 - NC / PE", "8 - VCC" },
                new String[] { "Chip Select (Activo Alto)", "Serial Clock / Reloj", "Serial Data Input", "Serial Data Output", "Tierra 0V", "Organización: VCC=x16, GND=x8", "Sin Conexión / Program Enable", "Alimentación 5V / 3.3V" });

        dibujarNota(canvas, ctx.getString(R.string.str_pinout_mw93_note));
        aplicar(bmp, target);
    }

    // ────────── 5. Parallel Flash / EPROM DIP28 / DIP32 ───────────────────────

    public static void dibujarParallelDIP(Context ctx, ImageView target) {
        Bitmap bmp = crearBitmap();
        Canvas canvas = new Canvas(bmp);
        dibujarHeader(canvas, ctx.getString(R.string.str_pinout_parallel_header));

        float x = 30, y = 50, w = 740, h = 455;
        dibujarRectanguloRedondeado(canvas, x, y, w, h, 8, COL_PANEL);
        bordesRedondeados(canvas, x, y, w, h, 8, COL_BORDE);

        configurarTexto(12f, true);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_parallel_title), x + 16, y + 24, COL_YELLOW);

        // Tabla 2 columnas DIP32
        float colW = 345;
        configurarTexto(9.5f, true);
        dibujarRectangulo(canvas, x + 14, y + 36, colW, 20, COL_HEADER);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_left_pins), x + 22, y + 50, COL_TITULO);

        dibujarRectangulo(canvas, x + 375, y + 36, colW, 20, COL_HEADER);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_right_pins), x + 383, y + 50, COL_TITULO);

        String[] leftPins = {
                "1: VPP / A18 (Voltaje Prog. / Dir)",
                "2: A16 (Bus Dirección 16)",
                "3: A15 (Bus Dirección 15)",
                "4: A12 (Bus Dirección 12)",
                "5: A7  (Bus Dirección 7)",
                "6: A6  (Bus Dirección 6)",
                "7: A5  (Bus Dirección 5)",
                "8: A4  (Bus Dirección 4)",
                "9: A3  (Bus Dirección 3)",
                "10: A2 (Bus Dirección 2)",
                "11: A1 (Bus Dirección 1)",
                "12: A0 (Bus Dirección 0)",
                "13: D0 (Bus de Datos 0)",
                "14: D1 (Bus de Datos 1)",
                "15: D2 (Bus de Datos 2)",
                "16: GND (Tierra / Masa 0V)"
        };

        String[] rightPins = {
                "32: VCC (Alimentación 5V / 3.3V)",
                "31: /WE / A17 (Write Enable / Dir)",
                "30: NC / A17 (No Conectado / Dir)",
                "29: A14 (Bus Dirección 14)",
                "28: A13 (Bus Dirección 13)",
                "27: A8  (Bus Dirección 8)",
                "26: A9  (Bus Dirección 9 / VPP en 27C)",
                "25: A11 (Bus Dirección 11)",
                "24: /OE / VPP (Output Enable / VPP)",
                "23: A10 (Bus Dirección 10)",
                "22: /CE (Chip Enable)",
                "21: D7  (Bus de Datos 7)",
                "20: D6  (Bus de Datos 6)",
                "19: D5  (Bus de Datos 5)",
                "18: D4  (Bus de Datos 4)",
                "17: D3  (Bus de Datos 3)"
        };

        for (int i = 0; i < 16; i++) {
            float ry = y + 60 + i * 23;
            int bg = (i % 2 == 0) ? COL_CHIP_BODY : COL_CARD;
            dibujarRectangulo(canvas, x + 14, ry, colW, 21, bg);
            dibujarRectangulo(canvas, x + 375, ry, colW, 21, bg);

            configurarTexto(9f, false);
            dibujarTexto(canvas, leftPins[i], x + 20, ry + 15, colorPin(leftPins[i]));
            dibujarTexto(canvas, rightPins[i], x + 381, ry + 15, colorPin(rightPins[i]));
        }

        dibujarNota(canvas, ctx.getString(R.string.str_pinout_parallel_note));
        aplicar(bmp, target);
    }

    // ────────── 6. Header ICSP TL866 / T48 6 Pines ───────────────────────────

    public static void dibujarICSP(Context ctx, ImageView target) {
        Bitmap bmp = crearBitmap();
        Canvas canvas = new Canvas(bmp);
        dibujarHeader(canvas, ctx.getString(R.string.str_pinout_icsp_header));

        // Dibujar conector ICSP 6 pines (Vista frontal macho)
        float hx = 35, hy = 65;
        dibujarRectanguloRedondeado(canvas, hx, hy, 185, 270, 8, COL_CHIP_BODY);
        bordesRedondeados(canvas, hx, hy, 185, 270, 8, COL_BORDE);

        configurarTexto(11.5f, true);
        dibujarTexto(canvas, "PUERTO ICSP", hx + 40, hy + 24, COL_TITULO);
        configurarTexto(9.5f, false);
        dibujarTexto(canvas, "TL866A / II+ / T48", hx + 35, hy + 40, COL_LABEL);

        // 6 Pines en hilera vertical
        String[] icspPins = {
                "1 - VPP / MCLR",
                "2 - VCC 3.3V/5V",
                "3 - GND Tierra",
                "4 - PGD / MOSI / SDI",
                "5 - PGC / SCK / CLK",
                "6 - AUX / MISO / SDO"
        };

        for (int i = 0; i < 6; i++) {
            float py = hy + 58 + i * 33;
            dibujarRectanguloRedondeado(canvas, hx + 16, py, 26, 22, 4, COL_PANEL);
            bordesRedondeados(canvas, hx + 16, py, 26, 22, 4, COL_BORDE);
            configurarTexto(9f, true);
            dibujarTexto(canvas, String.valueOf(i + 1), hx + 24, py + 15, COL_PIN_NUM);
            configurarTexto(8.5f, false);
            dibujarTexto(canvas, icspPins[i].substring(4), hx + 48, py + 15, colorPin(icspPins[i]));
        }

        // Tabla de aplicaciones a la derecha
        float tx = 240, ty = 52, tw = 525, th = 450;
        dibujarRectanguloRedondeado(canvas, tx, ty, tw, th, 8, COL_PANEL);
        bordesRedondeados(canvas, tx, ty, tw, th, 8, COL_BORDE);

        configurarTexto(12f, true);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_icsp_in_circuit), tx + 14, ty + 24, COL_YELLOW);

        configurarTexto(9.5f, false);
        dibujarFilaInfo(canvas, tx + 14, ty + 46, "Microchip PIC (12F, 16F, 18F, 24F, dsPIC):", "Pin 1→MCLR/VPP, 2→VDD(+5V), 3→VSS(GND), 4→PGD, 5→PGC", COL_SIGNAL);
        dibujarFilaInfo(canvas, tx + 14, ty + 90, "Atmel / AVR (ATmega328P, ATtiny, ATmega16):", "Pin 1→RESET, 2→VCC, 3→GND, 4→MOSI, 5→SCK, 6→MISO", COL_SIGNAL);
        dibujarFilaInfo(canvas, tx + 14, ty + 134, "SPI Flash en placa (Serie 25xxx):", "Pin 1→CS#, 2→VCC, 3→GND, 4→DI(MOSI), 5→CLK, 6→DO(MISO)", COL_ACCENT);

        configurarTexto(9.5f, true);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_icsp_precautions), tx + 14, ty + 200, COL_AVISO);
        configurarTexto(9f, false);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_icsp_warn_mclr), tx + 14, ty + 224, COL_LABEL);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_icsp_warn_vcc), tx + 14, ty + 246, COL_LABEL);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_icsp_warn_external), tx + 14, ty + 268, COL_LABEL);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_icsp_warn_length), tx + 14, ty + 290, COL_LABEL);

        dibujarNota(canvas, ctx.getString(R.string.str_pinout_icsp_note));
        aplicar(bmp, target);
    }

    // ────────── 7. Microchip PIC ICSP Pinouts (DIP8..DIP40) ──────────────────

    public static void dibujarPIC_ICSP(Context ctx, ImageView target) {
        Bitmap bmp = crearBitmap();
        Canvas canvas = new Canvas(bmp);
        dibujarHeader(canvas, ctx.getString(R.string.str_pinout_pic_icsp_header));

        float x = 30, y = 50, w = 740, h = 455;
        dibujarRectanguloRedondeado(canvas, x, y, w, h, 8, COL_PANEL);
        bordesRedondeados(canvas, x, y, w, h, 8, COL_BORDE);

        configurarTexto(12f, true);
        dibujarTexto(canvas, "GUÍA DE CONEXIÓN ICSP PARA MICROCONTROLADORES PIC", x + 16, y + 24, COL_YELLOW);

        float colW = 708;
        String[][] picFamilies = {
                { "PIC12F (DIP8) — ej: 12F629, 12F675, 12F683, 16F18313",
                  "VPP→Pin 4 (MCLR)  |  VDD→Pin 1 (+5V)  |  VSS→Pin 8 (GND)  |  PGD→Pin 7 (Datos)  |  PGC→Pin 6 (Reloj)" },
                { "PIC16F (DIP14) — ej: 16F676, 16F684, 16F1823, 16F1824",
                  "VPP→Pin 4 (MCLR)  |  VDD→Pin 1 (+5V)  |  VSS→Pin 14 (GND) |  PGD→Pin 10 (Datos) |  PGC→Pin 9 (Reloj)" },
                { "PIC16F / 18F (DIP18) — ej: 16F628A, 16F84A, 16F88, 18F1320",
                  "VPP→Pin 4 (MCLR)  |  VDD→Pin 14 (+5V) |  VSS→Pin 5 (GND)  |  PGD→Pin 13 (RB7)   |  PGC→Pin 12 (RB6)" },
                { "PIC16F / 18F (DIP28) — ej: 16F876A, 18F2550, 18F26K22",
                  "VPP→Pin 1 (MCLR)  |  VDD→Pin 20 (+5V) |  VSS→Pin 8,19(GND)|  PGD→Pin 28 (RB7)   |  PGC→Pin 27 (RB6)" },
                { "PIC16F / 18F (DIP40) — ej: 16F877A, 18F4550, 18F45K22, 18F4620",
                  "VPP→Pin 1 (MCLR)  |  VDD→Pin 11,32(+5V)| VSS→Pin 12,31(GND)| PGD→Pin 39 (RB7)   |  PGC→Pin 40 (RB6)" }
        };

        for (int i = 0; i < picFamilies.length; i++) {
            float ry = y + 42 + i * 78;
            dibujarRectanguloRedondeado(canvas, x + 14, ry, colW, 68, 6, COL_CARD);
            bordesRedondeados(canvas, x + 14, ry, colW, 68, 6, COL_BORDE);

            configurarTexto(10f, true);
            dibujarTexto(canvas, picFamilies[i][0], x + 24, ry + 22, COL_ACCENT);

            configurarTexto(9f, false);
            dibujarTexto(canvas, picFamilies[i][1], x + 24, ry + 48, COL_SIGNAL);
        }

        dibujarNota(canvas, ctx.getString(R.string.str_pinout_pic_icsp_note));
        aplicar(bmp, target);
    }

    // ────────── 8. AVR ISP 6 / 10 Pines ──────────────────────────────────────

    public static void dibujarAVRISP(Context ctx, ImageView target) {
        Bitmap bmp = crearBitmap();
        Canvas canvas = new Canvas(bmp);
        dibujarHeader(canvas, ctx.getString(R.string.str_pinout_avrisp_header));

        float x = 30, y = 50, w = 740, h = 455;
        dibujarRectanguloRedondeado(canvas, x, y, w, h, 8, COL_PANEL);
        bordesRedondeados(canvas, x, y, w, h, 8, COL_BORDE);

        configurarTexto(12f, true);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_avr_standard), x + 16, y + 24, COL_YELLOW);

        // Header 6 Pines (2x3)
        float b1x = x + 16, b1y = y + 42, b1w = 345, b1h = 290;
        dibujarRectanguloRedondeado(canvas, b1x, b1y, b1w, b1h, 6, COL_CHIP_BODY);
        bordesRedondeados(canvas, b1x, b1y, b1w, b1h, 6, COL_BORDE);
        configurarTexto(10.5f, true);
        dibujarTexto(canvas, "AVR ISP 6-PIN (Header 2x3)", b1x + 14, b1y + 22, COL_TITULO);

        String[] p6 = {
                "1: MISO (Datos Salida)",
                "2: VCC / VTG (+5V / +3.3V)",
                "3: SCK (Reloj Serie)",
                "4: MOSI (Datos Entrada)",
                "5: /RESET (Activo Bajo)",
                "6: GND (Tierra / Masa)"
        };
        for (int i = 0; i < 6; i++) {
            float py = b1y + 42 + i * 38;
            configurarTexto(9f, false);
            dibujarTexto(canvas, p6[i], b1x + 20, py + 14, colorPin(p6[i]));
        }

        // Header 10 Pines (2x5)
        float b2x = x + 375, b2y = y + 42, b2w = 345, b2h = 360;
        dibujarRectanguloRedondeado(canvas, b2x, b2y, b2w, b2h, 6, COL_CHIP_BODY);
        bordesRedondeados(canvas, b2x, b2y, b2w, b2h, 6, COL_BORDE);
        configurarTexto(10.5f, true);
        dibujarTexto(canvas, "AVR ISP 10-PIN (Header 2x5 IDC)", b2x + 14, b2y + 22, COL_TITULO);

        String[] p10 = {
                "1: MOSI (Master Out)",   "2: VCC (+5V / +3.3V)",
                "3: NC / LED",            "4: GND (Tierra)",
                "5: /RESET",              "6: GND (Tierra)",
                "7: SCK (Reloj)",         "8: GND (Tierra)",
                "9: MISO (Master In)",    "10: GND (Tierra)"
        };
        for (int i = 0; i < 10; i++) {
            float py = b2y + 40 + i * 30;
            configurarTexto(9f, false);
            dibujarTexto(canvas, p10[i], b2x + 20, py + 12, colorPin(p10[i]));
        }

        configurarTexto(9f, false);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_avr_compatible), b1x + 4, y + 360, COL_LABEL);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_avr_use_icsp), b1x + 4, y + 382, COL_ACCENT);

        dibujarNota(canvas, ctx.getString(R.string.str_pinout_avrisp_note));
        aplicar(bmp, target);
    }

    // ────────── 9. PLCC32 a DIP32 Adapter ────────────────────────────────────

    public static void dibujarPLCC32(Context ctx, ImageView target) {
        Bitmap bmp = crearBitmap();
        Canvas canvas = new Canvas(bmp);
        dibujarHeader(canvas, ctx.getString(R.string.str_pinout_plcc32_header));

        // Cuadro PLCC32
        float px = 45, py = 80, pw = 220, ph = 220;
        dibujarRectanguloRedondeado(canvas, px, py, pw, ph, 8, COL_CHIP_BODY);
        bordesRedondeados(canvas, px, py, pw, ph, 8, COL_BORDE);

        // Chaflán de Pin 1
        dibujarLinea(canvas, px + 16, py, px, py + 16, COL_AVISO);
        paint.setColor(0xFF00E5FF);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(px + 30, py + 30, 6, paint);

        configurarTexto(12f, true);
        dibujarTexto(canvas, "PLCC32", px + 75, py + 105, COL_TITULO);
        configurarTexto(9f, false);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_top_view), px + 68, py + 130, COL_LABEL);

        // Mapeo explicativo
        float tx = 295, ty = 52, tw = 475, th = 450;
        dibujarRectanguloRedondeado(canvas, tx, ty, tw, th, 8, COL_PANEL);
        bordesRedondeados(canvas, tx, ty, tw, th, 8, COL_BORDE);

        configurarTexto(12f, true);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_plcc_adapter), tx + 14, ty + 24, COL_YELLOW);

        configurarTexto(9.5f, false);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_plcc_used_for), tx + 14, ty + 50, COL_TITULO);
        dibujarTexto(canvas, "• SST39SF010A / 020A / 040 en PLCC32", tx + 14, ty + 72, COL_SIGNAL);
        dibujarTexto(canvas, "• Winbond W29EE011 / W49F002 en PLCC32", tx + 14, ty + 94, COL_SIGNAL);
        dibujarTexto(canvas, "• EPROM 27C256 / 27C512 / 29C010 en PLCC32", tx + 14, ty + 116, COL_SIGNAL);

        configurarTexto(9.5f, true);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_plcc_orientation), tx + 14, ty + 155, COL_ACCENT);
        configurarTexto(9f, false);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_plcc_step1), tx + 14, ty + 180, COL_LABEL);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_plcc_step2), tx + 14, ty + 225, COL_LABEL);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_plcc_step3), tx + 14, ty + 270, COL_LABEL);

        dibujarNota(canvas, ctx.getString(R.string.str_pinout_plcc32_note));
        aplicar(bmp, target);
    }

    // ────────── Helpers de dibujo ────────────────────────────────────────────

    private static Bitmap crearBitmap() {
        return Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
    }

    private static void dibujarHeader(Canvas g, String titulo) {
        g.drawColor(COL_BG);
        dibujarRectangulo(g, 0, 0, W, 40, COL_HEADER);
        configurarTexto(13f, true);
        dibujarTexto(g, titulo, 16, 26, COL_TITULO);
        dibujarLinea(g, 0, 40, W, 40, COL_BORDE);
    }

    private static void dibujarNota(Canvas g, String nota) {
        dibujarRectangulo(g, 0, H - 34, W, 34, 0xFF141722);
        dibujarLinea(g, 0, H - 34, W, H - 34, COL_AVISO);
        configurarTexto(9.5f, false);
        dibujarTexto(g, nota, 14, H - 13, COL_AVISO);
    }

    private static void dibujarChipSOIC8(Canvas g, float x, float y,
                                         String[] pines, boolean conPunto, String chipLabel) {
        float CW = 120, CH = 220;
        float pinW = 28, pinH = 20, gap = (CH - 4 * pinH) / 5f;

        dibujarRectanguloRedondeado(g, x, y, CW, CH, 8, COL_CHIP_BODY);
        bordesRedondeados(g, x, y, CW, CH, 8, COL_BORDE);

        // Muesca superior
        paint.setColor(0xFF37474F);
        paint.setStyle(Paint.Style.FILL);
        g.drawCircle(x + CW / 2f, y + 2, 12, paint);

        if (conPunto) {
            paint.setColor(0xFF00E5FF);
            paint.setStyle(Paint.Style.FILL);
            g.drawCircle(x + 16, y + 20, 5, paint);
        }

        configurarTexto(10.5f, true);
        dibujarTexto(g, chipLabel, x + 16, y + CH / 2f + 4, COL_ACCENT);

        for (int i = 0; i < 4; i++) {
            float py = y + gap + i * (pinH + gap);

            // Pines izquierdos 1..4
            dibujarRectanguloRedondeado(g, x - pinW, py, pinW, pinH, 4, COL_CARD);
            bordesRedondeados(g, x - pinW, py, pinW, pinH, 4, COL_LINEA);
            configurarTexto(9.5f, true);
            dibujarTexto(g, String.valueOf(i + 1), x - pinW + 8, py + pinH - 5, COL_PIN_NUM);
            configurarTexto(9f, false);
            int col = colorPin(pines[i]);
            dibujarTexto(g, pines[i], x - pinW - 48, py + pinH - 5, col);

            // Pines derechos 8..5
            int ri = 7 - i;
            float prx = x + CW;
            dibujarRectanguloRedondeado(g, prx, py, pinW, pinH, 4, COL_CARD);
            bordesRedondeados(g, prx, py, pinW, pinH, 4, COL_LINEA);
            configurarTexto(9.5f, true);
            dibujarTexto(g, String.valueOf(ri + 1), prx + 8, py + pinH - 5, COL_PIN_NUM);
            configurarTexto(9f, false);
            int colR = colorPin(pines[ri]);
            dibujarTexto(g, pines[ri], prx + pinW + 8, py + pinH - 5, colR);
        }
    }

    private static void dibujarTablaConexion(Canvas g, float x, float y,
                                             String headerLeft, String headerRight, String pinChipLabel,
                                             String[] col1, String[] col2) {
        float rowH = 26, col1W = 120, col2W = 270;
        configurarTexto(11f, true);
        dibujarTexto(g, headerLeft + " → " + headerRight, x, y - 8, COL_LABEL);
        dibujarRectanguloRedondeado(g, x, y, col1W + col2W, rowH, 4, COL_HEADER);
        dibujarTexto(g, pinChipLabel, x + 8, y + 18, COL_TITULO);
        dibujarTexto(g, headerRight, x + col1W + 8, y + 18, COL_TITULO);
        for (int i = 0; i < col1.length; i++) {
            float ry = y + (i + 1) * rowH;
            int bg = (i % 2 == 0) ? COL_PANEL : COL_CARD;
            dibujarRectangulo(g, x, ry, col1W + col2W, rowH, bg);
            bordes(g, x, ry, col1W + col2W, rowH, 0xFF2A3142);
            configurarTexto(9f, true);
            dibujarTexto(g, col1[i], x + 8, ry + 18, COL_PIN_NUM);
            configurarTexto(8.5f, false);
            dibujarTexto(g, col2[i], x + col1W + 8, ry + 18, colorPin(col2[i]));
        }
    }

    private static void dibujarFilaInfo(Canvas g, float x, float y, String label, String value, int valColor) {
        configurarTexto(9.5f, true);
        dibujarTexto(g, label, x, y + 12, COL_TITULO);
        configurarTexto(8.5f, false);
        dibujarTexto(g, value, x + 4, y + 26, valColor);
    }

    private static void dibujarFlecha(Canvas g, float x, float y) {
        dibujarLinea(g, x, y - 14, x + 24, y, 0xFF90A4AE);
        dibujarLinea(g, x, y + 14, x + 24, y, 0xFF90A4AE);
        dibujarLinea(g, x, y, x + 24, y, 0xFF90A4AE);
    }

    private static void configurarTexto(float size, boolean negrita) {
        paint.setTextSize(size * 1.35f);
        paint.setTypeface(negrita ? Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) : Typeface.MONOSPACE);
    }

    private static void dibujarTexto(Canvas c, String text, float x, float y, int color) {
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        c.drawText(text, x, y, paint);
    }

    private static void dibujarRectangulo(Canvas c, float x, float y, float w, float h, int color) {
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        c.drawRect(x, y, x + w, y + h, paint);
    }

    private static void dibujarRectanguloRedondeado(Canvas c, float x, float y, float w, float h, float rx, int color) {
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        rectF.set(x, y, x + w, y + h);
        c.drawRoundRect(rectF, rx, rx, paint);
    }

    private static void dibujarLinea(Canvas c, float x1, float y1, float x2, float y2, int color) {
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.5f);
        c.drawLine(x1, y1, x2, y2, paint);
    }

    private static void bordes(Canvas g, float x, float y, float w, float h, int color) {
        dibujarLinea(g, x, y, x + w, y, color);
        dibujarLinea(g, x + w, y, x + w, y + h, color);
        dibujarLinea(g, x, y + h, x + w, y + h, color);
        dibujarLinea(g, x, y, x, y + h, color);
    }

    private static void bordesRedondeados(Canvas g, float x, float y, float w, float h, float rx, int color) {
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.5f);
        rectF.set(x, y, x + w, y + h);
        g.drawRoundRect(rectF, rx, rx, paint);
    }

    private static int colorPin(String pin) {
        if (pin == null) return COL_LABEL;
        String p = pin.toUpperCase();
        if (p.contains("VCC") || p.contains("VDD") || p.contains("3.3") || p.contains("5V") || p.contains("VTG")) return COL_VCC;
        if (p.contains("GND") || p.contains("VSS") || p.contains("TIERRA") || p.contains("MASA")) return COL_GND;
        if (p.contains("MOSI") || p.contains("DI") || p.contains("PGD") || p.contains("SDA") || p.contains("DATOS")) return COL_SIGNAL;
        if (p.contains("MISO") || p.contains("DO") || p.contains("SDO")) return COL_ACCENT;
        if (p.contains("CLK") || p.contains("SCK") || p.contains("PGC") || p.contains("SCL") || p.contains("RELOJ")) return COL_YELLOW;
        if (p.contains("CS") || p.contains("CE")) return COL_PURPLE;
        if (p.contains("VPP") || p.contains("MCLR") || p.contains("RESET") || p.contains("WP")) return COL_AVISO;
        if (p.contains("A") && p.length() <= 5) return 0xFF81D4FA; // Dirección
        if (p.contains("D") && p.length() <= 5) return 0xFFA5D6A7; // Datos
        return COL_LABEL;
    }

    private static void aplicar(Bitmap bmp, ImageView target) {
        target.setImageBitmap(bmp);
        target.setScaleType(ImageView.ScaleType.FIT_CENTER);
    }
}
