package com.example.csit228capstone.service.report;

import com.example.csit228capstone.model.report.ReportData;
import com.example.csit228capstone.model.report.ReportFormData;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Clean Word (.docx) exporter with completely re-arranged symmetric metadata layout grids.
 */
public class DocxReportExporter implements ReportExporter<List<List<String>>> {

    private static final String TEAL  = "1D9E75";
    private static final String DARK  = "1A2E44";
    private static final String GRAY  = "888888";
    private static final String WHITE = "FFFFFF";
    private static final String ALT   = "F4FAF8";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
    private static final DateTimeFormatter DT_FMT   = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    @Override public String getFormatName() { return "DOCX"; }

    // ── 1. Generic Tabular Report ────────────────────────────────────────────
    @Override
    public void export(ReportData<List<List<String>>> data, File dest) throws Exception {
        ReportFormData fd = data.getFormData() != null ? data.getFormData() : new ReportFormData();

        try (XWPFDocument doc = new XWPFDocument();
             FileOutputStream fos = new FileOutputStream(dest)) {

            setMargins(doc, 720);
            govHeader(doc);

            centeredPara(doc, nvl(data.getTitle()).toUpperCase(), 18, true, DARK);
            if (data.getReportType() != null) {
                centeredPara(doc, data.getReportType().getDisplayName(), 11, false, TEAL);
            }
            hRule(doc, TEAL);

            // Resolve Dynamic Strings
            String formDate = fd.getDate() != null ? fd.getDate().format(DATE_FMT)
                    : (data.getGeneratedDateTime() != null ? data.getGeneratedDateTime().toLocalDate().format(DATE_FMT) : "—");
            String genOn = data.getGeneratedDateTime() != null ? data.getGeneratedDateTime().format(DT_FMT) : formDate;

            String startStr = fd.getStartDate() != null ? fd.getStartDate().format(DATE_FMT) : (data.getStartDate() != null ? data.getStartDate().format(DATE_FMT) : "");
            String endStr = fd.getEndDate() != null ? fd.getEndDate().format(DATE_FMT) : (data.getEndDate() != null ? data.getEndDate().format(DATE_FMT) : "");
            String coveragePeriod = (!startStr.isEmpty() || !endStr.isEmpty()) ? startStr + " — " + endStr : formDate;

            String reportedBy = (fd != null && !nvl(fd.getReportedBy()).isBlank()) ? fd.getReportedBy()
                    : (!nvl(data.getGeneratedBy()).isBlank() ? data.getGeneratedBy() : "—");
            String reportNo   = (fd != null && !nvl(fd.getReportNo()).isBlank()) ? fd.getReportNo() : "—";
            String recordedBy = (fd != null && !nvl(fd.getRecordedBy()).isBlank()) ? fd.getRecordedBy() : "—";
            String repContact = (fd != null && !nvl(fd.getReporterContactInfo()).isBlank()) ? fd.getReporterContactInfo() : "—";
            String recContact = (fd != null && !nvl(fd.getRecorderContactInfo()).isBlank()) ? fd.getRecorderContactInfo() : "—";
            String computedType = data.getReportType() != null ? data.getReportType().getDisplayName() : "—";

            // --- RE-ARRANGED STRUCTURAL MATRIX GRID ---
            twoColRow(doc, "Date:", formDate, "Report No.:", reportNo);
            twoColRow(doc, "Reported by:", reportedBy, "Recorded by:", recordedBy);
            twoColRow(doc, "Reporter Contact:", repContact, "Recorder Contact:", recContact);
            twoColRow(doc, "Classification Type:", computedType, "Generated on:", genOn);
            twoColRow(doc, "Period Covered:", coveragePeriod, "", ""); // Aligned symmetrically inside grid structure

            if (data.getSummaryLines() != null && !data.getSummaryLines().isEmpty()) {
                sectionHeader(doc, "Summary");
                for (String line : data.getSummaryLines()) {
                    bodyPara(doc, line, 10, false, null);
                }
            }

            List<List<String>> rows = data.getPayload() != null ? data.getPayload() : List.of();
            if (!rows.isEmpty()) {
                sectionHeader(doc, "Data");
                List<String> headers = data.getHeaders() != null ? data.getHeaders() : List.of();
                if (headers.isEmpty()) {
                    headers = new java.util.ArrayList<>();
                    for(int i=0; i < rows.get(0).size(); i++) headers.add("Column " + (i+1));
                }
                dataTable(doc, headers, rows);
            }

            blank(doc); blank(doc);
            String sigName = !recordedBy.equals("—") ? recordedBy : (!reportedBy.equals("—") ? reportedBy : "Authorized Signatory");
            buildSignatureBlock(doc, sigName);

            doc.write(fos);
        }
    }

