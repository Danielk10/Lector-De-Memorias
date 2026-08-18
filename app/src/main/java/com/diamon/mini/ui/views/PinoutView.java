package com.diamon.mini.ui.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.widget.ImageView;

import com.diamon.mini.R;

/**
 * PinoutView: Dibuja diagramas de conexión y pinouts de hardware para
 * programadores MiniPro (TL866II+, TL866A/CS, T48, T56, CH341A) y chips soportados.
 */
public class PinoutView {

    // Paleta de colores Dark Theme
    private static final int COL_BG = 0xFF12141D;
    private static final int COL_HEADER = 0xFF0D47A1;
    private static final int COL_PANEL = 0xFF1C2234;
    private static final int COL_BORDE = 0xFF455A64;
    private static final int COL_LINEA = 0xFF607D8B;
    private static final int COL_TITULO = 0xFFECEFF1;
    private static final int COL_LABEL = 0xFFB0BEC5;
    private static final int COL_PIN_NUM = 0xFF80CBC4;
    private static final int COL_AVISO = 0xFFFF7043;
    private static final int COL_VCC = 0xFFEF5350;
    private static final int COL_GND = 0xFF78909C;
    private static final int COL_SIGNAL = 0xFF66BB6A;
    private static final int COL_CHIP = 0xFF1565C0;
    private static final int COL_CHIP_BODY = 0xFF21252B;
    private static final int COL_ACCENT = 0xFF29B6F6;
    private static final int COL_YELLOW = 0xFFFFCA28;
    private static final int COL_PURPLE = 0xFFCE93D8;

    private static final int W = 800;
    private static final int H = 560;

    private static final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ────────── 1. ZIF Socket 40 Pines TL866 / T48 / T56 ─────────────────────

