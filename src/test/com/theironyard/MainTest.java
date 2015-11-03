package com.theironyard;

import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * Created by jessicahuffstutler on 11/3/15.
 */
public class MainTest {
    public Connection startConncetion() throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:h2:./test");
        Main.createTables(conn);
        return conn;
    }

    public void endConnection(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.execute("DROP TABLE users");
        stmt.execute("DROP TABLE messages");
        conn.close();
    }

    @Test //this is how it knows its a test method below
    public void testUser() throws SQLException {
        Connection conn = startConncetion();
        Main.insertUser(conn, "Alice", ""); //creates a test user
        User user = Main.selectUser(conn, "Alice"); //just the connection and the name is needed to select the user
        endConnection(conn);

        assertTrue(user != null); //argument is a boolean; this will assert true if it finds this user in the database
    }

    @Test
    public void testMessage() throws SQLException {
        Connection conn = startConncetion();
        Main.insertUser(conn, "Alice", ""); //insert a user named alice and a message "hello world!"
        Main.insertMessage(conn, 1, -1, "Hello, world!"); //1 = id, -1 = replyId
        Message message = Main.selectMessage(conn, 1);
        endConnection(conn);

        assertTrue(message != null);
    }

    @Test
    public void testReplies() throws SQLException {
        Connection conn = startConncetion();
        Main.insertUser(conn, "Alice", "");
        Main.insertUser(conn, "Bob", "");
        Main.insertMessage(conn, 1, -1, "Hello, world!"); //-1 means it's a top level thread
        Main.insertMessage(conn, 2, 1, "This is a reply."); //replyid is the id of the message which is 1
        Main.insertMessage(conn, 2, 1, "This is a ANOTHER reply.");
        ArrayList<Message> replies = Main.selectReplies(conn, 1);
        endConnection(conn);

        assertTrue(replies.size() == 2);
    }

}