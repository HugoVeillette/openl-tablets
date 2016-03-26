package org.openl.extension.xmlrules.utils;

import java.util.Date;

public class InternalFunctions {
    public static String[][] Out(Object o) {
        return Out(o, true, false);
    }

    public static String[][] Out(Object o, boolean horizontalRowValues) {
        return Out(o, horizontalRowValues, false);
    }

    public static String[][] Out(Object o, boolean horizontalRowValues, boolean showColumnNames) {
        return OutFunction.run(o, horizontalRowValues, showColumnNames);
    }

    public static String Print(int row, int column, Object o) {
        return Print(row, column, o, true, false);
    }

    public static String Print(int row, int column, Object o, boolean horizontalRowValues) {
        return Print(row, column, o, horizontalRowValues, false);
    }

    public static String Print(int row, int column, Object o, boolean horizontalRowValues, boolean showColumnNames) {
        String[][] out = Out(o, horizontalRowValues, showColumnNames);
        String result;
        if (out == null) {
            return "";
        }

        String[] columns;

        if (out.length <= row) {
            if (out.length != 1) {
                return "";
            }

            columns = out[0];
        } else {
            columns = out[row];
        }

        if (columns.length <= column) {
            if (columns.length != 1) {
                return "";
            }

            result = columns[0];
        } else {
            result = columns[column];
        }

        return result == null ? "" : result;
    }

    public static Object[][] TRANSPOSE(Object[][] array) {
        return HelperFunctions.transpose(array);
    }

    public static Object[] ReadIn(String typeName, Object[][] array) {
        return ReadInFunction.readIn(typeName, array);
    }

    public static Object[][] SplitUp(Object object) {
        return SplitUpFunction.splitUp(object);
    }

    public static Object[][] SplitUp(Object[][] array) {
        return SplitUpFunction.splitUp(array);
    }

    public static Object SplitUp(int row, Object array) {
        Object[][] result = SplitUp(array);
        if (result == null || result.length <= row) {
            return "";
        }

        return result[row][0];
    }

    public static Object[] Merge(Object object) {
        Object[][] array;
        try {
            array = (Object[][]) object;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Can't merge non-array parameter " + object);
        }

        return MergeFunction.merge(array);
    }

    public static Object[] Merge(Object[][] array) {
        return MergeFunction.merge(array);
    }

    // To support array calls correctly
    public static Object INDEX(Object[][] object, int userRow, int userColumn) {
        return INDEX((Object) object, userRow, userColumn);
    }

    public static Object INDEX(Object object, int userRow, int userColumn) {
        return IndexFunction.index(object, userRow, userColumn);
    }

    public static Double AVERAGE(Object[] array) {
        return AverageFunction.average(array);
    }

    public static Integer ROWS(Object array) {
        return ROWS((Object[][]) array);
    }

    public static Integer ROWS(Object[][] array) {
        return array.length;
    }

    public static Integer COLUMNS(Object array) {
        return COLUMNS((Object[][]) array);
    }

    public static Integer COLUMNS(Object[][] array) {
        return array[0].length;
    }

    public static Double SUM(Object array) {
        return SumFunction.sum(array);
    }

    public static Double SUM(Object[] array) {
        return SumFunction.sum(array);
    }

    public static Integer DATEDIF(Object startDate, Object endDate, String unit) {
        return DateFunctions.diff(startDate, endDate, unit);
    }

    public static Date NOW() {
        return DateFunctions.now();
    }

    public static Integer YEAR(Object date) {
        return DateFunctions.year(date);
    }

    public static Integer MONTH(Object date) {
        return DateFunctions.month(date);
    }

    public static Integer DAY(Object date) {
        return DateFunctions.day(date);
    }

    public static Integer HOUR(Object date) {
        return DateFunctions.hour(date);
    }

    public static Integer MINUTE(Object date) {
        return DateFunctions.minute(date);
    }

    public static Integer SECOND(Object date) {
        return DateFunctions.second(date);
    }

    public static Date DATE(int year, int month, int day) {
        return DateFunctions.date(year, month, day);
    }
}
