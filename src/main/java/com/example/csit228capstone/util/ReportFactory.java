package com.example.csit228capstone.util;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.example.csit228capstone.model.document.DocumentType;
import com.example.csit228capstone.model.Resident;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Factory — generates iText PDF documents based on DocumentType.
 */
public class ReportFactory {

    public static byte[] generate(DocumentType type, Resident resident, String purpose) {
        return switch (type) {
            case BARANGAY_CLEARANCE   -> generateClearance(resident, purpose);
            case CERTIFICATE_OF_INDIGENCY -> generateIndigency(resident, purpose);
            case CERTIFICATE_OF_RESIDENCY -> generateResidency(resident);
            case BLOTTER_REPORT       -> generateBlotter(resident, purpose);
            case BUSINESS_PERMIT      -> generateBusinessPermit(resident, purpose);
            case OTHER                -> generateGeneric(resident, purpose);
        };
    }

    private static byte[] generateClearance(Resident resident, String purpose) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
             Document doc = new Document(pdf)) {

            doc.add(new Paragraph("Republic of the Philippines")
                    .setTextAlignment(TextAlignment.CENTER).setFontSize(12));
            doc.add(new Paragraph("BARANGAY CLEARANCE")
                    .setTextAlignment(TextAlignment.CENTER).setBold().setFontSize(18));
            doc.add(new Paragraph("\n"));
            doc.add(new Paragraph("TO WHOM IT MAY CONCERN:"));
            doc.add(new Paragraph(String.format(
                    "This is to certify that %s, %d years old, residing at Purok %s, " +
                    "is a bonafide resident of this Barangay and has no derogatory record filed in this office.",
                    resident.getFullName(), resident.getAge(), "___"
            )));
            doc.add(new Paragraph(String.format(
                    "This clearance is being issued upon the request of the above-named person for %s.",
                    purpose != null ? purpose : "whatever legal purpose it may serve"
            )));
            doc.add(new Paragraph("\nIssued this " +
                    LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")) + "."));
            doc.add(new Paragraph("\n\n\n_________________________\nBarangay Captain"));
        }
        return baos.toByteArray();
    }

    private static byte[] generateIndigency(Resident r, String purpose) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
             Document doc = new Document(pdf)) {
            doc.add(new Paragraph("CERTIFICATE OF INDIGENCY").setBold().setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph(String.format(
                    "This is to certify that %s belongs to an indigent family of this Barangay.", r.getFullName())));
            doc.add(new Paragraph("Purpose: " + (purpose != null ? purpose : "N/A")));
            doc.add(new Paragraph("\n\n_________________________\nBarangay Captain"));
        }
        return baos.toByteArray();
    }

    private static byte[] generateResidency(Resident r) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
             Document doc = new Document(pdf)) {
            doc.add(new Paragraph("CERTIFICATE OF RESIDENCY").setBold().setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph(String.format(
                    "%s is a bonafide resident of this Barangay.", r.getFullName())));
            doc.add(new Paragraph("\n\n_________________________\nBarangay Captain"));
        }
        return baos.toByteArray();
    }

    private static byte[] generateBlotter(Resident r, String details) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
             Document doc = new Document(pdf)) {
            doc.add(new Paragraph("BARANGAY BLOTTER REPORT").setBold().setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("Complainant: " + r.getFullName()));
            doc.add(new Paragraph("Details: " + (details != null ? details : "N/A")));
            doc.add(new Paragraph("Date: " + LocalDate.now()));
        }
        return baos.toByteArray();
    }

    private static byte[] generateBusinessPermit(Resident r, String purpose) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
             Document doc = new Document(pdf)) {
            doc.add(new Paragraph("BARANGAY BUSINESS PERMIT").setBold().setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("Applicant: " + r.getFullName()));
            doc.add(new Paragraph("Business: " + (purpose != null ? purpose : "N/A")));
        }
        return baos.toByteArray();
    }

    private static byte[] generateGeneric(Resident r, String purpose) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
             Document doc = new Document(pdf)) {
            doc.add(new Paragraph("BARANGAY CERTIFICATE").setBold().setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("Name: " + r.getFullName()));
            doc.add(new Paragraph("Purpose: " + (purpose != null ? purpose : "General purpose")));
        }
        return baos.toByteArray();
    }
}
