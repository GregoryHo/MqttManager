package com.ns.greg.mqttmanager;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
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
  private Connection connection;
  private TextView messageTV;

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.demo);
    messageTV = (TextView) findViewById(R.id.message);
    findViewById(R.id.window_open_btn).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        connection.publishTopic(new Publishing("window", "open window"),
            new MqttActionListener.OnActionListener() {
              @Override
              public <T extends Subscription> void onSuccess(T subscription, String message) {

              }

              @Override public void onFailure(Subscription subscription, Throwable throwable) {

              }
            });
      }
    });

    findViewById(R.id.door_close_btn).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        // publish, the manager will check the connection is connected or not
        // if not, the manager will automatic connect to server and publish when connected
        connection.publishTopic(new Publishing("door", "close door"),
            new MqttActionListener.OnActionListener() {
              @Override
              public <T extends Subscription> void onSuccess(T subscription, String message) {

              }

              @Override public void onFailure(Subscription subscription, Throwable throwable) {

              }
            });
      }
    });
  }

  @Override protected void onResume() {
    super.onResume();
    MqttManager.getInstance().onResume(CLIENT_ID);
    connect2Server();
    subscribeTopic();
  }

  private void connect2Server() {
    connection = MqttManager.getInstance().getConnection(CLIENT_ID);
    if (connection == null) {
      connection = MqttManager.getInstance()
          .addConnection(Connection.createConnectionWithTimeStamp(getApplicationContext(),
              "tcp://iot.eclipse.org:1883" /* Eclipse Public Server */, CLIENT_ID)
              .addConnectionOptions("test", "1234".toCharArray(), 30, 10, true, false));
    }

    // connection
    connection.connect(new MqttActionListener.OnActionListener() {
      @Override public <T extends Subscription> void onSuccess(T subscription, String message) {

      }

      @Override public void onFailure(Subscription subscription, Throwable throwable) {

      }
    });
  }

  private void subscribeTopic() {
    if (connection == null) {
      // shouldn't happened
      return;
    }

    // subscribe single, the manager will check the connection is connected or not
    // if not, the manager will automatic connect to server and subscribe when connected
    connection.subscribeTopic(new Subscription("door"), new MqttActionListener.OnActionListener() {
      @Override public <T extends Subscription> void onSuccess(T subscription, String message) {
        updateReceivedMessage(message);
      }

      @Override public void onFailure(Subscription subscription, Throwable throwable) {

      }
    }, 0);

    // subscribe multiple, the manager will check the connection is connected or not
    // if not, the manager will automatic connect to server and subscribe when connected
    List<Subscription> subscriptionList = new ArrayList<>();
    subscriptionList.add(new Subscription("window"));
    subscriptionList.add(new Subscription("light"));
    connection.subscribeTopics(subscriptionList, new MqttActionListener.OnActionListener() {
      @Override public <T extends Subscription> void onSuccess(T subscription, String message) {
        updateReceivedMessage(message);
      }

      @Override public void onFailure(Subscription subscription, Throwable throwable) {

      }
    }, 0);
  }

  private void updateReceivedMessage(final String message) {
    runOnUiThread(new Runnable() {
      @Override public void run() {
        messageTV.setText(message);
      }
    });
  }
}
