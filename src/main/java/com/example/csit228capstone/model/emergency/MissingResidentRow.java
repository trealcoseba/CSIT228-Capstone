package com.example.csit228capstone.model.emergency;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.UUID;

/** Observable row for the Missing Residents TableView. */
public class MissingResidentRow {

    private final UUID residentId;

    private final StringProperty residentName       = new SimpleStringProperty();
    private final StringProperty contactNumber      = new SimpleStringProperty();
    private final StringProperty lastKnownLocation  = new SimpleStringProperty();

    public MissingResidentRow(UUID id, String name, String contact, String location) {
        this.residentId = id;
        residentName.set(name);
        contactNumber.set(contact != null ? contact : "—");
        lastKnownLocation.set(location != null ? location : "Unknown");
    }

    public StringProperty residentNameProperty()      { return residentName; }
    public StringProperty contactNumberProperty()     { return contactNumber; }
    public StringProperty lastKnownLocationProperty() { return lastKnownLocation; }

    public String getResidentName()      { return residentName.get(); }
    public String getContactNumber()     { return contactNumber.get(); }
    public String getLastKnownLocation() { return lastKnownLocation.get(); }

    public UUID getResidentId() { return residentId; }
}
