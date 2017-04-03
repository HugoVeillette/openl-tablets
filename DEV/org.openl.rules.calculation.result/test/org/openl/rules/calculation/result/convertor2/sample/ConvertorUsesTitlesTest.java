package org.openl.rules.calculation.result.convertor2.sample;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;

import org.junit.Test;
import org.openl.rules.TestHelper;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calculation.result.convertor2.CalculationStep;
import org.openl.rules.calculation.result.convertor2.CompoundStep;
import org.openl.rules.calculation.result.convertor2.sample.result.ResultConvertor;

public class ConvertorUsesTitlesTest {
    public interface ITestCalc {
        SpreadsheetResult calc();
    }

    @Test
    public void test1() {
        File xlsFile = new File("test/rules/calc0-1.xls");
        TestHelper<ITestCalc> testHelper = new TestHelper<ITestCalc>(xlsFile, ITestCalc.class);

        ITestCalc test = testHelper.getInstance();
        SpreadsheetResult result = test.calc();
        assertEquals(2, result.getHeight());
        assertEquals(3, result.getWidth());
        
        ResultConvertor resultConvertor = new ResultConvertor();
        CompoundStep compoundStep = resultConvertor.process(result);
        List<CalculationStep> steps = compoundStep.getSteps();
        assertEquals(2, steps.size());
        assertEquals("Row1 : DoubleValue", steps.get(0).getStepName());
        assertEquals("Row2", steps.get(1).getStepName());
    }
}
