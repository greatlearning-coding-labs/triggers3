package com.gl.examination;
import org.junit.jupiter.api.*;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DatabaseQueryTest {

    private static Connection connection;

    @BeforeAll
    public static void setup() throws SQLException {
        String url = "jdbc:mysql://localhost:3306/stocks_db";
        String user = "root";       // <-- Replace with your DB username
        String password = "";   // <-- Replace with your DB password
        connection = DriverManager.getConnection(url, user, password);
    }

    @AfterAll
    public static void teardown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Test
    @Order(1)
    void testOrdersTableExists() throws SQLException {
        String query = "SHOW TABLES LIKE 'orders'";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            assertTrue(rs.next(), "'orders' table should exist");
        }
    }
    @Test
    @Order(2)
    void testProcedureExists() throws SQLException {
        String query = "SELECT routine_name FROM information_schema.routines " +
                       "WHERE routine_type = 'PROCEDURE' " +
                       "AND routine_schema = 'stocks_db' " +
                       "AND routine_name = 'UpdateOrderStatus'";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            assertTrue(rs.next(), "Stored procedure 'UpdateOrderStatus' should exist");
        }
    }

    @Test
    @Order(3)
    void testUpdateOrderStatus_ProcessingToShipped() throws SQLException {
        int orderId = 1;

        // Reset status to 'Processing'
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE orders SET status = 'Processing' WHERE order_id = ?")) {
            ps.setInt(1, orderId);
            ps.executeUpdate();
        }

        // Call procedure
        try (CallableStatement cs = connection.prepareCall("{CALL UpdateOrderStatus(?)}")) {
            cs.setInt(1, orderId);
            cs.execute();
        }

        // Check updated status
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT status FROM orders WHERE order_id = ?")) {
            ps.setInt(1, orderId);
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals("Shipped", rs.getString("status"));
        }
    }

    @Test
    @Order(4)
    void testUpdateOrderStatus_AlreadyShipped() throws SQLException {
        int orderId = 2;

        // Ensure status is 'Shipped'
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE orders SET status = 'Shipped' WHERE order_id = ?")) {
            ps.setInt(1, orderId);
            ps.executeUpdate();
        }

        // Call procedure
        try (CallableStatement cs = connection.prepareCall("{CALL UpdateOrderStatus(?)}")) {
            cs.setInt(1, orderId);
            cs.execute();
        }

        // Status should remain 'Shipped'
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT status FROM orders WHERE order_id = ?")) {
            ps.setInt(1, orderId);
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals("Shipped", rs.getString("status"));
        }
    }

    @Test
    @Order(5)
    void testUpdateOrderStatus_PendingToShipped() throws SQLException {
        int orderId = 3;

        // Set to 'Pending'
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE orders SET status = 'Pending' WHERE order_id = ?")) {
            ps.setInt(1, orderId);
            ps.executeUpdate();
        }

        // Call procedure
        try (CallableStatement cs = connection.prepareCall("{CALL UpdateOrderStatus(?)}")) {
            cs.setInt(1, orderId);
            cs.execute();
        }

        // Verify status is now 'Shipped'
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT status FROM orders WHERE order_id = ?")) {
            ps.setInt(1, orderId);
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals("Shipped", rs.getString("status"));
        }
    }
}
