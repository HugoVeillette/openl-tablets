package org.openl.rules.webstudio.web.test.export;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.openl.rules.testmethod.ITestUnit;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.rules.testmethod.TestStatus;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.util.FileUtils;

public abstract class ResultExport extends BaseExport implements AutoCloseable {
    protected final TestUnitsResults[] results;
    private final int testsPerPage;
    private File tempFile;
    private List<List<TestUnitsResults>> listsWithResults = new ArrayList<>();

    protected ResultExport(TestUnitsResults[] results, int testsPerPage) {
        this.results = results;
        this.testsPerPage = testsPerPage;
    }

    public File createExcelFile() throws IOException {
        tempFile = File.createTempFile("test-results", ".xlsx");
        export(new FileOutputStream(tempFile));
        return tempFile;
    }

    public void export(OutputStream outputStream) throws IOException {
        export(results, testsPerPage, outputStream);
    }

    public void export(TestUnitsResults[] results, int testsPerPage, OutputStream outputStream) throws IOException {
        close(); // Clear previous file if invoked twice

        SXSSFWorkbook workbook = new SXSSFWorkbook();
        try {
            styles = new Styles(workbook);
            ParameterExport parameterExport = new ParameterExport(styles);

            SXSSFSheet sheet = workbook.createSheet("Result " + 1);
            listsWithResults.add(new ArrayList<TestUnitsResults>());
            sheet.trackAllColumnsForAutoSizing();
            int rowNum = FIRST_ROW;
            for (int i = 0; i < results.length; i++) {
                if (testsPerPage > 0) {
                    int pageNum = i / testsPerPage + 1;
                    int inPage = i % testsPerPage;
                    if (inPage == 0 && pageNum > 1) {
                        // AutoSize previous sheet
                        autoSizeColumns(sheet);

                        sheet = workbook.createSheet("Result " + pageNum);
                        listsWithResults.add(new ArrayList<TestUnitsResults>());
                        sheet.trackAllColumnsForAutoSizing();
                        rowNum = FIRST_ROW;
                    }
                }

                rowNum = write(sheet, results[i], rowNum) + SPACE_BETWEEN_RESULTS;
                listsWithResults.get(listsWithResults.size() - 1).add(results[i]);
            }
            autoSizeColumns(sheet);

            for (int i = 0; i < listsWithResults.size(); i++) {
                List<TestUnitsResults> resultsList = listsWithResults.get(i);
                sheet = workbook.createSheet("Parameters " + (i + 1));
                sheet.trackAllColumnsForAutoSizing();
                parameterExport.write(sheet, resultsList);
                autoSizeColumns(sheet);
            }

            workbook.write(outputStream);
            workbook.close();
        } finally {
            workbook.dispose();
        }
    }

    @Override
    public void close() {
        styles = null;
        FileUtils.deleteQuietly(tempFile);
        listsWithResults.clear();
    }

    private int write(Sheet sheet, TestUnitsResults result, int startRow) {
        int rowNum = writeInfo(sheet, result, startRow);
        rowNum = writeHeader(sheet, result, rowNum);
        rowNum = writeResults(sheet, result, rowNum);

        return rowNum;
    }

    protected abstract int writeInfo(Sheet sheet, TestUnitsResults result, int rowNum);

    private int writeHeader(Sheet sheet, TestUnitsResults result, int rowNum) {
        Row row = sheet.createRow(rowNum++);
        int colNum = FIRST_COLUMN;
        createCell(row, colNum++, "ID", styles.header);
        if (result.hasExpected()) {
            createCell(row, colNum++, "Status", styles.header);
        }

        if (result.hasDescription()) {
            createCell(row, colNum++, "Description", styles.header);
        }

        // Context
        if (result.hasContext()) {
            for (String name : result.getContextColumnDisplayNames()) {
                createCell(row, colNum++, name, styles.header);
            }
        }

        // Input data
        for (String name : result.getTestDataColumnDisplayNames()) {
            createCell(row, colNum++, name, styles.header);
        }

        // Result
        writeResultHeader(result, row, colNum);

        return rowNum;
    }

    protected abstract void writeResultHeader(TestUnitsResults result, Row row, int colNum);

    private int writeResults(Sheet sheet, TestUnitsResults result, int rowNum) {
        Row row;
        int colNum;
        boolean hasExpected = result.hasExpected();
        for (ITestUnit testUnit : result.getTestUnits()) {
            TestStatus testStatus = hasExpected ? testUnit.getResultStatus() : TestStatus.TR_OK;
            boolean ok = testStatus == TestStatus.TR_OK;

            row = sheet.createRow(rowNum++);
            // ID
            colNum = FIRST_COLUMN;
            createCell(row, colNum++, testUnit.getTest().getId(), ok ? styles.resultSuccessId : styles.resultFailureId);

            // Status
            if (hasExpected) {
                String status;
                switch (testStatus) {
                    case TR_OK:
                        status = "Passed";
                        break;
                    case TR_NEQ:
                        status = "Failed";
                        break;
                    case TR_EXCEPTION:
                        status = "Error";
                        break;
                    default:
                        throw new UnsupportedOperationException();

                }
                createCell(row, colNum++, status, ok ? styles.resultSuccessStatus : styles.resultFailureStatus);
            }

            // Description
            if (result.hasDescription()) {
                createCell(row, colNum++, testUnit.getDescription(), styles.resultOther);
            }

            // Context
            if (result.hasContext()) {
                for (ParameterWithValueDeclaration parameter : testUnit.getContextParams(result)) {
                    createCell(row, colNum++, parameter.getValue(), styles.resultOther);
                }
            }

            // Input data
            for (ParameterWithValueDeclaration parameter : testUnit.getTest().getExecutionParams()) {
                createCell(row, colNum++, parameter, styles.resultOther);
            }

            // Result
            writeResult(row, colNum, testUnit);
        }
        return rowNum;
    }

    protected abstract void writeResult(Row row, int colNum, ITestUnit testUnit);

}
