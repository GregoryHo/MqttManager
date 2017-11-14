package com.ns.greg.mqttmanager.topic;

import com.ns.greg.library.mqtt_manager.external.MqttTopic;

/**
 * @author Gregory
 * @since 2017/11/14
 */

public class DemoTopic extends MqttTopic {

  private String topic;
  private String message;

  public DemoTopic(String topic) {
    this("/DEMO/" + topic, "");
    this.topic = topic;
  }

  public DemoTopic(String mqttTopic, String message) {
    super(mqttTopic);
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
