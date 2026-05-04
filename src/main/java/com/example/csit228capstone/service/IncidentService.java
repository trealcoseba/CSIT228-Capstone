package com.example.csit228capstone.service;

import com.example.csit228capstone.model.incident.Incident;
import com.example.csit228capstone.model.incident.IncidentStatus;
import com.example.csit228capstone.repository.IncidentRepository;
import com.example.csit228capstone.util.AlertEventBus;
import com.example.csit228capstone.util.AlertEventBus.EventType;

import java.util.List;
import java.util.UUID;

public class IncidentService {

    private final IncidentRepository repo = new IncidentRepository();

    public List<Incident> getAllIncidents() { return repo.findAll(); }
    public List<Incident> getActiveIncidents() { return repo.findActive(); }
    public int getActiveCount() { return repo.countActive(); }

    public Incident getById(UUID id) {
        return repo.findById(id).orElseThrow(() -> new RuntimeException("Incident not found: " + id));
    }

    public UUID reportIncident(Incident incident) {
        UUID id = repo.insert(incident);
        incident.setId(id);
        AlertEventBus.getInstance().publish(EventType.INCIDENT_CREATED, incident);
        return id;
    }

    public void updateStatus(UUID incidentId, IncidentStatus newStatus, UUID changedBy, String note) {
        repo.updateStatus(incidentId, newStatus, changedBy, note);
        Incident updated = getById(incidentId);
        EventType event = newStatus == IncidentStatus.RESOLVED
                ? EventType.INCIDENT_RESOLVED : EventType.INCIDENT_UPDATED;
        AlertEventBus.getInstance().publish(event, updated);
    }
}
