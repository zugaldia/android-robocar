package com.zugaldia.robocar.software.webserver;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;

import java.net.InetAddress;

import fi.iki.elonen.NanoHTTPD;
import timber.log.Timber;

import static android.content.Context.WIFI_SERVICE;

/**
 * Created by antonio on 4/1/17.
 */

public class LocalWebServer extends NanoHTTPD {
  public LocalWebServer() {
    super(8080);
    logServerInformation();
  }

  public LocalWebServer(int port) {
    super(port);
    logServerInformation();
  }

  @Override
  public Response serve(IHTTPSession session) {
    logSessionInformation(session);
    return newFixedLengthResponse(Response.Status.OK, "text/plain", "Done.");
  }

  public static String getIpAddress(Context context) {
    WifiManager wm = (WifiManager) context.getSystemService(WIFI_SERVICE);
    return Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
  }

  private void logServerInformation() {
    Timber.d("Hostname: %s", getHostname());
    Timber.d("Port: %d", getListeningPort());
  }

  private void logSessionInformation(IHTTPSession session) {
    // http://localhost:8080/foo.json?echo=true&foo=bar
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
}
