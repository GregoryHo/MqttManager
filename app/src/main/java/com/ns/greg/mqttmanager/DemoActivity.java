package com.ns.greg.mqttmanager;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import com.ns.greg.library.mqtt_manager.MqttManager;
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
  private TextView messageTV;
  private TopicWindow topicWindow;
  private TopicDoor topicDoor;
  private TopicLight topicLight;

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_demo);
    createTopic();
    messageTV = findViewById(R.id.message);
    findViewById(R.id.window_open_btn).setOnClickListener(
        v -> connection.publishTopic(topicWindow, null));
    findViewById(R.id.door_close_btn).setOnClickListener(v -> {
      // publish, the manager will check the connection is connected or not
      // if not, the manager will automatic connect to server and publish when connected
      topicDoor.setMessage("close");
      connection.publishTopic(topicDoor, null);
    });
  }

  private void createTopic() {
    topicWindow = new TopicWindow("window");
    topicDoor = new TopicDoor("door", "close");
    topicLight = new TopicLight("light");
  }

  @Override protected void onResume() {
    super.onResume();
    MqttManager.getInstance().onResume(CLIENT_ID);
    connectToServer();
    subscribeTopic();
  }

  private void connectToServer() {
    connection = MqttManager.getInstance().getConnection(CLIENT_ID);
    if (connection == null) {
      connection = Connection.createConnection(getApplicationContext(),
          "iot.eclipse.org" /* Eclipse Public Server */, CLIENT_ID)
          .addConnectionOptions(new Connection.ConnectOptionsBuilder().setUser("test")
              .setPassword("1234")
              .setConnectionTimeout(30)
              .setKeppAliveInterval(10));
      connection = MqttManager.getInstance()
          .addConnection(connection);
    }

    connection.connect(new OnActionListener() {
      @Override public void onSuccess(MqttTopic mqttTopic, String message) {

      }

      @Override public void onFailure(MqttTopic mqttTopic, Throwable throwable) {

      }
    });
  }

  private void subscribeTopic() {
    if (connection == null) {
      // shouldn't happened
      return;
    }

    // subscribe single, the connection will check the state is connected or not
    // if not, it will automatic connect to server and subscribe when connected
    connection.subscribeTopic(topicDoor, new OnActionListener<TopicDoor>() {
      @Override public void onSuccess(TopicDoor mqttTopic, String message) {
        System.out.println(
            "Topic: " + mqttTopic.getTopic() + ", mqttTopic: " + mqttTopic.getMqttTopic());
        updateReceivedMessage(message);
      }

      @Override public void onFailure(TopicDoor mqttTopic, Throwable throwable) {

      }
    }, 0);

    // subscribe multiple, the manager will check the connection is connected or not
    // if not, the manager will automatic connect to server and subscribe when connected
    List<DemoTopic> subscriptionList = new ArrayList<>();
    subscriptionList.add(topicDoor);
    subscriptionList.add(topicLight);
    connection.subscribeTopics(subscriptionList, new OnActionListener<DemoTopic>() {
      @Override public void onSuccess(DemoTopic mqttTopic, String message) {
        System.out.println(
            "Topic: " + mqttTopic.getTopic() + ", mqttTopic: " + mqttTopic.getMqttTopic());
        updateReceivedMessage(message);
      }

      @Override public void onFailure(DemoTopic mqttTopic, Throwable throwable) {

      }
    }, 0);
  }

  private void updateReceivedMessage(final String message) {
    runOnUiThread(() -> messageTV.setText(message));
  }
}
