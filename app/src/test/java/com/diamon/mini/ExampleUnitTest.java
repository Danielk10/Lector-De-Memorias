package com.diamon.mini;

import com.diamon.mini.utils.ChipDatabase;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for EEPROM Flasher core catalog and data models.
 */
public class ExampleUnitTest {

    @Test
    public void testChipDatabaseCatalogNotEmpty() {
        List<String> chips = ChipDatabase.getAllPredefinedChips();
        assertNotNull("Catalog of predefined chips should not be null", chips);
        assertTrue("Catalog should contain chips", chips.size() > 50);
        assertTrue("Catalog should contain W25Q128FV", chips.contains("W25Q128FV"));
        assertTrue("Catalog should contain AT24C02", chips.contains("AT24C02"));
        assertTrue("Catalog should contain ATmega328P", chips.contains("ATmega328P"));
    }

    @Test
    public void testChipCategories() {
        assertFalse("Chip categories map should not be empty", ChipDatabase.CHIP_CATEGORIES.isEmpty());
        assertTrue("Should contain SPI Flash category", ChipDatabase.CHIP_CATEGORIES.containsKey("SPI Flash (25xxx)"));
        assertTrue("Should contain I2C EEPROM category", ChipDatabase.CHIP_CATEGORIES.containsKey("I2C EEPROM (24xxx)"));
    }
}