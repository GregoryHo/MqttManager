package com.ns.greg.mqttmanager.topic;

import com.ns.greg.library.mqtt_manager.external.MqttTopic;

/**
 * @author Gregory
 * @since 2017/11/14
 */

public class DemoTopic extends MqttTopic {

  private final String topic;
  private String message;

  public DemoTopic(String topic) {
    this(topic, "");
  }

  public DemoTopic(String topic, String message) {
    super("/DEMO/" + topic);
    this.topic = topic;
    this.message = message;
  }

  public String getTopic() {
    return topic;

  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }
}
