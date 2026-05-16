package com.example.csit228capstone.service.report;

import com.example.csit228capstone.model.report.ReportData;
import com.example.csit228capstone.model.report.ReportFormData;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Clean PDF exporter pulling parameters into perfectly symmetric re-arranged layout grids.
 */
public class PDFReportExporter implements ReportExporter<List<List<String>>> {

    private static final Color TEAL     = new DeviceRgb(0x1D, 0x9E, 0x75);
    private static final Color TEAL_ALT = new DeviceRgb(0xF4, 0xFA, 0xF8);
    private static final Color DARK     = new DeviceRgb(0x1A, 0x2E, 0x44);
    private static final Color GRAY     = new DeviceRgb(0x88, 0x88, 0x88);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
    private static final DateTimeFormatter DT_FMT   = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    @Override public String getFormatName() { return "PDF"; }

    // ── 1. Generic Tabular Report ────────────────────────────────────────────
    @Override
    public void export(ReportData<List<List<String>>> data, File dest) throws Exception {
        ReportFormData fd = data != null ? data.getFormData() : null;

        try (PdfDocument pdf = new PdfDocument(new PdfWriter(dest));
             Document doc    = new Document(pdf, PageSize.A4)) {

            doc.setMargins(40, 40, 40, 40);
            govHeader(doc);

            doc.add(para(nvl(data.getTitle()).toUpperCase(), 18, true, DARK)
                    .setTextAlignment(TextAlignment.CENTER).setMarginTop(4));
            if (data.getReportType() != null) {
                doc.add(para(data.getReportType().getDisplayName(), 11, false, TEAL)
                        .setTextAlignment(TextAlignment.CENTER));
            }
            doc.add(new LineSeparator(new SolidLine(0.5f)).setMarginBottom(6));

            String formDate = fd != null && fd.getDate() != null ? fd.getDate().format(DATE_FMT)
                    : (data != null && data.getGeneratedDateTime() != null ? data.getGeneratedDateTime().toLocalDate().format(DATE_FMT) : "—");
            String genOn = (data != null && data.getGeneratedDateTime() != null) ? data.getGeneratedDateTime().format(DT_FMT) : formDate;

            String startStr = data.getStartDate() != null ? data.getStartDate().format(DATE_FMT) : "";
            String endStr = data.getEndDate() != null ? data.getEndDate().format(DATE_FMT) : "";
            String coveragePeriod = (!startStr.isEmpty() || !endStr.isEmpty()) ? startStr + " — " + endStr : formDate;

            String reportedBy = (fd != null && !nvl(fd.getReportedBy()).isBlank()) ? fd.getReportedBy()
                    : (data != null && !nvl(data.getGeneratedBy()).isBlank() ? data.getGeneratedBy() : "—");
            String reportNo   = (fd != null && !nvl(fd.getReportNo()).isBlank()) ? fd.getReportNo() : "—";
            String recordedBy = (fd != null && !nvl(fd.getRecordedBy()).isBlank()) ? fd.getRecordedBy() : "—";
            String repContact = (fd != null && !nvl(fd.getReporterContactInfo()).isBlank()) ? fd.getReporterContactInfo() : "—";
            String recContact = (fd != null && !nvl(fd.getRecorderContactInfo()).isBlank()) ? fd.getRecorderContactInfo() : "—";
            String computedType = data.getReportType() != null ? data.getReportType().getDisplayName() : "—";

            // --- RE-ARRANGED SYMMETRIC GRID MAPPING ---
            doc.add(twoColRow("Date:", formDate, "Report No.:", reportNo));
            doc.add(twoColRow("Reported by:", reportedBy, "Recorded by:", recordedBy));
            doc.add(twoColRow("Reporter Contact:", repContact, "Recorder Contact:", recContact));
            doc.add(twoColRow("Classification Type:", computedType, "Generated on:", genOn));
            doc.add(twoColRow("Period Covered:", coveragePeriod, "", ""));

            if (data.getSummaryLines() != null && !data.getSummaryLines().isEmpty()) {
                sectionHeader(doc, "Summary");
                for (String line : data.getSummaryLines()) {
                    doc.add(para(line, 10, false, null).setMarginLeft(8));
                }
            }

            List<List<String>> rows = data.getPayload() != null ? data.getPayload() : List.of();
            if (!rows.isEmpty()) {
                sectionHeader(doc, "Data");
                List<String> headers = data.getHeaders();
                if (headers == null || headers.isEmpty()) {
                    headers = new java.util.ArrayList<>();
                    for(int i=0; i < rows.get(0).size(); i++) headers.add("Column " + (i+1));
                }
                doc.add(dataTable(headers, rows));
            }

            String sigName = !recordedBy.equals("—") ? recordedBy
                    : (!reportedBy.equals("—") ? reportedBy : "Authorized Signatory");
            signature(doc, sigName);
        }
    }

