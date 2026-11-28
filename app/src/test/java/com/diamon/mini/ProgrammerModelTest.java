package com.diamon.mini;

import com.diamon.mini.core.ProgrammerModel;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/** Unit tests for the "minipro -k" programmer probe parser. */
public class ProgrammerModelTest {

    @Test
    public void parsesTl866ii() {
        assertEquals("TL866II+", ProgrammerModel.fromMiniproProbe("tl866ii: TL866II+\n"));
    }

    @Test
    public void parsesT48() {
        assertEquals("T48", ProgrammerModel.fromMiniproProbe("t48: T48\n"));
    }

    @Test
    public void parsesT56AndT76() {
        assertEquals("T56", ProgrammerModel.fromMiniproProbe("t56: T56\n"));
        assertEquals("T76", ProgrammerModel.fromMiniproProbe("t76: T76\n"));
    }

    @Test
    public void parsesTl866aAndCs() {
        assertEquals("TL866A", ProgrammerModel.fromMiniproProbe("tl866a: TL866A\n"));
        assertEquals("TL866CS", ProgrammerModel.fromMiniproProbe("tl866a: TL866CS\n"));
    }

    @Test
    public void ignoresNoiseAndMissingProgrammer() {
        assertNull(ProgrammerModel.fromMiniproProbe("[No programmer found]\n"));
        assertNull(ProgrammerModel.fromMiniproProbe("Found TL866II+ 04.2.143 (0x28f)\n"));
        assertNull(ProgrammerModel.fromMiniproProbe(null));
        assertNull(ProgrammerModel.fromMiniproProbe(""));
    }

    @Test
    public void handlesWindowsLineEndingsAndLeadingSpaces() {
        assertEquals("T48", ProgrammerModel.fromMiniproProbe("  t48: T48\r\n"));
    }
}
