package com.RadioMSG;

import org.junit.Test;
import static org.junit.Assert.*;

public class RMsgUtilTest {

    // --- Round ---

    @Test
    public void round_twoDecimalPlaces() {
        assertEquals(3.14, RMsgUtil.Round(3.14159, 2), 0.0001);
    }

    @Test
    public void round_zeroDecimalPlaces() {
        assertEquals(4.0, RMsgUtil.Round(3.7, 0), 0.0001);
    }

    @Test
    public void round_negativeValue() {
        assertEquals(-3.14, RMsgUtil.Round(-3.14159, 2), 0.0001);
    }

    @Test
    public void round_alreadyRounded() {
        assertEquals(5.0, RMsgUtil.Round(5.0, 2), 0.0001);
    }

    // --- extractDestination ---

    @Test
    public void extractDestination_aliasEqualsDestFormat_returnsDestination() {
        assertEquals("VK2ETA", RMsgUtil.extractDestination("RELAY=VK2ETA"));
    }

    @Test
    public void extractDestination_plainCallsign_returnsAsIs() {
        assertEquals("VK2ETA", RMsgUtil.extractDestination("VK2ETA"));
    }

    @Test
    public void extractDestination_aliasWithEmptyDestination_returnsOriginal() {
        // alias= with no dest: group(2) is "", so falls through and returns original
        assertEquals("alias=", RMsgUtil.extractDestination("alias="));
    }

    // --- extractAliasOnly ---

    @Test
    public void extractAliasOnly_aliasEqualsDestFormat_returnsAliasWithEquals() {
        assertEquals("RELAY=", RMsgUtil.extractAliasOnly("RELAY=VK2ETA"));
    }

    @Test
    public void extractAliasOnly_plainCallsign_returnsEmpty() {
        assertEquals("", RMsgUtil.extractAliasOnly("VK2ETA"));
    }

    // --- getDestinationFromAliasAndDest ---

    @Test
    public void getDestinationFromAliasAndDest_aliasFormat_returnsDestination() {
        assertEquals("VK2ETA", RMsgUtil.getDestinationFromAliasAndDest("RELAY=VK2ETA"));
    }

    @Test
    public void getDestinationFromAliasAndDest_noAlias_returnsOriginal() {
        assertEquals("VK2ETA", RMsgUtil.getDestinationFromAliasAndDest("VK2ETA"));
    }

    // --- stringsEqualsCaseUnsensitive ---

    @Test
    public void stringsEquals_sameCase_returnsTrue() {
        assertTrue(RMsgUtil.stringsEqualsCaseUnsensitive("hello", "hello"));
    }

    @Test
    public void stringsEquals_differentCase_returnsTrue() {
        assertTrue(RMsgUtil.stringsEqualsCaseUnsensitive("Hello", "hELLO"));
    }

    @Test
    public void stringsEquals_differentStrings_returnsFalse() {
        assertFalse(RMsgUtil.stringsEqualsCaseUnsensitive("hello", "world"));
    }

    // --- stringsStartsWithCaseUnsensitive ---

    @Test
    public void stringsStartsWith_matchingPrefix_returnsTrue() {
        assertTrue(RMsgUtil.stringsStartsWithCaseUnsensitive("HelloWorld", "hello"));
    }

    @Test
    public void stringsStartsWith_nonMatchingPrefix_returnsFalse() {
        assertFalse(RMsgUtil.stringsStartsWithCaseUnsensitive("HelloWorld", "world"));
    }

    // --- stringsContainsCaseUnsensitive ---

    @Test
    public void stringsContains_substringPresent_returnsTrue() {
        assertTrue(RMsgUtil.stringsContainsCaseUnsensitive("Alert: high temperature", "ALERT"));
    }

    @Test
    public void stringsContains_substringAbsent_returnsFalse() {
        assertFalse(RMsgUtil.stringsContainsCaseUnsensitive("normal message", "alert"));
    }
}
