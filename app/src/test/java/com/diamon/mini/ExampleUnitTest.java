package com.diamon.mini;

import com.diamon.mini.utils.ChipDatabase;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.*;

/**
 * Suite completa de pruebas unitarias para la lógica Java de la aplicación.
 */
public class ExampleUnitTest {

    @Test
    public void testChipDatabaseCatalogNotEmpty() {
        List<String> chips = ChipDatabase.getAllPredefinedChips();
        assertNotNull("Catalog of predefined chips should not be null", chips);
        assertTrue("Catalog should contain chips", chips.size() > 50);
        assertTrue("Catalog should contain W25Q128FV", chips.contains("W25Q128FV"));
        assertTrue("Catalog should contain 24LC02B", chips.contains("24LC02B"));
        assertTrue("Catalog should contain ATmega328P", chips.contains("ATmega328P"));
        assertTrue("Catalog should contain PIC16F628A", chips.contains("PIC16F628A"));
        assertTrue("Catalog should contain SST39SF040", chips.contains("SST39SF040"));
    }

    @Test
    public void testChipCategories() {
        assertFalse("Chip categories map should not be empty", ChipDatabase.CHIP_CATEGORIES.isEmpty());
        assertTrue(ChipDatabase.CHIP_CATEGORIES.containsKey("SPI Flash 25xxx"));
        assertTrue(ChipDatabase.CHIP_CATEGORIES.containsKey("I2C EEPROM 24xxx"));
        assertTrue(ChipDatabase.CHIP_CATEGORIES.containsKey("Microwire 93xxx"));
        assertTrue(ChipDatabase.CHIP_CATEGORIES.containsKey("SPI EEPROM 25Cxx y 95xxx"));
        assertTrue(ChipDatabase.CHIP_CATEGORIES.containsKey("Paralelo 27 / 28 / 29 / 39 / 49"));
        assertTrue(ChipDatabase.CHIP_CATEGORIES.containsKey("Microchip PIC"));
        assertTrue(ChipDatabase.CHIP_CATEGORIES.containsKey("AVR / 8051"));
        assertTrue(ChipDatabase.CHIP_CATEGORIES.containsKey("GAL / PLD"));
    }

    @Test
    public void testTypoNormalizationFilter() {
        // Simular búsqueda con error tipográfico O en lugar de 0
        String query = "24LCO2B";
        String queryNormalized = query.replace('O', '0');
        assertEquals("24LC02B", queryNormalized);

        List<String> allChips = ChipDatabase.getAllPredefinedChips();
        boolean matched = false;
        for (String chip : allChips) {
            String upper = chip.toUpperCase();
            if (upper.contains(query) || upper.contains(queryNormalized)) {
                matched = true;
                break;
            }
        }
        assertTrue("La búsqueda tolerante debe encontrar 24LC02B", matched);
    }

    @Test
    public void testProgrammerFlagMapping() {
        assertEquals("TL866II", mapProgrammer("TL866II+"));
        assertEquals("TL866A", mapProgrammer("TL866A"));
        assertEquals("TL866A", mapProgrammer("TL866CS"));
        assertEquals("T48", mapProgrammer("T48"));
        assertEquals("T56", mapProgrammer("T56"));
        assertEquals("T76", mapProgrammer("T76"));
    }

    private String mapProgrammer(String dev) {
        if (dev == null) return "TL866II";
        String d = dev.trim().toUpperCase();
        if (d.contains("II")) return "TL866II";
        if (d.contains("TL866A") || d.contains("TL866CS") || d.contains("CS") || d.contains("866A")) return "TL866A";
        if (d.contains("T48")) return "T48";
        if (d.contains("T56")) return "T56";
        if (d.contains("T76")) return "T76";
        return "TL866II";
    }

