package com.zugaldia.robocar.software.webserver;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;

import com.google.gson.GsonBuilder;
import com.zugaldia.robocar.software.webserver.models.RobocarMove;
import com.zugaldia.robocar.software.webserver.models.RobocarResponse;
import com.zugaldia.robocar.software.webserver.models.RobocarSpeed;

import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

/**
 * Created by antonio on 4/1/17.
 */

public class LocalWebServer extends NanoHTTPD {

  private static final String APPLICATION_JSON = "application/json";

  public static final String ENDPOINT_ROOT = "/";
  public static final String ENDPOINT_GET_STATUS = "api/status";
  public static final String ENDPOINT_POST_MOVE = "api/move";
  public static final String ENDPOINT_POST_SPEED = "api/speed";

  private RequestListener requestListener;

  public LocalWebServer(RequestListener requestListener) {
    super(8080);
    this.requestListener = requestListener;
  }

  public LocalWebServer(RequestListener requestListener, int port) {
    super(port);
    this.requestListener = requestListener;
  }

  @Override
  public Response serve(IHTTPSession session) {
    requestListener.onRequest(session);

    Object result = null;
    switch (session.getMethod()) {
      case GET:
        switch (session.getUri()) {
          case ENDPOINT_ROOT + ENDPOINT_GET_STATUS:
            result = requestListener.onStatus();
            break;
          default:
            // No action.
            break;
        }
        break;
      case POST:
        switch (session.getUri()) {
          case ENDPOINT_ROOT + ENDPOINT_POST_MOVE:
            RobocarMove move = (RobocarMove) readPostAsObject(session, RobocarMove.class);
            result = requestListener.onMove(move);
            break;
          case ENDPOINT_ROOT + ENDPOINT_POST_SPEED:
            RobocarSpeed speed = (RobocarSpeed)readPostAsObject(session, RobocarSpeed.class);
            result = requestListener.onSpeed(speed);
            break;
          default:
            // No action.
            break;
        }
        break;
      default:
        // No action.
        break;
    }

    if (result == null) {
      result = new RobocarResponse(404, String.format(
          "Unknown %s endpoint: %s", session.getMethod(), session.getUri()));
    }

    return buildResponse(result);
  }

  private Response buildResponse(Object object) {
    return newFixedLengthResponse(Response.Status.OK, APPLICATION_JSON,
        new GsonBuilder().create().toJson(object));
  }

  public static String getIpAddress(Context context) {
    WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    return Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
  }

  /**
   * Print out session log information.
   */
  public static void logSession(IHTTPSession session) {
    // E.g.: http://localhost:8080/foo.json?echo=true&foo=bar
    Timber.d("getMethod: %s", session.getMethod()); // GET
    Timber.d("getQueryParameterString: %s", session.getQueryParameterString()); // echo=true&foo=bar
    Timber.d("getUri: %s", session.getUri()); // /foo.json
    Timber.d("getRemoteIpAddress: %s", session.getRemoteIpAddress());
    Timber.d("getRemoteHostName: %s", session.getRemoteHostName());
    Timber.d("Cookies present: %b", session.getCookies() != null);
    Timber.d("Headers present: %b", session.getHeaders() != null);
    Timber.d("InputStream present: %b", session.getInputStream() != null);
    Timber.d("Parameters present: %b", session.getParameters() != null);
  }

  private static Object readPostAsObject(IHTTPSession session, Class clazz) {
    Map<String, String> files = new HashMap<>();

    try {
      session.parseBody(files);
    } catch (IOException | ResponseException e) {
      Timber.e(e, "Failed to parse POST response.");
      return null;
    }

    String postBody = files.get("postData");
    return new GsonBuilder().create().fromJson(postBody, clazz);
  }
}