    // ── 2. Incident Report ───────────────────────────────────────────────────
    public void exportIncident(ReportData<ReportFormData> data, File dest) throws Exception {
        ReportFormData fd = data != null ? (data.getFormData() != null ? data.getFormData() : data.getPayload()) : null;
        if (fd == null) {
            fd = new ReportFormData();
        }

        try (PdfDocument pdf = new PdfDocument(new PdfWriter(dest));
             Document doc    = new Document(pdf, PageSize.A4)) {

            doc.setMargins(40, 40, 40, 40);
            govHeader(doc);

            doc.add(para(data != null ? nvl(data.getTitle()).toUpperCase() : "INCIDENT REPORT", 18, true, DARK)
                    .setTextAlignment(TextAlignment.CENTER).setMarginTop(4));
            doc.add(para("Incident Report", 11, false, TEAL)
                    .setTextAlignment(TextAlignment.CENTER));
            doc.add(new LineSeparator(new SolidLine(0.5f)).setMarginBottom(6));

            String formDate = fd.getDate() != null ? fd.getDate().format(DATE_FMT)
                    : (data != null && data.getGeneratedDateTime() != null ? data.getGeneratedDateTime().toLocalDate().format(DATE_FMT) : "—");
            String genOn = (data != null && data.getGeneratedDateTime() != null) ? data.getGeneratedDateTime().format(DT_FMT) : formDate;

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

            StringBuilder typeBuilder = new StringBuilder();
            if (fd.getIncidentTypes() != null && !fd.getIncidentTypes().isEmpty()) {
                typeBuilder.append(String.join(", ", fd.getIncidentTypes()));
            }
            if (!nvl(fd.getIncidentTypeOther()).isBlank()) {
                if (typeBuilder.length() > 0) typeBuilder.append(" | Other: ");
                typeBuilder.append(fd.getIncidentTypeOther().trim());
            }
            String finalClassificationType = typeBuilder.length() > 0 ? typeBuilder.toString() : "—";

            // --- RE-ARRANGED SYMMETRIC GRID MAPPING ---
            doc.add(twoColRow("Date:", formDate, "Report No.:", reportNo));
            doc.add(twoColRow("Reported by:", reportedBy, "Recorded by:", recordedBy));
            doc.add(twoColRow("Reporter Contact:", repContact, "Recorder Contact:", recContact));
            doc.add(twoColRow("Classification Type:", finalClassificationType, "Generated on:", genOn));
            doc.add(twoColRow("Period Covered:", coveragePeriod, "", ""));

            sectionHeader(doc, "Incident Details");
            doc.add(twoColRow("Date of Incident:", fd.getDateOfIncident() != null ? fd.getDateOfIncident().format(DATE_FMT) : "—", "Location:", nvl(fd.getLocation()).isBlank() ? "—" : fd.getLocation()));
            doc.add(twoColRow("Description:", nvl(fd.getDescription()).isBlank() ? "—" : fd.getDescription(), "", ""));

            String ins = fd.getHasInsurance() == null ? "N/A" : fd.getHasInsurance() ? "Yes" : "No";
            sectionHeader(doc, "Insurance Context");
            doc.add(twoColRow("Insurance State:", ins, "Policy Number:", nvl(fd.getInsurancePolicy()).isBlank() ? "—" : fd.getInsurancePolicy()));
            doc.add(twoColRow("Coverage Value:", nvl(fd.getInsuranceCoverageAmt()).isBlank() ? "—" : fd.getInsuranceCoverageAmt(), "", ""));

            sectionHeader(doc, "Property Damages");
            List<List<String>> damageRows = fd.getDamages() == null ? List.of() :
                    fd.getDamages().stream()
                            .filter(r -> r != null && (!nvl(r.damage).isBlank() || !nvl(r.value).isBlank()))
                            .map(r -> List.of(nvl(r.damage).isBlank() ? "—" : r.damage, nvl(r.value).isBlank() ? "—" : r.value, nvl(r.repairPlan).isBlank() ? "—" : r.repairPlan, nvl(r.repairCost).isBlank() ? "—" : r.repairCost))
                            .toList();
            doc.add(dataTable(List.of("Damage Item", "Estimated Value", "Repair Plan", "Repair Cost"), damageRows));

            sectionHeader(doc, "Casualties / Injuries");
            List<List<String>> injuryRows = fd.getInjuries() == null ? List.of() :
                    fd.getInjuries().stream()
                            .filter(r -> r != null && (!nvl(r.injuredPerson).isBlank() || !nvl(r.position).isBlank()))
                            .map(r -> List.of(nvl(r.injuredPerson).isBlank() ? "—" : r.injuredPerson, nvl(r.position).isBlank() ? "—" : r.position, nvl(r.medicalCost).isBlank() ? "—" : r.medicalCost, nvl(r.insurance).isBlank() ? "—" : r.insurance))
                            .toList();
            doc.add(dataTable(List.of("Injured Person", "Position", "Medical Cost", "Insurance"), injuryRows));

            String sigName = !recordedBy.equals("—") ? recordedBy
                    : (!reportedBy.equals("—") ? reportedBy : "Authorized Signatory");
            signature(doc, sigName);
        }
    }

