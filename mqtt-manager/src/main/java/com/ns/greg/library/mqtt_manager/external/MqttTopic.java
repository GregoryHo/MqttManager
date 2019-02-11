package com.ns.greg.library.mqtt_manager.external;

/**
 * @author Gregory
 * @since 2017/11/13
 */

public abstract class MqttTopic {

  private final String mqttTopic;

  public MqttTopic(String mqttTopic) {
    this.mqttTopic = mqttTopic;
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
}
