package hospitalmanagement;

import hospitalmanagement.HospitalRepository.Counts;
import hospitalmanagement.HospitalRepository.Doctor;
import hospitalmanagement.HospitalRepository.Patient;
import hospitalmanagement.HospitalRepository.Role;
import hospitalmanagement.HospitalRepository.StaffMember;
import hospitalmanagement.HospitalRepository.StaffType;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public final class HospitalManagementApp extends Application {
    private final HospitalRepository repository = new HospitalRepository();
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "hospital-database");
        thread.setDaemon(true);
        return thread;
    });

    private Stage stage;
    private StackPane pageContainer;
    private Role currentRole;
    private String currentUsername;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        stage.setTitle("Hospital Management System");
        stage.setMinWidth(960);
        stage.setMinHeight(640);
        showAuthentication(false, Role.USER);
        stage.show();
    }

    @Override
    public void stop() {
        databaseExecutor.shutdownNow();
    }

    private void showAuthentication(boolean registering, Role initialRole) {
        ComboBox<Role> roleChoice = new ComboBox<>(FXCollections.observableArrayList(Role.values()));
        roleChoice.setValue(initialRole);
        roleChoice.setMaxWidth(Double.MAX_VALUE);
        roleChoice.getStyleClass().add("input-control");

        TextField username = new TextField();
        username.setPromptText("Username");
        username.getStyleClass().add("input-control");

        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        password.getStyleClass().add("input-control");

        PasswordField confirmation = new PasswordField();
        confirmation.setPromptText("Confirm password");
        confirmation.getStyleClass().add("input-control");

        Label feedback = new Label(registering
                ? "Create an account for the selected role."
                : "Sign in to continue.");
        feedback.getStyleClass().add("auth-feedback");
        feedback.setWrapText(true);

        VBox form = new VBox(14);
        form.getStyleClass().add("auth-card");
        form.setMaxWidth(440);
        form.setAlignment(Pos.CENTER_LEFT);
        form.getChildren().addAll(
                new Label(registering ? "Create account" : "Welcome back"),
                new Label(registering
                        ? "Register a hospital system account."
                        : "Sign in to the Hospital Management System."),
                field("Account type", roleChoice),
                field("Username", username),
                field("Password", password));
        if (registering) {
            form.getChildren().add(field("Confirm password", confirmation));
        }
        form.getChildren().add(feedback);

        Button submit = new Button(registering ? "Create account" : "Sign in");
        submit.getStyleClass().add("primary-button");
        submit.setMaxWidth(Double.MAX_VALUE);
        Button switchMode = new Button(registering
                ? "Back to sign in"
                : "Create an account");
        switchMode.getStyleClass().add("link-button");
        switchMode.setMaxWidth(Double.MAX_VALUE);
        form.getChildren().addAll(submit, switchMode);

        switchMode.setOnAction(event ->
                showAuthentication(!registering, roleChoice.getValue()));

        submit.setOnAction(event -> {
            Role selectedRole = roleChoice.getValue();
            String accountName = username.getText().trim();
            String secret = password.getText();
            if (selectedRole == null || accountName.isEmpty() || secret.isBlank()) {
                feedback.setText("Enter a username and password.");
                feedback.getStyleClass().setAll("auth-feedback", "error-text");
                return;
            }
            if (registering && !secret.equals(confirmation.getText())) {
                feedback.setText("The passwords do not match.");
                feedback.getStyleClass().setAll("auth-feedback", "error-text");
                return;
            }

            submit.setDisable(true);
            feedback.setText(registering ? "Creating account..." : "Signing in...");
            feedback.getStyleClass().setAll("auth-feedback");

            if (registering) {
                executeDatabase(() -> {
                    repository.register(selectedRole, accountName, secret);
                    return null;
                }, ignored -> {
                    showAlert(Alert.AlertType.INFORMATION, "Account created",
                            "Your account has been created. You can now sign in.");
                    showAuthentication(false, selectedRole);
                }, error -> {
                    submit.setDisable(false);
                    feedback.setText(error.getMessage());
                    feedback.getStyleClass().setAll("auth-feedback", "error-text");
                });
            } else {
                executeDatabase(
                        () -> repository.authenticate(selectedRole, accountName, secret),
                        authenticated -> {
                            submit.setDisable(false);
                            if (authenticated) {
                                showDashboard(selectedRole, accountName);
                            } else {
                                feedback.setText("Username or password is incorrect.");
                                feedback.getStyleClass().setAll("auth-feedback", "error-text");
                            }
                        },
                        error -> {
                            submit.setDisable(false);
                            showDatabaseError(error);
                        });
            }
        });

        VBox brand = new VBox(18);
        brand.getStyleClass().add("auth-brand");
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.getChildren().addAll(
                new Label("HOSPITAL MANAGEMENT"),
                new Label("Care organized.\nTeams connected."),
                new Label("A desktop workspace for patient records and hospital staff."));

        HBox layout = new HBox(36, brand, form);
        layout.getStyleClass().add("auth-layout");
        layout.setAlignment(Pos.CENTER);
        HBox.setHgrow(form, Priority.ALWAYS);

        StackPane root = new StackPane(layout);
        root.getStyleClass().add("auth-root");
        root.setPadding(new Insets(36));
        setScene(root, 1120, 720);
    }

    private void showDashboard(Role role, String username) {
        currentRole = role;
        currentUsername = username;
        pageContainer = new StackPane();
        pageContainer.getStyleClass().add("page-container");

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");
        root.setTop(createTopBar());

        VBox navigation = new VBox(8);
        navigation.getStyleClass().add("sidebar");
        navigation.setPadding(new Insets(24, 14, 24, 14));
        if (role == Role.ADMIN) {
            addNavigationButton(navigation, "Overview");
        }
        addNavigationButton(navigation, "Patients");
        if (role == Role.ADMIN) {
            addNavigationButton(navigation, "Doctors");
            addNavigationButton(navigation, "Receptionists");
            addNavigationButton(navigation, "Workers");
        }
        root.setLeft(navigation);
        root.setCenter(pageContainer);

        setScene(root, 1280, 820);
        showPage(role == Role.ADMIN ? "Overview" : "Patients");
    }

    private Node createTopBar() {
        Label title = new Label("Hospital Management System");
        title.getStyleClass().add("topbar-title");
        Label account = new Label(currentUsername + "  ·  " + currentRole);
        account.getStyleClass().add("muted-text");
        Button signOut = new Button("Sign out");
        signOut.getStyleClass().add("secondary-button");
        signOut.setOnAction(event -> showAuthentication(false, Role.USER));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(16, title, spacer, account, signOut);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(18, 28, 18, 28));
        bar.getStyleClass().add("topbar");
        return bar;
    }

    private void addNavigationButton(VBox navigation, String name) {
        Button button = new Button(name);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.getStyleClass().add("nav-button");
        button.setOnAction(event -> showPage(name));
        navigation.getChildren().add(button);
    }

    private void showPage(String name) {
        Node page = switch (name) {
            case "Overview" -> overviewPage();
            case "Patients" -> patientsPage();
            case "Doctors" -> doctorsPage();
            case "Receptionists" -> staffPage(StaffType.RECEPTIONIST);
            case "Workers" -> staffPage(StaffType.WORKER);
            default -> throw new IllegalArgumentException("Unknown page: " + name);
        };
        pageContainer.getChildren().setAll(page);
    }

    private Node overviewPage() {
        VBox page = new VBox(24);
        page.getStyleClass().add("content-page");
        page.getChildren().add(pageHeading("Overview",
                "A quick summary of the hospital records."));

        Label patientCount = new Label("—");
        Label doctorCount = new Label("—");
        Label receptionistCount = new Label("—");
        Label workerCount = new Label("—");
        HBox cards = new HBox(16,
                countCard("Patients", patientCount),
                countCard("Doctors", doctorCount),
                countCard("Receptionists", receptionistCount),
                countCard("Workers", workerCount));
        cards.getChildren().forEach(node -> HBox.setHgrow(node, Priority.ALWAYS));
        page.getChildren().add(cards);

        Button viewPatients = new Button("Open patient records");
        viewPatients.getStyleClass().add("primary-button");
        viewPatients.setOnAction(event -> showPage("Patients"));
        page.getChildren().add(viewPatients);

        executeDatabase(repository::loadCounts, counts -> {
            patientCount.setText(Long.toString(counts.patients()));
            doctorCount.setText(Long.toString(counts.doctors()));
            receptionistCount.setText(Long.toString(counts.receptionists()));
            workerCount.setText(Long.toString(counts.workers()));
        });
        return page;
    }

    private VBox countCard(String title, Label value) {
        VBox card = new VBox(8, value, new Label(title));
        card.getStyleClass().add("count-card");
        value.getStyleClass().add("count-value");
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private Node patientsPage() {
        VBox page = new VBox(18);
        page.getStyleClass().add("content-page");
        page.getChildren().add(pageHeading("Patients",
                currentRole == Role.ADMIN
                        ? "Search, add, update, and remove patient records."
                        : "Search patient records or register a new patient."));

        TextField search = new TextField();
        search.setPromptText("Search by first or last name");
        search.getStyleClass().add("input-control");
        Button searchButton = new Button("Search");
        searchButton.getStyleClass().add("secondary-button");
        Button refreshButton = new Button("Refresh");
        refreshButton.getStyleClass().add("secondary-button");
        Button addButton = new Button("Add patient");
        addButton.getStyleClass().add("primary-button");

        TableView<Patient> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No patient records found."));
        table.getColumns().setAll(List.of(
                stringColumn("First name", Patient::firstName),
                stringColumn("Last name", Patient::lastName),
                stringColumn("Gender", Patient::gender),
                stringColumn("City", Patient::city),
                stringColumn("Email", Patient::email),
                stringColumn("Age", Patient::age),
                stringColumn("Address", Patient::address),
                stringColumn("Date added", Patient::createdAt),
                stringColumn("Mobile", Patient::mobile)));
        VBox.setVgrow(table, Priority.ALWAYS);

        HBox actions = new HBox(10, search, searchButton, refreshButton, addButton);
        actions.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(search, Priority.ALWAYS);

        if (currentRole == Role.ADMIN) {
            Button editButton = new Button("Edit selected");
            editButton.getStyleClass().add("secondary-button");
            Button deleteButton = new Button("Delete selected");
            deleteButton.getStyleClass().add("danger-button");
            editButton.disableProperty().bind(table.getSelectionModel()
                    .selectedItemProperty().isNull());
            deleteButton.disableProperty().bind(table.getSelectionModel()
                    .selectedItemProperty().isNull());
            editButton.setOnAction(event -> {
                Patient selected = table.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    showPatientForm(selected, table, search);
                }
            });
            deleteButton.setOnAction(event -> {
                Patient selected = table.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    confirmDeletePatient(selected, table, search);
                }
            });
            actions.getChildren().addAll(editButton, deleteButton);
        }

        searchButton.setOnAction(event -> loadPatients(table, search.getText()));
        search.setOnAction(event -> loadPatients(table, search.getText()));
        refreshButton.setOnAction(event -> {
            search.clear();
            loadPatients(table, "");
        });
        addButton.setOnAction(event -> showPatientForm(null, table, search));

        page.getChildren().addAll(actions, table);
        loadPatients(table, "");
        return page;
    }

    private void loadPatients(TableView<Patient> table, String query) {
        executeDatabase(() -> repository.findPatients(query),
                patients -> table.getItems().setAll(patients));
    }

    private void confirmDeletePatient(Patient patient, TableView<Patient> table,
                                     TextField search) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.initOwner(stage);
        confirmation.setTitle("Delete patient");
        confirmation.setHeaderText("Delete " + patient.firstName() + " "
                + patient.lastName() + "?");
        confirmation.setContentText("This action cannot be undone.");
        ButtonType delete = new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE);
        confirmation.getButtonTypes().setAll(delete, ButtonType.CANCEL);
        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) != delete) {
            return;
        }
        executeDatabase(() -> {
            repository.deletePatient(patient.mobile());
            return null;
        }, ignored -> {
            loadPatients(table, search.getText());
            showAlert(Alert.AlertType.INFORMATION, "Patient deleted",
                    "The patient record was deleted.");
        });
    }

    private void showPatientForm(Patient existing, TableView<Patient> table, TextField search) {
        Stage dialog = createFormStage(existing == null ? "Add patient" : "Edit patient");
        TextField firstName = formTextField("First name");
        TextField lastName = formTextField("Last name");
        TextField gender = formTextField("Gender");
        TextField mobile = formTextField("Mobile number");
        TextField city = formTextField("City");
        TextField email = formTextField("Email");
        TextField age = formTextField("Age");
        TextField address = formTextField("Address");
        if (existing != null) {
            firstName.setText(existing.firstName());
            lastName.setText(existing.lastName());
            gender.setText(existing.gender());
            mobile.setText(existing.mobile());
            mobile.setDisable(true);
            city.setText(existing.city());
            email.setText(existing.email());
            age.setText(existing.age());
            address.setText(existing.address());
        }

        GridPane fields = formGrid();
        addFormField(fields, 0, "First name", firstName);
        addFormField(fields, 1, "Last name", lastName);
        addFormField(fields, 2, "Gender", gender);
        addFormField(fields, 3, "Mobile number", mobile);
        addFormField(fields, 4, "City", city);
        addFormField(fields, 5, "Email", email);
        addFormField(fields, 6, "Age", age);
        addFormField(fields, 7, "Address", address);

        Button save = new Button(existing == null ? "Save patient" : "Save changes");
        save.getStyleClass().add("primary-button");
        save.setOnAction(event -> {
            if (firstName.getText().isBlank() || lastName.getText().isBlank()
                    || mobile.getText().isBlank()) {
                showAlert(Alert.AlertType.WARNING, "Missing information",
                        "First name, last name, and mobile number are required.");
                return;
            }
            Patient patient = new Patient(
                    firstName.getText().trim(), lastName.getText().trim(),
                    clean(gender.getText()), clean(city.getText()), clean(email.getText()),
                    clean(age.getText()), clean(address.getText()),
                    existing == null ? "" : existing.createdAt(), mobile.getText().trim());
            executeDatabase(() -> {
                if (existing == null) {
                    repository.createPatient(patient);
                } else {
                    repository.updatePatient(patient);
                }
                return null;
            }, ignored -> {
                dialog.close();
                loadPatients(table, search.getText());
                showAlert(Alert.AlertType.INFORMATION, "Patient saved",
                        "The patient record was saved.");
            });
        });
        showForm(dialog, fields, save);
    }

    private Node doctorsPage() {
        VBox page = new VBox(18);
        page.getStyleClass().add("content-page");
        page.getChildren().add(pageHeading("Doctors",
                "Search and maintain the hospital doctor directory."));

        TextField search = new TextField();
        search.setPromptText("Search by first or last name");
        search.getStyleClass().add("input-control");
        Button searchButton = new Button("Search");
        searchButton.getStyleClass().add("secondary-button");
        Button refreshButton = new Button("Refresh");
        refreshButton.getStyleClass().add("secondary-button");
        Button addButton = new Button("Add doctor");
        addButton.getStyleClass().add("primary-button");

        TableView<Doctor> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No doctor records found."));
        TableColumn<Doctor, Integer> id = new TableColumn<>("ID");
        id.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().id()));
        table.getColumns().setAll(List.of(id,
                stringColumn("First name", Doctor::firstName),
                stringColumn("Last name", Doctor::lastName),
                stringColumn("Gender", Doctor::gender),
                stringColumn("Mobile", Doctor::mobile),
                stringColumn("City", Doctor::city),
                stringColumn("Email", Doctor::email),
                stringColumn("Age", Doctor::age),
                stringColumn("Address", Doctor::address),
                stringColumn("Date added", Doctor::createdAt),
                stringColumn("Qualification", Doctor::qualification)));
        VBox.setVgrow(table, Priority.ALWAYS);

        HBox actions = new HBox(10, search, searchButton, refreshButton, addButton);
        actions.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(search, Priority.ALWAYS);
        searchButton.setOnAction(event -> loadDoctors(table, search.getText()));
        search.setOnAction(event -> loadDoctors(table, search.getText()));
        refreshButton.setOnAction(event -> {
            search.clear();
            loadDoctors(table, "");
        });
        addButton.setOnAction(event -> showDoctorForm(table, search));
        page.getChildren().addAll(actions, table);
        loadDoctors(table, "");
        return page;
    }

    private void loadDoctors(TableView<Doctor> table, String query) {
        executeDatabase(() -> repository.findDoctors(query),
                doctors -> table.getItems().setAll(doctors));
    }

    private void showDoctorForm(TableView<Doctor> table, TextField search) {
        Stage dialog = createFormStage("Add doctor");
        TextField id = formTextField("Unique ID");
        TextField firstName = formTextField("First name");
        TextField lastName = formTextField("Last name");
        TextField gender = formTextField("Gender");
        TextField mobile = formTextField("Mobile number");
        TextField city = formTextField("City");
        TextField email = formTextField("Email");
        TextField age = formTextField("Age");
        TextField address = formTextField("Address");
        TextField qualification = formTextField("Qualification");

        GridPane fields = formGrid();
        addFormField(fields, 0, "Unique ID", id);
        addFormField(fields, 1, "First name", firstName);
        addFormField(fields, 2, "Last name", lastName);
        addFormField(fields, 3, "Gender", gender);
        addFormField(fields, 4, "Mobile number", mobile);
        addFormField(fields, 5, "City", city);
        addFormField(fields, 6, "Email", email);
        addFormField(fields, 7, "Age", age);
        addFormField(fields, 8, "Address", address);
        addFormField(fields, 9, "Qualification", qualification);

        Button save = new Button("Save doctor");
        save.getStyleClass().add("primary-button");
        save.setOnAction(event -> {
            if (id.getText().isBlank() || firstName.getText().isBlank()
                    || lastName.getText().isBlank() || mobile.getText().isBlank()) {
                showAlert(Alert.AlertType.WARNING, "Missing information",
                        "ID, first name, last name, and mobile number are required.");
                return;
            }
            final int doctorId;
            try {
                doctorId = Integer.parseInt(id.getText().trim());
            } catch (NumberFormatException error) {
                showAlert(Alert.AlertType.WARNING, "Invalid ID",
                        "Doctor ID must be a whole number.");
                return;
            }
            Doctor doctor = new Doctor(doctorId, firstName.getText().trim(),
                    lastName.getText().trim(), clean(gender.getText()), mobile.getText().trim(),
                    clean(city.getText()), clean(email.getText()), clean(age.getText()),
                    clean(address.getText()), "", clean(qualification.getText()));
            executeDatabase(() -> {
                repository.createDoctor(doctor);
                return null;
            }, ignored -> {
                dialog.close();
                loadDoctors(table, search.getText());
                showAlert(Alert.AlertType.INFORMATION, "Doctor saved",
                        "The doctor record was saved.");
            });
        });
        showForm(dialog, fields, save);
    }

    private Node staffPage(StaffType type) {
        VBox page = new VBox(18);
        page.getStyleClass().add("content-page");
        page.getChildren().add(pageHeading(type.label() + "s",
                "Search and maintain the hospital " + type.label().toLowerCase()
                        + " directory."));

        TextField search = new TextField();
        search.setPromptText("Search by first or last name");
        search.getStyleClass().add("input-control");
        Button searchButton = new Button("Search");
        searchButton.getStyleClass().add("secondary-button");
        Button refreshButton = new Button("Refresh");
        refreshButton.getStyleClass().add("secondary-button");
        Button addButton = new Button("Add " + type.label().toLowerCase());
        addButton.getStyleClass().add("primary-button");

        TableView<StaffMember> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No " + type.label().toLowerCase()
                + " records found."));
        table.getColumns().setAll(List.of(
                stringColumn("First name", StaffMember::firstName),
                stringColumn("Last name", StaffMember::lastName),
                stringColumn("Mobile", StaffMember::mobile),
                stringColumn("Date added", StaffMember::createdAt)));
        VBox.setVgrow(table, Priority.ALWAYS);

        HBox actions = new HBox(10, search, searchButton, refreshButton, addButton);
        actions.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(search, Priority.ALWAYS);
        searchButton.setOnAction(event -> loadStaff(type, table, search.getText()));
        search.setOnAction(event -> loadStaff(type, table, search.getText()));
        refreshButton.setOnAction(event -> {
            search.clear();
            loadStaff(type, table, "");
        });
        addButton.setOnAction(event -> showStaffForm(type, table, search));

        page.getChildren().addAll(actions, table);
        loadStaff(type, table, "");
        return page;
    }

    private void loadStaff(StaffType type, TableView<StaffMember> table, String query) {
        executeDatabase(() -> repository.findStaff(type, query),
                staff -> table.getItems().setAll(staff));
    }

    private void showStaffForm(StaffType type, TableView<StaffMember> table,
                               TextField search) {
        Stage dialog = createFormStage("Add " + type.label().toLowerCase());
        TextField firstName = formTextField("First name");
        TextField lastName = formTextField("Last name");
        TextField mobile = formTextField("Mobile number");
        GridPane fields = formGrid();
        addFormField(fields, 0, "First name", firstName);
        addFormField(fields, 1, "Last name", lastName);
        addFormField(fields, 2, "Mobile number", mobile);

        Button save = new Button("Save " + type.label().toLowerCase());
        save.getStyleClass().add("primary-button");
        save.setOnAction(event -> {
            if (firstName.getText().isBlank() || lastName.getText().isBlank()
                    || mobile.getText().isBlank()) {
                showAlert(Alert.AlertType.WARNING, "Missing information",
                        "First name, last name, and mobile number are required.");
                return;
            }
            StaffMember member = new StaffMember(firstName.getText().trim(),
                    lastName.getText().trim(), mobile.getText().trim(), "");
            executeDatabase(() -> {
                repository.createStaff(type, member);
                return null;
            }, ignored -> {
                dialog.close();
                loadStaff(type, table, search.getText());
                showAlert(Alert.AlertType.INFORMATION, "Record saved",
                        "The " + type.label().toLowerCase() + " record was saved.");
            });
        });
        showForm(dialog, fields, save);
    }

    private Stage createFormStage(String title) {
        Stage dialog = new Stage();
        dialog.initOwner(stage);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(title);
        dialog.setMinWidth(520);
        return dialog;
    }

    private void showForm(Stage dialog, GridPane fields, Button save) {
        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("secondary-button");
        cancel.setOnAction(event -> dialog.close());
        HBox actions = new HBox(10, cancel, save);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox content = new VBox(20, fields, actions);
        content.setPadding(new Insets(24));
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("form-scroll");
        Scene scene = new Scene(scrollPane, 560, 650);
        addStylesheet(scene);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private GridPane formGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(12);
        grid.setPadding(new Insets(4));
        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(130);
        ColumnConstraints inputColumn = new ColumnConstraints();
        inputColumn.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelColumn, inputColumn);
        return grid;
    }

    private void addFormField(GridPane grid, int row, String caption, TextField input) {
        Label label = new Label(caption);
        label.getStyleClass().add("field-label");
        input.setMaxWidth(Double.MAX_VALUE);
        input.getStyleClass().add("input-control");
        grid.add(label, 0, row);
        grid.add(input, 1, row);
    }

    private TextField formTextField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        return field;
    }

    private VBox field(String caption, Node input) {
        Label label = new Label(caption);
        label.getStyleClass().add("field-label");
        return new VBox(7, label, input);
    }

    private VBox pageHeading(String title, String subtitle) {
        Label heading = new Label(title);
        heading.getStyleClass().add("page-title");
        Label description = new Label(subtitle);
        description.getStyleClass().add("muted-text");
        return new VBox(6, heading, description);
    }

    private static <T> TableColumn<T, String> stringColumn(
            String title, Function<T, String> value) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(clean(value.apply(cell.getValue()))));
        return column;
    }

    private <T> void executeDatabase(SqlAction<T> action, Consumer<T> success) {
        executeDatabase(action, success, this::showDatabaseError);
    }

    private <T> void executeDatabase(SqlAction<T> action, Consumer<T> success,
                                    Consumer<SQLException> failure) {
        databaseExecutor.execute(() -> {
            try {
                T result = action.run();
                Platform.runLater(() -> success.accept(result));
            } catch (SQLException error) {
                Platform.runLater(() -> failure.accept(error));
            } catch (RuntimeException error) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR,
                        "Unexpected error", error.getMessage() == null
                                ? error.getClass().getSimpleName() : error.getMessage()));
            }
        });
    }

    private void showDatabaseError(SQLException error) {
        String details = error.getMessage() == null
                ? error.getClass().getSimpleName() : error.getMessage();
        showAlert(Alert.AlertType.ERROR, "Database operation failed",
                "Check that MySQL is running, the hospital database has been imported, "
                        + "and the HOSPITAL_DB_URL, HOSPITAL_DB_USER, and "
                        + "HOSPITAL_DB_PASSWORD settings are correct.\n\n" + details);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.initOwner(stage);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message == null ? "" : message);
        alert.showAndWait();
    }

    private void setScene(Parent root, double width, double height) {
        Scene scene = new Scene(root, width, height);
        addStylesheet(scene);
        stage.setScene(scene);
    }

    private void addStylesheet(Scene scene) {
        var stylesheet = getClass().getResource("/hospitalmanagement/hospitalmanagement.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    @FunctionalInterface
    private interface SqlAction<T> {
        T run() throws SQLException;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
