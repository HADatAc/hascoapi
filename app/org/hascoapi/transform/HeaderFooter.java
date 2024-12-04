package org.hascoapi.transform;

import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.*;
import org.hascoapi.vocabularies.VSTOI;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class HeaderFooter {

	protected static String pageHeaderHTML(Instrument instrument, int page) {
		Annotation pageTopLeftAnnotation = Annotation.findByContainerAndPosition(instrument.getUri(),VSTOI.PAGE_TOP_LEFT);
		Annotation pageTopCenterAnnotation = Annotation.findByContainerAndPosition(instrument.getUri(),VSTOI.PAGE_TOP_CENTER);
		Annotation pageTopRightAnnotation = Annotation.findByContainerAndPosition(instrument.getUri(),VSTOI.PAGE_TOP_RIGHT);
		Annotation pageLineBelowTopAnnotation = Annotation.findByContainerAndPosition(instrument.getUri(),VSTOI.PAGE_LINE_BELOW_TOP);

 		String pageTopLeft = " ";
		if (pageTopLeftAnnotation != null) {
			pageTopLeft = pageTopLeftAnnotation.getRendering();
			pageTopLeft = Renderings.runtimeRendering(pageTopLeft,page);
		}
		String pageTopCenter = " ";
		if (pageTopCenterAnnotation != null) {
			pageTopCenter = pageTopCenterAnnotation.getRendering();
			pageTopCenter = Renderings.runtimeRendering(pageTopCenter,page);
		}
		String pageTopRight = " ";
		if (pageTopRightAnnotation != null) {
			pageTopRight = pageTopRightAnnotation.getRendering();
			pageTopRight = Renderings.runtimeRendering(pageTopRight,page);
		}
		String pageLineBelowTop = " ";
		if (pageLineBelowTopAnnotation != null) {
			pageLineBelowTop = pageLineBelowTopAnnotation.getRendering();
			pageLineBelowTop = Renderings.runtimeRendering(pageLineBelowTop,page);
		}

		return "<table id=\"tbl1\"> " +
				"  <tr id=\"tr1\"> " +
				"	  <td id=\"leftcell\">" + pageTopLeft + "</td> " +
				"	  <td id=\"centercell\">" + pageTopCenter + "</td> " +
				"	  <td id=\"rightcell\">" + pageTopRight + "</td> " +
				"  </tr>" +
				"</table> " +
				"<br>" +
				pageLineBelowTop + "<br>" +
				"<br>\n";
	}

	protected static String pageFooterHTML(Instrument instrument, int page) {

		Annotation pageBottomLeftAnnotation = Annotation.findByContainerAndPosition(instrument.getUri(),VSTOI.PAGE_BOTTOM_LEFT);
		Annotation pageBottomCenterAnnotation = Annotation.findByContainerAndPosition(instrument.getUri(),VSTOI.PAGE_BOTTOM_CENTER);
		Annotation pageBottomRightAnnotation = Annotation.findByContainerAndPosition(instrument.getUri(),VSTOI.PAGE_BOTTOM_RIGHT);
		Annotation pageLineAboveBottomAnnotation = Annotation.findByContainerAndPosition(instrument.getUri(),VSTOI.PAGE_LINE_ABOVE_BOTTOM);

		String pageLineAboveBottom = " ";
		if (pageLineAboveBottomAnnotation != null) {
			pageLineAboveBottom = pageLineAboveBottomAnnotation.getRendering();
			pageLineAboveBottom = Renderings.runtimeRendering(pageLineAboveBottom,page);
		}
 		String pageBottomLeft = " ";
		if (pageBottomLeftAnnotation != null) {
			pageBottomLeft = pageBottomLeftAnnotation.getRendering();
			pageBottomLeft = Renderings.runtimeRendering(pageBottomLeft,page);
		}
		String pageBottomCenter = " ";
		if (pageBottomCenterAnnotation != null) {
			pageBottomCenter = pageBottomCenterAnnotation.getRendering();
			pageBottomCenter = Renderings.runtimeRendering(pageBottomCenter,page);
		}
		String pageBottomRight = " ";
		if (pageBottomRightAnnotation != null) {
			pageBottomRight = pageBottomRightAnnotation.getRendering();
			pageBottomRight = Renderings.runtimeRendering(pageBottomRight,page);
		}

		return pageLineAboveBottom + "<br><br>" + 
				"<table id=\"tbl1\"> " +
				"  <tr id=\"tr1\"> " +
				"	  <td id=\"leftcell\">" + pageBottomLeft + "</td> " +
				"	  <td id=\"centercell\">" + pageBottomCenter + "</td> " +
				"	  <td id=\"rightcell\">" + pageBottomRight + "</td> " +
				"  </tr>" +
				"</table> " +
				"<br><br><br><br>";
	}

	protected static String sectionHeaderHTML(Subcontainer subcontainer, int page) {
		Annotation sectionTopLeftAnnotation = Annotation.findByContainerAndPosition(subcontainer.getUri(),VSTOI.TOP_LEFT);
		Annotation sectionTopCenterAnnotation = Annotation.findByContainerAndPosition(subcontainer.getUri(),VSTOI.TOP_CENTER);
		Annotation sectionTopRightAnnotation = Annotation.findByContainerAndPosition(subcontainer.getUri(),VSTOI.TOP_RIGHT);
		Annotation sectionLineBelowTopAnnotation = Annotation.findByContainerAndPosition(subcontainer.getUri(),VSTOI.LINE_BELOW_TOP);

 		String sectionTopLeft = " ";
		if (sectionTopLeftAnnotation != null) {
			sectionTopLeft = sectionTopLeftAnnotation.getRendering();
			sectionTopLeft = Renderings.runtimeRendering(sectionTopLeft,page);
		}
		String sectionTopCenter = " ";
		if (sectionTopCenterAnnotation != null) {
			sectionTopCenter = sectionTopCenterAnnotation.getRendering();
			sectionTopCenter = Renderings.runtimeRendering(sectionTopCenter,page);
		}
		String sectionTopRight = " ";
		if (sectionTopRightAnnotation != null) {
			sectionTopRight = sectionTopRightAnnotation.getRendering();
			sectionTopRight = Renderings.runtimeRendering(sectionTopRight,page);
		}
		String sectionLineBelowTop = " ";
		if (sectionLineBelowTopAnnotation != null) {
			sectionLineBelowTop = sectionLineBelowTopAnnotation.getRendering();
			sectionLineBelowTop = Renderings.runtimeRendering(sectionLineBelowTop,page);
		}

		return "<table id=\"tbl1\"> " +
				"  <tr id=\"tr1\"> " +
				"	  <td id=\"leftcell\">" + sectionTopLeft + "</td> " +
				"	  <td id=\"centercell\">" + sectionTopCenter + "</td> " +
				"	  <td id=\"rightcell\">" + sectionTopRight + "</td> " +
				"  </tr>" +
				"</table> " +
				"<br>" +
				sectionLineBelowTop + "<br>" +
				"<br>\n";
	}

	protected static String sectionFooterHTML(Subcontainer subcontainer, int page) {

		Annotation sectionBottomLeftAnnotation = Annotation.findByContainerAndPosition(subcontainer.getUri(),VSTOI.BOTTOM_LEFT);
		Annotation sectionBottomCenterAnnotation = Annotation.findByContainerAndPosition(subcontainer.getUri(),VSTOI.BOTTOM_CENTER);
		Annotation sectionBottomRightAnnotation = Annotation.findByContainerAndPosition(subcontainer.getUri(),VSTOI.BOTTOM_RIGHT);
		Annotation sectionLineAboveBottomAnnotation = Annotation.findByContainerAndPosition(subcontainer.getUri(),VSTOI.LINE_ABOVE_BOTTOM);

		String sectionLineAboveBottom = " ";
		if (sectionLineAboveBottomAnnotation != null) {
			sectionLineAboveBottom = sectionLineAboveBottomAnnotation.getRendering();
			sectionLineAboveBottom = Renderings.runtimeRendering(sectionLineAboveBottom,page);
		}
 		String sectionBottomLeft = " ";
		if (sectionBottomLeftAnnotation != null) {
			sectionBottomLeft = sectionBottomLeftAnnotation.getRendering();
			sectionBottomLeft = Renderings.runtimeRendering(sectionBottomLeft,page);
		}
		String sectionBottomCenter = " ";
		if (sectionBottomCenterAnnotation != null) {
			sectionBottomCenter = sectionBottomCenterAnnotation.getRendering();
			sectionBottomCenter = Renderings.runtimeRendering(sectionBottomCenter,page);
		}
		String sectionBottomRight = " ";
		if (sectionBottomRightAnnotation != null) {
			sectionBottomRight = sectionBottomRightAnnotation.getRendering();
			sectionBottomRight = Renderings.runtimeRendering(sectionBottomRight,page);
		}

		return sectionLineAboveBottom + "<br><br>" + 
				"<table id=\"tbl1\"> " +
				"  <tr id=\"tr1\"> " +
				"	  <td id=\"leftcell\">" + sectionBottomLeft + "</td> " +
				"	  <td id=\"centercell\">" + sectionBottomCenter + "</td> " +
				"	  <td id=\"rightcell\">" + sectionBottomRight + "</td> " +
				"  </tr>" +
				"</table> " +
				"<br><br><br><br>";
	}

}
