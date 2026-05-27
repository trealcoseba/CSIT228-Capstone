package com.example.csit228capstone.model.report;

import com.example.csit228capstone.model.Resident;
import com.example.csit228capstone.model.Resource;
import com.example.csit228capstone.model.incident.Incident;
import com.example.csit228capstone.repository.IncidentRepository;
import com.example.csit228capstone.repository.ResidentRepository;
import com.example.csit228capstone.repository.ResourceRepository;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ReportDataProvider {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    private final ResidentRepository residentRepo = new ResidentRepository();
    private final ResourceRepository resourceRepo = new ResourceRepository();
    private final IncidentRepository incidentRepo = new IncidentRepository();

    public ReportData<List<List<String>>> getData(Report report) {
        ReportType type = ReportType.fromDisplayName(report.getType());
        return switch (type) {
            case RESIDENT_SUMMARY           -> buildResidentSummary(report);
            case RESOURCE_INVENTORY         -> buildResourceInventory(report);
            case INCIDENT_REPORT            -> buildIncidentSummary(report);
            case EVACUATION_REPORT          -> buildEvacuationReport(report);
            case MONTHLY_SUMMARY            -> buildMonthlySummary(report);
            case CUSTOM_REPORT              -> buildCustomReport(report);
        };
    }

    private ReportData<List<List<String>>> buildResidentSummary(Report report) {
        List<Resident> residents = residentRepo.findAll();

        if (report.getStartDate() != null) {
            residents = residents.stream()
                    .filter(r -> r.getCreatedAt() == null ||
                            !r.getCreatedAt().toLocalDate().isBefore(report.getStartDate()))
                    .toList();
        }
        if (report.getEndDate() != null) {
            residents = residents.stream()
                    .filter(r -> r.getCreatedAt() == null ||
                            !r.getCreatedAt().toLocalDate().isAfter(report.getEndDate()))
                    .toList();
        }

        List<String> headers = List.of(
                "#", "Full Name", "Sex", "Age", "Civil Status",
                "Address", "Contact", "Vulnerabilities", "Date Registered");

        List<List<String>> rows = new ArrayList<>();
        int i = 1;
        for (Resident r : residents) {
            String vulns = "None";
            try {
                if (r.getVulnerabilities() != null && !r.getVulnerabilities().isEmpty()) {
                    vulns = r.getVulnerabilities().stream()
                            .map(v -> v.getDisplayName())
                            .reduce((a, b) -> a + ", " + b).orElse("None");
                }
            } catch (Exception e) {
                vulns = "Error loading";
            }

            String registrationDate = "-";
            try {
                if (r.getCreatedAt() != null) {
                    registrationDate = r.getCreatedAt().format(DT_FMT);
                }
            } catch (Exception e) {
                registrationDate = String.valueOf(r.getCreatedAt());
            }

            rows.add(List.of(
                    String.valueOf(i++),
                    nvl(r.getFullName()),
                    nvl(r.getSex()),
                    String.valueOf(r.getAge()),
                    nvl(r.getCivilStatus()),
                    nvl(r.getAddress()),
                    nvl(r.getContactNumber()),
                    vulns,
                    registrationDate
            ));
        }

        long male = residents.stream()
                .filter(r -> r.getSex() != null &&
                        ("M".equalsIgnoreCase(r.getSex().trim()) || "Male".equalsIgnoreCase(r.getSex().trim())))
                .count();

        long female = residents.stream()
                .filter(r -> r.getSex() != null &&
                        ("F".equalsIgnoreCase(r.getSex().trim()) || "Female".equalsIgnoreCase(r.getSex().trim())))
                .count();

        long other = residents.size() - male - female;

        return base(report, ReportType.RESIDENT_SUMMARY, headers, rows,
                List.of(
                        "Total Residents: " + residents.size(),
                        "Male: " + male + "  |  Female: " + female + "  |  Other/Unspecified: " + other
                ));
    }

    private ReportData<List<List<String>>> buildResourceInventory(Report report) {
        List<Resource> resources = resourceRepo.findAll();

        List<String> headers = List.of(
                "#", "Resource Name", "Category", "Total Qty",
                "Available Qty", "Warning Level", "Unit", "Last Updated");

        List<List<String>> rows = new ArrayList<>();
        int idx = 1;
        for (Resource res : resources) {
            String lastUpdated = "-";
            try {
                if (res.getUpdatedAt() != null) {
                    lastUpdated = res.getUpdatedAt().format(DT_FMT);
                }
            } catch (Exception e) {
                lastUpdated = String.valueOf(res.getUpdatedAt());
            }

            rows.add(List.of(
                    String.valueOf(idx++),
                    nvl(res.getName()),
                    nvl(res.getCategory()),
                    String.valueOf(res.getTotalQty()),
                    String.valueOf(res.getAvailableQty()),
                    String.valueOf(res.getWarningLevel()),
                    nvl(res.getUnit()),
                    lastUpdated
            ));
        }

        long below = resources.stream()
                .filter(r -> r.getAvailableQty() < r.getWarningLevel()).count();

        return base(report, ReportType.RESOURCE_INVENTORY, headers, rows,
                List.of(
                        "Total Resource Types: " + resources.size(),
                        "Below Warning Level: " + below
                ));
    }

    private ReportData<List<List<String>>> buildIncidentSummary(Report report) {
        List<Incident> incidents = incidentRepo.findAll();

        if (report.getStartDate() != null) {
            incidents = incidents.stream()
                    .filter(i -> i.getReportedAt() == null ||
                            !i.getReportedAt().toLocalDate().isBefore(report.getStartDate()))
                    .toList();
        }
        if (report.getEndDate() != null) {
            incidents = incidents.stream()
                    .filter(i -> i.getReportedAt() == null ||
                            !i.getReportedAt().toLocalDate().isAfter(report.getEndDate()))
                    .toList();
        }

        List<String> headers = List.of(
                "#", "Title", "Type", "Severity", "Status",
                "Location (Purok)", "Reported At", "Resolved At");

        List<List<String>> rows = new ArrayList<>();
        int idx = 1;
        for (Incident i : incidents) {
            String reportedStr = "-";
            String resolvedStr = "Ongoing";

            try {
                if (i.getReportedAt() != null) reportedStr = i.getReportedAt().format(DT_FMT);
            } catch (Exception ex) {
                reportedStr = String.valueOf(i.getReportedAt());
            }

            try {
                if (i.getResolvedAt() != null) resolvedStr = i.getResolvedAt().format(DT_FMT);
            } catch (Exception ex) {
                resolvedStr = String.valueOf(i.getResolvedAt());
            }

            rows.add(List.of(
                    String.valueOf(idx++),
                    nvl(i.getTitle()),
                    i.getType()     != null ? i.getType().name()     : "-",
                    i.getSeverity() != null ? i.getSeverity().name() : "-",
                    i.getStatus()   != null ? i.getStatus().name()   : "-",
                    nvl(i.getLocationPurok()),
                    reportedStr,
                    resolvedStr
            ));
        }

        long resolved = incidents.stream()
                .filter(i -> i.getStatus() != null &&
                        "RESOLVED".equalsIgnoreCase(i.getStatus().name()))
                .count();

        return base(report, ReportType.INCIDENT_REPORT, headers, rows,
                List.of(
                        "Total Incidents: " + incidents.size(),
                        "Resolved: " + resolved + "  |  Ongoing: " + (incidents.size() - resolved)
                ));
    }

    private ReportData<List<List<String>>> buildEvacuationReport(Report report) {
        List<Resident> residents = residentRepo.findAll();

        List<String> headers = List.of(
                "#", "Full Name", "Sex", "Age", "Address", "Contact Number");

        List<List<String>> rows = new ArrayList<>();
        int idx = 1;
        for (Resident r : residents) {
            rows.add(List.of(
                    String.valueOf(idx++),
                    nvl(r.getFullName()),
                    nvl(r.getSex()),
                    String.valueOf(r.getAge()),
                    nvl(r.getAddress()),
                    nvl(r.getContactNumber())
            ));
        }

        return base(report, ReportType.EVACUATION_REPORT, headers, rows,
                List.of("Total Evacuees Listed: " + residents.size()));
    }

    private ReportData<List<List<String>>> buildMonthlySummary(Report report) {
        List<Resident> residents = residentRepo.findAll();
        List<Resource> resources = resourceRepo.findAll();
        List<Incident> incidents = incidentRepo.findAll();

        long resolved = incidents.stream()
                .filter(i -> i.getStatus() != null &&
                        "RESOLVED".equalsIgnoreCase(i.getStatus().name()))
                .count();
        long belowWarn = resources.stream()
                .filter(r -> r.getAvailableQty() < r.getWarningLevel()).count();

        List<String> headers = List.of("Category", "Value");
        List<List<String>> rows = List.of(
                List.of("Total Registered Residents",  String.valueOf(residents.size())),
                List.of("Total Resource Types",         String.valueOf(resources.size())),
                List.of("Resources Below Warning Level",String.valueOf(belowWarn)),
                List.of("Total Incidents",              String.valueOf(incidents.size())),
                List.of("Resolved Incidents",           String.valueOf(resolved)),
                List.of("Ongoing Incidents",            String.valueOf(incidents.size() - resolved))
        );

        return base(report, ReportType.MONTHLY_SUMMARY, headers, rows,
                List.of("Barangay Monthly Operations Overview"));
    }

    private ReportData<List<List<String>>> buildCustomReport(Report report) {
        return base(report, ReportType.CUSTOM_REPORT,
                List.of("Note"),
                List.of(List.of("No data configured for this custom report.")),
                List.of());
    }

    private ReportData<List<List<String>>> base(Report report, ReportType type,
                                                List<String> headers,
                                                List<List<String>> rows,
                                                List<String> summary) {
        ReportData<List<List<String>>> d = new ReportData<>();
        d.setTitle(report.getName());
        d.setGeneratedBy(report.getGeneratedBy());
        d.setStartDate(report.getStartDate());
        d.setEndDate(report.getEndDate());
        d.setReportType(type);
        d.setHeaders(headers);
        d.setSummaryLines(summary);
        d.setPayload(rows);
        return d;
    }

    private String nvl(String s) { return s != null ? s : "-"; }
}