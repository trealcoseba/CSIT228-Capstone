package com.example.csit228capstone.model.report;

import com.example.csit228capstone.model.Resident;
import com.example.csit228capstone.model.Resource;
import com.example.csit228capstone.model.incident.Incident;
import com.example.csit228capstone.repository.IncidentRepository;
import com.example.csit228capstone.repository.ReportRepository;
import com.example.csit228capstone.repository.ResidentRepository;
import com.example.csit228capstone.repository.ResourceRepository;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ReportDataProvider {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    private final ResidentRepository residentRepo = new ResidentRepository();
    private final ResourceRepository resourceRepo = new ResourceRepository();
    private final IncidentRepository incidentRepo = new IncidentRepository();

    public ReportData getData(Report report) {
        ReportType type = ReportType.fromDisplayName(report.getType());
        return switch (type) {
            case RESIDENT_SUMMARY   -> buildResidentSummary(report);
            case RESOURCE_INVENTORY -> buildResourceInventory(report);
            case INCIDENT_SUMMARY   -> buildIncidentSummary(report);
            case EVACUATION_REPORT  -> buildEvacuationReport(report);
            case MONTHLY_SUMMARY    -> buildMonthlySummary(report);
            case CUSTOM_REPORT      -> buildCustomReport(report);
        };
    }

    private ReportData buildResidentSummary(Report report) {
        List<Resident> residents = residentRepo.findAll();

        if (report.getStartDate() != null) {
            residents = residents.stream()
                    .filter(r -> !r.getCreatedAt().toLocalDate()
                            .isAfter(report.getStartDate())).toList();
        }

        if (report.getEndDate() != null) {
            residents = residents.stream()
                    .filter(r -> !r.getCreatedAt().toLocalDate()
                            .isAfter(report.getEndDate())).toList();
        }

        List<String> headers = List.of (
                "#", "Full Name", "Sex", "Age", "Civil Status",
                "Address", "Contract", "Vulnerabilities", "Date Registered");

        List<List<String>> rows = new ArrayList<>();
        int i = 1;
        for (Resident r : residents) {
            List<String> row = new ArrayList<>();
            row.add(String.valueOf(i++));
            row.add(r.getFullName());
            row.add(r.getSex() != null ? r.getSex() : "-");
            row.add(String.valueOf(r.getAge()));
            row.add(r.getCivilStatus() != null ? r.getCivilStatus() : "-");
            row.add(r.getAddress() != null ? r.getAddress() : "-");
            row.add(r.getContactNumber() != null ? r.getContactNumber() : "-");
            row.add(r.getVulnerabilities().isEmpty() ? "None"
                    : r.getVulnerabilities().stream()
                    .map(v -> v.getDisplayName())
                    .reduce((a, b) -> a + ", " + b).orElse(""));
            row.add(r.getCreatedAt() != null ? r.getCreatedAt().format(DT_FMT) : "-");
            rows.add(row);
        }

        long male   = residents.stream().filter(r -> "Male".equalsIgnoreCase(r.getSex())).count();
        long female = residents.stream().filter(r -> "Female".equalsIgnoreCase(r.getSex())).count();

        ReportData data = base(report, ReportType.RESIDENT_SUMMARY);
        data.setHeaders(headers);
        data.setRows(rows);
        data.setSummaryLines(List.of(
                "Total Residents: " + residents.size(),
                "Male: " + male + "  |  Female: " + female
        ));
        return data;
    }

    private ReportData buildResourceInventory(Report report) {
        List<Resource> resources = resourceRepo.findAll();

        List<String> headers = List.of(
                "#", "Resource Name", "Category", "Total Qty",
                "Available Qty", "Warning Level", "Unit", "Last Updated");

        List<List<String>> rows = new ArrayList<>();
        int idx = 1;
        for (Resource res : resources) {
            List<String> row = new ArrayList<>();
            row.add(String.valueOf(idx++));
            row.add(res.getName());
            row.add(res.getCategory() != null ? res.getCategory() : "-");
            row.add(String.valueOf(res.getTotalQty()));
            row.add(String.valueOf(res.getAvailableQty()));
            row.add(String.valueOf(res.getWarningLevel()));
            row.add(res.getUnit() != null ? res.getUnit() : "-");
            row.add(res.getUpdatedAt() != null ? res.getUpdatedAt().format(DT_FMT) : "-");
            rows.add(row);
        }

        long belowWarning = resources.stream()
                .filter(r -> r.getAvailableQty() < r.getWarningLevel())
                .count();

        ReportData data = base(report, ReportType.RESOURCE_INVENTORY);
        data.setHeaders(headers);
        data.setRows(rows);
        data.setSummaryLines(List.of(
                "Total Resource Types: " + resources.size(),
                "Below Warning Level: " + belowWarning
        ));
        return data;
    }

    private ReportData buildIncidentSummary(Report report) {
        List<Incident> incidents = incidentRepo.findAll();

        // Apply date-range filter (reported_at)
        if (report.getStartDate() != null) {
            incidents = incidents.stream()
                    .filter(i -> i.getReportedAt() != null
                            && !i.getReportedAt().toLocalDate().isBefore(report.getStartDate()))
                    .toList();
        }
        if (report.getEndDate() != null) {
            incidents = incidents.stream()
                    .filter(i -> i.getReportedAt() != null
                            && !i.getReportedAt().toLocalDate().isAfter(report.getEndDate()))
                    .toList();
        }

        List<String> headers = List.of(
                "#", "Title", "Type", "Severity", "Status",
                "Location (Purok)", "Reported At", "Resolved At");

        List<List<String>> rows = new ArrayList<>();
        int idx = 1;
        for (Incident i : incidents) {
            List<String> row = new ArrayList<>();
            row.add(String.valueOf(idx++));
            row.add(i.getTitle() != null ? i.getTitle() : "-");
            row.add(i.getType() != null ? i.getType().name() : "-");
            row.add(i.getSeverity() != null ? i.getSeverity().name() : "-");
            row.add(i.getStatus() != null ? i.getStatus().name() : "-");
            row.add(i.getLocationPurok() != null ? i.getLocationPurok() : "-");
            row.add(i.getReportedAt() != null ? i.getReportedAt().format(DT_FMT) : "-");
            row.add(i.getResolvedAt() != null ? i.getResolvedAt().format(DT_FMT) : "Ongoing");
            rows.add(row);
        }

        long resolved = incidents.stream()
                .filter(i -> i.getStatus() != null
                        && "RESOLVED".equalsIgnoreCase(i.getStatus().name()))
                .count();

        ReportData data = base(report, ReportType.INCIDENT_SUMMARY);
        data.setHeaders(headers);
        data.setRows(rows);
        data.setSummaryLines(List.of(
                "Total Incidents: " + incidents.size(),
                "Resolved: " + resolved + "  |  Ongoing: " + (incidents.size() - resolved)
        ));
        return data;
    }

    // ── Evacuation Report ─────────────────────────────────────────────────────

    private ReportData buildEvacuationReport(Report report) {
        // Uses ResidentRepository for now; can be replaced with EvacuationRepository
        List<Resident> residents = residentRepo.findAll();

        List<String> headers = List.of(
                "#", "Full Name", "Sex", "Age", "Address", "Contact");

        List<List<String>> rows = new ArrayList<>();
        int idx = 1;
        for (Resident r : residents) {
            List<String> row = new ArrayList<>();
            row.add(String.valueOf(idx++));
            row.add(r.getFullName());
            row.add(r.getSex() != null ? r.getSex() : "-");
            row.add(String.valueOf(r.getAge()));
            row.add(r.getAddress() != null ? r.getAddress() : "-");
            row.add(r.getContactNumber() != null ? r.getContactNumber() : "-");
            rows.add(row);
        }

        ReportData data = base(report, ReportType.EVACUATION_REPORT);
        data.setHeaders(headers);
        data.setRows(rows);
        data.setSummaryLines(List.of("Total Evacuees Listed: " + residents.size()));
        return data;
    }

    // ── Monthly Summary ───────────────────────────────────────────────────────

    private ReportData buildMonthlySummary(Report report) {
        List<Resident>  residents = residentRepo.findAll();
        List<Resource>  resources = resourceRepo.findAll();
        List<Incident>  incidents = incidentRepo.findAll();

        List<String> headers = List.of("Category", "Count");
        List<List<String>> rows = List.of(
                List.of("Total Residents", String.valueOf(residents.size())),
                List.of("Total Resource Types", String.valueOf(resources.size())),
                List.of("Total Incidents", String.valueOf(incidents.size()))
        );

        ReportData data = base(report, ReportType.MONTHLY_SUMMARY);
        data.setHeaders(headers);
        data.setRows(rows);
        data.setSummaryLines(List.of("Barangay Monthly Overview"));
        return data;
    }

    // ── Custom / Fallback ─────────────────────────────────────────────────────

    private ReportData buildCustomReport(Report report) {
        ReportData data = base(report, ReportType.CUSTOM_REPORT);
        data.setHeaders(List.of("Note"));
        data.setRows(List.of(List.of("Custom report — no data configured.")));
        data.setSummaryLines(List.of());
        return data;
    }

    private ReportData base(Report report, ReportType type) {
        ReportData d = new ReportData();
        d.setTitle(report.getName());
        d.setGeneratedBy(report.getGeneratedBy());
        d.setStartDate(report.getStartDate());
        d.setEndDate(report.getEndDate());
        d.setReportType(type);
        return d;
    }
}
