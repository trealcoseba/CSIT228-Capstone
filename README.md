# LIGTAS-Brgy
**Local Integrated Guard & Task-Alert System for Barangays**

A Java-based desktop disaster management command center for Barangay officials.

---

## Group Members

| Name |
|---|
| Lemuel Vincent Alcoseba III |
| Daniel Duarte Jr. |
| Michael Lois Gaviola |
| Nathanael Andrew Mendoza |
| Jacy Erl Sangre |

---

## Project Description

At the barangay level, disaster response is often reactive — fragmented records, no centralized equipment tracking, and slow manual reporting leave officials underprepared when emergencies strike. Critical data on vulnerable residents (senior citizens, PWDs, pregnant women, children) is frequently kept in physical logbooks that can be lost or damaged during the very disasters they are meant to help manage.

**LIGTAS-Brgy** solves this by giving barangay officials a single, centralized desktop system to manage resident risk profiles, track emergency resources, monitor evacuation centers, manage responders, issue broadcast alerts, generate official documents, and produce standardized disaster reports — all backed by a live cloud database and an AI-powered resident chatbot.

---

## Features

### Dashboard
- Real-time overview of active incidents, resource status, evacuation center occupancy, and pending document requests
- Emergency Mode toggle with passkey authorization to activate full disaster response protocol
- Quick-access panels for recent alerts and key statistics

### Incident Management
- Log incidents with type (Flood, Fire, Typhoon, Earthquake, Landslide, Medical Emergency, Missing Person, Other), severity (Critical, Major, Minor), and GPS-pinned location via Leaflet.js map
- Track incidents through a full status lifecycle: Reported → Dispatched → Responding → Monitoring → Resolved
- Full timeline view per incident showing every status change with timestamps, notes, and the user who made the update
- Add and view incidents through dedicated form and detail dialogs

### Emergency Mode
- Passkey-protected Emergency Mode that launches a dedicated command dashboard
- Trigger Incident form for declaring a disaster with incident type, severity, radius, and map-pinned location
- Emergency dashboard shows live data: priority residents (vulnerable groups), missing residents, resource levels, evacuation center status, and dispatched rescue teams — all auto-refreshed
- Bar chart visualization of resource availability during active emergencies

### Responder Management
- Maintain a registry of emergency responders from agencies: BFP, PNP, MDRRMO, NBI, PCG, AFP, DOH
- Track responder status: Available, On Mission, Off Duty
- View total and active dispatch counts per responder
- Dispatch responders to incidents with location, severity, and time-of-occurrence via a dedicated dispatch form
- Dispatch history log with status tracking: Dispatched, Returned, Cancelled

### Resident Records
- Full resident profiles: name, date of birth, sex, civil status, contact number, address, and optional photo (webcam or file upload)
- Vulnerability tagging per resident: Senior Citizen, PWD, Pregnant, Child (0–5), Solo Parent, Indigenous
- Household-based grouping with purok and GPS coordinates
- Search and filter by name, purok, or vulnerability tag

### Resource Inventory
- Track emergency supplies by category (Relief Goods, Medical Supplies, Equipment, etc.) with total quantity, available quantity, warning levels, and unit
- Status badges per item: OK, LOW, OUT
- Resource usage and transfer logging — records which evacuation center received what, how much, and for what purpose
- Usage log with sortable history and per-resource filtering with a clear-filter button

### Evacuation Center Monitoring
- View all evacuation centers with name, address, GPS coordinates, capacity, and current occupancy
- Add and edit centers via a form with an integrated map picker for pinning the exact location
- Occupancy percentage tracking and active/inactive status

### Broadcast Alerts
- Create and send alerts with type (Weather Advisory, Flood Warning, Evacuation Order, Health Notice, Security Alert, General Announcement) and priority (Info, Warning, Critical)
- Optional scheduling with an expiry date
- Edit existing broadcasts and view full alert history with timestamps
- Alerts stored in the database with the issuing official's name

