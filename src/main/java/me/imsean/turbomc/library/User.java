package me.imsean.turbomc.library;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by sean on 1/2/16.
 */
public class User {

    private final MySQLConnection conn;
    private final String requestToken;
    private final int snipeID;

    public User(MySQLConnection conn, String requestToken, int snipeID) {
        this.conn = conn;
        this.requestToken = requestToken;
        this.snipeID = snipeID;
    }

    public void refund() {
        PreparedStatement stmt = this.conn.query("UPDATE `users` SET `credits` = `credits` + 1 WHERE `token`=?").getStatement();
        try {
            stmt.setString(1, this.requestToken);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void sell() {
        PreparedStatement stmt = this.conn.query("UPDATE `users` SET `credits` = GREATEST(0, `credits` - 1) WHERE `token`=?").getStatement();
        try {
            stmt.setString(1, this.requestToken);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setSnipeStatus(SnipeStatus status) {
        PreparedStatement stmt = this.conn.query("UPDATE `snipes` SET `status`=? WHERE `id`=?").getStatement();
        try {
            stmt.setString(1, status.name().toLowerCase());
            stmt.setInt(2, this.snipeID);
            stmt.executeUpdate();
            System.out.println("Set status of snipe " + this.snipeID + " to " + status.name().toLowerCase());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isSnipeCancelled() {
        PreparedStatement stmt = this.conn.query("SELECT `status` FROM `snipes` WHERE `id`=?").getStatement();
        try {
            stmt.setInt(1, this.snipeID);
            ResultSet results = stmt.executeQuery();
            while(results.next()) {
                if(!results.getString("status").equalsIgnoreCase("cancelled")) {
                    return false;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    public boolean isSnipeFailed() {
        PreparedStatement stmt = this.conn.query("SELECT `status` FROM `snipes` WHERE `id`=?").getStatement();
        try {
            stmt.setInt(1, this.snipeID);
            ResultSet results = stmt.executeQuery();
            while(results.next()) {
                if(!results.getString("status").equalsIgnoreCase("failed")) {
                    return false;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

}
