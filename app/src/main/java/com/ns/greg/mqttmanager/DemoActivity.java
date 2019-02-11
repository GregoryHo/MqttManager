package com.ns.greg.mqttmanager;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import com.ns.greg.library.mqtt_manager.external.MqttTopic;
import com.ns.greg.library.mqtt_manager.internal.Connection;
import com.ns.greg.library.mqtt_manager.internal.OnActionListener;
import com.ns.greg.mqttmanager.topic.DemoTopic;
import com.ns.greg.mqttmanager.topic.TopicDoor;
import com.ns.greg.mqttmanager.topic.TopicLight;
import com.ns.greg.mqttmanager.topic.TopicWindow;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Gregory
 * @since 2017/8/4
 */

public class DemoActivity extends AppCompatActivity {

  private static final String TAG = "DemoActivity";
  private static final String CLIENT_ID = "DEMO";

  private Connection connection;
  private TextView stateTv;
  private TextView topicTv;
  private TextView messageTv;
  private final TopicLight topicLight = new TopicLight();
  private final TopicWindow topicWindow = new TopicWindow();
  private final TopicDoor topicDoor = new TopicDoor();

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_demo);
    stateTv = findViewById(R.id.state_tv);
    topicTv = findViewById(R.id.topic_tv);
    messageTv = findViewById(R.id.message_tv);
    registerClickListener();
  }

  private void registerClickListener() {
    /* connect */
    findViewById(R.id.connect_btn).setOnClickListener(v -> createConnection());
    /* disconnect */
    findViewById(R.id.disconnect_btn).setOnClickListener(v -> closeConnection());
    /* open/close light */
    findViewById(R.id.light_open_btn).setOnClickListener(v -> {
      topicLight.setMessage("open");
      connection.publishTopic(topicLight, null);
    });
    findViewById(R.id.light_close_btn).setOnClickListener(v -> {
      topicLight.setMessage("close");
      connection.publishTopic(topicLight, null);
    });
    /* open/close window */
    findViewById(R.id.window_open_btn).setOnClickListener(v -> {
      topicWindow.setMessage("open");
      connection.publishTopic(topicWindow, null);
    });
    findViewById(R.id.window_close_btn).setOnClickListener(v -> {
      topicWindow.setMessage("close");
      connection.publishTopic(topicWindow, null);
    });
    /* open/close door */
    findViewById(R.id.door_open_btn).setOnClickListener(v -> {
      topicDoor.setMessage("open");
      connection.publishTopic(topicDoor, null);
    });
    findViewById(R.id.door_close_btn).setOnClickListener(v -> {
      topicDoor.setMessage("close");
      connection.publishTopic(topicDoor, null);
    });
  }

  private void createConnection() {
    if (connection == null) {
      connection = Connection.createConnection(getApplicationContext(),
          "iot.eclipse.org" /* Eclipse Public Server */, CLIENT_ID)
          .addConnectionOptions(new Connection.ConnectOptionsBuilder().setUser("test")
              .setPassword("1234")
              .setConnectionTimeout(30)
              .setKeppAliveInterval(10));
    }

    connection.connect(new OnActionListener() {
      @Override public void onSuccess(MqttTopic topic, String message) {
        subscribeTopics();
        runOnUiThread(() -> stateTv.setText("connected"));
      }

      @Override public void onFailure(MqttTopic topic, Throwable throwable) {
        runOnUiThread(() -> stateTv.setText("disconnected"));
      }
    });
  }

  private void closeConnection() {
    if (connection == null) {
      return;
    }

    connection.disconnect();
    connection = null;
    stateTv.setText("disconnected");
  }

  private void subscribeTopics() {
    // subscribe multiple, the manager will check the connection is connected or not
    // if not, the manager will automatic connect to server and subscribe when connected
    List<DemoTopic> subscriptionList = new ArrayList<>();
    subscriptionList.add(topicLight);
    subscriptionList.add(topicWindow);
    subscriptionList.add(topicDoor);
    connection.subscribeTopics(subscriptionList, new OnActionListener<DemoTopic>() {
      @Override public void onSuccess(DemoTopic topic, String message) {
        updateReceivedTopic(topic.getMqttTopic());
        updateReceivedMessage(message);
      }

      @Override public void onFailure(DemoTopic topic, Throwable throwable) {

      }
    });
  }

  private void updateReceivedTopic(String mqttTopic) {
    runOnUiThread(() -> topicTv.setText(mqttTopic));
  }

  private void updateReceivedMessage(final String message) {
    runOnUiThread(() -> messageTv.setText(message));
  }
}
