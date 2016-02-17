package me.imsean.turbomc;

import me.imsean.easyhttp.EasyHttpRequest;
import me.imsean.easyhttp.EasyHttpResponse;
import me.imsean.easyhttp.RequestType;
import me.imsean.turbomc.library.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by sean on 12/30/15.
 */
public class Sniper {

    private final int snipeID;
    private final String username, mojangEmail, mojangPassword, releaseDate, accountToken;
    private final MySQLConnection conn;
    private final User user;
    private final Config config;
    private long snipeTime;
    private String[] proxy;
    private EasyHttpResponse response;

    private final String MOJANG_LOGIN = "https://account.mojang.com/login";
    private final String MOJANG_PROFILE = "https://account.mojang.com/me";
    private final String MOJANG_RENAME = "https://account.mojang.com/me/renameProfile/";
    private final String USER_AGENT = "Mozilla";

    private String uuid;
    private String authenticityToken;

    public Sniper(final int snipeID,
                  final String username,
                  final String mojangEmail,
                  final String mojangPassword,
                  final String releaseDate,
                  final String accountToken,
                  final MySQLConnection conn,
                  final Config config) {
        this.snipeID = snipeID;
        this.username = username;
        this.mojangEmail = mojangEmail;
        this.mojangPassword = mojangPassword;
        this.releaseDate = releaseDate;
        this.accountToken = accountToken;
        this.conn = conn;
        this.user = new User(this.conn, this.accountToken, this.snipeID);
        this.config = config;

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a");
        try {
            Date date = simpleDateFormat.parse(this.releaseDate);
            Calendar calendar = Calendar.getInstance();
            if(!this.config.getConfig().getProperty("timezone").isEmpty())
                calendar.setTimeZone(TimeZone.getTimeZone(this.config.getConfig().getProperty("timezone")));
            calendar.setTime(date);
            this.snipeTime = calendar.getTimeInMillis() - System.currentTimeMillis();
            this.queue();
            System.out.println("Queued username: " + username + " for date: " + calendar.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public void loadProxy() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("proxies.txt"));
            String line;
            List<String> lines = new ArrayList<>();
            while((line = reader.readLine()) != null) {
                lines.add(line);
            }
            reader.close();
            Random random = new Random();
            if(lines.size() == 0) {
                this.proxy = null;
            } else {
                this.proxy = lines.get(random.nextInt(lines.size())).split(":");
                if(!ProxyCache.contains(conn, this.proxy[0])) {
                    ProxyCache.put(conn, this.proxy[0], this.proxy[1]);
                    System.out.println("Using proxy " + this.proxy[0] + ":" + this.proxy[1]);
                } else {
                    loadProxy();
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Couldn't find or read proxy file.");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void queue() {
        new Thread(() -> {
            this.loadProxy();
            this.user.setSnipeStatus(SnipeStatus.PENDING);
            this.user.sell();
            this.login();
            try {
                this.changeUsername();
            } catch (RuntimeException e) {
                System.out.println("Snipe completed for username " + username);
            }
        }).start();
    }

    public void login() {
        if(this.user.isSnipeCancelled()) return;
        Timer timer = new Timer();
        TimerTask loginTask = new TimerTask() {
            @Override
            public void run() {
                System.out.println("Logging into account for username " + username);

                try {
                    // Login
                    response = new EasyHttpRequest(MOJANG_LOGIN)
                            .addField("username", mojangEmail)
                            .addField("password", mojangPassword)
                            .setType(RequestType.POST)
                            //.setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxy[0], Integer.valueOf(proxy[1]))))
                            .execute();

                    if(config.getConfig().getProperty("debug").equalsIgnoreCase("true")) {
                        System.out.println(response.getResponse());
                    }

                    response = new EasyHttpRequest(MOJANG_PROFILE)
                            .setCookies(response.getCookies())
                            //.setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxy[0], Integer.valueOf(proxy[1]))))
                            .setType(RequestType.GET)
                            .execute();

                    // Get UUID
                    Document document = Jsoup.parse(response.getResponse());
                    String[] split = document.select("a[href^=\"/me/renameProfile/\"]").attr("href").split("/");
                    uuid = split[split.length - 1];

                    if(config.getConfig().getProperty("debug").equalsIgnoreCase("true")) {
                        System.out.println(document.body());
                    }

                    response = new EasyHttpRequest(MOJANG_RENAME + uuid)
                            .setCookies(response.getCookies())
                            //.setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxy[0], Integer.valueOf(proxy[1]))))
                            .setType(RequestType.GET)
                            .execute();


                    // Get Authenticity Token
                    document = Jsoup.parse(response.getResponse());
                    authenticityToken = document.select("input[name=\"authenticityToken\"]").val();

                    StringBuilder processDebug = new StringBuilder();
                    processDebug.append("Login process completed for username ").append(username).append(System.lineSeparator());
                    processDebug.append("UUID: " ).append(uuid).append(System.lineSeparator());
                    processDebug.append("Authenticity Token: ").append(authenticityToken).append(System.lineSeparator());
                    System.out.println(processDebug.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Snipe failed for username " + username + " on login");
                    user.setSnipeStatus(SnipeStatus.FAILED);
                    user.refund();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        long bufferTime = this.snipeTime - Long.valueOf(this.config.getConfig().getProperty("loginBuffer"));
        timer.schedule(loginTask, bufferTime);
    }

    public void changeUsername() {
        if(this.user.isSnipeCancelled() || this.user.isSnipeFailed()) return;
        ScheduledExecutorService scheduledExecutorService =
                Executors.newScheduledThreadPool(1000);

        long bufferTime = this.snipeTime - Long.valueOf(this.config.getConfig().getProperty("snipeBuffer"));
        boolean success = false;

        ScheduledFuture scheduledFuture =
                scheduledExecutorService.schedule(() -> {
                    System.out.println("Sniping process for username " + username + " started");
                    response = new EasyHttpRequest(MOJANG_RENAME + uuid)
                            .setCookies(response.getCookies())
                            //.setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxy[0], Integer.valueOf(proxy[1]))))
                            .setType(RequestType.POST)
                            .addField("authenticityToken", authenticityToken)
                            .addField("newName", username)
                            .addField("password", mojangPassword)
                            .execute();

                    Document document = Jsoup.parse(response.getResponse());
                    if(document.body().text().trim().equalsIgnoreCase("Name Changed.")) {
                        System.out.println("Username " + username + " successfully sniped");
                        scheduledExecutorService.shutdown();
                    } else {
                        if(config.getConfig().getProperty("debug").equalsIgnoreCase("true")) {
                            System.out.println(response.getResponse());
                        }
                        System.out.println("Snipe for username " + username + " failed");
                        user.setSnipeStatus(SnipeStatus.FAILED);
                        user.refund();
                    }
                    return document.body();
                }, bufferTime, TimeUnit.MILLISECONDS);
        try {
            System.out.println(scheduledFuture.get());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

}
