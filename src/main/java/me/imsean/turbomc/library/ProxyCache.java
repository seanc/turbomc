package me.imsean.turbomc.library;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by sean on 1/3/16.
 */
public class ProxyCache {

    public static void put(MySQLConnection conn, String key, String value) {
        Thread thread = new Thread(() -> {
            PreparedStatement stmt = conn.query("INSERT INTO `proxy_cache` (`host`, `port`) VALUES (?, ?)").getStatement();
            try {
                stmt.setString(1, key);
                stmt.setString(2, value);
                stmt.executeUpdate();
                System.out.println("Cached Proxy: " + key + ":" + value);

                Timer timer = new Timer();
                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        PreparedStatement stmt = conn.query("DELETE FROM `proxy_cache` WHERE `host`=?").getStatement();
                        try {
                            stmt.setString(1, key);
                            stmt.executeUpdate();
                            System.out.println("Refreshed proxy: " + key + ":" + value);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                };
                timer.schedule(task, 360000L);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }

    public static void remove(MySQLConnection conn, String key) {
        Thread thread = new Thread(() -> {
            PreparedStatement stmt = conn.query("DELETE FROM `proxy_cache` WHERE `host`=?").getStatement();
            try {
                stmt.setString(1, key);
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }

    public static boolean contains(MySQLConnection conn, String key) {
        PreparedStatement stmt = conn.query("SELECT * FROM `proxy_cache` WHERE `host`=?").getStatement();
        try {
            stmt.setString(1, key);
            ResultSet resultSet = stmt.executeQuery();
            while(resultSet.next()) {
                if(resultSet.last()) return resultSet.getRow() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
