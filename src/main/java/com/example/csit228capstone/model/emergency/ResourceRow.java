package com.example.csit228capstone.model.emergency;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/** Observable row for the Resources TableView. */
public class ResourceRow {

    private final StringProperty resourceType = new SimpleStringProperty();
    private final StringProperty quantity     = new SimpleStringProperty();
    private final StringProperty status       = new SimpleStringProperty();

    public ResourceRow(String type, double available, double total, String unit) {
        resourceType.set(type);
        String unitStr = (unit != null && !unit.isBlank()) ? " " + unit : "";
        quantity.set(String.format("%.0f%s", available, unitStr));
        // Derive status from availability ratio
        double ratio = total > 0 ? available / total : 1.0;
        if (ratio >= 0.5)       status.set("Adequate");
        else if (ratio >= 0.2)  status.set("Low");
        else                    status.set("Critical");
    }

    public StringProperty resourceTypeProperty() { return resourceType; }
    public StringProperty quantityProperty()     { return quantity; }
    public StringProperty statusProperty()       { return status; }

    public String getResourceType() { return resourceType.get(); }
    public String getQuantity()     { return quantity.get(); }
    public String getStatus()       { return status.get(); }
}
