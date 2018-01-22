package net.kajos;

import net.kajos.Manager.Manager;
import net.kajos.Handlers.ControlsHandler;
import net.kajos.Handlers.FilterHandler;
import org.webbitserver.*;

import java.net.*;
import java.util.concurrent.Executors;

/**
 * Created by kajos on 6-8-17.
 */
public class Server {
    private WebServer webServer;
    private ControlsHandler controlsHandler;

    private Manager manager;
    private ScreenRecorder recorder;

    public Server() {
    }

    public void start() throws InterruptedException {
        manager = new Manager();

        FilterHandler filterHandler = new FilterHandler(manager);
        controlsHandler = new ControlsHandler(manager);

        webServer = WebServers.createWebServer(Executors.newFixedThreadPool(Config.MAX_VIEWERS), Config.WEB_PORT);
        webServer.add(filterHandler);
        webServer.add("/control", controlsHandler);
        webServer.add("/websocket", manager);
        webServer.start();

        System.out.println("Web server started on port: " + Config.WEB_PORT);

        recorder = new ScreenRecorder(manager);
        recorder.start();

        while(true) Thread.sleep(1000);
    }
}
