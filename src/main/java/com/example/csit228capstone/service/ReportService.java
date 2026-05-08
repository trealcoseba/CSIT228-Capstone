package com.example.csit228capstone.service;

import com.example.csit228capstone.model.report.Report;
import com.example.csit228capstone.model.report.ReportData;
import com.example.csit228capstone.model.report.ReportDataProvider;
import com.example.csit228capstone.repository.ReportRepository;
import com.example.csit228capstone.service.report.*;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central service for the Report Generation feature.
 *
 * Responsibilities:
 *  - Provides the available export formats (PDF, Excel, CSV).
 *  - Delegates data fetching to {@link ReportDataProvider}.
 *  - Delegates export to the correct {@link ReportExporter} implementation.
 *  - Saves/loads report metadata via {@link ReportRepository}.
 *
 * HOW TO ADD A NEW FORMAT (e.g. HTML):
 *   1. Create HtmlReportExporter implements ReportExporter.
 *   2. Add one line in the constructor below: exporters.put("html", new HtmlReportExporter()).
 *   Done — the UI ComboBox will pick it up automatically.
 */
public class ReportService {

    private final ReportDataProvider dataProvider = new ReportDataProvider();
    private final ReportRepository   repository   = new ReportRepository();

    /** Registry of all supported export formats. Order determines UI display order. */
    private final Map<String, ReportExporter> exporters = new LinkedHashMap<>();

    public ReportService() {
        exporters.put("pdf",  new PDFReportExporter());
        exporters.put("xlsx", new ExcelReportExporter());
        exporters.put("csv",  new CSVReportExporter());
    }

    // ── Export ────────────────────────────────────────────────────────────────

    /**
     * Build a {@link ReportData} from the live DB and export it to {@code destination}.
     *
     * @param report      the saved report metadata row
     * @param format      "pdf", "xlsx", or "csv"
     * @param destination file to write
     * @throws Exception  on any I/O or rendering failure
     */
    public void export(Report report, String format, File destination) throws Exception {
        ReportExporter exporter = exporters.get(format.toLowerCase());
        if (exporter == null) {
            throw new IllegalArgumentException("Unsupported format: " + format);
        }
        ReportData data = dataProvider.getData(report);
        exporter.export(data, destination);
    }

    /**
     * Build a {@link ReportData} without exporting — used by the preview window.
     */
    public ReportData buildPreview(Report report) {
        return dataProvider.getData(report);
    }

    // ── Format registry ───────────────────────────────────────────────────────

    /** Returns all registered exporters (format key → exporter). */
    public Map<String, ReportExporter> getExporters() {
        return exporters;
    }

    /** Convenience: returns just the format keys, e.g. ["pdf","xlsx","csv"]. */
    public List<String> getSupportedFormats() {
        return List.copyOf(exporters.keySet());
    }

    // ── Repository passthrough ────────────────────────────────────────────────

    public List<Report> findAll() {
        return repository.findAll();
    }

    public List<Report> findAllFiltered(String searchText, String sortField, boolean ascending) {
        return repository.findAllFiltered(searchText, sortField, ascending);
    }

    public void deleteById(java.util.UUID id) {
        repository.deleteById(id);
    }
}