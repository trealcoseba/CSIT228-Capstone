package com.example.csit228capstone.controller.resident;

import com.example.csit228capstone.model.Resident;
import com.example.csit228capstone.model.vulnerability.VulnerabilityTag;
import com.example.csit228capstone.repository.ResidentRepository;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class ResidentsController {


    @FXML private TableView<Resident> residentsTable;
    @FXML private TableColumn<Resident, String> colId;
    @FXML private TableColumn<Resident, String> colName;
    @FXML private TableColumn<Resident, Integer> colAge;
    @FXML private TableColumn<Resident, String> colCategory;
    @FXML private TableColumn<Resident, String> colStatus;
    @FXML private TableColumn<Resident, String> colPhoneNumber;
    @FXML private TableColumn<Resident, String> colRegisteredDate;
    @FXML private TableColumn<Resident, Void> colActions;
    @FXML private TableColumn<Resident, String> colAddress;

    @FXML private Label lblTotalResidents;
    @FXML private Label lblRegisteredToday;
    @FXML private Label lblVulnerable;
    @FXML private Label lblMissing;

    @FXML private ComboBox<String> filterStatus;
    @FXML private ComboBox<String> filterCategory;
    @FXML private ComboBox<String> filterAscOrDesc;

    @FXML private TextField tfSearch;

    private final ResidentRepository repository = new ResidentRepository();
    private final ObservableList<Resident> masterData = FXCollections.observableArrayList();
    private FilteredList<Resident> filteredData;

    private SortedList<Resident> sortedData;

    @FXML
    public void initialize() {

        setupTableColumns();
        setupFiltering();
        loadData();
    }

    public void exportData(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Resident Data");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("residents_" + LocalDate.now() + ".csv");

        File file = fileChooser.showSaveDialog(((Node) event.getSource()).getScene().getWindow());

        if (file != null) {
            ResidentRepository repo = new ResidentRepository();
            List<Resident> residents = repo.findAll();

            residents.sort((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt()));

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file)))) {
                writer.println("Date Created,ID,First Name,Last Name,Sex,Birth Date,Address,Vulnerabilities,Household Head");

                for (Resident r : residents) {
                    StringBuilder row = new StringBuilder();

                    // Add the Created At date first
                    row.append(r.getCreatedAt().format(formatter)).append(",");

                    row.append(r.getId()).append(",");
                    row.append(escape(r.getFirstName())).append(",");
                    row.append(escape(r.getLastName())).append(",");
                    row.append(r.getSex()).append(",");
                    row.append(r.getDateOfBirth()).append(",");
                    row.append(escape(r.getAddress())).append(",");

                    String tags = r.getVulnerabilities().stream()
                            .map(Enum::name)
                            .collect(Collectors.joining("; "));
                    row.append(escape(tags)).append(",");

                    row.append(r.isHouseholdHead());

                    writer.println(row.toString());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void openRegistration(ActionEvent actionEvent) {
        openForm(null);
    }



    //HELPERS BELOW
    private void setupFiltering() {
        filteredData = new FilteredList<>(masterData, p -> true);

        filterStatus.setItems(FXCollections.observableArrayList("All", "Single", "Married", "Widowed", "Separated"));
        filterCategory.setItems(FXCollections.observableArrayList("All", "General", "Senior Citizen", "PWD", "Solo Parent", "Indigenous" , "Child"));
        filterStatus.setValue("All");
        filterCategory.setValue("All");

        filterAscOrDesc.setItems(FXCollections.observableArrayList("Newest", "Oldest"));
        filterAscOrDesc.setValue("Newest");


        sortedData = new SortedList<>(filteredData);
        filterAscOrDesc.valueProperty().addListener((obs, oldVal, newVal) -> updateSort());
        tfSearch.textProperty().addListener((obs, old, newValue) -> updateFilter());
        filterStatus.valueProperty().addListener((obs, old, newValue) -> updateFilter());
        filterCategory.valueProperty().addListener((obs, old, newValue) -> updateFilter());

        residentsTable.setItems(sortedData);
    }

    private void updateFilter() {
        filteredData.setPredicate(resident -> {

            String search = tfSearch.getText() == null ? "" : tfSearch.getText().toLowerCase().trim();
            boolean matchesSearch = search.isEmpty() || resident.getFullName().toLowerCase().contains(search);

            String status = filterStatus.getValue() == null ? "All" : filterStatus.getValue().toString();
            boolean matchesStatus = status.equals("All") || resident.getCivilStatus().equalsIgnoreCase(status);

            String category = filterCategory.getValue() == null ? "All" : filterCategory.getValue().toString();
            boolean matchesCategory = true;
            if (!category.equals("All")) {
                if (category.equals("General")) {
                    matchesCategory = resident.getVulnerabilities().isEmpty();
                } else {
                    matchesCategory = resident.getVulnerabilities().stream()
                            .anyMatch(tag -> tag.getDisplayName().equalsIgnoreCase(category));
                }
            }

            return matchesSearch && matchesStatus && matchesCategory;
        });

        lblTotalResidents.setText(String.valueOf(filteredData.size()));
    }

    public void loadData() {
        try {
            masterData.setAll(repository.findAll());
            lblTotalResidents.setText(String.valueOf(masterData.size()));

            long vulnerableCount = masterData.stream()
                    .filter(r -> !r.getVulnerabilities().isEmpty())
                    .count();
            lblVulnerable.setText(String.valueOf(vulnerableCount));

            java.time.LocalDate today = java.time.LocalDate.now();
            long todayCount = masterData.stream()
                    .filter(r -> r.getCreatedAt().toLocalDate().isEqual(today))
                    .count();
            lblRegisteredToday.setText(String.valueOf(todayCount));

            updateSort();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Database Error", "Could not fetch data from server.");
        }
    }

    private void setupTableColumns() {

        //FORMAT TABLE
        formatTable();

        colId.setCellValueFactory(cellData -> {
            String fullId = cellData.getValue().getId().toString();
            String shortId = (fullId.length() > 8) ? fullId.substring(0, 8) : fullId;

            return new javafx.beans.property.ReadOnlyStringWrapper(shortId.toUpperCase());
        });


        colName.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getFullName()));
        colAge.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getAge()));
        colPhoneNumber.setCellValueFactory(new PropertyValueFactory<>("contactNumber"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("civilStatus"));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        colRegisteredDate.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(cellData.getValue().getCreatedAt().format(formatter)));

        colAddress.setCellValueFactory(new PropertyValueFactory<>("address"));

        colCategory.setCellValueFactory(cellData -> {
            Resident r = cellData.getValue();
            List<VulnerabilityTag> tags = r.getVulnerabilities();
            if (tags == null || tags.isEmpty()) return new ReadOnlyStringWrapper("General");

            String formattedTags = tags.stream()
                    .map(VulnerabilityTag::getDisplayName)
                    .collect(Collectors.joining(", "));
            return new ReadOnlyStringWrapper(formattedTags);
        });

        setupActionButtons();
    }

    private void setupActionButtons() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final HBox container = new HBox(10, btnEdit, btnDelete);
            {
                btnEdit.getStyleClass().add("btn-edit");
                btnDelete.getStyleClass().add("btn-delete");
                container.setStyle("-fx-alignment: CENTER;");

                btnDelete.setOnAction(event -> handleDeleteResident(getTableView().getItems().get(getIndex())));
                btnEdit.setOnAction(event -> handleEditResident(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }


    private void handleEditResident(Resident resident) {
        openForm(resident);
    }

    private void openForm(Resident resident) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/csit228capstone/resident/ResidentForm.fxml"));
            Parent root = loader.load();
            ResidentFormController controller = loader.getController();
            controller.setParentController(this);
            if (resident != null) controller.setResidentData(resident);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(resident == null ? "Register New Resident" : "Edit Resident");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Could not open form.");
        }
    }

    private void handleDeleteResident(Resident resident) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + resident.getFullName() + "?", ButtonType.YES, ButtonType.NO);
        if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            repository.delete(resident.getId());
            loadData();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }


    private void updateSort() {
        String sortOrder = filterAscOrDesc.getValue();
        if (sortOrder == null) return;

        if (sortOrder.equals("Newest")) {
            sortedData.setComparator((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt()));
        } else {
            sortedData.setComparator((r1, r2) -> r1.getCreatedAt().compareTo(r2.getCreatedAt()));
        }
    }

    private String escape(String data) {
        if (data == null) return "";
        if (data.contains(",") || data.contains("\"")) {
            return "\"" + data.replace("\"", "\"\"") + "\"";
        }
        return data;
    }


    private void formatTable(){
        colId.prefWidthProperty().bind(residentsTable.widthProperty().multiply(0.10));
        colName.prefWidthProperty().bind(residentsTable.widthProperty().multiply(0.20));
        colAge.prefWidthProperty().bind(residentsTable.widthProperty().multiply(0.05));
        colCategory.prefWidthProperty().bind(residentsTable.widthProperty().multiply(0.15));
        colStatus.prefWidthProperty().bind(residentsTable.widthProperty().multiply(0.10));
        colPhoneNumber.prefWidthProperty().bind(residentsTable.widthProperty().multiply(0.12));
        colRegisteredDate.prefWidthProperty().bind(residentsTable.widthProperty().multiply(0.10));
        colActions.prefWidthProperty().bind(residentsTable.widthProperty().multiply(0.15));
        colAddress.prefWidthProperty().bind(residentsTable.widthProperty().multiply(0.20));

        colId.setResizable(false);
        colAddress.setResizable(false);
        colName.setResizable(false);
        colAge.setResizable(false);
        colCategory.setResizable(false);
        colStatus.setResizable(false);
        colPhoneNumber.setResizable(false);
        colRegisteredDate.setResizable(false);
        colActions.setResizable(false);

        colId.setReorderable(false);
        colAddress.setReorderable(false);
        colName.setReorderable(false);
        colAge.setReorderable(false);
        colCategory.setReorderable(false);
        colStatus.setReorderable(false);
        colPhoneNumber.setReorderable(false);
        colRegisteredDate.setReorderable(false);
        colActions.setReorderable(false);
    }
}