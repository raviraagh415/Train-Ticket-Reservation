import java.sql.*;
import java.util.Scanner;

public class TrainReservationDB {
    static final String URL = "jdbc:mysql://localhost:3306/train_reservation";
    static final String USER = "root"; // replace with your MySQL username
    static final String PASS = "password"; // replace with your MySQL password

    public static void viewTrains() throws SQLException {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM trains");
            System.out.println("Available Trains:");
            while (rs.next()) {
                System.out.printf("Train %d - %s (%s to %s), Seats: %d\n",
                        rs.getInt("train_no"),
                        rs.getString("train_name"),
                        rs.getString("source"),
                        rs.getString("destination"),
                        rs.getInt("seats_available"));
            }
        }
    }

    public static void bookTicket(int trainNo, String userName, int seats) throws SQLException {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            conn.setAutoCommit(false);

            PreparedStatement check = conn.prepareStatement("SELECT seats_available FROM trains WHERE train_no = ?");
            check.setInt(1, trainNo);
            ResultSet rs = check.executeQuery();

            if (rs.next()) {
                int available = rs.getInt(1);
                if (available >= seats) {
                    PreparedStatement update = conn.prepareStatement("UPDATE trains SET seats_available = seats_available - ? WHERE train_no = ?");
                    update.setInt(1, seats);
                    update.setInt(2, trainNo);
                    update.executeUpdate();

                    PreparedStatement insert = conn.prepareStatement("INSERT INTO bookings(train_no, user_name, seats_booked) VALUES (?, ?, ?)");
                    insert.setInt(1, trainNo);
                    insert.setString(2, userName);
                    insert.setInt(3, seats);
                    insert.executeUpdate();

                    conn.commit();
                    System.out.println("Booking successful!");
                } else {
                    System.out.println("Not enough seats available.");
                }
            } else {
                System.out.println("Train not found.");
            }
        }
    }

    public static void cancelTicket(int bookingId) throws SQLException {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            conn.setAutoCommit(false);

            PreparedStatement fetch = conn.prepareStatement("SELECT train_no, seats_booked FROM bookings WHERE booking_id = ?");
            fetch.setInt(1, bookingId);
            ResultSet rs = fetch.executeQuery();

            if (rs.next()) {
                int trainNo = rs.getInt("train_no");
                int seats = rs.getInt("seats_booked");

                PreparedStatement update = conn.prepareStatement("UPDATE trains SET seats_available = seats_available + ? WHERE train_no = ?");
                update.setInt(1, seats);
                update.setInt(2, trainNo);
                update.executeUpdate();

                PreparedStatement delete = conn.prepareStatement("DELETE FROM bookings WHERE booking_id = ?");
                delete.setInt(1, bookingId);
                delete.executeUpdate();

                conn.commit();
                System.out.println("Booking cancelled successfully.");
            } else {
                System.out.println("Booking ID not found.");
            }
        }
    }

    public static void viewMyBookings(String userName) throws SQLException {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            PreparedStatement stmt = conn.prepareStatement("SELECT b.booking_id, t.train_name, t.source, t.destination, b.seats_booked " +
                    "FROM bookings b JOIN trains t ON b.train_no = t.train_no WHERE b.user_name = ?");
            stmt.setString(1, userName);
            ResultSet rs = stmt.executeQuery();

            System.out.println("Your Bookings:");
            while (rs.next()) {
                System.out.printf("Booking ID: %d | Train: %s | Route: %s to %s | Seats: %d\n",
                        rs.getInt("booking_id"),
                        rs.getString("train_name"),
                        rs.getString("source"),
                        rs.getString("destination"),
                        rs.getInt("seats_booked"));
            }
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String userName;

        System.out.print("Enter your name: ");
        userName = sc.nextLine();

        int choice;
        try {
            do {
                System.out.println("\n--- Train Reservation System ---");
                System.out.println("1. View Trains");
                System.out.println("2. Book Ticket");
                System.out.println("3. Cancel Ticket");
                System.out.println("4. View My Bookings");
                System.out.println("5. Exit");
                System.out.print("Enter your choice: ");
                choice = sc.nextInt();

                switch (choice) {
                    case 1:
                        viewTrains();
                        break;
                    case 2:
                        System.out.print("Enter Train No: ");
                        int trainNo = sc.nextInt();
                        System.out.print("Enter number of seats: ");
                        int seats = sc.nextInt();
                        bookTicket(trainNo, userName, seats);
                        break;
                    case 3:
                        System.out.print("Enter Booking ID to cancel: ");
                        int bId = sc.nextInt();
                        cancelTicket(bId);
                        break;
                    case 4:
                        viewMyBookings(userName);
                        break;
                    case 5:
                        System.out.println("Goodbye!");
                        break;
                    default:
                        System.out.println("Invalid choice.");
                }
            } while (choice != 5);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        sc.close();
    }
}



//mysql//

-- Create the database
CREATE DATABASE IF NOT EXISTS train_reservation;
USE train_reservation;

-- Create the trains table
CREATE TABLE IF NOT EXISTS trains (
    train_no INT PRIMARY KEY,
    train_name VARCHAR(50) NOT NULL,
    source VARCHAR(50) NOT NULL,
    destination VARCHAR(50) NOT NULL,
    seats_available INT NOT NULL CHECK (seats_available >= 0)
);

-- Create the bookings table
CREATE TABLE IF NOT EXISTS bookings (
    booking_id INT AUTO_INCREMENT PRIMARY KEY,
    train_no INT NOT NULL,
    user_name VARCHAR(50) NOT NULL,
    seats_booked INT NOT NULL CHECK (seats_booked > 0),
    FOREIGN KEY (train_no) REFERENCES trains(train_no)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- Insert sample trains
INSERT INTO trains (train_no, train_name, source, destination, seats_available) VALUES 
(101, 'Express A', 'City A', 'City B', 50),
(102, 'Express B', 'City B', 'City C', 60),
(103, 'Express C', 'City C', 'City D', 70);
