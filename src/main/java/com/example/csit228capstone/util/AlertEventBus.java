package com.example.csit228capstone.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class AlertEventBus {

    public enum EventType {
        INCIDENT_CREATED, INCIDENT_UPDATED, INCIDENT_RESOLVED,
        DOCUMENT_STATUS_CHANGED, ALERT_BROADCAST,
        RESOURCE_UPDATED, EVACUATION_UPDATED, CHATBOT_ESCALATED
    }

    public record AppEvent(EventType type, Object payload) {}

    private static final AlertEventBus INSTANCE = new AlertEventBus();
    private final List<Consumer<AppEvent>> listeners = new CopyOnWriteArrayList<>();

    private AlertEventBus() {}

    public static AlertEventBus getInstance() { return INSTANCE; }

    public void subscribe(Consumer<AppEvent> listener) {
        listeners.add(listener);
    }

    public void unsubscribe(Consumer<AppEvent> listener) {
        listeners.remove(listener);
    }

    public void publish(AppEvent event) {
        for (Consumer<AppEvent> listener : listeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                System.err.println("EventBus listener error: " + e.getMessage());
            }
        }
    }

    public void publish(EventType type, Object payload) {
        publish(new AppEvent(type, payload));
    }
}
