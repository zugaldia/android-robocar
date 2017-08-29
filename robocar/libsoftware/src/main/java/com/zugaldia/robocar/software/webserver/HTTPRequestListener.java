package com.zugaldia.robocar.software.webserver;

import com.zugaldia.robocar.software.webserver.models.RobocarMove;
import com.zugaldia.robocar.software.webserver.models.RobocarResponse;
import com.zugaldia.robocar.software.webserver.models.RobocarSpeed;
import com.zugaldia.robocar.software.webserver.models.RobocarStatus;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by antonio on 4/5/17.
 */

public interface HTTPRequestListener {

  void onRequest(NanoHTTPD.IHTTPSession session);

  RobocarStatus onStatus();

  RobocarResponse onMove(RobocarMove move);

  RobocarResponse onSpeed(RobocarSpeed speed);
}