    // ── iText Helpers ─────────────────────────────────────────────────────────
    private void govHeader(Document doc) {
        doc.add(para("Republic of the Philippines", 9, false, GRAY)
                .setTextAlignment(TextAlignment.CENTER));
        doc.add(para("Barangay Management System — LIGTAS", 9, false, GRAY)
                .setTextAlignment(TextAlignment.CENTER));
    }

    private Paragraph para(String text, float size, boolean bold, Color color) {
        Paragraph p = new Paragraph(text).setFontSize(size);
        if (bold) p.setBold();
        if (color != null) p.setFontColor(color);
        return p;
    }

    private void sectionHeader(Document doc, String title) {
        doc.add(new Paragraph(title)
                .setFontSize(12).setBold().setFontColor(TEAL)
                .setBorderBottom(new SolidBorder(TEAL, 0.5f))
                .setMarginTop(10).setMarginBottom(4));
    }

    private Table twoColRow(String l1, String v1, String l2, String v2) {
        // Formatted with precise proportional sizing percentages
        Table t = new Table(UnitValue.createPercentArray(new float[]{1.8f, 3.2f, 1.8f, 3.2f}))
                .useAllAvailableWidth().setMarginBottom(3);

        t.addCell(lc(l1)); t.addCell(vc(v1));
        t.addCell(lc(l2)); t.addCell(vc(v2));
        return t;
    }

    private Cell lc(String text) {
        if (text == null || text.isBlank()) {
            return new Cell().setBorder(Border.NO_BORDER).setPadding(2);
        }
        return new Cell().add(para(text, 9, true, DARK))
                .setBorder(Border.NO_BORDER).setPadding(2);
    }

    private Cell vc(String text) {
        if (text == null || text.isBlank()) {
            return new Cell().setBorder(Border.NO_BORDER).setPadding(2);
        }
        return new Cell().add(para(text, 9, false, null))
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f))
                .setPadding(2);
    }

    private Table dataTable(List<String> headers, List<List<String>> rows) {
        float[] w = new float[headers.size()];
        for (int i = 0; i < w.length; i++) w[i] = 1f;

        Table t = new Table(UnitValue.createPercentArray(w)).useAllAvailableWidth();
        for (String h : headers)
            t.addHeaderCell(new Cell()
                    .add(para(h, 9, true, ColorConstants.WHITE).setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(TEAL).setPadding(4).setBorder(Border.NO_BORDER));

        int idx = 0;
        for (List<String> row : rows) {
            for (int c = 0; c < headers.size(); c++) {
                String v = c < row.size() ? nvl(row.get(c)) : "";
                Cell cell = new Cell().add(para(v.isBlank() ? "—" : v, 8.5f, false, null).setTextAlignment(TextAlignment.CENTER))
                        .setPadding(4)
                        .setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.3f));
                if (idx % 2 == 1) cell.setBackgroundColor(TEAL_ALT);
                t.addCell(cell);
            }
            idx++;
        }
        return t;
    }

    private void signature(Document doc, String name) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{6f, 4f}))
                .useAllAvailableWidth().setMarginTop(45);

        table.addCell(new Cell().setBorder(Border.NO_BORDER));

        Cell sigCell = new Cell().setBorder(Border.NO_BORDER);

        Paragraph linePara = para(name, 10, true, DARK)
                .setTextAlignment(TextAlignment.CENTER)
                .setPaddingTop(4);

        Paragraph labelPara = para("Authorized Signatory", 9, false, GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(2);

        sigCell.add(linePara);
        sigCell.add(labelPara);
        table.addCell(sigCell);

        doc.add(table);
    }

    private String nvl(String s) { return s != null ? s : ""; }
}