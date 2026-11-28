package com.diamon.mini.core;

import java.util.Locale;

/**
 * Parses the output of {@code minipro -k} (print connected programmer).
 *
 * <p>The TL866II+, T48 and T56 share the same USB VID:PID (a466:0a53), so the
 * VID/PID alone cannot tell them apart. minipro reads the programmer type from
 * the firmware status packet and prints one of these lines:</p>
 *
 * <pre>
 * tl866a: TL866A
 * tl866a: TL866CS
 * tl866ii: TL866II+
 * t48: T48
 * t56: T56
 * t76: T76
 * </pre>
 */
public final class ProgrammerModel {

    private ProgrammerModel() {
    }

    /**
     * @param output raw stdout/stderr captured from {@code minipro -k}
     * @return the canonical programmer name used by the app spinner, or null
     *         when the output does not identify a supported programmer.
     */
    public static String fromMiniproProbe(String output) {
        if (output == null) {
            return null;
        }
        for (String rawLine : output.split("\\r?\\n")) {
            String line = rawLine.trim().toLowerCase(Locale.US);
            if (line.startsWith("tl866a:")) {
                return line.contains("tl866cs") ? "TL866CS" : "TL866A";
            }
            if (line.startsWith("tl866ii:")) {
                return "TL866II+";
            }
            if (line.startsWith("t48:")) {
                return "T48";
            }
            if (line.startsWith("t56:")) {
                return "T56";
            }
            if (line.startsWith("t76:")) {
                return "T76";
            }
        }
        return null;
    }
}
