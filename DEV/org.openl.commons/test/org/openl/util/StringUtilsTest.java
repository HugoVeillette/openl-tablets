package org.openl.util;

import static org.junit.Assert.*;

import java.util.regex.Pattern;

import org.junit.Test;

/**
 * Created by tsaltsevich on 5/3/2016.
 */
public class StringUtilsTest {

    @Test
    public void testSplit() throws Exception {
        assertNull(StringUtils.split(null, ' '));
        assertNull(StringUtils.split(null, '*'));
        assertArrayEquals("Returned array is not empty", new String[]{}, StringUtils.split("", '*'));
        assertArrayEquals("Returned array is not valid",
                new String[]{"a", "b", "c"},
                StringUtils.split("a.b.c", '.'));
        assertArrayEquals("Returned array is not valid",
                new String[]{"a", "b", "c"},
                StringUtils.split("a..b.c", '.'));
        assertArrayEquals("Returned array is not valid", new String[]{"a:b:c"}, StringUtils.split("a:b:c", '.'));
        assertArrayEquals("Returned array is not valid",
                new String[]{"a", "b", "c"},
                StringUtils.split("a b c", ' '));
        assertArrayEquals("Returned array is not valid",
                new String[]{"a", "b", "c"},
                StringUtils.split("a..b.c.", '.'));
        assertArrayEquals("Returned array is not valid",
                new String[]{"a", "b", "c"},
                StringUtils.split("..a..b.c..", '.'));
        assertArrayEquals("Returned array is not valid", new String[]{"a"}, StringUtils.split("a..", '.'));
        assertArrayEquals("Returned array is not valid", new String[]{"a"}, StringUtils.split("a.", '.'));
        assertArrayEquals("Returned array is not valid", new String[]{"a"}, StringUtils.split(".a", '.'));
        assertArrayEquals("Returned array is not valid", new String[]{"a"}, StringUtils.split("..a", '.'));
        assertArrayEquals("Returned array is not valid", new String[]{"a"}, StringUtils.split("..a.", '.'));
        assertArrayEquals("Returned array is not valid", new String[]{"a"}, StringUtils.split("..a..", '.'));

        assertArrayEquals("Returned array is not empty", new String[]{}, StringUtils.split(" \t\r\n", '*'));
        assertArrayEquals("Returned array is not empty",
                new String[]{},
                StringUtils.split(" \t\r\n *  * * \t\n", '*'));
        assertArrayEquals("Returned array is not valid",
                new String[]{"a", "b", "c"},
                StringUtils.split(" a .b .c ", '.'));
        assertArrayEquals("Returned array is not valid",
                new String[]{"a", "b", "c"},
                StringUtils.split(" a . . b . c ", '.'));
        assertArrayEquals("Returned array is not valid",
                new String[]{"a : b : c"},
                StringUtils.split(" a : b : c ", '.'));
        assertArrayEquals("Returned array is not valid",
                new String[]{"a", "b", "c"},
                StringUtils.split("a b \t\r\nc", ' '));
        assertArrayEquals("Returned array is not valid",
                new String[]{"a", "b", "c"},
                StringUtils.split("a. .b.c .", '.'));
        assertArrayEquals("Returned array is not valid",
                new String[]{"a", "b", "c"},
                StringUtils.split(". . a..b.c..", '.'));
        assertArrayEquals("Returned array is not valid", new String[]{"a"}, StringUtils.split("a\t..\n", '.'));
        assertArrayEquals("Returned array is not valid", new String[]{"a"}, StringUtils.split("a\t", '.'));
        assertArrayEquals("Returned array is not valid", new String[]{"a"}, StringUtils.split("\na", '.'));
        assertArrayEquals("Returned array is not valid", new String[]{"a"}, StringUtils.split("  a", '.'));
        assertArrayEquals("Returned array is not valid", new String[]{"a"}, StringUtils.split("  a ", '.'));
        assertArrayEquals("Returned array is not valid", new String[]{"a"}, StringUtils.split(". a. ", '.'));
    }

