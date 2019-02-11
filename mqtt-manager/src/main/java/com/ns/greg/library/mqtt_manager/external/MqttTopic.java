package com.ns.greg.library.mqtt_manager.external;

/**
 * @author Gregory
 * @since 2017/11/13
 */

public abstract class MqttTopic {

  private final String mqttTopic;
  private String message;

  public MqttTopic(String mqttTopic, String message) {
    this.mqttTopic = mqttTopic;
    this.message = message;
  }

  @Override public String toString() {
    return "Topic: " + mqttTopic;
  }

  @Override public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }

    if (!(obj instanceof MqttTopic)) {
      return false;
    }

    MqttTopic target = (MqttTopic) obj;
    return mqttTopic.equals(target.mqttTopic);
  }

  public String getMqttTopic() {
    return mqttTopic;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }
}
