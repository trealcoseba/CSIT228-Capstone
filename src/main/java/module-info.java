module com.example.csit228capstone {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;

    opens com.example.csit228capstone to javafx.fxml;
    exports com.example.csit228capstone;
}