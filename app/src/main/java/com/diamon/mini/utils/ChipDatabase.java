package com.diamon.mini.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Catálogo y gestor de dispositivos soportados por MiniPro / TL866 / T48 / T56.
 */
public class ChipDatabase {

    private static final String PREFS_CHIPS = "minipro_chip_prefs";
    private static final String KEY_CUSTOM_CHIPS = "custom_chips_list";
    private static final String KEY_RECENT_CHIPS = "recent_chips_list";

    public static final Map<String, List<String>> CHIP_CATEGORIES = new LinkedHashMap<>();

    static {        // 1. SPI Flash 25xxx
        CHIP_CATEGORIES.put("SPI Flash 25xxx", Arrays.asList(
                "W25Q128FV@SOIC8", "W25Q128JV@SOIC8", "W25Q64FV", "W25Q64JV", "W25Q32BV",
                "W25Q32JV", "W25Q16BV", "W25Q16JV@SOIC8", "W25Q80BV", "W25Q40BV", "W25X40V@SOIC8",
                "W25X80V@SOIC8", "MX25L6405D", "MX25L6406E@SOP8", "MX25L6436E@SOP8",
                "MX25L3205D@SOP8", "MX25L3206E", "MX25L1605D@SOP8", "MX25L1606E", "MX25L8005",
                "MX25L4005", "GD25Q64B", "GD25Q32B", "GD25Q16B", "GD25Q80B", "GD25Q40",
                "SST25VF032B", "SST25VF016B", "SST25VF080B", "SST25VF040B", "SST25VF020B",
                "SST25VF512A", "EN25F80", "EN25F16", "EN25Q32B@SOP8", "EN25Q64", "EN25T80",
                "N25Q064A@SOIC8", "N25Q128A@SOIC8", "M25P80@SOIC8", "M25P16@SOIC8", "M25P32@SOIC8",
                "M25P64@SOIC8", "AT25DF041A", "AT25DF081A", "AT25DF161", "AT25DF321",
                "AT25DF641@SOIC8"
        ));

        // 2. I2C EEPROM 24xxx
        CHIP_CATEGORIES.put("I2C EEPROM 24xxx", Arrays.asList(
                "AT24C01", "AT24C02", "AT24C04", "AT24C08", "AT24C16", "AT24C32", "AT24C64",
                "AT24C128", "AT24C256", "AT24C512", "AT24C1024", "24LC01B", "24LC02B", "24LC04B",
                "24LC08B", "24LC16B", "24LC32A", "24LC64", "24LC128", "24LC256", "24LC512",
                "24LC1025", "M24C01", "M24C02", "M24C04", "M24C08", "M24C16", "M24C32", "M24C64",
                "M24C128", "M24C256", "M24512"
        ));

        // 3. Microwire 93xxx
        CHIP_CATEGORIES.put("Microwire 93xxx", Arrays.asList(
                "93C46(x8)", "93C46(x16)", "93C56(x8)", "93C56(x16)", "93C66(x8)", "93C66(x16)",
                "93C76(x8)", "93C76(x16)", "93C86(x8)", "93C86(x16)", "93LC46B(x16)",
                "93LC56B(x16)", "93LC66B(x16)", "93LC76B(x16)", "93LC86B(x16)", "AT93C46(x8)",
                "AT93C46(x16)", "AT93C56(x8)", "AT93C56(x16)", "AT93C66(x8)", "AT93C66(x16)",
                "AT93C86(x8)", "AT93C86(x16)", "CAT93C46(x16)@SOIC8", "CAT93C56(x16)@SOIC8",
                "CAT93C66(x16)@SOIC8"
        ));

        // 4. SPI EEPROM 25Cxx y 95xxx
        CHIP_CATEGORIES.put("SPI EEPROM 25Cxx y 95xxx", Arrays.asList(
                "25LC010A", "25LC020A", "25LC040A", "25LC080A", "25LC160A", "25LC320", "25LC640",
                "25LC128", "25LC256", "25LC512", "25LC1024", "M95010", "M95020", "M95040",
                "M95080", "M95160", "M95320", "M95640", "M95128", "M95256", "M95512R@SOIC8",
                "M95M01R@SOIC8", "AT25010", "AT25020", "AT25040", "AT25080", "AT25160", "AT25320",
                "AT25640"
        ));

        // 5. Paralelo 27 / 28 / 29 / 39 / 49
        CHIP_CATEGORIES.put("Paralelo 27 / 28 / 29 / 39 / 49", Arrays.asList(
                "27C64@DIP28", "27C128@DIP28", "27C256@DIP28", "27C512@DIP28", "27C010@DIP32",
                "27C020@DIP32", "27C040@DIP32", "AM27C080@DIP32", "AT28C16@DIP24", "AT28C64",
                "AT28C256", "AT29C256@DIP28", "AT29C512", "AT29C010A", "AT29C020", "AT29C040A",
                "SST39SF010A", "SST39SF020A", "SST39SF040", "W27C512@DIP28", "W27E512@DIP28",
                "W49F002U", "W29EE011", "AM29F010B@DIP32", "AM29F040B@DIP32", "M27C256B@DIP28",
                "M27C512@DIP28", "M27C1001@DIP32", "M27C4001@DIP32"
        ));

        // 6. Microchip PIC
        CHIP_CATEGORIES.put("Microchip PIC", Arrays.asList(
                "PIC12F508", "PIC12F629", "PIC12F675", "PIC12F683", "PIC16F628A", "PIC16F84A",
                "PIC16F876A", "PIC16F877A", "PIC16F88", "PIC16F886", "PIC16F887", "PIC16F72",
                "PIC16F73", "PIC16F77", "PIC18F2550", "PIC18F4550", "PIC18F25K22@DIP28",
                "PIC18F45K22@DIP40", "PIC18F252", "PIC18F452"
        ));

        // 7. AVR / 8051
        CHIP_CATEGORIES.put("AVR / 8051", Arrays.asList(
                "ATmega8", "ATmega8A", "ATmega16", "ATmega16A", "ATmega32", "ATmega32A",
                "ATmega48", "ATmega88", "ATmega168", "ATmega328P", "ATtiny13", "ATtiny13A",
                "ATtiny25", "ATtiny45", "ATtiny85", "ATtiny2313", "ATtiny24", "ATtiny44",
                "AT89S51", "AT89S52", "AT89C51", "AT89C52", "AT89S8253"
        ));

        // 8. GAL / PLD
        CHIP_CATEGORIES.put("GAL / PLD", Arrays.asList(
                "GAL16V8", "GAL16V8A", "GAL16V8B", "GAL16V8D", "GAL20V8", "GAL20V8A", "GAL20V8B",
                "GAL20V8D", "GAL22V10", "GAL22V10B", "GAL22V10D", "ATF16V8B", "ATF20V8B",
                "ATF22V10C"
        ));

        // 9. SRAM / Logic Test
        CHIP_CATEGORIES.put("SRAM / Logic Test", Arrays.asList(
                "6264", "62256", "628128", "7400", "7402", "7404", "7408", "7432", "7474", "7486",
                "74138", "74139", "74157", "74161", "74245", "74373", "74595"
        ));
    }

