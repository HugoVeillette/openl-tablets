package org.openl.rules.convertor;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class String2FloatConvertorTest {

    @Test
    public void testConvertPositive() {
        String2FloatConvertor converter = new String2FloatConvertor();
        Number result = converter.parse("123.456", null, null);
        assertEquals(123.456f, result);
    }

    @Test
    public void testConvertNegative() {
        String2FloatConvertor converter = new String2FloatConvertor();
        Number result = converter.parse("-123.456", null, null);
        assertEquals(-123.456f, result);
    }

    @Test
    public void testConvertPositiveInfinity() {
        String2FloatConvertor converter = new String2FloatConvertor();
        Number result = converter.parse("Infinity", null, null);
        assertEquals(Float.POSITIVE_INFINITY, result);
    }

    @Test
    public void testConvertNegativeInfinity() {
        String2FloatConvertor converter = new String2FloatConvertor();
        Number result = converter.parse("-Infinity", null, null);
        assertEquals(Float.NEGATIVE_INFINITY, result);
    }

    @Test(expected = NumberFormatException.class)
    public void testConvertPositiveOverflow() {
        String2FloatConvertor converter = new String2FloatConvertor();
        converter.parse("1E39", null, null);
    }

    @Test(expected = NumberFormatException.class)
    public void testConvertNegativeOverflow() {
        String2FloatConvertor converter = new String2FloatConvertor();
        converter.parse("-1E39", null, null);
    }

    @Test
    public void testFormat() {
        String2FloatConvertor converter = new String2FloatConvertor();
        String result = converter.format(98765.43f, null);
        assertEquals("98765.43", result);
    }

    @Test
    public void testFormatZero() {
        String2FloatConvertor converter = new String2FloatConvertor();
        String result = converter.format(0f, null);
        assertEquals("0.0", result);
    }

    @Test
    public void testFormatPrecision() {
        String2FloatConvertor converter = new String2FloatConvertor();
        String result = converter.format(0.000000000012345678f, null);
        assertEquals("0.000000000012345678", result);
    }
}