    // ── 2. Incident Report ───────────────────────────────────────────────────
    public void exportIncident(ReportData<ReportFormData> data, File dest) throws Exception {
        ReportFormData fd = data.getFormData() != null ? data.getFormData() :
                (data.getPayload() != null ? data.getPayload() : new ReportFormData());

        try (XWPFDocument doc = new XWPFDocument();
             FileOutputStream fos = new FileOutputStream(dest)) {

            setMargins(doc, 720);
            govHeader(doc);

            centeredPara(doc, data.getTitle().toUpperCase(), 18, true, DARK);
            centeredPara(doc, "Incident Report", 11, false, TEAL);
            hRule(doc, TEAL);

            String formDate = fd.getDate() != null ? fd.getDate().format(DATE_FMT)
                    : (data.getGeneratedDateTime() != null ? data.getGeneratedDateTime().toLocalDate().format(DATE_FMT) : "—");
            String genOn = data.getGeneratedDateTime() != null ? data.getGeneratedDateTime().format(DT_FMT) : formDate;

            java.time.LocalDate startLoc = fd.getStartDate() != null ? fd.getStartDate() : (data != null ? data.getStartDate() : null);
            java.time.LocalDate endLoc = fd.getEndDate() != null ? fd.getEndDate() : (data != null ? data.getEndDate() : null);
            String startStr = startLoc != null ? startLoc.format(DATE_FMT) : "";
            String endStr = endLoc != null ? endLoc.format(DATE_FMT) : "";
            String coveragePeriod = (!startStr.isEmpty() || !endStr.isEmpty()) ? startStr + " — " + endStr : formDate;

            String reportedBy = !nvl(fd.getReportedBy()).isBlank() ? fd.getReportedBy()
                    : (data != null && !nvl(data.getGeneratedBy()).isBlank() ? data.getGeneratedBy() : "—");
            String reportNo   = !nvl(fd.getReportNo()).isBlank() ? fd.getReportNo() : "—";
            String recordedBy = !nvl(fd.getRecordedBy()).isBlank() ? fd.getRecordedBy() : "—";
            String repContact = !nvl(fd.getReporterContactInfo()).isBlank() ? fd.getReporterContactInfo() : "—";
            String recContact = !nvl(fd.getRecorderContactInfo()).isBlank() ? fd.getRecorderContactInfo() : "—";

            // Compute classification string cleanly
            StringBuilder typeBuilder = new StringBuilder();
            if (fd.getIncidentTypes() != null && !fd.getIncidentTypes().isEmpty()) {
                typeBuilder.append(String.join(", ", fd.getIncidentTypes()));
            }
            if (!nvl(fd.getIncidentTypeOther()).isBlank()) {
                if (typeBuilder.length() > 0) typeBuilder.append(" | Other: ");
                typeBuilder.append(fd.getIncidentTypeOther().trim());
            }
            String finalClassificationType = typeBuilder.length() > 0 ? typeBuilder.toString() : "—";

            // --- RE-ARRANGED STRUCTURAL MATRIX GRID ---
            twoColRow(doc, "Date:", formDate, "Report No.:", reportNo);
            twoColRow(doc, "Reported by:", reportedBy, "Recorded by:", recordedBy);
            twoColRow(doc, "Reporter Contact:", repContact, "Recorder Contact:", recContact);
            twoColRow(doc, "Classification Type:", finalClassificationType, "Generated on:", genOn);
            twoColRow(doc, "Period Covered:", coveragePeriod, "", "");

            sectionHeader(doc, "Incident Details");
            twoColRow(doc, "Date of Incident:", fd.getDateOfIncident() != null ? fd.getDateOfIncident().format(DATE_FMT) : "—", "Location:", nvl(fd.getLocation()).isBlank() ? "—" : fd.getLocation());
            twoColRow(doc, "Description:", nvl(fd.getDescription()).isBlank() ? "—" : fd.getDescription(), "", "");

            String ins = fd.getHasInsurance() == null ? "N/A" : fd.getHasInsurance() ? "Yes" : "No";
            sectionHeader(doc, "Insurance Context");
            twoColRow(doc, "Insurance State:", ins, "Policy Number:", nvl(fd.getInsurancePolicy()).isBlank() ? "—" : fd.getInsurancePolicy());
            twoColRow(doc, "Coverage Value:", nvl(fd.getInsuranceCoverageAmt()).isBlank() ? "—" : fd.getInsuranceCoverageAmt(), "", "");

            sectionHeader(doc, "Property Damages");
            List<List<String>> damageRows = fd.getDamages() == null ? List.of() :
                    fd.getDamages().stream()
                            .filter(r -> !nvl(r.damage).isBlank() || !nvl(r.value).isBlank())
                            .map(r -> List.of(nvl(r.damage).isBlank() ? "—" : r.damage, nvl(r.value).isBlank() ? "—" : r.value, nvl(r.repairPlan).isBlank() ? "—" : r.repairPlan, nvl(r.repairCost).isBlank() ? "—" : r.repairCost))
                            .toList();
            dataTable(doc, List.of("Damage Item", "Estimated Value", "Repair Plan", "Repair Cost"), damageRows);

            sectionHeader(doc, "Casualties / Injuries");
            List<List<String>> injuryRows = fd.getInjuries() == null ? List.of() :
                    fd.getInjuries().stream()
                            .filter(r -> !nvl(r.injuredPerson).isBlank() || !nvl(r.position).isBlank())
                            .map(r -> List.of(nvl(r.injuredPerson).isBlank() ? "—" : r.injuredPerson, nvl(r.position).isBlank() ? "—" : r.position, nvl(r.medicalCost).isBlank() ? "—" : r.medicalCost, nvl(r.insurance).isBlank() ? "—" : r.insurance))
                            .toList();
            dataTable(doc, List.of("Injured Person", "Position", "Medical Cost", "Insurance"), injuryRows);

            blank(doc); blank(doc);
            String sigName = !recordedBy.equals("—") ? recordedBy : (!reportedBy.equals("—") ? reportedBy : "Authorized Signatory");
            buildSignatureBlock(doc, sigName);

            doc.write(fos);
        }
    }

