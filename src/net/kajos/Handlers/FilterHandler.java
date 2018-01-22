package net.kajos.Handlers;

import net.kajos.Manager.Manager;
import net.kajos.Util;
import org.webbitserver.HttpControl;
import org.webbitserver.HttpHandler;
import org.webbitserver.HttpRequest;
import org.webbitserver.HttpResponse;

import java.util.ArrayList;

public class FilterHandler implements HttpHandler {
    private final ArrayList<String> secretKeys = new ArrayList<>();

    private Manager manager;

    public FilterHandler(Manager manager) {
        this.manager = manager;
        secretKeys.add("put_secret_key_here");
    }

    public static void errorCode(HttpResponse response, int code) {
        response.header("Access-Control-Allow-Origin", "*").status(code).end();
    }

    @Override
    public void handleHttpRequest(HttpRequest request, HttpResponse response, HttpControl control) throws Exception {
        String viewerHash = request.queryParam("viewerHash");
        if (viewerHash == null ||
                viewerHash.isEmpty()) {
            System.out.println("No viewerHash");
            errorCode(response, 500);
            return;
        }

        if (!manager.viewers.containsKey(viewerHash)) {
            // Other URLs will go to another handler.
            if (!secretKeys.contains(viewerHash)) {
                System.out.println("Invalid key");
                Thread.sleep(1000   );
                errorCode(response, 500);
                return;
            }
            manager.addViewer(viewerHash);
            control.nextHandler();
        } else {
            // Other URLs will go to another handler.
            control.nextHandler();
        }
    }
}
