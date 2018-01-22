package net.kajos.Handlers;

import org.webbitserver.BaseWebSocketHandler;
import org.webbitserver.WebSocketConnection;

import java.util.HashMap;

public class WebSocketHandler extends BaseWebSocketHandler {
    private int connectionCount;

    private HashMap<String, WebSocketConnection> connections = new HashMap<>();

    public void sendImage(String viewerHash, byte[] data) {
        WebSocketConnection conn = connections.get(viewerHash);
        if (conn != null) {
            conn.send(data);
        }
    }

    final static byte[] EMPTY_IMAGE = new byte[]{0};
    public void sendEmptyImage(String viewerHash) {
        sendImage(viewerHash, EMPTY_IMAGE);
    }

    public void closeConnection(String viewerHash) {
        WebSocketConnection conn = connections.get(viewerHash);
        if (conn != null) {
            conn.close();

            connections.remove(viewerHash);
        }
    }

    public void onOpen(WebSocketConnection connection) {
        String viewerHash = connection.httpRequest().queryParam("viewerHash");

        closeConnection(viewerHash);
        connections.put(viewerHash, connection);

        connectionCount++;
        System.out.println("There are " + connectionCount + " connections active");
    }

    public void onClose(WebSocketConnection connection) {
        String viewerHash = connection.httpRequest().queryParam("viewerHash");

        connections.remove(viewerHash);

        connectionCount--;
        System.out.println("There are " + connectionCount + " connections active");
    }
}
