package com.ns.greg.mqttmanager;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import com.ns.greg.library.mqtt_manager.MqttManager;
import com.ns.greg.library.mqtt_manager.internal.Connection;
import com.ns.greg.library.mqtt_manager.internal.MqttActionListener;
import com.ns.greg.library.mqtt_manager.internal.Publishing;
import com.ns.greg.library.mqtt_manager.internal.Subscription;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Gregory
 * @since 2017/8/4
 */

public class DemoActivity extends AppCompatActivity {

  private static final String CLIENT_ID = "DEMO";

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Connection connection = MqttManager.getInstance().getConnection(CLIENT_ID);
    if (connection == null) {
      connection = MqttManager.getInstance()
          .addConnection(
              Connection.createConnectionWithTimeStamp(getApplicationContext(), "demo.url",
                  CLIENT_ID));
    }

    // connection
    connection.connect(new MqttActionListener.OnActionListener() {
      @Override public <T extends Subscription> void onSuccess(T subscription, String message) {

      }

      @Override public void onFailure(Subscription subscription, Throwable throwable) {

      }
    });

    // subscribe single, the manager will check the connection is connected or not
    // if not, the manager will automatic connect to server and subscribe when connected
    connection.subscribeTopic(new Subscription("/door"), new MqttActionListener.OnActionListener() {
      @Override public <T extends Subscription> void onSuccess(T subscription, String message) {

      }

      @Override public void onFailure(Subscription subscription, Throwable throwable) {

      }
    }, 0);

    // subscribe multiple, the manager will check the connection is connected or not
    // if not, the manager will automatic connect to server and subscribe when connected
    List<Subscription> subscriptionList = new ArrayList<>();
    subscriptionList.add(new Subscription("/window"));
    subscriptionList.add(new Subscription("/light"));
    connection.subscribeTopics(subscriptionList, new MqttActionListener.OnActionListener() {
      @Override public <T extends Subscription> void onSuccess(T subscription, String message) {

      }

      @Override public void onFailure(Subscription subscription, Throwable throwable) {

      }
    }, 0);

    // publish, the manager will check the connection is connected or not
    // if not, the manager will automatic connect to server and publish when connected
    connection.publishTopic(new Publishing("/door", "close"),
        new MqttActionListener.OnActionListener() {
          @Override public <T extends Subscription> void onSuccess(T subscription, String message) {

          }

          @Override public void onFailure(Subscription subscription, Throwable throwable) {

          }
        });
  }
}
