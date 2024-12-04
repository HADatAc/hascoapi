package org.hascoapi.transform;

public class Style {

	protected static String styleHTML() {
		return "<style>\n" +
				"table, tr, td {\n" +
				"  border: 1px solid;\n" +
				"  border-collapse: collapse;\n" +
				"  padding: 5px;\n" +
				"  width: 100%;\n" +
				"}\n" +
				"tr:nth-child(even) {\n" +
				"  background-color: #f2f2f2;\n" +
				"}\n" +
				"#tbl1, #tr1, #leftcell, #centercell, #rightcell {\n" +
				"  border: 0px solid;\n" +
				"  border-collapse: collapse;\n" +
				"  padding: 5px;\n" +
				"  white-space: nowrap;\n" +
				"}\n" +
				"#leftcell {\n" +
				"	text-align: left;\n" +
				"}\n" +
				"#centercell {\n" +
				"   padding-right: 80px;\n" +
				"	text-align: center;\n" +
				"   padding-left: 80px;\n" +
				"}\n" +
				"#rightcell {\n" +
				"	text-align: right;\n" +
				"}\n" +
				"</style>\n";
	}

}