    /**
     * Obtiene todos los chips del catálogo predefinido ordenados alfabéticamente.
     */
    public static List<String> getAllPredefinedChips() {
        Set<String> all = new HashSet<>();
        for (List<String> list : CHIP_CATEGORIES.values()) {
            all.addAll(list);
        }
        List<String> result = new ArrayList<>(all);
        Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
        return result;
    }

    /**
     * Obtiene la lista de chips personalizados agregados por el usuario.
     */
    public static List<String> getCustomChips(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_CHIPS, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_CUSTOM_CHIPS, "");
        if (raw.isEmpty()) return new ArrayList<>();
        List<String> list = new ArrayList<>(Arrays.asList(raw.split(",")));
        Collections.sort(list, String.CASE_INSENSITIVE_ORDER);
        return list;
    }

    /**
     * Agrega un nuevo chip personalizado a la base de datos local.
     */
    public static void addCustomChip(Context context, String chipName) {
        if (chipName == null || chipName.trim().isEmpty()) return;
        String cleanName = chipName.trim().toUpperCase();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_CHIPS, Context.MODE_PRIVATE);
        List<String> existing = getCustomChips(context);
        if (!existing.contains(cleanName)) {
            existing.add(cleanName);
            prefs.edit().putString(KEY_CUSTOM_CHIPS, String.join(",", existing)).apply();
        }
        addRecentChip(context, cleanName);
    }

    /**
     * Registra un chip en el historial de chips recientes.
     */
    public static void addRecentChip(Context context, String chipName) {
        if (chipName == null || chipName.trim().isEmpty()) return;
        String cleanName = chipName.trim().toUpperCase();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_CHIPS, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_RECENT_CHIPS, "");
        List<String> recents = new ArrayList<>();
        if (!raw.isEmpty()) {
            recents.addAll(Arrays.asList(raw.split(",")));
        }
        recents.remove(cleanName);
        recents.add(0, cleanName);
        while (recents.size() > 15) {
            recents.remove(recents.size() - 1);
        }
        prefs.edit().putString(KEY_RECENT_CHIPS, String.join(",", recents)).apply();
    }

    /**
     * Obtiene los chips recientes seleccionados o utilizados por el usuario.
     */
    public static List<String> getRecentChips(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_CHIPS, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_RECENT_CHIPS, "");
        if (raw.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(raw.split(",")));
    }
}