    @Test
    public void testSplitWS() throws Exception {
        assertNull(StringUtils.split(null));
        assertArrayEquals("Returned array is not empty", new String[]{}, StringUtils.split(""));
        assertArrayEquals("Returned array is not empty", new String[]{}, StringUtils.split("  \n\r  \t \r\n  \t\t"));
        assertArrayEquals("Returned array is not valid", new String[]{"a", "b", "c"}, StringUtils.split("a b c"));
        assertArrayEquals("Returned array is not valid", new String[]{"a", "b", "c"}, StringUtils.split("a \tb\nc"));
        assertArrayEquals("Returned array is not valid", new String[]{"a:b:c"}, StringUtils.split("a:b:c"));
        assertArrayEquals("Returned array is not valid", new String[]{"a", "b", "c"}, StringUtils.split("a\tb\rc"));
        assertArrayEquals("Returned array is not valid",
                new String[]{"a", "b", "c"},
                StringUtils.split("a\n\nb c\n"));
        assertArrayEquals("Returned array is not valid",
                new String[]{"a", "b", "c"},
                StringUtils.split("\t\ta  b c  "));
        assertArrayEquals("Returned array is not valid", new String[]{"a"}, StringUtils.split("a  "));
        assertArrayEquals("Returned array is not valid", new String[]{"a"}, StringUtils.split("a "));
        assertArrayEquals("Returned array is not valid", new String[]{"a"}, StringUtils.split(" a"));
        assertArrayEquals("Returned array is not valid", new String[]{"a"}, StringUtils.split("  a"));
        assertArrayEquals("Returned array is not valid", new String[]{"a"}, StringUtils.split("  a "));
        assertArrayEquals("Returned array is not valid", new String[]{"a"}, StringUtils.split("\t a\n\r"));
    }

    @Test
    public void testJoinObject() throws Exception {
        assertEquals("Returned string is not valid", null, StringUtils.join(null, "*"));
        assertEquals("Returned string is not valid", "", StringUtils.join(new Object[]{}, "*"));
        assertEquals("Returned string is not valid", "null", StringUtils.join(new Object[]{null}, "*"));
        assertEquals("Returned string is not valid", "null,null", StringUtils.join(new Object[]{null, null}, ","));
        assertEquals("Returned string is not valid", "a--b--c", StringUtils.join(new Object[]{"a", "b", "c"}, "--"));
        assertEquals("Returned string is not valid", "null,,a", StringUtils.join(new Object[]{null, "", "a"}, ","));
    }

    @Test
    public void testIsEmpty() throws Exception {
        assertTrue("Returned value is false", StringUtils.isEmpty(null));
        assertTrue("Returned value is false", StringUtils.isEmpty(""));
        assertFalse("Returned value is true", StringUtils.isEmpty(" "));
        assertFalse("Returned value is true", StringUtils.isEmpty("boo"));
        assertFalse("Returned value is true", StringUtils.isEmpty("  boo  "));
    }

    @Test
    public void testIsNotEmpty() throws Exception {
        assertFalse("Returned value is true", StringUtils.isNotEmpty(null));
        assertFalse("Returned value is true", StringUtils.isNotEmpty(""));
        assertTrue("Returned value is false", StringUtils.isNotEmpty(" "));
        assertTrue("Returned value is false", StringUtils.isNotEmpty("boo"));
        assertTrue("Returned value is false", StringUtils.isNotEmpty("  boo  "));
    }

    @Test
    public void testIsBlank() throws Exception {
        assertTrue("Returned value is false", StringUtils.isBlank(null));
        assertTrue("Returned value is false", StringUtils.isBlank(""));
        assertTrue("Returned value is false", StringUtils.isBlank(" "));
        assertFalse("Returned value is true", StringUtils.isBlank("boo"));
        assertFalse("Returned value is true", StringUtils.isBlank("  boo  "));
    }

    @Test
    public void testIsNotBlank() throws Exception {
        assertFalse("Returned value is true", StringUtils.isNotBlank(null));
        assertFalse("Returned value is true", StringUtils.isNotBlank(""));
        assertFalse("Returned value is true", StringUtils.isNotBlank(" "));
        assertTrue("Returned value is false", StringUtils.isNotBlank("boo"));
        assertTrue("Returned value is false", StringUtils.isNotBlank("  boo  "));
    }

