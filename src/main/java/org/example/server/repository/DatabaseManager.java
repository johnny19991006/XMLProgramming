package org.example.server.repository;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

public class DatabaseManager {
    private static final String PROPERTIES_FILE = "application.properties";

    private static String databaseUrl;
    private static String databaseUser;
    private static String databasePassword;

    public DatabaseManager() {
        loadProperties();
    }

    private void loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (input == null) {
                System.out.println("Sorry, unable to find " + PROPERTIES_FILE);
                return;
            }
            properties.load(input);
            databaseUrl = properties.getProperty("datasource.url");
            databaseUser = properties.getProperty("datasource.username");
            databasePassword = properties.getProperty("datasource.password");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static final String CREATE_USERS_TABLE = "CREATE TABLE IF NOT EXISTS users (id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(255) NOT NULL);";
    private static final String INSERT_USER = "INSERT INTO users (username) VALUES (?);";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            createUsersTable();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    public static void saveMessage(String sender, String receiver, String message) {
        try (Connection connection = DriverManager.getConnection(databaseUrl, databaseUser, databasePassword);
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO messages (sender, receiver, message) VALUES (?, ?, ?)")) {

            statement.setString(1, sender);
            statement.setString(2, receiver);
            statement.setString(3, message);
            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createUsersTable() {
        try (Connection connection = DriverManager.getConnection(databaseUrl, databaseUser, databasePassword);
             PreparedStatement preparedStatement = connection.prepareStatement(CREATE_USERS_TABLE)) {
            preparedStatement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void addUser(String username) {
        try (Connection connection = DriverManager.getConnection(databaseUrl, databaseUser, databasePassword);
             PreparedStatement preparedStatement = connection.prepareStatement(INSERT_USER)) {
            preparedStatement.setString(1, username);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