### Document Requests & Generation
- Process resident requests for official barangay documents: Barangay Clearance, Certificate of Indigency, Certificate of Residency, Blotter Report, Business Permit
- Admin workflow: Pending → Processing → Ready → Released / Rejected
- Approved requests generate a formatted PDF using iText 7

### Reports
- Generate formal reports in **PDF** and **Word (.docx)** format — both export formats fully supported
- Report types: Residents Report, Resource Inventory, Incident Report (Natural Disaster), Evacuation Report, Monthly Summary, Custom Report
- Incident reports include structured sections for disaster type checkboxes (Fire, Flooding, Earthquake, Hurricane, Tornado, Tsunami, Volcano, Avalanche, Blizzard, Drought, Storm), property damage table, injury table, and insurance details
- All reports include official government header, reporter/recorder metadata, report number, and coverage period
- Reports are saved to the database and viewable/downloadable from a central report log
- Background thread pool (4 threads) handles all report generation to keep the UI responsive

### Analytics
- Visual charts and statistics on incidents by type, resource levels, resident demographics, and evacuation data
- Month-over-month incident comparisons pulled directly from the database

### Settings
- User account and system configuration management

---

## Technologies Used

| Technology | Version | Purpose |
|---|---|---|
| **Java** | 21 | Core application language |
| **JavaFX + FXML** | 21 | Desktop GUI framework |
| **JDBC + HikariCP** | 5.1.0 | Database connection pooling |
| **Supabase (PostgreSQL)** | — | Cloud-hosted relational database |
| **iText 7** | 8.0.3 | PDF report and document generation |
| **Apache POI** | — | Word (.docx) report generation |
| **Leaflet.js** | (bundled) | Interactive map rendering via JavaFX WebView |
| **Anthropic Claude API** | claude-sonnet-4 | AI chatbot (LIGTAS-AI) integration |
| **Webcam Capture** | 0.3.12 | Resident photo capture |
| **Gson** | 2.10.1 | JSON parsing |
| **Maven** | — | Build and dependency management |

---

## Architecture & Design Patterns

### MVC (Model-View-Controller)
Every module follows a strict MVC structure. FXML files define the view, dedicated controller classes handle all UI logic, and model classes hold the data. Complex modules (Reports, Incidents, Responders) use multiple controllers — a list controller, a form controller, and a detail/preview controller — to keep responsibilities separated.

### Singleton
`SupabaseConnectionManager` manages a single shared HikariCP connection pool across the entire application. It is instantiated once at startup and shut down gracefully on exit.

```java
SupabaseConnectionManager.getInstance().getConnection();
```

`AlertEventBus` is also a Singleton — one shared event bus instance routes events between services and controllers.

### Factory
`ReportFactory` generates iText PDF documents (Barangay Clearance, Certificate of Indigency, Blotter Report, etc.) from a single `generate(DocumentType, Resident, String)` entry point. The `ReportService` further extends this with a registry-based exporter map — PDF and DOCX exporters are registered by key, and the correct one is dispatched at runtime without conditional logic at the call site.

```java
// One entry point, different output per type
ReportFactory.generate(DocumentType.BARANGAY_CLEARANCE, resident, purpose);

// ReportService dispatches to the correct exporter from a registry
exporters.get("pdf").export(data, file);
```

### Observer (Event Bus)
`AlertEventBus` is a publish/subscribe event bus that decouples services from UI controllers. Services publish typed events; controllers subscribe and react without any direct dependency on the service that fired them.

```java
// Service publishes
AlertEventBus.getInstance().publish(EventType.INCIDENT_CREATED, incident);

// Controller subscribes
AlertEventBus.getInstance().subscribe(event -> refreshTable());
```

Supported event types: `INCIDENT_CREATED`, `INCIDENT_UPDATED`, `INCIDENT_RESOLVED`, `DOCUMENT_STATUS_CHANGED`, `ALERT_BROADCAST`, `RESOURCE_UPDATED`, `EVACUATION_UPDATED`, `CHATBOT_ESCALATED`.