    @Test
    public void testContainsIgnoreCase() throws Exception {
        assertFalse("Returned value is true", StringUtils.containsIgnoreCase(null, ""));
        assertFalse("Returned value is true", StringUtils.containsIgnoreCase("", null));
        assertFalse("Returned value is true", StringUtils.containsIgnoreCase("abc", "z"));
        assertFalse("Returned value is true", StringUtils.containsIgnoreCase("абя", "в"));
        assertFalse("Returned value is true", StringUtils.containsIgnoreCase("abc", "Z"));

        assertTrue("Returned value is false", StringUtils.containsIgnoreCase("", ""));
        assertTrue("Returned value is false", StringUtils.containsIgnoreCase("abc", ""));
        assertTrue("Returned value is false", StringUtils.containsIgnoreCase("abc", "a"));
        assertTrue("Returned value is false", StringUtils.containsIgnoreCase("абв", "б"));
        assertTrue("Returned value is false", StringUtils.containsIgnoreCase("abc", "B"));
    }

    @Test
    public void testMatches() throws Exception {
        assertFalse("Returned value is true", StringUtils.matches(Pattern.compile("\\d"), ""));
        assertTrue("Returned value is true", StringUtils.matches(Pattern.compile("\\d"), "1"));
        assertTrue("Returned value is true", StringUtils.matches(Pattern.compile("\\d"), "2"));
        assertFalse("Returned value is true", StringUtils.matches(Pattern.compile("\\d"), "12"));
    }

    @Test
    public void testTrim() throws Exception {
        assertEquals("Returned string is not valid", null, StringUtils.trim(null));
        assertEquals("Returned string is not valid", "", StringUtils.trim(""));
        assertEquals("Returned string is not valid", "", StringUtils.trim("     "));
        assertEquals("Returned string is not valid", "boo", StringUtils.trim("boo"));
        assertEquals("Returned string is not valid", "boo", StringUtils.trim("    boo    "));
    }

    @Test
    public void testTrimToNull() throws Exception {
        assertEquals("Returned string is not valid", null, StringUtils.trimToNull(null));
        assertEquals("Returned string is not valid", null, StringUtils.trimToNull(""));
        assertEquals("Returned string is not valid", null, StringUtils.trimToNull("     "));
        assertEquals("Returned string is not valid", "boo", StringUtils.trimToNull("boo"));
        assertEquals("Returned string is not valid", "boo", StringUtils.trimToNull("    boo    "));
    }

    @Test
    public void testTrimToEmpty() throws Exception {
        assertEquals("Returned string is not valid", "", StringUtils.trimToEmpty(null));
        assertEquals("Returned string is not valid", "", StringUtils.trimToEmpty(""));
        assertEquals("Returned string is not valid", "", StringUtils.trimToEmpty("     "));
        assertEquals("Returned string is not valid", "boo", StringUtils.trimToEmpty("boo"));
        assertEquals("Returned string is not valid", "boo", StringUtils.trimToEmpty("    boo    "));
    }

    @Test
    public void testCapitalize() throws Exception {
        assertEquals("Returned string is not valid", null, StringUtils.capitalize(null));
        assertEquals("Returned string is not valid", "", StringUtils.capitalize(""));
        assertEquals("Returned string is not valid", "Foo", StringUtils.capitalize("foo"));
        assertEquals("Returned string is not valid", "FOo", StringUtils.capitalize("fOo"));
        assertEquals("Returned string is not valid", "МУу", StringUtils.capitalize("мУу"));
    }

    @Test
    public void testUnCapitalize() throws Exception {
        assertEquals("Returned string is not valid", null, StringUtils.uncapitalize(null));
        assertEquals("Returned string is not valid", "", StringUtils.uncapitalize(""));
        assertEquals("Returned string is not valid", "foo", StringUtils.uncapitalize("Foo"));
        assertEquals("Returned string is not valid", "fOO", StringUtils.uncapitalize("FOO"));
        assertEquals("Returned string is not valid", "муУ", StringUtils.uncapitalize("МуУ"));
    }
}