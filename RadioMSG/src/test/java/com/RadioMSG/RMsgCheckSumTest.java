package com.RadioMSG;

import org.junit.Test;
import static org.junit.Assert.*;

public class RMsgCheckSumTest {

    // --- bytesToHex ---

    @Test
    public void bytesToHex_emptyArray_returnsEmptyString() {
        assertEquals("", RMsgCheckSum.bytesToHex(new byte[]{}));
    }

    @Test
    public void bytesToHex_singleZeroByte() {
        assertEquals("00", RMsgCheckSum.bytesToHex(new byte[]{0x00}));
    }

    @Test
    public void bytesToHex_mixedBytes() {
        assertEquals("010aff", RMsgCheckSum.bytesToHex(new byte[]{0x01, 0x0A, (byte) 0xFF}));
    }

    // --- Crc16 ---

    @Test
    public void crc16_emptyString_returnsInitialCrcEncoded() {
        // No bytes processed: CRC stays 0xffff → hex "ffff", all chars >= 'a', no remapping
        assertEquals("ffff", RMsgCheckSum.Crc16(""));
    }

    @Test
    public void crc16_knownInput_returnsCorrectEncodedCrc() {
        // CRC-16/MODBUS of "123456789" = 0x4B37
        // Remapping: '4'→'k', 'b'→'b', '3'→'j', '7'→'n'
        assertEquals("kbjn", RMsgCheckSum.Crc16("123456789"));
    }

    @Test
    public void crc16_sameInputProducesSameOutput() {
        String a = RMsgCheckSum.Crc16("hello world");
        String b = RMsgCheckSum.Crc16("hello world");
        assertEquals(a, b);
    }

    @Test
    public void crc16_differentInputsProduceDifferentOutputs() {
        assertNotEquals(RMsgCheckSum.Crc16("message1"), RMsgCheckSum.Crc16("message2"));
    }

    @Test
    public void crc16_outputIsAlwaysFourChars() {
        assertEquals(4, RMsgCheckSum.Crc16("").length());
        assertEquals(4, RMsgCheckSum.Crc16("a").length());
        assertEquals(4, RMsgCheckSum.Crc16("hello world this is a longer string").length());
    }

    @Test
    public void crc16_outputContainsNoUpperCaseOrDigits() {
        // The encoding shifts digits up by 55 so output should only contain a-z
        String result = RMsgCheckSum.Crc16("test message 12345");
        assertTrue("CRC output should be all lowercase letters: " + result,
                result.matches("[a-z]{4}"));
    }

    // --- checkCrcWithPasswordAndTime (no-password and fixed-password paths) ---

    @Test
    public void checkCrc_noPassword_correctCrc_returnsTrue() {
        String message = "HELLO";
        String crc = RMsgCheckSum.Crc16(message);
        assertTrue(RMsgCheckSum.checkCrcWithPasswordAndTime(message, "", "irrelevant.txt", crc));
    }

    @Test
    public void checkCrc_noPassword_wrongCrc_returnsFalse() {
        assertFalse(RMsgCheckSum.checkCrcWithPasswordAndTime("HELLO", "", "irrelevant.txt", "zzzz"));
    }

    @Test
    public void checkCrc_fixedPassword_correctCrc_returnsTrue() {
        String message = "HELLO";
        String password = "mypassword";
        String crc = RMsgCheckSum.Crc16(message + password);
        assertTrue(RMsgCheckSum.checkCrcWithPasswordAndTime(message, password, "irrelevant.txt", crc));
    }

    @Test
    public void checkCrc_fixedPassword_wrongPassword_returnsFalse() {
        String message = "HELLO";
        String crc = RMsgCheckSum.Crc16(message + "rightpassword");
        assertFalse(RMsgCheckSum.checkCrcWithPasswordAndTime(message, "wrongpassword", "irrelevant.txt", crc));
    }

    @Test
    public void checkCrc_fixedPassword_notTreatedAsTimeBased() {
        // A password NOT starting with '_' must use the simple path, not time-based
        String message = "TEST";
        String password = "noUnderscore";
        String crc = RMsgCheckSum.Crc16(message + password);
        // Should pass regardless of filename content
        assertTrue(RMsgCheckSum.checkCrcWithPasswordAndTime(message, password, "anythinghere", crc));
    }
}
