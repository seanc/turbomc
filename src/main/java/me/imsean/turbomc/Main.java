package me.imsean.turbomc;

import me.imsean.turbomc.library.Config;
import me.imsean.turbomc.library.MySQLConnection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by sean on 12/30/15.
 */
public class Main {

    private MySQLConnection conn;

    public Main() {
        System.out.println("Listening for queue requests from database");

        final Config config = new Config();

        conn = new MySQLConnection(config.getConfig().getProperty("host"), config.getConfig().getProperty("name"),
                config.getConfig().getProperty("user"), config.getConfig().getProperty("pass"));
        Timer autoQueueTimer = new Timer();
        TimerTask autoQueue = new TimerTask() {
            @Override
            public void run() {
                try {
                    ResultSet results = conn.query("SELECT * FROM `snipes` WHERE `status`='available'").execute();
                    while(results.next()) {
                        new Sniper(
                                results.getInt("id"),
                                results.getString("username"),
                                results.getString("mojang_email"),
                                results.getString("mojang_password"),
                                results.getString("release_date"),
                                results.getString("request_token"),
                                conn,
                                config
                        );
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        };
        autoQueueTimer.scheduleAtFixedRate(autoQueue, 1000, 2000);
    }

    public static void main(final String[] args) {
        System.out.println("TurboMC Started");
        if(args.length == 1) {
            if(args[0].equalsIgnoreCase("help")) {
                System.out.println("Usage: java -jar turbomc2.jar [username] [mojang-email] [mojang-password] [release-date] [token]");
            }
            return;
        }
        if(args.length >= 3) {
            new Sniper(0, args[0], args[1], args[2], args[3], args[4], null, null);
        } else {
            new Main();
        }
    }

}
