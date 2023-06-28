package org.sirapi.transform;

//import com.itextpdf.text.*;
//import com.itextpdf.text.pdf.PdfWriter;
import org.sirapi.entity.pojo.Attachment;
import org.sirapi.entity.pojo.Detector;
import org.sirapi.entity.pojo.Experience;
import org.sirapi.entity.pojo.Instrument;
import org.sirapi.entity.pojo.ResponseOption;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class Renderings {

	/*
	private static Font catFont = new Font(Font.FontFamily.TIMES_ROMAN, 18, Font.BOLD);
	private static Font redFont = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.NORMAL, BaseColor.RED);
	private static Font subFont = new Font(Font.FontFamily.TIMES_ROMAN, 16, Font.BOLD);
	private static Font smallBold = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.BOLD);
	private static Font smallNormal = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.NORMAL);
	*/

	public static String toString(String uri, int width) {
		Instrument instr = Instrument.find(uri);
		if (instr == null) {
			return "";
		}
		String str = "";

		str += centerText(instr.getHasShortName(), width) + "\n";
		str += "\n";

		for (String line : breakString("Instructions: " + instr.getHasInstruction(), width)) {
			str += line + "\n";
		}
		str += "\n";
		if (instr.getAttachments() != null) {
			for (Attachment attachment : instr.getAttachments()) {
				Detector detector = attachment.getDetector();
				if (detector == null) {
					str += " " + attachment.getHasPriority() + ".  \n  ";
				} else {
					str += " " + attachment.getHasPriority() + ". " + detector.getHasContent() + " ";
					Experience experience = detector.getExperience();
					if (experience != null) {
						if (experience.getResponseOptions() != null) {
							str += "\n     ";
							for (ResponseOption responseOption : experience.getResponseOptions()) {
								str += " " + responseOption.getHasContent() + "( )  ";
							}
						}
					}
				}
				str += "\n\n";
			}
			str += "\n";
		}
		return str;
	}

	private static String centerText(String str, int width) {
		if (str == null) {
			str = "";
		}
		if (str.length() > width) {
			return str;
		}
		int left = (width - str.length()) / 2;
		StringBuffer newStr = new StringBuffer();
		for (int i=0; i < left; i++) {
			newStr.append(" ");
		}
		newStr.append(str);
		return newStr.toString();
	}

	private static List<String> breakString(String str, int width) {
		List<String> lines = new ArrayList<String>();
		StringTokenizer st = new StringTokenizer(str);
		String newLine = "";
		String nextWord = "";
		while (st.hasMoreTokens()) {
			nextWord = st.nextToken();
			if (nextWord.length() >= width) {
				if (newLine.equals("")) {
					newLine = nextWord;
				} else {
					newLine = newLine + " " + nextWord;
				}
				lines.add(newLine);
				newLine = "";
			} else if (newLine.length() + nextWord.length() > width) {
				lines.add(newLine);
				newLine = nextWord;
			} else {
				if (newLine.equals("")) {
					newLine = nextWord;
				} else {
					newLine = newLine + " " + nextWord;
				}
			}

		}
		if (newLine.length() > 0) {
			lines.add(newLine);
		}
		return lines;
	}

	private static String headerHTML (Instrument instr) {
		String dateField = "";
		if (instr.getHasDateField() != null && !instr.getHasDateField().isEmpty()) {
			dateField = instr.getHasDateField();
		}
		String subjectIDField = "";
		if (instr.getHasSubjectIDField() != null && !instr.getHasSubjectIDField().isEmpty()) {
			subjectIDField = instr.getHasSubjectIDField();
		}
		String subjectRelationshipField = "";
		if (instr.getHasSubjectRelationshipField() != null && !instr.getHasSubjectRelationshipField().isEmpty()) {
			subjectRelationshipField = instr.getHasSubjectRelationshipField();
		}
		return "<table id=\"tbl1\"> " +
  				"  <tr id=\"tr1\"> " +
    			"	  <td id=\"cell1\">" + dateField + "</td> " +
    			"	  <td id=\"cell2\"><h2>" + instr.getHasShortName() + "</h2></td> " +
    			"	  <td id=\"cell3\">" + subjectIDField + "<br>" + subjectRelationshipField + "</td> " +
  				"  </tr>" +
				"</table> ";
	}

	private static String footerHTML (Instrument instr, int page) {
		String pageNumber = "";
		if (instr.getHasPageNumber() != null && !instr.getHasPageNumber().isEmpty()) {
			pageNumber = instr.getHasPageNumber() + " " + page;
		}
		String copyrightNotice = "";
		if (instr.getHasCopyrightNotice() != null && !instr.getHasCopyrightNotice().isEmpty()) {
			copyrightNotice = instr.getHasCopyrightNotice();
		}
		return "<table id=\"tbl1\"> " +
				"  <tr id=\"tr1\"> " +
				"	  <td id=\"cell1\">" + pageNumber + "</td> " +
				"	  <td id=\"cell4\"></td> " +
				"	  <td id=\"cell3\">" + copyrightNotice + "</td> " +
				"  </tr>" +
				"</table> ";
	}

	private static String styleHTML() {
		return "<style>\n" +
				"table, tr, td {\n" +
				"  border: 1px solid;\n" +
				"  border-collapse: collapse;\n" +
				"  padding: 5px;\n" +
				"}\n" +
				"tr:nth-child(even) {\n" +
				"  background-color: #f2f2f2;\n" +
				"}\n" +
				"#tbl1, #tr1, #cell1, #cell2, #cell3 , #cell4 {\n" +
				"  border: 0px solid;\n" +
				"  border-collapse: collapse;\n" +
				"  padding: 5px;\n" +
				"}\n" +
				"#cell1 {\n" +
				"	text-align: left;\n" +
				"}\n" +
				"#cell2 {\n" +
				"   padding-right: 140px;\n" +
				"	text-align: center;\n" +
				"   padding-left: 160px;\n" +
				"}\n" +
				"#cell3 {\n" +
				"	text-align: right;\n" +
				"}\n" +
				"#cell4 {\n" +
				"   padding-right: 300px;\n" +
				"	text-align: center;\n" +
				"   padding-left: 300px;\n" +
				"}\n" +
				"</style>\n";
	}

	private static String printPage() {
		return "";
	}


	public static String toHTML(String uri, int width) {
		Instrument instr = Instrument.find(uri);
		int page = 1;
		if (instr == null) {
			return "";
		}
		String html = "";

		html += "<!DOCTYPE html>\n" +
				"<html>\n" +
				"<head>\n" +
				styleHTML() +
				"</head>\n" +
				"<body>\n";

		// PRINT HEADER
		html += headerHTML(instr);
		html += "<br>\n";

		html += instr.getHasInstruction() + "<br>";
		html += "<br>\n";

		// PRINT ITEMS
		int elements = 0;
		if (instr.getAttachments() != null) {
			html += "<table>\n";
			for (Attachment attachment : instr.getAttachments()) {
				Detector detector = attachment.getDetector();
				if (detector == null) {
					html += "<tr><td>" + attachment.getHasPriority() + ".</tr></td>\n";
				} else {
					html += "<tr>";
					elements = elements + 1;
					html += "<td>" + attachment.getHasPriority() + ". " + detector.getHasContent() + "</td>";
					Experience experience = detector.getExperience();
					if (experience != null) {
						if (experience.getResponseOptions() != null) {
							for (ResponseOption responseOption : experience.getResponseOptions()) {
								html += "<td>" + responseOption.getHasContent() + "</td>";
							}
						}
					}
					html += "</tr>\n";
				}
			}
			html += "</table>\n";
		}

		// FILL THE REST OF THE PAGE BLANK
		for (int aux = 0; aux + elements <= 28; aux++) {
			html += "<br>";
		}

		// PRINT FOOTER
		html += footerHTML(instr, page);

		html += "</body>\n" +
				"</html>";
		return html;
	}

	public static ByteArrayOutputStream toPDF(String uri, int width) {
		Instrument instr = Instrument.find(uri);
		if (instr == null) {
			return null;
		}
		String fileName = "https://example.com/" + instr.getHasShortName() + "_V" + instr.getHasVersion() + ".pdf";

		Document document = Jsoup.parse(Renderings.toHTML(uri,width), "UTF-8");
		document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			ITextRenderer renderer = new ITextRenderer();
			SharedContext sharedContext = renderer.getSharedContext();
			sharedContext.setPrint(true);
			sharedContext.setInteractive(false);
			renderer.setDocumentFromString(document.html(),fileName);
			renderer.layout();
			renderer.createPDF(outputStream);
			return outputStream;
		} catch (Exception e) {
			e.printStackTrace();
		}
		/*
		Document document = new Document();
		try {
			PdfWriter.getInstance(document, new FileOutputStream(fileName));
		} catch (DocumentException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		document.open();
		Font font = FontFactory.getFont(FontFactory.COURIER, 16, BaseColor.BLACK);
		Chunk chunk = new Chunk(Renderings.toString(uri,width), font);

		try {
			//document.add(chunk);
			Renderings.addTitlePage(document, instr);
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		document.close();
		 */

		return null;
	}

	/*
	private static void addTitlePage(Document document, Instrument instr)
			throws DocumentException {
		Paragraph preface = new Paragraph();
		// We add one empty line
		addEmptyLine(preface, 1);
		// Lets write a big header
		preface.add(new Paragraph(instr.getHasShortName(), catFont));

		addEmptyLine(preface, 1);
		// Will create: Report generated by: _name, _date
		//preface.add(new Paragraph("Report generated by: " + System.getProperty("user.name") + smallBold));
		//addEmptyLine(preface, 3);
		preface.add(new Paragraph(instr.getHasInstruction(), smallNormal));
		addEmptyLine(preface, 1);
		//preface.add(new Paragraph("This document is a preliminary version and not subject to your license agreement or any other agreement with vogella.com ;-).", redFont));

		document.add(preface);
		// Start a new page
		document.newPage();
	}

	private static void addEmptyLine(Paragraph paragraph, int number) {
		for (int i = 0; i < number; i++) {
			paragraph.add(new Paragraph(" "));
		}
	}
	 */


}