    @Test
    public void testCustomCommandParsing() {
        String raw = "minipro -p \"W25Q128FV\" -r \"my rom.bin\" -f ihex";
        String command = raw.trim();
        if (command.startsWith("minipro ")) {
            command = command.substring(8).trim();
        }

        List<String> args = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (char c : command.toCharArray()) {
            if (c == '"' || c == '\'') {
                inQuotes = !inQuotes;
            } else if (c == ' ' && !inQuotes) {
                if (current.length() > 0) {
                    args.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            args.add(current.toString());
        }

        assertEquals(6, args.size());
        assertEquals("-p", args.get(0));
        assertEquals("W25Q128FV", args.get(1));
        assertEquals("-r", args.get(2));
        assertEquals("my rom.bin", args.get(3));
        assertEquals("-f", args.get(4));
        assertEquals("ihex", args.get(5));
    }

    @Test
    public void testCommandFlagInjectionForDatabaseQueries() {
        List<String> args = new ArrayList<>();
        args.add("-L");
        args.add("24LC02B");

        boolean hasListFlag = false;
        boolean hasProgrammerFlag = false;
        for (String a : args) {
            if ("-l".equals(a) || "-L".equals(a) || "--list".equals(a) || "--search".equals(a)
                    || "-d".equals(a) || "--get_info".equals(a)) {
                hasListFlag = true;
            }
            if ("-q".equals(a) || "--programmer".equals(a)) {
                hasProgrammerFlag = true;
            }
        }

        if (hasListFlag && !hasProgrammerFlag) {
            args.add(0, "-q");
            args.add(1, "TL866II");
        }

        assertEquals("-q", args.get(0));
        assertEquals("TL866II", args.get(1));
        assertEquals("-L", args.get(2));
        assertEquals("24LC02B", args.get(3));
    }

    @Test
    public void testIntelHexParserLogic() throws Exception {
        String hexContent =
                ":10000000214601360121470136000E00010003158B\n" +
                ":1000100000000000000000000000000000000000E0\n" +
                ":00000001FF\n";

        byte[] hexBytes = hexContent.getBytes(StandardCharsets.UTF_8);
        Map<Integer, Byte> memoryMap = new TreeMap<>();
        int baseAddress = 0;
        int minAddr = Integer.MAX_VALUE;
        int maxAddr = Integer.MIN_VALUE;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(hexBytes)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.startsWith(":") || line.length() < 11) continue;

                int byteCount = Integer.parseInt(line.substring(1, 3), 16);
                int address = Integer.parseInt(line.substring(3, 7), 16);
                int recordType = Integer.parseInt(line.substring(7, 9), 16);

                if (recordType == 0x00) { // Data
                    for (int i = 0; i < byteCount; i++) {
                        int fullAddr = baseAddress + address + i;
                        int byteVal = Integer.parseInt(line.substring(9 + (i * 2), 11 + (i * 2)), 16);
                        memoryMap.put(fullAddr, (byte) byteVal);
                        if (fullAddr < minAddr) minAddr = fullAddr;
                        if (fullAddr > maxAddr) maxAddr = fullAddr;
                    }
                } else if (recordType == 0x01) { // EOF
                    break;
                } else if (recordType == 0x02) { // Extended Segment Address
                    baseAddress = Integer.parseInt(line.substring(9, 13), 16) << 4;
                } else if (recordType == 0x04) { // Extended Linear Address
                    baseAddress = Integer.parseInt(line.substring(9, 13), 16) << 16;
                }
            }
        }

