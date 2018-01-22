package net.kajos.Manager;

import net.kajos.Handlers.WebSocketHandler;
import org.webbitserver.WebSocketConnection;

import java.util.HashMap;
import java.util.Iterator;

public class Manager extends WebSocketHandler {

    public HashMap<String, Viewer> viewers = new HashMap<>();

    public Manager() {
    }

    public Viewer addViewer(String hash) {
        Viewer newViewer = new Viewer(hash);
        viewers.put(hash, newViewer);
        System.out.println("New viewer entered: " + hash);
        return newViewer;
    }

    public void removeViewer(String hash) {
        Viewer viewer = viewers.get(hash);
        if (viewer != null) {
            viewers.remove(hash);
            System.out.println("Viewer left: " + hash);
        }
        closeConnection(hash);
    }

    @Override
    public void onMessage(WebSocketConnection connection, String message) {
        String viewerHash = connection.httpRequest().queryParam("viewerHash");

        if (message.equals("1")) {
            Viewer viewer = viewers.get(viewerHash);
            if (viewer != null) viewer.frameUpdate();
        }
    }

    public float getSumBandwidth() {
        float b = 0f;
        Iterator<Viewer> pIt = viewers.values().iterator();
        while (pIt.hasNext()) {
            b += pIt.next().bandwidth.get();
        }
        return b;
    }
}
