package com.diamon.mini;

import com.diamon.mini.binary.DiffAnalyzer;
import com.diamon.mini.binary.IntegrityManager;
import com.diamon.mini.binary.PatchTransaction;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Unit tests for the new Pro binary engines. */
public class ProEnginesTest {

    @Test
    public void integrityManagerMatchesKnownVectors() {
        byte[] data = "abc".getBytes(StandardCharsets.UTF_8);
        IntegrityManager.HashResult hashes = IntegrityManager.computeHashes(data);

        assertEquals("900150983cd24fb0d6963f7d28e17f72", hashes.getMd5());
        assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d", hashes.getSha1());
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                hashes.getSha256());
        assertEquals("352441c2", hashes.getCrc32());
        assertEquals(3L, hashes.getByteCount());
    }

    @Test
    public void diffAnalyzerDetectsSectorsAndBoundaries() {
        byte[] a = new byte[5000];
        byte[] b = new byte[5000];
        b[0] = 1;
        b[4096] = 2;

        DiffAnalyzer.Result result = DiffAnalyzer.analyze(a, b);
        assertEquals(2L, result.getDiffCount());
        assertEquals(0L, result.getFirstDiff());
        assertEquals(4096L, result.getLastDiff());
        assertEquals(2, result.getTotalSectors());
        assertEquals(2, result.getChangedSectorCount());
        assertEquals(0L, result.getChangedSectors().get(0).getStart());
        assertEquals(1, result.getChangedSectors().get(0).getDiffCount());
        assertEquals(4096L, result.getChangedSectors().get(1).getStart());
        assertEquals(1, result.getChangedSectors().get(1).getDiffCount());
        assertEquals(2.0 / 5000.0 * 100.0, result.getPercent(), 0.0001);

        DiffAnalyzer.Result identical = DiffAnalyzer.analyze(a, a.clone());
        assertTrue(identical.isIdentical());
        assertEquals(0L, identical.getDiffCount());
    }

    @Test
    public void patchTransactionUndoRedoAndApply() {
        byte[] source = new byte[]{0x00, 0x01, 0x02, 0x03};
        PatchTransaction transaction = new PatchTransaction();

        assertTrue(transaction.modifyByte(1, source[1], (byte) 0xAA));
        assertEquals((byte) 0xAA, transaction.getByte(1, source[1]));
        assertEquals(1, transaction.getModifiedCount());
        assertTrue(transaction.canUndo());
        assertFalse(transaction.canRedo());

        byte[] patched = transaction.applyTo(source);
        assertArrayEquals(new byte[]{0x00, (byte) 0xAA, 0x02, 0x03}, patched);
        // The source array must remain untouched.
        assertEquals(0x01, source[1]);

        assertTrue(transaction.undo());
        assertEquals(0x01, transaction.getByte(1, source[1]));
        assertTrue(transaction.canRedo());

        assertTrue(transaction.redo());
        assertEquals((byte) 0xAA, transaction.getByte(1, source[1]));

        // Writing the original value back removes the active patch.
        assertTrue(transaction.modifyByte(1, source[1], source[1]));
        assertEquals(0, transaction.getModifiedCount());
    }
}