    public static void dibujarZIF40(Context ctx, ImageView target) {
        Bitmap bmp = crearBitmap();
        Canvas canvas = new Canvas(bmp);
        dibujarHeader(canvas, ctx.getString(R.string.str_pinout_zif40_header));

        // Dibujar Socket ZIF 40 pines esquemático
        float zx = 50, zy = 55, zw = 180, zh = 440;
        dibujarRectangulo(canvas, zx, zy, zw, zh, COL_CHIP_BODY);
        bordes(canvas, zx, zy, zw, zh, COL_BORDE);

        // Palanca ZIF en la parte superior izquierda
        dibujarRectangulo(canvas, zx - 14, zy + 6, 12, 45, 0xFF78909C);
        dibujarRectangulo(canvas, zx - 22, zy + 2, 10, 16, 0xFFFFB74D); // Pomo palanca

        // Muesca superior
        paint.setColor(0xFF37474F);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(zx + zw / 2f, zy, 12, paint);

        configurarTexto(11f, true);
        dibujarTexto(canvas, "ZIF 40", zx + 65, zy + 26, COL_TITULO);

        // Pines del ZIF (20 a cada lado)
        float pinH = 14, pinW = 18, gap = 6.8f;
        for (int i = 0; i < 20; i++) {
            float py = zy + 35 + i * (pinH + gap);
            // Pines izquierdos: 1 a 20
            int pIzq = i + 1;
            dibujarRectangulo(canvas, zx - pinW, py, pinW, pinH, COL_PANEL);
            bordes(canvas, zx - pinW, py, pinW, pinH, COL_BORDE);
            configurarTexto(8.5f, false);
            dibujarTexto(canvas, String.valueOf(pIzq), zx - pinW + 3, py + pinH - 3, COL_PIN_NUM);

            // Pines derechos: 40 a 21
            int pDer = 40 - i;
            dibujarRectangulo(canvas, zx + zw, py, pinW, pinH, COL_PANEL);
            bordes(canvas, zx + zw, py, pinW, pinH, COL_BORDE);
            dibujarTexto(canvas, String.valueOf(pDer), zx + zw + 3, py + pinH - 3, COL_PIN_NUM);
        }

        // Panel de Alineación a la derecha
        float tx = 270, ty = 60, tw = 490, th = 430;
        dibujarRectangulo(canvas, tx, ty, tw, th, COL_PANEL);
        bordes(canvas, tx, ty, tw, th, COL_BORDE);

        configurarTexto(13f, true);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_universal_alignment), tx + 14, ty + 28, COL_YELLOW);

        configurarTexto(10.5f, false);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_dip_notch_up), tx + 14, ty + 60, COL_TITULO);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_dip_align_bottom), tx + 14, ty + 80, COL_TITULO);

        // Tabla de colocación
        float tby = ty + 105;
        dibujarFilaInfo(canvas, tx + 14, tby, "DIP8  (24xx, 25xx, 93xx):", "Pines 17-20 (Izq) y 21-24 (Der)", COL_SIGNAL);
        dibujarFilaInfo(canvas, tx + 14, tby + 35, "DIP16 (28xx, GAL16V8):", "Pines 13-20 (Izq) y 21-28 (Der)", COL_SIGNAL);
        dibujarFilaInfo(canvas, tx + 14, tby + 70, "DIP24 (27C64, GAL20V8):", "Pines 9-20 (Izq) y 21-32 (Der)", COL_SIGNAL);
        dibujarFilaInfo(canvas, tx + 14, tby + 105, "DIP28 (27C256, 28C256):", "Pines 7-20 (Izq) y 21-34 (Der)", COL_SIGNAL);
        dibujarFilaInfo(canvas, tx + 14, tby + 140, "DIP32 (27C512, 29F010):", "Pines 5-20 (Izq) y 21-36 (Der)", COL_SIGNAL);
        dibujarFilaInfo(canvas, tx + 14, tby + 175, "DIP40 (PIC, AVR, 27C400):", "Pines 1-20 (Izq) y 21-40 (Der - Completo)", COL_ACCENT);

        configurarTexto(10f, true);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_pin1_corner), tx + 14, ty + 335, COL_PIN_NUM);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_lock_lever), tx + 14, ty + 360, COL_LABEL);

        dibujarNota(canvas, ctx.getString(R.string.str_pinout_zif40_note));
        aplicar(bmp, target);
    }

    // ────────── 2. SPI Flash SOIC8 / SOP8 / DIP8 (25xxx) ─────────────────────

    public static void dibujarSPI25(Context ctx, ImageView target) {
        Bitmap bmp = crearBitmap();
        Canvas canvas = new Canvas(bmp);
        dibujarHeader(canvas, ctx.getString(R.string.str_pinout_spi25_header));

        canvas.save();
        canvas.translate(-10, 10);
        canvas.scale(1.4f, 1.4f);
        dibujarChipSOIC8(canvas, 75, 65, new String[] { "CS#", "DO", "WP#", "GND", "DI", "CLK", "HOLD#", "VCC" }, true, "25xxx SPI");
        dibujarFlecha(canvas, 275, 155);
        dibujarTablaConexion(canvas, 315, 60,
                "Flash 25xxx", "Función / MiniPro", "Pin",
                new String[] { "1 - CS#", "2 - DO (SO)", "3 - WP# / IO2", "4 - GND", "5 - DI (SI)", "6 - CLK", "7 - HOLD# / IO3", "8 - VCC" },
                new String[] { "Chip Select (Bajo activo)", "Data Out / MISO", "Write Protect (a VCC)", "Tierra / Masa (0V)", "Data In / MOSI", "Clock / Reloj SPI", "Hold / Reset (a VCC)", "Alimentación 3.3V" });
        canvas.restore();

        dibujarNota(canvas, ctx.getString(R.string.str_pinout_spi25_note));
        aplicar(bmp, target);
    }

    // ────────── 3. I2C EEPROM 24Cxx (24C01 - 24C1024) ────────────────────────

    public static void dibujarI2C24(Context ctx, ImageView target) {
        Bitmap bmp = crearBitmap();
        Canvas canvas = new Canvas(bmp);
        dibujarHeader(canvas, ctx.getString(R.string.str_pinout_i2c24_header));

        canvas.save();
        canvas.translate(-10, 10);
        canvas.scale(1.4f, 1.4f);
        dibujarChipSOIC8(canvas, 75, 65, new String[] { "A0", "A1", "A2", "GND", "SDA", "SCL", "WP", "VCC" }, true, "24Cxx I2C");
        dibujarFlecha(canvas, 275, 155);
        dibujarTablaConexion(canvas, 315, 60,
                "EEPROM 24Cxx", "Función / Conexión", "Pin",
                new String[] { "1 - A0", "2 - A1", "3 - A2", "4 - GND", "5 - SDA", "6 - SCL", "7 - WP", "8 - VCC" },
                new String[] { "Dirección Chip Bit 0", "Dirección Chip Bit 1", "Dirección Chip Bit 2", "Tierra / Masa (0V)", "Línea de Datos Serie", "Reloj I2C Serie", "Write Protect (GND=Escribir)", "Alimentación 1.8V / 3.3V / 5V" });
        canvas.restore();

        dibujarNota(canvas, ctx.getString(R.string.str_pinout_i2c24_note));
        aplicar(bmp, target);
    }

    // ────────── 4. Microwire EEPROM 93Cxx (93C46 - 93C86) ───────────────────

    public static void dibujarMicrowire93(Context ctx, ImageView target) {
        Bitmap bmp = crearBitmap();
        Canvas canvas = new Canvas(bmp);
        dibujarHeader(canvas, ctx.getString(R.string.str_pinout_mw93_header));

        canvas.save();
        canvas.translate(-10, 10);
        canvas.scale(1.4f, 1.4f);
        dibujarChipSOIC8(canvas, 75, 65, new String[] { "CS", "SK", "DI", "DO", "GND", "ORG", "NC", "VCC" }, true, "93Cxx uWire");
        dibujarFlecha(canvas, 275, 155);
        dibujarTablaConexion(canvas, 315, 60,
                "EEPROM 93Cxx", "Función / Microwire", "Pin",
                new String[] { "1 - CS", "2 - SK", "3 - DI", "4 - DO", "5 - GND", "6 - ORG", "7 - NC / PE", "8 - VCC" },
                new String[] { "Chip Select (Alto activo)", "Serial Clock / Reloj", "Serial Data Input", "Serial Data Output", "Tierra (0V)", "Organización (VCC=x16, GND=x8)", "Sin Conexión / Program Enable", "Alimentación 5V / 3.3V" });
        canvas.restore();

        dibujarNota(canvas, ctx.getString(R.string.str_pinout_mw93_note));
        aplicar(bmp, target);
    }

    // ────────── 5. Parallel Flash / EPROM DIP28 / DIP32 ───────────────────────

    public static void dibujarParallelDIP(Context ctx, ImageView target) {
        Bitmap bmp = crearBitmap();
        Canvas canvas = new Canvas(bmp);
        dibujarHeader(canvas, ctx.getString(R.string.str_pinout_parallel_header));

        float x = 40, y = 55, w = 720, h = 445;
        dibujarRectangulo(canvas, x, y, w, h, COL_PANEL);
        bordes(canvas, x, y, w, h, COL_BORDE);

        configurarTexto(13f, true);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_parallel_title), x + 16, y + 26, COL_YELLOW);

        // Tabla 2 columnas DIP32
        float colW = 340;
        configurarTexto(10f, false);
        dibujarRectangulo(canvas, x + 16, y + 42, colW, 20, COL_HEADER);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_left_pins), x + 24, y + 56, COL_TITULO);

        dibujarRectangulo(canvas, x + 360, y + 42, colW, 20, COL_HEADER);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_right_pins), x + 368, y + 56, COL_TITULO);

        String[] leftPins = {
                "1: VPP / A18 (Voltaje Prog. / Dir)",
                "2: A16 (Dirección 16)",
                "3: A15 (Dirección 15)",
                "4: A12 (Dirección 12)",
                "5: A7 (Dirección 7)",
                "6: A6 (Dirección 6)",
                "7: A5 (Dirección 5)",
                "8: A4 (Dirección 4)",
                "9: A3 (Dirección 3)",
                "10: A2 (Dirección 2)",
                "11: A1 (Dirección 1)",
                "12: A0 (Dirección 0)",
                "13: D0 (Bus de Datos 0)",
                "14: D1 (Bus de Datos 1)",
                "15: D2 (Bus de Datos 2)",
                "16: GND (Tierra / Masa)"
        };

        String[] rightPins = {
                "32: VCC (Alimentación 5V)",
                "31: /WE / A17 (Write Enable / Dir)",
                "30: NC / A17 (No Connect / Dir)",
                "29: A14 (Dirección 14)",
                "28: A13 (Dirección 13)",
                "27: A8 (Dirección 8)",
                "26: A9 (Dirección 9 - VPP en 27C)",
                "25: A11 (Dirección 11)",
                "24: /OE / VPP (Output Enable)",
                "23: A10 (Dirección 10)",
                "22: /CE (Chip Enable)",
                "21: D7 (Bus de Datos 7)",
                "20: D6 (Bus de Datos 6)",
                "19: D5 (Bus de Datos 5)",
                "18: D4 (Bus de Datos 4)",
                "17: D3 (Bus de Datos 3)"
        };

        for (int i = 0; i < 16; i++) {
            float ry = y + 66 + i * 22;
            int bg = (i % 2 == 0) ? COL_CHIP_BODY : COL_PANEL;
            dibujarRectangulo(canvas, x + 16, ry, colW, 20, bg);
            dibujarRectangulo(canvas, x + 360, ry, colW, 20, bg);

            configurarTexto(9.5f, false);
            dibujarTexto(canvas, leftPins[i], x + 22, ry + 14, colorPin(leftPins[i]));
            dibujarTexto(canvas, rightPins[i], x + 366, ry + 14, colorPin(rightPins[i]));
        }

        dibujarNota(canvas, ctx.getString(R.string.str_pinout_parallel_note));
        aplicar(bmp, target);
    }

    // ────────── 6. Header ICSP TL866 / T48 (6 Pines) ─────────────────────────

    public static void dibujarICSP(Context ctx, ImageView target) {
        Bitmap bmp = crearBitmap();
        Canvas canvas = new Canvas(bmp);
        dibujarHeader(canvas, ctx.getString(R.string.str_pinout_icsp_header));

        // Dibujar conector ICSP 6 pines (Vista frontal macho)
        float hx = 60, hy = 80;
        dibujarRectangulo(canvas, hx, hy, 160, 240, COL_CHIP_BODY);
        bordes(canvas, hx, hy, 160, 240, COL_BORDE);

        configurarTexto(12f, true);
        dibujarTexto(canvas, "PUERTO ICSP", hx + 25, hy + 26, COL_TITULO);
        configurarTexto(10f, false);
        dibujarTexto(canvas, "(TL866 / T48)", hx + 35, hy + 42, COL_LABEL);

        // 6 Pines en hilera vertical
        String[] icspPins = { "1 - VPP / MCLR", "2 - VCC (3.3V/5V)", "3 - GND (Tierra)", "4 - PGD / MOSI / SDI", "5 - PGC / SCK / CLK", "6 - AUX / MISO / SDO" };
        for (int i = 0; i < 6; i++) {
            float py = hy + 60 + i * 28;
            dibujarRectangulo(canvas, hx + 20, py, 24, 18, COL_PANEL);
            bordes(canvas, hx + 20, py, 24, 18, COL_BORDE);
            configurarTexto(9.5f, true);
            dibujarTexto(canvas, String.valueOf(i + 1), hx + 27, py + 13, COL_PIN_NUM);
            configurarTexto(9f, false);
            dibujarTexto(canvas, icspPins[i].substring(4), hx + 50, py + 13, colorPin(icspPins[i]));
        }

        // Tabla de aplicaciones a la derecha
        float tx = 260, ty = 75, tw = 500, th = 420;
        dibujarRectangulo(canvas, tx, ty, tw, th, COL_PANEL);
        bordes(canvas, tx, ty, tw, th, COL_BORDE);

        configurarTexto(12f, true);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_icsp_in_circuit), tx + 14, ty + 26, COL_YELLOW);

        configurarTexto(10f, false);
        dibujarFilaInfo(canvas, tx + 14, ty + 50, "Microchip PIC (ej. 16F, 18F):", "Pin 1→MCLR(VPP), 2→VDD, 3→VSS, 4→PGD, 5→PGC", COL_SIGNAL);
        dibujarFilaInfo(canvas, tx + 14, ty + 95, "Atmel / AVR (ej. ATmega, ATtiny):", "Pin 1→RESET, 2→VCC, 3→GND, 4→MOSI, 5→SCK, 6→MISO", COL_SIGNAL);
        dibujarFilaInfo(canvas, tx + 14, ty + 140, "SPI Flash en placa (25xxx):", "Pin 1→CS#, 2→VCC, 3→GND, 4→DI(MOSI), 5→CLK, 6→DO(MISO)", COL_ACCENT);

        configurarTexto(10f, true);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_icsp_precautions), tx + 14, ty + 205, COL_AVISO);
        configurarTexto(9.5f, false);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_icsp_warn_mclr), tx + 14, ty + 230, COL_LABEL);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_icsp_warn_vcc), tx + 14, ty + 252, COL_LABEL);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_icsp_warn_external), tx + 14, ty + 274, COL_LABEL);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_icsp_warn_length), tx + 14, ty + 296, COL_LABEL);

        dibujarNota(canvas, ctx.getString(R.string.str_pinout_icsp_note));
        aplicar(bmp, target);
    }

    // ────────── 7. AVR ISP 6 / 10 Pines ──────────────────────────────────────

    public static void dibujarAVRISP(Context ctx, ImageView target) {
        Bitmap bmp = crearBitmap();
        Canvas canvas = new Canvas(bmp);
        dibujarHeader(canvas, ctx.getString(R.string.str_pinout_avrisp_header));

        float x = 40, y = 55, w = 720, h = 445;
        dibujarRectangulo(canvas, x, y, w, h, COL_PANEL);
        bordes(canvas, x, y, w, h, COL_BORDE);

        configurarTexto(13f, true);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_avr_standard), x + 16, y + 26, COL_YELLOW);

        // Header 6 Pines
        float b1x = x + 20, b1y = y + 50, b1w = 320, b1h = 240;
        dibujarRectangulo(canvas, b1x, b1y, b1w, b1h, COL_CHIP_BODY);
        bordes(canvas, b1x, b1y, b1w, b1h, COL_BORDE);
        configurarTexto(11f, true);
        dibujarTexto(canvas, "AVR ISP 6-PIN (2x3 Header)", b1x + 14, b1y + 22, COL_TITULO);

        String[] p6 = { "1: MISO", "2: VCC (VTG)", "3: SCK", "4: MOSI", "5: RESET", "6: GND" };
        for (int i = 0; i < 6; i++) {
            float py = b1y + 40 + i * 30;
            configurarTexto(10f, false);
            dibujarTexto(canvas, p6[i], b1x + 20, py + 12, colorPin(p6[i]));
        }

        // Header 10 Pines
        float b2x = x + 360, b2y = y + 50, b2w = 340, b2h = 360;
        dibujarRectangulo(canvas, b2x, b2y, b2w, b2h, COL_CHIP_BODY);
        bordes(canvas, b2x, b2y, b2w, b2h, COL_BORDE);
        configurarTexto(11f, true);
        dibujarTexto(canvas, "AVR ISP 10-PIN (2x5 IDC Header)", b2x + 14, b2y + 22, COL_TITULO);

        String[] p10 = {
                "1: MOSI", "2: VCC (VTG)",
                "3: NC / LED", "4: GND",
                "5: /RESET", "6: GND",
                "7: SCK", "8: GND",
                "9: MISO", "10: GND"
        };
        for (int i = 0; i < 10; i++) {
            float py = b2y + 40 + i * 29;
            configurarTexto(10f, false);
            dibujarTexto(canvas, p10[i], b2x + 20, py + 12, colorPin(p10[i]));
        }

        configurarTexto(10f, false);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_avr_compatible), b1x + 4, y + 330, COL_LABEL);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_avr_use_icsp), b1x + 4, y + 355, COL_ACCENT);

        dibujarNota(canvas, ctx.getString(R.string.str_pinout_avrisp_note));
        aplicar(bmp, target);
    }

    // ────────── 8. PLCC32 a DIP32 Adapter ────────────────────────────────────

    public static void dibujarPLCC32(Context ctx, ImageView target) {
        Bitmap bmp = crearBitmap();
        Canvas canvas = new Canvas(bmp);
        dibujarHeader(canvas, ctx.getString(R.string.str_pinout_plcc32_header));

        canvas.save();
        canvas.translate(20, 20);

        // Cuadro PLCC32
        float px = 60, py = 60, pw = 200, ph = 200;
        dibujarRectangulo(canvas, px, py, pw, ph, COL_CHIP_BODY);
        bordes(canvas, px, py, pw, ph, COL_BORDE);

        // Chaflán de Pin 1 (Esquina superior centro-derecha o bisel)
        dibujarLinea(canvas, px + 8, py, px, py + 8, COL_AVISO);
        canvas.drawCircle(px + 20, py + 20, 4, paint);

        configurarTexto(11f, true);
        dibujarTexto(canvas, "PLCC32", px + 65, py + 95, COL_TITULO);
        configurarTexto(9f, false);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_top_view), px + 55, py + 115, COL_LABEL);

        // Mapeo explicativo
        float tx = 300, ty = 40, tw = 440, th = 440;
        dibujarRectangulo(canvas, tx, ty, tw, th, COL_PANEL);
        bordes(canvas, tx, ty, tw, th, COL_BORDE);

        configurarTexto(12f, true);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_plcc_adapter), tx + 14, ty + 26, COL_YELLOW);

        configurarTexto(10f, false);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_plcc_used_for), tx + 14, ty + 55, COL_TITULO);
        dibujarTexto(canvas, "• SST39SF010A / 020A / 040 (PLCC32)", tx + 14, ty + 78, COL_SIGNAL);
        dibujarTexto(canvas, "• W29EE011 / W49F002 (PLCC32)", tx + 14, ty + 100, COL_SIGNAL);
        dibujarTexto(canvas, "• 27C256 / 27C512 / 29C010 (PLCC32)", tx + 14, ty + 122, COL_SIGNAL);

        configurarTexto(10f, true);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_plcc_orientation), tx + 14, ty + 160, COL_ACCENT);
        configurarTexto(9.5f, false);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_plcc_step1), tx + 14, ty + 185, COL_LABEL);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_plcc_step2), tx + 14, ty + 230, COL_LABEL);
        dibujarTexto(canvas, ctx.getString(R.string.str_pinout_plcc_step3), tx + 14, ty + 275, COL_LABEL);

        canvas.restore();
        dibujarNota(canvas, ctx.getString(R.string.str_pinout_plcc32_note));
        aplicar(bmp, target);
    }

    // ────────── Helpers de dibujo ────────────────────────────────────────────

    private static Bitmap crearBitmap() {
        return Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
    }

    private static void dibujarHeader(Canvas g, String titulo) {
        g.drawColor(COL_BG);
        dibujarRectangulo(g, 0, 0, W, 38, COL_HEADER);
        configurarTexto(14f, true);
        dibujarTexto(g, titulo, 14, 25, COL_TITULO);
        dibujarLinea(g, 0, 38, W, 38, COL_BORDE);
    }

    private static void dibujarNota(Canvas g, String nota) {
        dibujarRectangulo(g, 0, H - 32, W, 32, 0xFF141722);
        dibujarLinea(g, 0, H - 32, W, H - 32, COL_AVISO);
        configurarTexto(10.5f, false);
        dibujarTexto(g, nota, 10, H - 12, COL_AVISO);
    }

    private static void dibujarChipSOIC8(Canvas g, float x, float y,
                                         String[] pines, boolean conPunto, String chipLabel) {
        float CW = 110, CH = 180;
        float pinW = 24, pinH = 16, gap = (CH - 4 * pinH) / 5f;

        dibujarRectangulo(g, x, y, CW, CH, COL_CHIP_BODY);
        bordes(g, x, y, CW, CH, COL_BORDE);

        // Muesca superior
        paint.setColor(0xFF37474F);
        paint.setStyle(Paint.Style.FILL);
        g.drawCircle(x + CW / 2f, y, 10, paint);

        if (conPunto) {
            paint.setColor(0xFFFFFFFF);
            paint.setStyle(Paint.Style.FILL);
            g.drawCircle(x + 12, y + 15, 4, paint);
        }

        configurarTexto(10f, true);
        dibujarTexto(g, chipLabel, x + 16, y + CH / 2f + 4, COL_ACCENT);

        for (int i = 0; i < 4; i++) {
            float py = y + gap + i * (pinH + gap);

            // Pines izquierdos 1..4
            dibujarRectangulo(g, x - pinW, py, pinW - 2, pinH - 2, COL_PANEL);
            dibujarLinea(g, x - pinW, py, x, py, COL_LINEA);
            dibujarLinea(g, x - pinW, py + pinH, x, py + pinH, COL_LINEA);
            dibujarLinea(g, x - pinW, py, x - pinW, py + pinH, COL_LINEA);
            configurarTexto(9.5f, false);
            dibujarTexto(g, String.valueOf(i + 1), x - pinW + 3, py + pinH - 3, COL_PIN_NUM);
            configurarTexto(8.5f, false);
            int col = colorPin(pines[i]);
            dibujarTexto(g, pines[i], x - pinW - 44, py + pinH - 3, col);

            // Pines derechos 8..5
            int ri = 7 - i;
            float prx = x + CW + 2;
            dibujarRectangulo(g, prx, py, pinW - 2, pinH - 2, COL_PANEL);
            dibujarLinea(g, prx, py, prx + pinW, py, COL_LINEA);
            dibujarLinea(g, prx, py + pinH, prx + pinW, py + pinH, COL_LINEA);
            dibujarLinea(g, prx + pinW, py, prx + pinW, py + pinH, COL_LINEA);
            configurarTexto(9.5f, false);
            dibujarTexto(g, String.valueOf(ri + 1), prx + 3, py + pinH - 3, COL_PIN_NUM);
            configurarTexto(8.5f, false);
            int colR = colorPin(pines[ri]);
            dibujarTexto(g, pines[ri], prx + pinW + 6, py + pinH - 3, colR);
        }
    }

    private static void dibujarTablaConexion(Canvas g, float x, float y,
                                             String headerLeft, String headerRight, String pinChipLabel,
                                             String[] col1, String[] col2) {
        float rowH = 22, col1W = 105, col2W = 195;
        configurarTexto(11f, true);
        dibujarTexto(g, headerLeft + " → " + headerRight, x, y - 6, COL_LABEL);
        dibujarRectangulo(g, x, y, col1W + col2W, rowH, COL_HEADER);
        dibujarTexto(g, pinChipLabel, x + 6, y + 15, COL_TITULO);
        dibujarTexto(g, headerRight, x + col1W + 6, y + 15, COL_TITULO);
        for (int i = 0; i < col1.length; i++) {
            float ry = y + (i + 1) * rowH;
            int bg = (i % 2 == 0) ? COL_PANEL : COL_BG;
            dibujarRectangulo(g, x, ry, col1W + col2W, rowH, bg);
            configurarTexto(9.5f, false);
            dibujarTexto(g, col1[i], x + 6, ry + 15, COL_PIN_NUM);
            dibujarTexto(g, col2[i], x + col1W + 6, ry + 15, colorPin(col2[i]));
        }
    }

    private static void dibujarFilaInfo(Canvas g, float x, float y, String label, String value, int valColor) {
        configurarTexto(10f, true);
        dibujarTexto(g, label, x, y + 14, COL_TITULO);
        configurarTexto(9.5f, false);
        dibujarTexto(g, value, x + 4, y + 28, valColor);
    }

    private static void dibujarFlecha(Canvas g, float x, float y) {
        dibujarLinea(g, x, y - 14, x + 24, y, 0xFF90A4AE);
        dibujarLinea(g, x, y + 14, x + 24, y, 0xFF90A4AE);
        dibujarLinea(g, x + 24, y, x + 24, y, 0xFF90A4AE);
    }

    private static void configurarTexto(float size, boolean negrita) {
        paint.setTextSize(size * 1.5f);
        paint.setTypeface(negrita ? Typeface.DEFAULT_BOLD : Typeface.MONOSPACE);
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

    private static int colorPin(String pin) {
        if (pin == null) return COL_LABEL;
        String p = pin.toUpperCase();
        if (p.contains("VCC") || p.contains("VDD") || p.contains("3.3") || p.contains("5V")) return COL_VCC;
        if (p.contains("GND") || p.contains("VSS") || p.contains("TIERRA") || p.contains("GROUND")) return COL_GND;
        if (p.contains("MOSI") || p.contains("DI") || p.contains("PGD") || p.contains("SDA")) return COL_SIGNAL;
        if (p.contains("MISO") || p.contains("DO") || p.contains("SDO")) return COL_ACCENT;
        if (p.contains("CLK") || p.contains("SCK") || p.contains("PGC") || p.contains("SCL")) return COL_YELLOW;
        if (p.contains("CS") || p.contains("CE")) return COL_PURPLE;
        if (p.contains("VPP") || p.contains("MCLR") || p.contains("RESET") || p.contains("WP")) return COL_AVISO;
        if (p.contains("A") && p.length() <= 4) return 0xFF81D4FA; // Dirección
        if (p.contains("D") && p.length() <= 4) return 0xFFA5D6A7; // Datos
        return COL_LABEL;
    }

    private static void aplicar(Bitmap bmp, ImageView target) {
        target.setImageBitmap(bmp);
        target.setScaleType(ImageView.ScaleType.FIT_CENTER);
    }
}
