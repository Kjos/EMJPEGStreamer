package net.kajos.Handlers;
import net.kajos.Manager.Manager;
import net.kajos.Manager.Viewer;
import net.kajos.Util;
import org.json.JSONObject;
import org.webbitserver.HttpControl;
import org.webbitserver.HttpHandler;
import org.webbitserver.HttpRequest;
import org.webbitserver.HttpResponse;

import java.util.Iterator;
import java.util.Set;

public class ControlsHandler implements HttpHandler {
    private Manager manager;
    
    public ControlsHandler(Manager manager) {
        this.manager = manager;
    }
    @Override
    public void handleHttpRequest(HttpRequest request, HttpResponse response, HttpControl control) throws Exception {
        String viewerHash = request.queryParam("viewerHash");

        Viewer player = manager.viewers.get(viewerHash);

        Set<String> qParamKeys = request.queryParamKeys();
        Iterator<String> keyIt = qParamKeys.iterator();

        while (keyIt.hasNext()) {
            String key = keyIt.next();
            String value = request.queryParam(key);

            try {
                switch(key) {
                    case "WIDTH": player.clientWidth = Integer.valueOf(value);
                        break;
                    case "HEIGHT": player.clientHeight = Integer.valueOf(value);
                        break;
                    case "LOGOUT":
                        manager.removeViewer(viewerHash);
                        FilterHandler.errorCode(response, 502);
                        return;
                }

                player.newUpdate();
            } catch (NumberFormatException e) {
                System.out.println("Malformed integer passing by");
                continue;
            }
        }

        JSONObject obj = new JSONObject();

        response.header("Content-type", "text/json")
                .header("Access-Control-Allow-Origin", "*")
                .content(obj.toString(1))
                .end();
    }
}
