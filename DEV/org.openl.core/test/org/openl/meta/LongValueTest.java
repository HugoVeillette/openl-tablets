package org.openl.meta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class LongValueTest {

    @Test
    public void testEquals() {
        LongValue value1 = new LongValue(2000000000);
        LongValue value2 = new LongValue(2000000000);
        assertEquals(value1, value2);
        value2.setMetaInfo(value1.getMetaInfo());
        assertEquals(value1, value2);
    }

    @Test
    public void testAdd() {
        LongValue value1 = new LongValue(187);
        LongValue value2 = new LongValue(100);
        LongValue result = LongValue.add(value1, value2);
        assertEquals(287, result.getValue());

        value2 = null;
        assertEquals(187, LongValue.add(value1, value2).intValue());

        value1 = null;
        value2 = null;
        assertNull(LongValue.add(value1, value2));

        assertEquals("0", LongValue.add((LongValue) null, new LongValue(0)).toString());
        assertEquals("0", LongValue.add(new LongValue(0), (LongValue) null).toString());
    }

    @Test
    public void testAutocastByte() {
        LongValue result = LongValue.autocast((byte) 126, null);
        assertEquals(126, result.getValue());
    }

    @Test
    public void testMin() {
        LongValue[] la = getTestArray();
        assertEquals(new LongValue(5), LongValue.min(la));

        LongValue[] nullArray = null;
        assertNull(LongValue.min(nullArray));

        LongValue[] emptyArray = new LongValue[0];
        assertNull(LongValue.min(emptyArray));
    }

    private LongValue[] getTestArray() {
        return new LongValue[] { new LongValue(10), new LongValue(100), new LongValue(5) };
    }

    @Test
    public void testMax() {
        LongValue[] la = getTestArray();
        assertEquals(new LongValue(100), LongValue.max(la));

        LongValue[] nullArray = null;
        assertNull(LongValue.max(nullArray));

        LongValue[] emptyArray = new LongValue[0];
        assertNull(LongValue.max(emptyArray));
    }

    @Test
    public void testSum() {
        LongValue[] la = getTestArray();
        assertEquals(new LongValue(115), LongValue.sum(la));

        LongValue[] nullArray = null;
        assertNull(LongValue.sum(nullArray));

        LongValue[] emptyArray = new LongValue[0];
        assertNull(LongValue.sum(emptyArray));
    }

    @Test
    public void testProduct() {
        LongValue[] la = getTestArray();
        assertEquals(new LongValue(5000), LongValue.product(la));

        LongValue[] nullArray = null;
        assertNull(LongValue.product(nullArray));

        LongValue[] emptyArray = new LongValue[0];
        assertNull(LongValue.product(emptyArray));
    }

    @Test
    public void testQuaotient() {
        assertEquals(new LongValue(5), LongValue.quotient(new LongValue(26), new LongValue(5)));

        LongValue nullObj = null;
        assertNull(LongValue.quotient(nullObj, new LongValue(5)));

        assertNull(LongValue.quotient(new LongValue(5), nullObj));
    }

    @Test(expected = ArithmeticException.class)
    public void testQuaotientByZero() {
        LongValue.quotient(new LongValue(5), new LongValue(0));
    }

    @Test
    public void testMod() {
        assertEquals(new LongValue(1), LongValue.mod(new LongValue(26), new LongValue(5)));

        LongValue nullObj = null;
        assertNull(LongValue.mod(nullObj, new LongValue(5)));

        assertNull(LongValue.mod(new LongValue(5), nullObj));
    }

    @Test(expected = ArithmeticException.class)
    public void testModByZero() {
        LongValue.mod(new LongValue(5), new LongValue(0));
    }

}
