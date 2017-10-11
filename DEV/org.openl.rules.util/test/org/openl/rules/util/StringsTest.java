package org.openl.rules.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.openl.rules.util.Strings.*;

import org.junit.Test;

public class StringsTest {

    @Test
    public void testContains() {
        // String
        assertFalse(contains(null, null));
        assertFalse(contains(null, ""));
        assertFalse(contains(null, "s"));
        assertFalse(contains("", null));
        assertTrue(contains("", ""));
        assertFalse(contains("", "s"));
        assertFalse(contains("asd", null));
        assertTrue(contains("asd", ""));
        assertTrue(contains("asd", "a"));
        assertTrue(contains("asd", "s"));
        assertTrue(contains("asd", "d"));
        assertTrue(contains("Testing string value", "string"));
        assertFalse(contains("Testing string value", "strong"));

        // char
        assertFalse(contains(null, 's'));
        assertFalse(contains("", 's'));
        assertTrue(contains("asd", 'a'));
        assertTrue(contains("asd", 's'));
        assertTrue(contains("asd", 'd'));
        assertFalse(contains("asd", 'f'));
    }

    @Test
    public void testContainsAny() {
        // String
        assertFalse(containsAny(null, (String) null));
        assertFalse(containsAny(null, ""));
        assertFalse(containsAny(null, "s"));
        assertFalse(containsAny("", (String) null));
        assertFalse(containsAny("", ""));
        assertFalse(containsAny("", "s"));
        assertFalse(containsAny("asd", (String) null));
        assertFalse(containsAny("asd", ""));
        assertTrue(containsAny("asd", "a"));
        assertTrue(containsAny("asd", "s"));
        assertTrue(containsAny("asd", "d"));
        assertFalse(containsAny("asd", "f"));
        assertTrue(containsAny("asd", "edc"));
        assertFalse(containsAny("asd", "qwe"));
        assertTrue(containsAny("Testing string value", "string"));
        assertTrue(containsAny("Testing string value", "strong"));

        // char
        assertFalse(containsAny(null));
        assertFalse(containsAny(null, (char[]) null));
        assertFalse(containsAny(null, new char[0]));
        assertFalse(containsAny(null, 's'));
        assertFalse(containsAny("", (char[]) null));
        assertFalse(containsAny("", new char[0]));
        assertFalse(containsAny("", 's'));
        assertFalse(containsAny("asd", (char[]) null));
        assertFalse(containsAny("asd", new char[0]));
        assertTrue(containsAny("asd", 'a'));
        assertTrue(containsAny("asd", 's'));
        assertTrue(containsAny("asd", 'd'));
        assertFalse(containsAny("asd", 'f'));
        assertTrue(containsAny("asd", 'e', 'd', 'c'));
        assertFalse(containsAny("asd", 'q', 'w', 'e'));
        assertTrue(containsAny("Testing string value", 's', 'i', 'g'));
        assertTrue(containsAny("Testing string value", 's', 'o', 'g'));
    }

    @Test
    public void testIsEmpty() {
        assertTrue(isEmpty(null));
        assertTrue(isEmpty(""));
        assertTrue(isEmpty(" "));
        assertFalse(isEmpty("  str  "));
        assertFalse(isEmpty("str"));
    }

    @Test
    public void testIsNotEmpty() {
        assertFalse(isNotEmpty(null));
        assertFalse(isNotEmpty(""));
        assertFalse(isNotEmpty(" "));
        assertTrue(isNotEmpty("  str  "));
        assertTrue(isNotEmpty("str"));
    }

    @Test
    public void testStartsWith() {
        assertTrue(startsWith(null, null));
        assertFalse(startsWith(null, ""));
        assertFalse(startsWith(null, "asd"));
        assertFalse(startsWith("", null));
        assertTrue(startsWith("", ""));
        assertFalse(startsWith("", "asd"));
        assertFalse(startsWith("asd", null));
        assertTrue(startsWith("asd", ""));
        assertTrue(startsWith("asd", "a"));
        assertFalse(startsWith("asd", "s"));
        assertFalse(startsWith("asd", "d"));
        assertTrue(startsWith("asd", "asd"));
        assertTrue(startsWith("asd", "as"));
        assertFalse(startsWith("asd", "sd"));
        assertFalse(startsWith("asd", "asdf"));
    }

    @Test
    public void testEndWith() {
        assertTrue(endsWith(null, null));
        assertFalse(endsWith(null, ""));
        assertFalse(endsWith(null, "asd"));
        assertFalse(endsWith("", null));
        assertTrue(endsWith("", ""));
        assertFalse(endsWith("", "asd"));
        assertFalse(endsWith("asd", null));
        assertTrue(endsWith("asd", ""));
        assertFalse(endsWith("asd", "a"));
        assertFalse(endsWith("asd", "s"));
        assertTrue(endsWith("asd", "d"));
        assertTrue(endsWith("asd", "asd"));
        assertFalse(endsWith("asd", "as"));
        assertTrue(endsWith("asd", "sd"));
        assertFalse(endsWith("asd", "asdf"));
    }

