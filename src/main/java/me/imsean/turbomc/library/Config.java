package me.imsean.turbomc.library;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by sean on 1/2/16.
 */
public class Config {

    private final Properties properties;

    public Config() {
        this.properties = new Properties();
        try {
            this.properties.load(new FileInputStream("sniper.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Properties getConfig() {
        return this.properties;
    }

}
