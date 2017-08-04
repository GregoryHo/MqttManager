package com.ns.greg.mqttmanager;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import com.ns.greg.library.mqtt_manager.MqttManager;

/**
 * @author Gregory
 * @since 2017/8/4
 */

public class DemoActivity extends AppCompatActivity {

  private static final String CLIENT_ID = "DEMO";

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    MqttManager.getInstance();
  }
}
