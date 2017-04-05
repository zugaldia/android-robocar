package com.zugaldia.robocar.mobile;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.zugaldia.robocar.software.controller.nes30.NES30Manager;
import com.zugaldia.robocar.software.webserver.LocalWebServer;
import com.zugaldia.robocar.software.webserver.RequestListener;
import com.zugaldia.robocar.software.webserver.RobocarClient;
import com.zugaldia.robocar.software.webserver.models.RobocarMove;
import com.zugaldia.robocar.software.webserver.models.RobocarResponse;
import com.zugaldia.robocar.software.webserver.models.RobocarStatus;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fi.iki.elonen.NanoHTTPD;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity
  implements NavigationView.OnNavigationItemSelectedListener, RequestListener {

  @BindView(R.id.button_left)
  Button buttonLeft;
  @BindView(R.id.button_right)
  Button buttonRight;
  @BindView(R.id.button_up)
  Button buttonUp;
  @BindView(R.id.button_down)
  Button buttonDown;
  @BindView(R.id.button_l)
  Button buttonL;
  @BindView(R.id.button_r)
  Button buttonR;
  @BindView(R.id.button_select)
  Button buttonSelect;
  @BindView(R.id.button_start)
  Button buttonStart;
  @BindView(R.id.button_y)
  Button buttonY;
  @BindView(R.id.button_a)
  Button buttonA;
  @BindView(R.id.button_x)
  Button buttonX;
  @BindView(R.id.button_b)
  Button buttonB;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    ButterKnife.bind(this);

    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
          .setAction("Action", null).show();
      }
    });

    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
    ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
      this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
    drawer.setDrawerListener(toggle);
    toggle.syncState();

    NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
    navigationView.setNavigationItemSelectedListener(this);

    // Local web server
    setupWebServer();
  }

  @OnClick( {R.id.button_left, R.id.button_right, R.id.button_up, R.id.button_down, R.id.button_l, R.id.button_r, R.id.button_select, R.id.button_start, R.id.button_y, R.id.button_a, R.id.button_x, R.id.button_b})
  public void onButtonClick(Button button) {
    RobocarClient client = new RobocarClient();

    Callback<RobocarResponse> callback = new Callback<RobocarResponse>() {
      @Override
      public void onResponse(Call<RobocarResponse> call, Response<RobocarResponse> response) {
        Timber.d("Response with code: %d", response.code());
        Timber.d("Response with Robocar code: %d", response.body().getCode());
        Timber.d("Response with Robocar message: %s", response.body().getMessage());
      }

      @Override
      public void onFailure(Call<RobocarResponse> call, Throwable t) {
        Timber.e(t, "Failed to contact Robocar.");
      }
    };

    switch (button.getId()) {
      case R.id.button_left:
        client.postMove(new RobocarMove(NES30Manager.BUTTON_LEFT_CODE), callback);
        break;
      case R.id.button_right:
        client.postMove(new RobocarMove(NES30Manager.BUTTON_RIGHT_CODE), callback);
        break;
      case R.id.button_up:
        client.postMove(new RobocarMove(NES30Manager.BUTTON_UP_CODE), callback);
        break;
      case R.id.button_down:
        client.postMove(new RobocarMove(NES30Manager.BUTTON_DOWN_CODE), callback);
        break;
      case R.id.button_l:
        client.postMove(new RobocarMove(NES30Manager.BUTTON_L_CODE), callback);
        break;
      case R.id.button_r:
        client.postMove(new RobocarMove(NES30Manager.BUTTON_R_CODE), callback);
        break;
      case R.id.button_select:
        client.postMove(new RobocarMove(NES30Manager.BUTTON_SELECT_CODE), callback);
        break;
      case R.id.button_start:
        client.postMove(new RobocarMove(NES30Manager.BUTTON_START_CODE), callback);
        break;
      case R.id.button_y:
        client.postMove(new RobocarMove(NES30Manager.BUTTON_Y_CODE), callback);
        break;
      case R.id.button_a:
        client.postMove(new RobocarMove(NES30Manager.BUTTON_A_CODE), callback);
        break;
      case R.id.button_x:
        client.postMove(new RobocarMove(NES30Manager.BUTTON_X_CODE), callback);
        break;
      case R.id.button_b:
        client.postMove(new RobocarMove(NES30Manager.BUTTON_B_CODE), callback);
        break;
    }
  }

  private void setupWebServer() {
    LocalWebServer localWebServer = new LocalWebServer(this);
    try {
      localWebServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, true);
    } catch (IOException e) {
      Timber.e(e, "Failed to start local web server.");
    }

    Timber.d("IP address: %s", LocalWebServer.getIpAddress(this));
  }

  @Override
  public void onBackPressed() {
    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
    if (drawer.isDrawerOpen(GravityCompat.START)) {
      drawer.closeDrawer(GravityCompat.START);
    } else {
      super.onBackPressed();
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @SuppressWarnings("StatementWithEmptyBody")
  @Override
  public boolean onNavigationItemSelected(MenuItem item) {
    // Handle navigation view item clicks here.
    int id = item.getItemId();

    if (id == R.id.nav_camera) {
      // Handle the camera action
    } else if (id == R.id.nav_gallery) {

    } else if (id == R.id.nav_slideshow) {

    } else if (id == R.id.nav_manage) {

    } else if (id == R.id.nav_share) {

    } else if (id == R.id.nav_send) {

    }

    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
    drawer.closeDrawer(GravityCompat.START);
    return true;
  }

  @Override
  public void onRequest(NanoHTTPD.IHTTPSession session) {
    LocalWebServer.logSession(session);
  }

  @Override
  public RobocarStatus onStatus() {
    return new RobocarStatus(200, "Ok.");
  }

  @Override
  public RobocarResponse onMove(RobocarMove move) {
    return new RobocarResponse(200, String.format("Moved: %d", move.getKeyCode()));
  }
}