    @Test
    public void testSubstring() {
        assertNull(substring(null, -1));
        assertNull(substring(null, 0));
        assertNull(substring(null, 1));
        assertEquals("", substring("", -1));
        assertEquals("", substring("", 0));
        assertEquals("", substring("", 1));
        assertEquals("asd", substring("asd", -4));
        assertEquals("asd", substring("asd", -3));
        assertEquals("sd", substring("asd", -2));
        assertEquals("d", substring("asd", -1));
        assertEquals("asd", substring("asd", 0));
        assertEquals("sd", substring("asd", 1));
        assertEquals("d", substring("asd", 2));
        assertEquals("", substring("asd", 3));
        assertEquals("", substring("asd", 4));
    }

    @Test
    public void testSubstringWithEnd() {
        assertNull(substring(null, -1, 0));
        assertNull(substring(null, 0, 1));
        assertNull(substring(null, 1, -1));
        assertEquals("", substring("", -1, 0));
        assertEquals("", substring("", 0, 1));
        assertEquals("", substring("", 1, -1));
        assertEquals("", substring("asd", -5, 0));
        assertEquals("", substring("asd", 0, -5));
        assertEquals("", substring("asd", 5, 0));
        assertEquals("asd", substring("asd", 0, 5));

        assertEquals("", substring("asd", -3, -3));
        assertEquals("a", substring("asd", -3, -2));
        assertEquals("as", substring("asd", -3, -1));
        assertEquals("", substring("asd", -3, 0));
        assertEquals("a", substring("asd", -3, 1));
        assertEquals("as", substring("asd", -3, 2));
        assertEquals("asd", substring("asd", -3, 3));

        assertEquals("", substring("asd", -2, -3));
        assertEquals("", substring("asd", -2, -2));
        assertEquals("s", substring("asd", -2, -1));
        assertEquals("", substring("asd", -2, 0));
        assertEquals("", substring("asd", -2, 1));
        assertEquals("s", substring("asd", -2, 2));
        assertEquals("sd", substring("asd", -2, 3));

        assertEquals("", substring("asd", -1, -3));
        assertEquals("", substring("asd", -1, -2));
        assertEquals("", substring("asd", -1, -1));
        assertEquals("", substring("asd", -1, 0));
        assertEquals("", substring("asd", -1, 1));
        assertEquals("", substring("asd", -1, 2));
        assertEquals("d", substring("asd", -1, 3));

        assertEquals("", substring("asd", 0, -3));
        assertEquals("a", substring("asd", 0, -2));
        assertEquals("as", substring("asd", 0, -1));
        assertEquals("", substring("asd", 0, 0));
        assertEquals("a", substring("asd", 0, 1));
        assertEquals("as", substring("asd", 0, 2));
        assertEquals("asd", substring("asd", 0, 3));

        assertEquals("", substring("asd", 1, -3));
        assertEquals("", substring("asd", 1, -2));
        assertEquals("s", substring("asd", 1, -1));
        assertEquals("", substring("asd", 1, 0));
        assertEquals("", substring("asd", 1, 1));
        assertEquals("s", substring("asd", 1, 2));
        assertEquals("sd", substring("asd", 1, 3));

        assertEquals("", substring("asd", 2, -3));
        assertEquals("", substring("asd", 2, -2));
        assertEquals("", substring("asd", 2, -1));
        assertEquals("", substring("asd", 2, 0));
        assertEquals("", substring("asd", 2, 1));
        assertEquals("", substring("asd", 2, 2));
        assertEquals("d", substring("asd", 2, 3));

        assertEquals("", substring("asd", 3, -3));
        assertEquals("", substring("asd", 3, -2));
        assertEquals("", substring("asd", 3, -1));
        assertEquals("", substring("asd", 3, 0));
        assertEquals("", substring("asd", 3, 1));
        assertEquals("", substring("asd", 3, 2));
        assertEquals("", substring("asd", 3, 3));
    }

    @Test
    public void testRemoveStart() {
        assertNull(removeStart(null, null));
        assertNull(removeStart(null, ""));
        assertNull(removeStart(null, "asd"));
        assertEquals("", removeStart("", null));
        assertEquals("", removeStart("", ""));
        assertEquals("", removeStart("", "asd"));
        assertEquals("asd", removeStart("asd", null));
        assertEquals("asd", removeStart("asd", ""));
        assertEquals("sd", removeStart("asd", "a"));
        assertEquals("asd", removeStart("asd", "s"));
        assertEquals("asd", removeStart("asd", "d"));
        assertEquals("", removeStart("asd", "asd"));
        assertEquals("d", removeStart("asd", "as"));
        assertEquals("asd", removeStart("asd", "sd"));
        assertEquals("asd", removeStart("asd", "asdf"));
    }