    // ── POI Helpers ───────────────────────────────────────────────────────────
    private void buildSignatureBlock(XWPFDocument doc, String name) {
        XWPFTable sigTable = doc.createTable(1, 2);
        sigTable.setWidth("100%");
        noBorders(sigTable);

        XWPFTableRow row = sigTable.getRow(0);
        XWPFTableCell cellLeftSpacer = row.getCell(0);
        XWPFTableCell sigCell = row.getCell(1);

        cellLeftSpacer.setWidth("65.0%");
        sigCell.setWidth("35.0%");

        cellLeftSpacer.getParagraphs().get(0).createRun().setText("");

        XWPFParagraph p1 = sigCell.getParagraphs().get(0);
        p1.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun runName = p1.createRun();
        runName.setText(name);
        runName.setFontSize(10);
        runName.setBold(true);
        runName.setColor(DARK);
        runName.setFontFamily("Segoe UI");

        XWPFParagraph p2 = sigCell.addParagraph();
        p2.setAlignment(ParagraphAlignment.CENTER);
        p2.setSpacingBefore(10);

        XWPFRun runLabel = p2.createRun();
        runLabel.setText("Authorized Signatory");
        runLabel.setFontSize(9);
        runLabel.setColor(GRAY);
        runLabel.setFontFamily("Segoe UI");
    }

