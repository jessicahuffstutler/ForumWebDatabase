package com.theironyard;

import spark.ModelAndView;
import spark.Session;
import spark.Spark;
import spark.template.mustache.MustacheTemplateEngine;


import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Main {

    public static void createTables(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS users (id IDENTITY, name VARCHAR, password VARCHAR)");
        stmt.execute("CREATE TABLE IF NOT EXISTS messages (id IDENTITY, user_id INT, reply_id INT, text VARCHAR)");
    }

    public static void insertUser(Connection conn, String name, String password) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO users VALUES (NULL, ?, ?)"); //null is the id of the user
        stmt.setString(1, name);
        stmt.setString(2, password);
        stmt.execute();
    }

    public static User selectUser(Connection conn, String name) throws SQLException { //return type is one User object or null if it cant find anything (string name tell us what user to select from the database)
        User user = null;
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE name = ?");
        stmt.setString(1, name);
        ResultSet results = stmt.executeQuery(); //WHAT DOES THIS DO?
        if (results.next()) { //using if because we only want to get the first one, no need for while to loop through the whole list
            user = new User();
            user.id = results.getInt("id");
            user.password = results.getString("password");
        }
        return user;
    }

    public static void insertMessage(Connection conn, int userId, int replyId, String text) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO messages VALUES (NULL, ?, ?, ?)");
        stmt.setInt(1, userId);
        stmt.setInt(2, replyId);
        stmt.setString(3, text);
        stmt.execute();
    }

    public static Message selectMessage(Connection conn, int id) throws SQLException { //returns a message
        Message message = null; //creating message, initializing below
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM messages INNER JOIN users ON messages.user_id = users.id WHERE messages.id = ?");
        stmt.setInt(1, id);
        ResultSet results = stmt.executeQuery();
        if (results.next()) {
            message = new Message(); //got "message cannot be applied to" error so we go into Message.java and we create a blank constructor
            message.id = results.getInt("messages.id");
            message.replyId = results.getInt("messages.reply_id"); //reply_id is a column created in the CREATE TABLE
            message.username = results.getString("users.name");
            message.text = results.getString("messages.text");
        }
        return message;
    }

    public static ArrayList<Message> selectReplies(Connection conn, int replyId) throws SQLException {
        ArrayList<Message> replies = new ArrayList();
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM messages INNER JOIN users ON messages.user_id = users.id WHERE messages.reply_id = ?"); //we can have more than one reply message
        stmt.setInt(1, replyId);
        ResultSet results = stmt.executeQuery();
        while (results.next()) { //need while since there can be multiple replies
            Message message = new Message(); //creating and initializing messsage
            message.id = results.getInt("messages.id");
            message.replyId = results.getInt("messages.reply_id"); //reply_id is a column created in the CREATE TABLE
            message.username = results.getString("users.name");
            message.text = results.getString("messages.text");
            replies.add(message);
        }
        return replies;
    }

    public static void main(String[] args) throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:h2:./main");
        createTables(conn);

//        no further need for these
//	    HashMap<String, User> users = new HashMap(); //storing things in volitile memory instead of databases
//        ArrayList<Message> messages = new ArrayList(); //storing things in volitile memory instead of databases
//
//        addTestUsers(users);
//        addTestMessages(messages);

        Spark.get(
                "/",
                ((request, response) -> {
                    Session session = request.session();
                    String username = session.attribute("username");

                    ArrayList<Message> threads = selectReplies(conn, -1); //-1 means its a top level thread
//                    for (Message message : messages) {
//                        if (message.replyId == -1) {
//                            threads.add(message);
//                        }
//                    }

                    HashMap m = new HashMap();
                    m.put("threads", threads);
                    m.put("username", username);
                    m.put("replyId", -1);
                    return new ModelAndView(m, "threads.html");
                }),
                new MustacheTemplateEngine()
        );
        Spark.get(
                "/replies",
                ((request, response) -> {
                    Session session = request.session();
                    String username = session.attribute("username");

                    HashMap m = new HashMap();
                    m.put("username", username);

                    String id = request.queryParams("id");
                    try {
                        int idNum = Integer.valueOf(id);
//                        Message message = messages.get(idNum); //replaced below
                        Message message = selectMessage(conn, idNum);
                        m.put("message", message);
                        m.put("replyId", message.id);

//                        ArrayList<Message> replies = new ArrayList(); //replaced below
                        ArrayList<Message> replies = selectReplies(conn, message.id);
//                        for (Message msg : messages) {
//                            if (msg.replyId == message.id) {
//                                replies.add(msg);
//                            }
//                        }
                        m.put("replies", replies);
                    } catch (Exception e) {

                    }

                    return new ModelAndView(m, "replies.html");
                }),
                new MustacheTemplateEngine()
        );
        Spark.post(
                "/login",
                ((request, response) -> {
                    String username = request.queryParams("username");
                    String password = request.queryParams("password");

                    if (username.isEmpty() || password.isEmpty()) {
                        Spark.halt(403);
                    }

//                    User user = users.get(username); //replaced below
                    User user = selectUser(conn, username);
                    if (user == null) {
//                        user = new User();
//                        user.password = password;
//                        users.put(username, user); //replaced below
                        insertUser(conn, username, password);
                    }
                    else if (!password.equals(user.password)) {
                        Spark.halt(403);
                    }

                    Session session = request.session();
                    session.attribute("username", username);

                    response.redirect(request.headers("Referer"));
                    return "";
                })
        );
        Spark.post(
                "/create-message",
                ((request, response) -> {
                    Session session = request.session();
                    String username = session.attribute("username");

                    if (username == null) {
                        Spark.halt(403);
                    }

                    String replyId = request.queryParams("replyId");
                    String text = request.queryParams("text");
                    try {
                        int replyIdNum = Integer.valueOf(replyId);
                        User me = selectUser(conn, username); //requesting user object
                        insertMessage(conn, me.id, replyIdNum, text);
//                        Message message = new Message(messages.size(), replyIdNum, username, text); //replaced above
//                        messages.add(message);
                    } catch (Exception e) {

                    }

                    response.redirect(request.headers("Referer"));
                    return "";
                })
        );
    }

//    static void addTestUsers(HashMap<String, User> users) {
//        users.put("Alice", new User());
//        users.put("Bob", new User());
//        users.put("Charlie", new User());
//    }
//
//    static void addTestMessages(ArrayList<Message> messages) {
//        messages.add(new Message(0, -1, "Alice", "This is a thread!"));
//        messages.add(new Message(1, -1, "Bob", "This is a thread!"));
//        messages.add(new Message(2, 0, "Charlie", "Cool thread, Alice."));
//        messages.add(new Message(3, 2, "Alice", "Thanks"));
//    }
}
