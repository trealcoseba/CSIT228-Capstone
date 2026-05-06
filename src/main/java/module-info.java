module ligtas.brgy {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.sql;
    requires javafx.base;
    requires java.net.http;
    requires com.zaxxer.hikari;
    requires org.postgresql.jdbc;
    requires com.google.gson;
    requires layout;
    requires kernel;

    opens com.example.csit228capstone.app to javafx.fxml;
    opens com.example.csit228capstone.model to javafx.base;

    exports com.example.csit228capstone.app;
    exports com.example.csit228capstone.model;
    exports com.example.csit228capstone.service;
    exports com.example.csit228capstone.repository;
    exports com.example.csit228capstone.util;
    exports com.example.csit228capstone.ai;
    exports com.example.csit228capstone.controller.resident;
    opens com.example.csit228capstone.controller.resident to javafx.fxml;
    exports com.example.csit228capstone.controller.login;
    opens com.example.csit228capstone.controller.login to javafx.fxml;
    exports com.example.csit228capstone.controller.mainlayout;
    opens com.example.csit228capstone.controller.mainlayout to javafx.fxml;
    exports com.example.csit228capstone.controller.dashboard;
    opens com.example.csit228capstone.controller.dashboard to javafx.fxml;
    exports com.example.csit228capstone.controller.documents;
    opens com.example.csit228capstone.controller.documents to javafx.fxml;
    exports com.example.csit228capstone.controller.reports;
    opens com.example.csit228capstone.controller.reports to javafx.fxml;
    exports com.example.csit228capstone.controller.settings;
    opens com.example.csit228capstone.controller.settings to javafx.fxml;
    exports com.example.csit228capstone.controller.incidents;
    opens com.example.csit228capstone.controller.incidents to javafx.fxml;
    exports com.example.csit228capstone.controller.chatbot;
    opens com.example.csit228capstone.controller.chatbot to javafx.fxml;
    exports com.example.csit228capstone.controller.resources;
    opens com.example.csit228capstone.controller.resources to javafx.fxml;
    exports com.example.csit228capstone.controller.alerts;
    opens com.example.csit228capstone.controller.alerts to javafx.fxml;
    exports com.example.csit228capstone.controller.evacuation;
    opens com.example.csit228capstone.controller.evacuation to javafx.fxml;
    exports com.example.csit228capstone.model.incident;
    opens com.example.csit228capstone.model.incident to javafx.base;
    exports com.example.csit228capstone.model.chatbot;
    opens com.example.csit228capstone.model.chatbot to javafx.base;
    exports com.example.csit228capstone.model.vulnerability;
    opens com.example.csit228capstone.model.vulnerability to javafx.base;
    exports com.example.csit228capstone.model.document;
    opens com.example.csit228capstone.model.document to javafx.base;
    exports com.example.csit228capstone.model.alert;
    opens com.example.csit228capstone.model.alert to javafx.base;
    opens com.example.csit228capstone.model.report to javafx.base, javafx.fxml;

}