    private void setMargins(XWPFDocument doc, long twips) {
        CTBody b = doc.getDocument().getBody();
        CTSectPr sect = b.isSetSectPr() ? b.getSectPr() : b.addNewSectPr();
        CTPageMar m = sect.isSetPgMar() ? sect.getPgMar() : sect.addNewPgMar();
        BigInteger v = BigInteger.valueOf(twips);
        m.setTop(v); m.setBottom(v); m.setLeft(v); m.setRight(v);
    }

    private void govHeader(XWPFDocument doc) {
        centeredPara(doc, "Republic of the Philippines", 9, false, GRAY);
        centeredPara(doc, "Barangay Management System — LIGTAS", 9, false, GRAY);
    }

    private void centeredPara(XWPFDocument doc, String text, int size, boolean bold, String color) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        run(p, text, size, bold, color);
    }

    private void bodyPara(XWPFDocument doc, String text, int size, boolean bold, String color) {
        run(doc.createParagraph(), text, size, bold, color);
    }

    private void blank(XWPFDocument doc) { doc.createParagraph(); }

    private void run(XWPFParagraph p, String text, int size, boolean bold, String color) {
        XWPFRun r = p.createRun();
        r.setText(text); r.setFontSize(size); r.setBold(bold);
        r.setFontFamily("Segoe UI");
        if (color != null) r.setColor(color);
    }

    private void hRule(XWPFDocument doc, String color) {
        XWPFParagraph p = doc.createParagraph();
        p.getCTP().addNewPPr().addNewRPr();
        CTBorder b = p.getCTP().getPPr().addNewPBdr().addNewBottom();
        b.setVal(STBorder.SINGLE); b.setSz(BigInteger.valueOf(6)); b.setColor(color);
    }

    private void sectionHeader(XWPFDocument doc, String title) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(120);
        XWPFRun r = p.createRun();
        r.setText(title); r.setFontSize(12); r.setBold(true); r.setColor(TEAL);
        r.setFontFamily("Segoe UI");

        CTBorder b = p.getCTP().addNewPPr().addNewPBdr().addNewBottom();
        b.setVal(STBorder.SINGLE); b.setSz(BigInteger.valueOf(4)); b.setColor(TEAL);
    }

    private void twoColRow(XWPFDocument doc, String l1, String v1, String l2, String v2) {
        XWPFTable t = doc.createTable(1, 4);
        t.setWidth("100%"); noBorders(t);

        // Explicit layout column constraints
        t.getRow(0).getCell(0).getCTTc().addNewTcPr().addNewTcW().setW(BigInteger.valueOf(1800));
        t.getRow(0).getCell(1).getCTTc().addNewTcPr().addNewTcW().setW(BigInteger.valueOf(3200));
        t.getRow(0).getCell(2).getCTTc().addNewTcPr().addNewTcW().setW(BigInteger.valueOf(1800));
        t.getRow(0).getCell(3).getCTTc().addNewTcPr().addNewTcW().setW(BigInteger.valueOf(3200));

        cellLabel(t, 0, 0, l1); cellValue(t, 0, 1, v1);
        cellLabel(t, 0, 2, l2); cellValue(t, 0, 3, v2);
    }

    private void fullRow(XWPFDocument doc, String label, String value) {
        XWPFTable t = doc.createTable(1, 2);
        t.setWidth("100%"); noBorders(t);
        t.getRow(0).getCell(0).getCTTc().addNewTcPr().addNewTcW().setW(BigInteger.valueOf(1800));
        t.getRow(0).getCell(1).getCTTc().addNewTcPr().addNewTcW().setW(BigInteger.valueOf(8200));

        cellLabel(t, 0, 0, label);
        cellValue(t, 0, 1, value);
    }

    private void threeColRow(XWPFDocument doc, String l1, String v1, String l2, String v2, String l3, String v3) {
        XWPFTable t = doc.createTable(1, 6);
        t.setWidth("100%"); noBorders(t);
        cellLabel(t, 0, 0, l1); cellValue(t, 0, 1, v1);
        cellLabel(t, 0, 2, l2); cellValue(t, 0, 3, v2);
        cellLabel(t, 0, 4, l3); cellValue(t, 0, 5, v3);
    }

    private void cellLabel(XWPFTable t, int row, int col, String text) {
        XWPFTableCell c = t.getRow(row).getCell(col);
        if (text == null || text.isBlank()) {
            c.getParagraphs().get(0).createRun().setText("");
            return;
        }
        clearAndRun(c, text, 9, true, DARK, false);
    }

    private void cellValue(XWPFTable t, int row, int col, String text) {
        XWPFTableCell c = t.getRow(row).getCell(col);
        if (t.getRow(row).getCell(col - 1).getParagraphs().get(0).getText().isBlank()) {
            c.getParagraphs().get(0).createRun().setText("");
            return;
        }
        String display = nvl(text).isBlank() ? "—" : text;
        clearAndRun(c, display, 9, false, null, true);
    }

    private void clearAndRun(XWPFTableCell c, String text, int size, boolean bold, String color, boolean bottomBorder) {
        XWPFParagraph p = c.getParagraphs().get(0);
        for (int i = p.getRuns().size() - 1; i >= 0; i--) p.removeRun(i);
        XWPFRun r = p.createRun();
        r.setText(text); r.setFontSize(size); r.setBold(bold);
        r.setFontFamily("Segoe UI");
        if (color != null) r.setColor(color);
        if (bottomBorder) {
            CTTcPr tcPr = c.getCTTc().isSetTcPr() ? c.getCTTc().getTcPr() : c.getCTTc().addNewTcPr();
            CTTcBorders borders = tcPr.isSetTcBorders() ? tcPr.getTcBorders() : tcPr.addNewTcBorders();
            CTBorder bot = borders.isSetBottom() ? borders.getBottom() : borders.addNewBottom();
            bot.setVal(STBorder.SINGLE); bot.setSz(BigInteger.valueOf(4)); bot.setColor("D3D3D3");
        }
    }

    private void dataTable(XWPFDocument doc, List<String> headers, List<List<String>> rows) {
        int cols = headers.size();
        XWPFTable t = doc.createTable(1, cols);
        t.setWidth("100%");

        XWPFTableRow hdr = t.getRow(0);
        for (int c = 0; c < cols; c++) {
            XWPFTableCell cell = hdr.getCell(c);
            if (cell == null) cell = hdr.createCell();
            bg(cell, TEAL);
            XWPFParagraph p = cell.getParagraphs().get(0);
            p.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun r = p.createRun();
            r.setText(headers.get(c)); r.setBold(true); r.setFontSize(9); r.setColor(WHITE);
            r.setFontFamily("Segoe UI");
        }

        int visualIdx = 0;
        for (List<String> row : rows) {
            XWPFTableRow tr = t.createRow();
            String bgColor = visualIdx % 2 == 1 ? ALT : WHITE;
            for (int c = 0; c < cols; c++) {
                XWPFTableCell cell = tr.getCell(c);
                if (cell == null) cell = tr.createCell();
                bg(cell, bgColor);
                XWPFParagraph p = cell.getParagraphs().get(0);
                p.setAlignment(ParagraphAlignment.CENTER);
                XWPFRun r = p.createRun();
                String v = c < row.size() ? nvl(row.get(c)) : "";
                r.setText(v.isBlank() ? "—" : v); r.setFontSize(9);
                r.setFontFamily("Segoe UI");
            }
            visualIdx++;
        }
    }

    private void bg(XWPFTableCell c, String hex) {
        CTShd sh = c.getCTTc().addNewTcPr().addNewShd();
        sh.setFill(hex); sh.setVal(STShd.CLEAR);
    }

    private void noBorders(XWPFTable t) {
        CTTblPr pr = t.getCTTbl().getTblPr() != null ? t.getCTTbl().getTblPr() : t.getCTTbl().addNewTblPr();
        CTTblBorders b = pr.isSetTblBorders() ? pr.getTblBorders() : pr.addNewTblBorders();
        b.addNewTop().setVal(STBorder.NONE);     b.addNewBottom().setVal(STBorder.NONE);
        b.addNewLeft().setVal(STBorder.NONE);    b.addNewRight().setVal(STBorder.NONE);
        b.addNewInsideH().setVal(STBorder.NONE); b.addNewInsideV().setVal(STBorder.NONE);
    }

    private String nvl(String s) { return s != null ? s : ""; }
}