package com.example.csit228capstone.service.report;

import com.example.csit228capstone.model.report.ReportData;

import java.io.File;

/**
 * Strategy interface for all report export formats.
 *
 * Each format (PDF, Excel, CSV) implements this interface.
 * To add a new format:
 *   1. Create a new class implementing ReportExporter.
 *   2. Register it in ReportService.
 *   Done — no other files need to change.
 */
public interface ReportExporter {

    /**
     * Export {@code data} to {@code destination} file.
     *
     * @param data        the report content to export
     * @param destination the target file path chosen by the user
     * @throws Exception if anything goes wrong during export
     */
    void export(ReportData data, File destination) throws Exception;

    /**
     * The file extension this exporter produces (without the dot).
     * Used to filter file chooser dialogs.
     * Example: "pdf", "xlsx", "csv"
     */
    String getExtension();

    /**
     * Human-readable description for file chooser dialogs.
     * Example: "PDF Files (*.pdf)"
     */
    String getDescription();
}