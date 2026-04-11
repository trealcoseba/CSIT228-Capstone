# Project LIGTAS-Bgy
Local Integrated Guard & Task-Alert System for Barangays

## Group Members
- Lemuel Vincent Alcoseba III
- Daniel Duarte Jr.
- Michael Lois Gaviola
- Nathanael Andrew Mendoza
- Jacy Erl Sangre



## Project Description
Project **LIGTAS-Bgy** is a Java-based desktop disaster management command center designed to assist Barangay officials in preparing for and responding to emergencies.

At the barangay level, disaster response is often reactive due to fragmented records, lack of equipment tracking, and slow reporting systems. Important data such as vulnerable residents (elderly, PWDs, children) are frequently stored in physical logbooks that may be lost or damaged during disasters.

LIGTAS-Bgy addresses this problem by providing a **centralized, offline-capable system** that allows officials to manage resident risk data, track emergency resources, monitor evacuation centers, and generate standardized disaster reports for submission to city authorities.

The system aims to improve **disaster preparedness, response coordination, and data reliability**, especially in situations where internet connectivity is unavailable.

## Proposed Features
- User login
- Post-disaster report generation
- Search and filter data
- Request emergency help
- Add/ edit/ delete records
- Individual risk assessment
- Quick information lookup
- Hyper-localized population grouping
- Display data in tables
- Inventory onitoring

## Planned Technologies
- **Java** – Core programming language for system logic
- **JavaFX** – GUI framework for building the desktop interface
- **JDBC** – Database connectivity for managing system data
- **SQLite / MySQL** – Database for storing resident, resource, and disaster data
- **iText Library** – PDF report generation

## Evaluation Criteria Mapping (Initial)
- **OOP:** Planned use of classes such as `Resident`, `Resource`, `EvacuationCenter`, `Report`, `User`, and `DatabaseManager` to model system entities and relationships.

- **GUI:** The system will use **JavaFX with FXML views** to provide an interactive dashboard, forms for data management, and status displays for evacuation centers.

- **UML:** The project will include **Use Case Diagrams** to represent user interactions and **Class Diagrams** to illustrate system architecture.

- **Design Pattern:**  
  Tentative patterns include:
    - **Singleton Pattern** for managing the database connection.
    - **Factory Pattern** for the ability to generate different types of reports.
    - **Observer Pattern** to notify all Observers (e.g., AdminUser) when report generation is complete.
    - **MVC (Model-View-Controller)** structure for organizing JavaFX application components.