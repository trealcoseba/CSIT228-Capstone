package com.example.csit228capstone.model.emergency;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.UUID;

/** Observable row for the Evacuation Centers TableView. */
public class EvacuationCenterRow {

    private final UUID centerId;

    private final StringProperty centerName = new SimpleStringProperty();
    private final StringProperty capacity   = new SimpleStringProperty();
    private final StringProperty occupancy  = new SimpleStringProperty();
    private final StringProperty distance   = new SimpleStringProperty();
    private final StringProperty status     = new SimpleStringProperty();

    public EvacuationCenterRow(UUID id, String name,
                               int cap, int occ, String dist, String stat) {
        this.centerId = id;
        centerName.set(name);
        capacity.set(String.valueOf(cap));
        occupancy.set(String.valueOf(occ));
        distance.set(dist);
        status.set(stat);
    }

    public StringProperty centerNameProperty() { return centerName; }
    public StringProperty capacityProperty()   { return capacity; }
    public StringProperty occupancyProperty()  { return occupancy; }
    public StringProperty distanceProperty()   { return distance; }
    public StringProperty statusProperty()     { return status; }

    public String getCenterName() { return centerName.get(); }
    public String getCapacity()   { return capacity.get(); }
    public String getOccupancy()  { return occupancy.get(); }
    public String getDistance()   { return distance.get(); }
    public String getStatus()     { return status.get(); }

    public UUID getCenterId()     { return centerId; }
    public int  getCapacityInt()  { return Integer.parseInt(capacity.get()); }
    public int  getOccupancyInt() { return Integer.parseInt(occupancy.get()); }
}
