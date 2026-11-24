package hospitalmanagement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class HospitalRepository {
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    public enum Role {
        USER("User", "login"),
        ADMIN("Administrator", "adminreg");

        private final String label;
        private final String table;

        Role(String label, String table) {
            this.label = label;
            this.table = table;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum StaffType {
        RECEPTIONIST("Receptionist", "recp"),
        WORKER("Worker", "worker");

        private final String label;
        private final String table;

        StaffType(String label, String table) {
            this.label = label;
            this.table = table;
        }

        public String label() {
            return label;
        }
    }

    public record Counts(long patients, long doctors, long receptionists, long workers) {
    }

    public record Patient(String firstName, String lastName, String gender, String city,
                          String email, String age, String address, String createdAt,
                          String mobile) {
    }

    public record Doctor(int id, String firstName, String lastName, String gender,
                         String mobile, String city, String email, String age,
                         String address, String createdAt, String qualification) {
    }

    public record StaffMember(String firstName, String lastName, String mobile,
                              String createdAt) {
    }

    public boolean authenticate(Role role, String username, String password) throws SQLException {
        String sql = "SELECT 1 FROM " + role.table + " WHERE username = ? AND password = ? LIMIT 1";
        try (Connection connection = DatabaseConnection.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, password);
            try (ResultSet results = statement.executeQuery()) {
                return results.next();
            }
        }
    }

    public void register(Role role, String username, String password) throws SQLException {
        String sql = "INSERT INTO " + role.table + " (username, password) VALUES (?, ?)";
        try (Connection connection = DatabaseConnection.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, password);
            statement.executeUpdate();
        }
    }

    public Counts loadCounts() throws SQLException {
        String sql = "SELECT (SELECT COUNT(*) FROM patient), "
                + "(SELECT COUNT(*) FROM doctor), "
                + "(SELECT COUNT(*) FROM recp), "
                + "(SELECT COUNT(*) FROM worker)";
        try (Connection connection = DatabaseConnection.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            results.next();
            return new Counts(results.getLong(1), results.getLong(2),
                    results.getLong(3), results.getLong(4));
        }
    }

    public List<Patient> findPatients(String search) throws SQLException {
        String sql = "SELECT fname, lname, gender, city, email, age, address, date, mobile "
                + "FROM patient WHERE fname LIKE ? OR lname LIKE ? ORDER BY lname, fname";
        String pattern = searchPattern(search);
        List<Patient> patients = new ArrayList<>();
        try (Connection connection = DatabaseConnection.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, pattern);
            statement.setString(2, pattern);
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    patients.add(new Patient(
                            results.getString("fname"), results.getString("lname"),
                            results.getString("gender"), results.getString("city"),
                            results.getString("email"), results.getString("age"),
                            results.getString("address"), results.getString("date"),
                            results.getString("mobile")));
                }
            }
        }
        return patients;
    }

    public void createPatient(Patient patient) throws SQLException {
        String sql = "INSERT INTO patient "
                + "(fname, lname, gender, city, email, age, address, date, mobile) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseConnection.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindPatient(statement, patient);
            statement.executeUpdate();
        }
    }

    public void updatePatient(Patient patient) throws SQLException {
        String sql = "UPDATE patient SET fname = ?, lname = ?, gender = ?, city = ?, "
                + "email = ?, age = ?, address = ? WHERE mobile = ?";
        try (Connection connection = DatabaseConnection.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, patient.firstName());
            statement.setString(2, patient.lastName());
            statement.setString(3, patient.gender());
            statement.setString(4, patient.city());
            statement.setString(5, patient.email());
            statement.setString(6, patient.age());
            statement.setString(7, patient.address());
            statement.setString(8, patient.mobile());
            statement.executeUpdate();
        }
    }

    public void deletePatient(String mobile) throws SQLException {
        try (Connection connection = DatabaseConnection.open();
             PreparedStatement statement =
                     connection.prepareStatement("DELETE FROM patient WHERE mobile = ?")) {
            statement.setString(1, mobile);
            statement.executeUpdate();
        }
    }

    public List<Doctor> findDoctors(String search) throws SQLException {
        String sql = "SELECT id, fname, lname, gender, mobile, city, email, age, address, "
                + "date, qualification FROM doctor "
                + "WHERE fname LIKE ? OR lname LIKE ? ORDER BY lname, fname";
        String pattern = searchPattern(search);
        List<Doctor> doctors = new ArrayList<>();
        try (Connection connection = DatabaseConnection.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, pattern);
            statement.setString(2, pattern);
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    doctors.add(new Doctor(
                            results.getInt("id"), results.getString("fname"),
                            results.getString("lname"), results.getString("gender"),
                            results.getString("mobile"), results.getString("city"),
                            results.getString("email"), results.getString("age"),
                            results.getString("address"), results.getString("date"),
                            results.getString("qualification")));
                }
            }
        }
        return doctors;
    }

    public void createDoctor(Doctor doctor) throws SQLException {
        String sql = "INSERT INTO doctor "
                + "(id, fname, lname, gender, mobile, city, email, age, address, date, qualification) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseConnection.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, doctor.id());
            statement.setString(2, doctor.firstName());
            statement.setString(3, doctor.lastName());
            statement.setString(4, doctor.gender());
            statement.setString(5, doctor.mobile());
            statement.setString(6, doctor.city());
            statement.setString(7, doctor.email());
            statement.setString(8, doctor.age());
            statement.setString(9, doctor.address());
            statement.setString(10, now());
            statement.setString(11, doctor.qualification());
            statement.executeUpdate();
        }
    }

    public List<StaffMember> findStaff(StaffType type, String search) throws SQLException {
        String sql = "SELECT fname, lname, mobile, date FROM " + type.table
                + " WHERE fname LIKE ? OR lname LIKE ? ORDER BY lname, fname";
        String pattern = searchPattern(search);
        List<StaffMember> staff = new ArrayList<>();
        try (Connection connection = DatabaseConnection.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, pattern);
            statement.setString(2, pattern);
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    staff.add(new StaffMember(results.getString("fname"),
                            results.getString("lname"), results.getString("mobile"),
                            results.getString("date")));
                }
            }
        }
        return staff;
    }

    public void createStaff(StaffType type, StaffMember member) throws SQLException {
        String sql = "INSERT INTO " + type.table
                + " (fname, lname, mobile, date) VALUES (?, ?, ?, ?)";
        try (Connection connection = DatabaseConnection.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, member.firstName());
            statement.setString(2, member.lastName());
            statement.setString(3, member.mobile());
            statement.setString(4, now());
            statement.executeUpdate();
        }
    }

    private static void bindPatient(PreparedStatement statement, Patient patient)
            throws SQLException {
        statement.setString(1, patient.firstName());
        statement.setString(2, patient.lastName());
        statement.setString(3, patient.gender());
        statement.setString(4, patient.city());
        statement.setString(5, patient.email());
        statement.setString(6, patient.age());
        statement.setString(7, patient.address());
        statement.setString(8, now());
        statement.setString(9, patient.mobile());
    }

    private static String searchPattern(String search) {
        return "%" + (search == null ? "" : search.trim()) + "%";
    }

    private static String now() {
        return LocalDateTime.now().format(DATE_FORMAT);
    }
}
