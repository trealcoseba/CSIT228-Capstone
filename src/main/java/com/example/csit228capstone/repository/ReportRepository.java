package com.example.csit228capstone.repository;

import com.example.csit228capstone.model.report.Report;
import com.example.csit228capstone.model.report.ReportFormData.DamageRow;
import com.example.csit228capstone.model.report.ReportFormData.InjuryRow;
import com.example.csit228capstone.util.SupabaseConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Data-access layer for the {@code reports} table and its structural sub-tables.
 */
public class ReportRepository {

    private static volatile boolean tableInitialized = false;

    private Connection conn() throws SQLException {
        return SupabaseConnectionManager.getInstance().getConnection();
    }

    // ── Table bootstrap ───────────────────────────────────────────────────────

    public void initTable() {
        if (tableInitialized) return;
        synchronized (ReportRepository.class) {
            if (tableInitialized) return;

            // FIXED: Added incident_type_other TEXT to the database layout
            String createReportsSql = """
                    CREATE TABLE IF NOT EXISTS reports (
                        id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        name                VARCHAR(255)  NOT NULL,
                        type                VARCHAR(100)  NOT NULL,
                        report_no           VARCHAR(50),
                        date_of_report      DATE,
                        generated_by        VARCHAR(255),
                        recorded_by         VARCHAR(255),
                        reporter_contact    TEXT,
                        recorder_contact    TEXT,
                        generated_date_time TIMESTAMPTZ   NOT NULL DEFAULT now(),
                        start_date          DATE,
                        end_date            DATE,
                        date_of_incident    DATE,
                        location            TEXT,
                        description         TEXT,
                        has_insurance       BOOLEAN       DEFAULT FALSE,
                        insurance_policy    VARCHAR(255),
                        insurance_coverage_amt VARCHAR(255),
                        incident_type_other TEXT
                    );
                    """;

            String createIncidentTypesSql = """
                    CREATE TABLE IF NOT EXISTS report_incident_types (
                        id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        report_id UUID REFERENCES reports(id) ON DELETE CASCADE,
                        type_name VARCHAR(100) NOT NULL
                    );
                    """;

            String createDamagesSql = """
                    CREATE TABLE IF NOT EXISTS report_damages (
                        id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        report_id   UUID REFERENCES reports(id) ON DELETE CASCADE,
                        damage_item TEXT,
                        est_value   VARCHAR(100),
                        repair_plan TEXT,
                        repair_cost VARCHAR(100)
                    );
                    """;

            String createInjuriesSql = """
                    CREATE TABLE IF NOT EXISTS report_injuries (
                        id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        report_id      UUID REFERENCES reports(id) ON DELETE CASCADE,
                        injured_person TEXT,
                        position_status VARCHAR(100),
                        medical_cost   VARCHAR(100),
                        insurance      VARCHAR(100)
                    );
                    """;

            try (Connection c = conn(); Statement st = c.createStatement()) {
                st.execute(createReportsSql);
                st.execute(createIncidentTypesSql);
                st.execute(createDamagesSql);
                st.execute(createInjuriesSql);

                // ALTER TABLE fallback execution check to update existing tables if they exist without column
                try {
                    st.execute("ALTER TABLE reports ADD COLUMN IF NOT EXISTS incident_type_other TEXT;");
                } catch (SQLException ignore) {
                    // Safe catch block for DBMS instances that don't support ADD COLUMN IF NOT EXISTS syntax smoothly
                }

                tableInitialized = true;
            } catch (SQLException e) {
                throw new RuntimeException("Could not initialise reports database topology: " + e.getMessage(), e);
            }
        }
    }

    // ── READ SUB-TABLE DATA METHODS ──────────────────────────────────────────