    @Test
    public void testRemoveEnd() {
        assertNull(removeEnd(null, null));
        assertNull(removeEnd(null, ""));
        assertNull(removeEnd(null, "asd"));
        assertEquals("", removeEnd("", null));
        assertEquals("", removeEnd("", ""));
        assertEquals("", removeEnd("", "asd"));
        assertEquals("asd", removeEnd("asd", null));
        assertEquals("asd", removeEnd("asd", ""));
        assertEquals("asd", removeEnd("asd", "a"));
        assertEquals("asd", removeEnd("asd", "s"));
        assertEquals("as", removeEnd("asd", "d"));
        assertEquals("", removeEnd("asd", "asd"));
        assertEquals("asd", removeEnd("asd", "as"));
        assertEquals("a", removeEnd("asd", "sd"));
        assertEquals("asd", removeEnd("asd", "asdf"));
    }

    @Test
    public void testLowerCase() {
        assertNull(lowerCase(null));
        assertEquals("", lowerCase(""));
        assertEquals(" ", lowerCase(" "));
        assertEquals("  asd  ", lowerCase("  asd  "));
        assertEquals("asd", lowerCase("asd"));
        assertEquals("  asd  ", lowerCase("  AsD  "));
        assertEquals("asd", lowerCase("ASD"));
    }

    @Test
    public void testUpperCase() {
        assertNull(upperCase(null));
        assertEquals("", upperCase(""));
        assertEquals(" ", upperCase(" "));
        assertEquals("  ASD  ", upperCase("  asd  "));
        assertEquals("ASD", upperCase("asd"));
        assertEquals("  ASD  ", upperCase("  AsD  "));
        assertEquals("ASD", upperCase("ASD"));
    }

    @Test
    public void testReplace() {
        assertNull(replace(null, null, null));
        assertNull(replace(null, "", ""));
        assertNull(replace(null, "asd", "xyz"));
        assertEquals("", replace("", null, null));
        assertEquals("", replace("", "", ""));
        assertEquals("", replace("", "asd", "xyz"));
        assertEquals("asd", replace("asd", null, null));
        assertEquals("asd", replace("asd", "", ""));
        assertEquals("asd", replace("asd", "asd", null));

        assertEquals("xyz", replace("asd", "asd", "xyz"));
        assertEquals("qxyzf", replace("qasdf", "asd", "xyz"));
        assertEquals("qf", replace("qasdf", "asd", ""));
        assertEquals("qasdf", replace("qasdf", "qsf", ""));
        assertEquals("xyzxyz", replace("asdasd", "asd", "xyz"));
        assertEquals("qxyzfxyzg", replace("qasdfasdg", "asd", "xyz"));
        assertEquals("qfg", replace("qasdfasdg", "asd", ""));
        assertEquals("qasdfasdg", replace("qasdfasdg", "qsf", ""));
    }

    @Test
    public void testReplaceMax() {
        assertNull(replace(null, null, null, 1));
        assertNull(replace(null, "", "", 1));
        assertNull(replace(null, "asd", "xyz", 1));
        assertEquals("", replace("", null, null, 1));
        assertEquals("", replace("", "", "", 1));
        assertEquals("", replace("", "asd", "xyz", 1));
        assertEquals("asd", replace("asd", null, null, 1));
        assertEquals("asd", replace("asd", "", "", 1));
        assertEquals("asd", replace("asd", "asd", null, 1));
        assertEquals("asd", replace("asd", "asd", "xyz", 0));

        assertEquals("xyz", replace("asd", "asd", "xyz", 1));
        assertEquals("qxyzf", replace("qasdf", "asd", "xyz", 1));
        assertEquals("qf", replace("qasdf", "asd", "", 1));
        assertEquals("qasdf", replace("qasdf", "qsf", "", 1));
        assertEquals("xyzasd", replace("asdasd", "asd", "xyz", 1));
        assertEquals("qxyzfasdg", replace("qasdfasdg", "asd", "xyz", 1));
        assertEquals("qfasdg", replace("qasdfasdg", "asd", "", 1));
        assertEquals("qasdfasdg", replace("qasdfasdg", "qsf", "", 1));
    }

    @Test
    public void testConcatenate() {
        assertNull(concatenate(null));
        assertNull(concatenate(new Object[0]));
        assertNull(concatenate(null));
        assertNull(concatenate(null, null));
        assertEquals("", concatenate(null, "", null));
        assertEquals("", concatenate(""));
        assertEquals(" ", concatenate(" "));
        assertEquals("  asd  ", concatenate("  asd  "));
        assertEquals("asd", concatenate("asd"));
        assertEquals("  AsD  ", concatenate("  AsD  "));
        assertEquals("1", concatenate(1));
        assertEquals("ASD12.0", concatenate("ASD", 1, 2.0));
        assertEquals("true12.03.0%SEN", concatenate(true, 1, 2.0, 3f, '%', "SEN"));
    }
}