        assertFalse("El mapa de memoria no debe estar vacío", memoryMap.isEmpty());
        assertEquals("Debe contener 32 bytes de datos", 32, memoryMap.size());
        assertEquals(0x0000, minAddr);
        assertEquals(0x001F, maxAddr);
        assertEquals((byte) 0x21, (byte) memoryMap.get(0));
        assertEquals((byte) 0x46, (byte) memoryMap.get(1));
    }

    @Test
    public void testTerminalCarriageReturnProgressSimulation() {
        List<StringBuilder> consoleLines = new ArrayList<>();
        int currentLineIndex = -1;
        boolean cursorAtStartOfLine = false;

        String[] chunks = {
                "Found TL866II+ 04.2.143\n",
                "Reading Code...   0%\r",
                "Reading Code...  50%\r",
                "Reading Code... 100% 1.2 Sec OK\n"
        };

        for (String text : chunks) {
            if (consoleLines.isEmpty()) {
                consoleLines.add(new StringBuilder());
                currentLineIndex = 0;
            }

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '\n') {
                    consoleLines.add(new StringBuilder());
                    currentLineIndex = consoleLines.size() - 1;
                    cursorAtStartOfLine = false;
                } else if (c == '\r') {
                    cursorAtStartOfLine = true;
                } else if (c == '\b') {
                    StringBuilder currentLine = consoleLines.get(currentLineIndex);
                    if (currentLine.length() > 0) {
                        currentLine.setLength(currentLine.length() - 1);
                    }
                } else {
                    StringBuilder currentLine = consoleLines.get(currentLineIndex);
                    if (cursorAtStartOfLine) {
                        currentLine.setLength(0);
                        cursorAtStartOfLine = false;
                    }
                    currentLine.append(c);
                }
            }
        }

        // Se tienen 2 líneas con contenido y 1 vacía preparada para el siguiente texto
        assertEquals(3, consoleLines.size());
        assertEquals("Found TL866II+ 04.2.143", consoleLines.get(0).toString());
        assertEquals("Reading Code... 100% 1.2 Sec OK", consoleLines.get(1).toString());
        assertEquals("", consoleLines.get(2).toString());
    }

    @Test
    public void testBinaryComparisonIdentical() {
        byte[] dataA = new byte[256];
        byte[] dataB = new byte[256];
        for (int i = 0; i < 256; i++) {
            dataA[i] = (byte) i;
            dataB[i] = (byte) i;
        }

        int maxLen = Math.max(dataA.length, dataB.length);
        int diffCount = 0;
        for (int i = 0; i < maxLen; i++) {
            byte a = i < dataA.length ? dataA[i] : (byte) 0xFF;
            byte b = i < dataB.length ? dataB[i] : (byte) 0xFF;
            if (a != b) diffCount++;
        }

        assertEquals(0, diffCount);
        double pct = (diffCount * 100.0) / maxLen;
        assertEquals(0.0, pct, 0.001);
    }

    @Test
    public void testBinaryComparisonWithDifferences() {
        byte[] dataA = new byte[]{0x10, 0x20, 0x30, 0x40, 0x50};
        byte[] dataB = new byte[]{0x10, (byte) 0xAA, 0x30, (byte) 0xBB, 0x50};

        int maxLen = Math.max(dataA.length, dataB.length);
        int diffCount = 0;
        for (int i = 0; i < maxLen; i++) {
            byte a = i < dataA.length ? dataA[i] : (byte) 0xFF;
            byte b = i < dataB.length ? dataB[i] : (byte) 0xFF;
            if (a != b) diffCount++;
        }

        assertEquals(2, diffCount);
        double pct = (diffCount * 100.0) / maxLen;
        assertEquals(40.0, pct, 0.001);
    }

    @Test
    public void testBinaryComparisonDifferentSizesWithPadding() {
        // dataA es de 4 bytes, dataB es de 6 bytes con relleno 0xFF
        byte[] dataA = new byte[]{0x01, 0x02, 0x03, 0x04};
        byte[] dataB = new byte[]{0x01, 0x02, 0x03, 0x04, (byte) 0xFF, (byte) 0xFF};

        int maxLen = Math.max(dataA.length, dataB.length);
        int diffCount = 0;
        for (int i = 0; i < maxLen; i++) {
            byte a = i < dataA.length ? dataA[i] : (byte) 0xFF;
            byte b = i < dataB.length ? dataB[i] : (byte) 0xFF;
            if (a != b) diffCount++;
        }

        // Ya que los bytes faltantes en dataA se asumen 0xFF, coinciden con dataB
        assertEquals(0, diffCount);

        // Si dataB tiene un byte distinto de 0xFF al final:
        dataB[5] = 0x00;
        diffCount = 0;
        for (int i = 0; i < maxLen; i++) {
            byte a = i < dataA.length ? dataA[i] : (byte) 0xFF;
            byte b = i < dataB.length ? dataB[i] : (byte) 0xFF;
            if (a != b) diffCount++;
        }
        assertEquals(1, diffCount);
        assertEquals(100.0 / 6.0, (diffCount * 100.0) / maxLen, 0.001);
    }

    @Test
    public void testBinaryComparisonRowDiffFormatting() {
        byte[] dataA = new byte[]{
                'H', 'E', 'L', 'L', 'O', ' ', 'W', 'O', 'R', 'L', 'D', '!', 0x00, 0x01, 0x02, 0x03
        };
        byte[] dataB = new byte[]{
                'H', 'E', 'L', 'P', 'O', ' ', 'W', 'O', 'R', 'K', 'D', '!', 0x00, 0x01, 0x02, 0x03
        };

        int maxLen = Math.max(dataA.length, dataB.length);
        int rowStart = 0;
        int length = Math.min(16, maxLen - rowStart);

        StringBuilder hexBuilder = new StringBuilder();
        StringBuilder asciiBuilder = new StringBuilder();
        boolean rowHasDiff = false;

        for (int i = 0; i < 16; i++) {
            if (i < length) {
                int idx = rowStart + i;
                byte a = idx < dataA.length ? dataA[idx] : (byte) 0xFF;
                byte b = idx < dataB.length ? dataB[idx] : (byte) 0xFF;
                boolean isDiff = (a != b);
                if (isDiff) rowHasDiff = true;

                if (isDiff) {
                    hexBuilder.append(String.format("%02X→%02X ", a & 0xFF, b & 0xFF));
                } else {
                    hexBuilder.append(String.format("%02X     ", a & 0xFF));
                }

                if (a >= 32 && a <= 126) {
                    asciiBuilder.append((char) a);
                } else {
                    asciiBuilder.append(".");
                }
            } else {
                hexBuilder.append("       ");
                asciiBuilder.append(" ");
            }
        }

        assertTrue("La fila debe detectar diferencias", rowHasDiff);
        assertEquals("HELLO WORLD!....", asciiBuilder.toString());
        assertTrue(hexBuilder.toString().contains("4C→50")); // 'L' -> 'P'
        assertTrue(hexBuilder.toString().contains("4C→4B")); // 'L' -> 'K'
    }
}