### Repository Pattern
Every entity has a dedicated repository (`IncidentRepository`, `ResidentRepository`, `ResourceRepository`, `ResponderRepository`, `DispatchedResponderRepository`, `AlertsRepository`, `ReportRepository`, `AnalyticsRepository`, etc.) that owns all SQL for that entity. Controllers and services never write raw SQL.

### Strategy Pattern (Report Exporters)
`ReportExporter<T>` is a generic interface with two concrete implementations: `PDFReportExporter` and `DocxReportExporter`. `ReportService` holds both in a map and selects the right one at runtime — adding a new export format requires only a new class implementing the interface.

```java
public interface ReportExporter<T> {
    void export(ReportData<T> data, File destination) throws Exception;
    String getFormatName();
}
```

---

## Project Structure

```
src/main/java/com/example/csit228capstone/
├── app/                        # Application entry point (LigtasApp.java)
├── ai/                         # Claude API chatbot integration (ChatbotService)
├── controller/
│   ├── dashboard/
│   ├── incidents/              # IncidentsController, AddIncidentController, ViewIncidentController
│   ├── emergency/              # EmergencyController, TriggerEmergencyController, EmergencyAuthController
│   ├── responder/              # ResponderController, ResponderFormController, DispatchedFormController
│   ├── resident/               # ResidentsController, ResidentFormController
│   ├── resources/              # ResourcesController
│   ├── evacuation/             # EvacuationController, EvacuationFormController
│   ├── alerts/                 # AlertsController, AlertsFormController
│   ├── reports/                # ReportsController, ReportFormController, ReportPreviewController
│   ├── documents/              # DocumentsController
│   ├── analytics/              # AnalyticsController
│   ├── chatbot/                # ChatbotController
│   ├── settings/               # SettingsController
│   ├── map/                    # MapPickerController (shared Leaflet picker)
│   └── mainlayout/             # MainLayoutController (navigation shell)
├── model/
│   ├── incident/               # Incident, IncidentType, IncidentSeverity, IncidentStatus, IncidentTimelineEntry
│   ├── emergency/              # EmergencyContext, PriorityResidentRow, MissingResidentRow, ResourceRow, RescueTeamRow, EvacuationCenterRow
│   ├── responder/              # Responder, DispatchedResponder
│   ├── document/               # DocumentRequest, DocumentType, DocumentStatus
│   ├── report/                 # Report, ReportType, ReportData, ReportFormData, ReportDataProvider, ReportFormData
│   ├── chatbot/                # ChatbotSession, ChatbotMessage, ChatbotCategory
│   ├── broadcast/              # Broadcast
│   ├── alert/                  # Alert, AlertPriority
│   ├── vulnerability/          # VulnerabilityTag
│   ├── Resident.java
│   ├── Resource.java
│   ├── ResourceLog.java
│   ├── EvacuationCenter.java
│   └── Household.java
├── repository/                 # JDBC data access — one class per entity
├── service/
│   ├── report/                 # ReportExporter (interface), PDFReportExporter, DocxReportExporter
│   ├── AlertService.java
│   ├── DocumentService.java
│   ├── EvacuationService.java
│   ├── IncidentService.java
│   └── ReportService.java      # Multithreaded report orchestration (4-thread pool)
└── util/
    ├── SupabaseConnectionManager.java   # Singleton DB pool
    ├── ReportFactory.java               # Factory for barangay document PDFs
    └── AlertEventBus.java               # Observer event bus

src/main/resources/
├── css/application.css
├── images/                     # App icon (ligtas-brgy-logo.png), default avatar
├── map/                        # Bundled Leaflet.js + map.html
└── com/example/.../            # FXML views per module
```

---

## UML Diagrams

The `diagrams/` folder contains:
- **`use-case-diagram.png`** — models interactions between the Barangay Admin and all system modules
- **`class-diagram.png`** — illustrates relationships between core model classes, repositories, services, and controllers
