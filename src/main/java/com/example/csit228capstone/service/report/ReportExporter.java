package com.example.csit228capstone.service.report;

import com.example.csit228capstone.model.report.ReportData;
import java.io.File;

public interface ReportExporter<T> {
    void export(ReportData<T> data, File destination) throws Exception;
    String getFormatName();
}