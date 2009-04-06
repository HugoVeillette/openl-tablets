/*
 * Created on Oct 7, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.rules.lang.xls;

/**
 * @author snshor
 *
 */
public interface IXlsTableNames
{
	static final String 
		DECISION_TABLE = "DT",
		DECISION_TABLE2 = "Rules",
		SPREADSHEET_TABLE ="Spreadsheet",
		SPREADSHEET_TABLE2 = "Calc",
		TBASIC_TABLE = "TBasic",
        TBASIC_TABLE2 = "Algorithm",
        COLUMN_MATCH = "ColumnMatch",
		
		PROPERTY_TABLE = "Properties",
		METHOD_TABLE = "Code",
	    METHOD_TABLE2 = "Method",
	  
		
		DATA_TABLE = "Data",
		DATATYPE_TABLE = "Datatype",
		
		LANG_PROPERTY = "language",
		INCLUDE_TABLE = "include",
		IMPORT_PROPERTY = "import",
		VOCABULARY_PROPERTY = "vocabulary",
		
		ENVIRONMENT_TABLE = "Environment",
		
		TEST_METHOD_TABLE = "Testmethod",
		RUN_METHOD_TABLE = "Runmethod",
		PERSISTENCE_TABLE = "Persistent";
	
	static public final String VIEW_BUSINESS = "view.business";
	static public final String VIEW_DEVELOPER = "view.developer";


}