    public List<String> findIncidentTypesByReportId(UUID reportId) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT type_name FROM report_incident_types WHERE report_id = ? ORDER BY type_name";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, reportId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getString("type_name"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find incident types: " + e.getMessage(), e);
        }
        return list;
    }

    public List<DamageRow> findDamagesByReportId(UUID reportId) {
        List<DamageRow> list = new ArrayList<>();
        String sql = "SELECT * FROM report_damages WHERE report_id = ?";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, reportId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DamageRow row = new DamageRow();
                    row.damage = rs.getString("damage_item");
                    row.value = rs.getString("est_value");
                    row.repairPlan = rs.getString("repair_plan");
                    row.repairCost = rs.getString("repair_cost");
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not load damages: " + e.getMessage(), e);
        }
        return list;
    }

    public List<InjuryRow> findInjuriesByReportId(UUID reportId) {
        List<InjuryRow> list = new ArrayList<>();
        String sql = "SELECT * FROM report_injuries WHERE report_id = ?";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, reportId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    InjuryRow row = new InjuryRow();
                    row.injuredPerson = rs.getString("injured_person");
                    row.position = rs.getString("position_status");
                    row.medicalCost = rs.getString("medical_cost");
                    row.insurance = rs.getString("insurance");
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not load injuries: " + e.getMessage(), e);
        }
        return list;
    }

    // ── WRITE SUB-TABLE DATA METHODS ─────────────────────────────────────────

    public void saveIncidentTypes(UUID reportId, List<String> types) {
        if (types == null || types.isEmpty()) return;
        String sql = "INSERT INTO report_incident_types (report_id, type_name) VALUES (?, ?)";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (String t : types) {
                ps.setObject(1, reportId);
                ps.setString(2, t);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Could not batch save incident type checks: " + e.getMessage(), e);
        }
    }

    public void saveDamages(UUID reportId, List<DamageRow> damages) {
        if (damages == null || damages.isEmpty()) return;
        String sql = "INSERT INTO report_damages (report_id, damage_item, est_value, repair_plan, repair_cost) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (DamageRow dr : damages) {
                ps.setObject(1, reportId);
                ps.setString(2, dr.damage);
                ps.setString(3, dr.value);
                ps.setString(4, dr.repairPlan);
                ps.setString(5, dr.repairCost);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Could not batch save property damages: " + e.getMessage(), e);
        }
    }

    public void saveInjuries(UUID reportId, List<InjuryRow> injuries) {
        if (injuries == null || injuries.isEmpty()) return;
        String sql = "INSERT INTO report_injuries (report_id, injured_person, position_status, medical_cost, insurance) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (InjuryRow ir : injuries) {
                ps.setObject(1, reportId);
                ps.setString(2, ir.injuredPerson);
                ps.setString(3, ir.position);
                ps.setString(4, ir.medicalCost);
                ps.setString(5, ir.insurance);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Could not batch save injuries: " + e.getMessage(), e);
        }
    }

    // ── CORE READ ─────────────────────────────────────────────────────────────

    public List<Report> findAll() {
        return findAllFiltered("", "generated_date_time", false);
    }

    public List<Report> findAllFiltered(String searchText, String sortField, boolean ascending) {
        String safeSort = switch ((sortField == null ? "" : sortField).toLowerCase()) {
            case "name"         -> "LOWER(name)";
            case "generated_by" -> "LOWER(generated_by)";
            case "type"         -> "LOWER(type)";
            default             -> "generated_date_time";
        };
        String dir  = ascending ? "ASC" : "DESC";
        String sql  = "SELECT * FROM reports " +
                "WHERE (LOWER(name) LIKE ? OR LOWER(generated_by) LIKE ?) " +
                "ORDER BY " + safeSort + " " + dir;
        String like = "%" + (searchText == null ? "" : searchText.toLowerCase()) + "%";

        List<Report> list = new ArrayList<>();
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not load reports: " + e.getMessage(), e);
        }
        return list;
    }

    public Report findById(UUID id) {
        try (Connection c = conn();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM reports WHERE id = ?")) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not find report: " + e.getMessage(), e);
        }
    }

    // ── CORE WRITE ────────────────────────────────────────────────────────────

    public UUID insert(Report report) {
        String sql = """
                INSERT INTO reports
                  (name, type, report_no, date_of_report, generated_by, recorded_by,
                   reporter_contact, recorder_contact, generated_date_time,
                   start_date, end_date, date_of_incident, location, description,
                   has_insurance, insurance_policy, insurance_coverage_amt, incident_type_other)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id
                """;
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1,  report.getName());
            ps.setString(2,  report.getType());
            ps.setString(3,  report.getReportNo());
            ps.setObject(4,  report.getDateOfReport() != null ? Date.valueOf(report.getDateOfReport()) : null);
            ps.setString(5,  report.getGeneratedBy());
            ps.setString(6,  report.getRecordedBy());
            ps.setString(7,  report.getReporterContact());
            ps.setString(8,  report.getRecorderContact());
            ps.setTimestamp(9, report.getGeneratedDateTime() != null
                    ? Timestamp.valueOf(report.getGeneratedDateTime()) : null);
            ps.setObject(10, report.getStartDate()       != null ? Date.valueOf(report.getStartDate())       : null);
            ps.setObject(11, report.getEndDate()         != null ? Date.valueOf(report.getEndDate())         : null);
            ps.setObject(12, report.getDateOfIncident()  != null ? Date.valueOf(report.getDateOfIncident())  : null);
            ps.setString(13, report.getLocation());
            ps.setString(14, report.getDescription());

            ps.setObject(15, report.getHasInsurance(), java.sql.Types.BOOLEAN);
            ps.setString(16, report.getInsurancePolicy());
            ps.setString(17, report.getInsuranceCoverageAmt());

            // FIXED: Map to column index 18
            ps.setString(18, report.getIncidentTypeOther());

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getObject("id", UUID.class) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not insert report: " + e.getMessage(), e);
        }
    }

    public void update(Report report) {
        String sql = """
                UPDATE reports SET
                    name = ?, type = ?, report_no = ?, date_of_report = ?,
                    generated_by = ?, recorded_by = ?,
                    reporter_contact = ?, recorder_contact = ?,
                    start_date = ?, end_date = ?, date_of_incident = ?,
                    location = ?, description = ?,
                    has_insurance = ?, insurance_policy = ?, insurance_coverage_amt = ?,
                    incident_type_other = ?
                WHERE id = ?
                """;
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1,  report.getName());
            ps.setString(2,  report.getType());
            ps.setString(3,  report.getReportNo());
            ps.setObject(4,  report.getDateOfReport()    != null ? Date.valueOf(report.getDateOfReport())    : null);
            ps.setString(5,  report.getGeneratedBy());
            ps.setString(6,  report.getRecordedBy());
            ps.setString(7,  report.getReporterContact());
            ps.setString(8,  report.getRecorderContact());
            ps.setObject(9,  report.getStartDate()       != null ? Date.valueOf(report.getStartDate())       : null);
            ps.setObject(10, report.getEndDate()         != null ? Date.valueOf(report.getEndDate())         : null);
            ps.setObject(11, report.getDateOfIncident()  != null ? Date.valueOf(report.getDateOfIncident())  : null);
            ps.setString(12, report.getLocation());
            ps.setString(13, report.getDescription());

            ps.setObject(14, report.getHasInsurance(), java.sql.Types.BOOLEAN);
            ps.setString(15, report.getInsurancePolicy());
            ps.setString(16, report.getInsuranceCoverageAmt());

            // FIXED: Map incidentTypeOther to position 17
            ps.setString(17, report.getIncidentTypeOther());

            ps.setObject(18, report.getId());
            if (ps.executeUpdate() == 0)
                throw new RuntimeException("No report found with id " + report.getId());
        } catch (SQLException e) {
            throw new RuntimeException("Could not update report: " + e.getMessage(), e);
        }
    }

    public void deleteById(UUID id) {
        try (Connection c = conn();
             PreparedStatement ps = c.prepareStatement("DELETE FROM reports WHERE id = ?")) {
            ps.setObject(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete report: " + e.getMessage(), e);
        }
    }

    public void deleteByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) return;
        StringBuilder sb = new StringBuilder("DELETE FROM reports WHERE id IN (");
        for (int i = 0; i < ids.size(); i++) { sb.append("?"); if (i < ids.size()-1) sb.append(","); }
        sb.append(")");
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sb.toString())) {
            for (int i = 0; i < ids.size(); i++) ps.setObject(i+1, ids.get(i));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not bulk-delete reports: " + e.getMessage(), e);
        }
    }

    // ── MAPPING ───────────────────────────────────────────────────────────────

    private Report mapRow(ResultSet rs) throws SQLException {
        Report report = new Report();
        report.setId(rs.getObject("id", UUID.class));
        report.setName(rs.getString("name"));
        report.setType(rs.getString("type"));
        report.setReportNo(rs.getString("report_no"));

        Date dOfReport = rs.getDate("date_of_report");
        if (dOfReport != null) report.setDateOfReport(dOfReport.toLocalDate());

        report.setGeneratedBy(rs.getString("generated_by"));
        report.setRecordedBy(rs.getString("recorded_by"));
        report.setReporterContact(rs.getString("reporter_contact"));
        report.setRecorderContact(rs.getString("recorder_contact"));

        Timestamp gdt = rs.getTimestamp("generated_date_time");
        if (gdt != null) report.setGeneratedDateTime(gdt.toLocalDateTime());

        Date sDate = rs.getDate("start_date");
        if (sDate != null) report.setStartDate(sDate.toLocalDate());

        Date eDate = rs.getDate("end_date");
        if (eDate != null) report.setEndDate(eDate.toLocalDate());

        Date dOfIncident = rs.getDate("date_of_incident");
        if (dOfIncident != null) report.setDateOfIncident(dOfIncident.toLocalDate());

        report.setLocation(rs.getString("location"));
        report.setDescription(rs.getString("description"));

        report.setHasInsurance((Boolean) rs.getObject("has_insurance"));
        report.setInsurancePolicy(rs.getString("insurance_policy"));
        report.setInsuranceCoverageAmt(rs.getString("insurance_coverage_amt"));

        // FIXED: Added mapping column logic back out from Supabase row values
        report.setIncidentTypeOther(rs.getString("incident_type_other"));

        return report;
    }
}