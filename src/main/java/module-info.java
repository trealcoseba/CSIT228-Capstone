module ligtas.brgy {

    // ── JavaFX Core ──────────────────────────────────────────────────────────
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.base;

    // ── Database & Connectivity ──────────────────────────────────────────────
    requires java.sql;
    requires java.net.http;
    requires java.desktop;
    requires com.zaxxer.hikari;
    requires org.postgresql.jdbc;

    // ── JSON & Logging ───────────────────────────────────────────────────────
    requires com.google.gson;
    requires org.slf4j;

    // ── Document Generation (iText 8 & Apache POI) ──────────────────────────
    requires commons;
    requires io;
    requires kernel;
    requires layout;
    requires forms;
    requires org.apache.poi.ooxml;

    // ── Hardware & Browser Integration ───────────────────────────────────────
    requires webcam.capture;
    requires jdk.jsobject;

    // ── Package Exports ──────────────────────────────────────────────────────
    // (Allows other modules to access these classes directly)
    exports com.example.csit228capstone.app;
    exports com.example.csit228capstone.model;
    exports com.example.csit228capstone.model.incident;
    exports com.example.csit228capstone.model.chatbot;
    exports com.example.csit228capstone.model.vulnerability;
    exports com.example.csit228capstone.model.document;
    exports com.example.csit228capstone.model.alert;
    exports com.example.csit228capstone.model.report;

    exports com.example.csit228capstone.service;
    exports com.example.csit228capstone.repository;
    exports com.example.csit228capstone.util;
    exports com.example.csit228capstone.ai;

    exports com.example.csit228capstone.controller.resident;
    exports com.example.csit228capstone.controller.login;
    exports com.example.csit228capstone.controller.mainlayout;
    exports com.example.csit228capstone.controller.dashboard;
    exports com.example.csit228capstone.controller.documents;
    exports com.example.csit228capstone.controller.reports;
    exports com.example.csit228capstone.controller.settings;
    exports com.example.csit228capstone.controller.incidents;
    exports com.example.csit228capstone.controller.chatbot;
    exports com.example.csit228capstone.controller.resources;
    exports com.example.csit228capstone.controller.alerts;
    exports com.example.csit228capstone.controller.evacuation;
    exports com.example.csit228capstone.controller.analytics;
    exports com.example.csit228capstone.controller.map;
    exports com.example.csit228capstone.controller.emergency;

    // ── Package Opens ────────────────────────────────────────────────────────
    // (Allows JavaFX FXML and TableView Reflection to access these classes)

    // Models need to be open to javafx.base for TableView reflection
    opens com.example.csit228capstone.model to javafx.base;
    opens com.example.csit228capstone.model.report to javafx.base, javafx.fxml;
    opens com.example.csit228capstone.model.incident to javafx.base;
    opens com.example.csit228capstone.model.chatbot to javafx.base;
    opens com.example.csit228capstone.model.vulnerability to javafx.base;
    opens com.example.csit228capstone.model.document to javafx.base;
    opens com.example.csit228capstone.model.alert to javafx.base;

    // Controllers and App need to be open to javafx.fxml
    opens com.example.csit228capstone.app to javafx.fxml;
    opens com.example.csit228capstone.service to javafx.fxml;
    opens com.example.csit228capstone.service.report to javafx.fxml;

    opens com.example.csit228capstone.controller.resident to javafx.fxml;
    opens com.example.csit228capstone.controller.login to javafx.fxml;
    opens com.example.csit228capstone.controller.mainlayout to javafx.fxml;
    opens com.example.csit228capstone.controller.dashboard to javafx.fxml;
    opens com.example.csit228capstone.controller.documents to javafx.fxml;
    opens com.example.csit228capstone.controller.reports to javafx.fxml;
    opens com.example.csit228capstone.controller.settings to javafx.fxml;
    opens com.example.csit228capstone.controller.incidents to javafx.fxml;
    opens com.example.csit228capstone.controller.chatbot to javafx.fxml;
    opens com.example.csit228capstone.controller.resources to javafx.fxml;
    opens com.example.csit228capstone.controller.alerts to javafx.fxml;
    opens com.example.csit228capstone.controller.evacuation to javafx.fxml;
    opens com.example.csit228capstone.controller.analytics to javafx.fxml;
    opens com.example.csit228capstone.controller.map to javafx.fxml;
    opens com.example.csit228capstone.controller.emergency to javafx.fxml;

    // Legacy or specific FXML access
    opens com.example.csit228capstone.incident to javafx.fxml;
}