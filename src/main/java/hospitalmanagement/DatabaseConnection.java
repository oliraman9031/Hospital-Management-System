package hospitalmanagement;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {
    private static final String DEFAULT_URL =
            "jdbc:postgresql://localhost:5432/hospital";

    private DatabaseConnection() {
    }

    public static Connection open() throws SQLException {
        String url = setting("HOSPITAL_DB_URL", DEFAULT_URL);
        String username = setting("HOSPITAL_DB_USER", "postgres");
        String password = System.getenv("HOSPITAL_DB_PASSWORD");
        if (password == null || password.isBlank()) {
            throw new SQLException("Set the HOSPITAL_DB_PASSWORD environment variable.");
        }
        return DriverManager.getConnection(url, username, password);
    }

    private static String setting(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
