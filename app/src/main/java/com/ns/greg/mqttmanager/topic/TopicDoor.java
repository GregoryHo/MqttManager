package com.ns.greg.mqttmanager.topic;

import com.ns.greg.library.mqtt_manager.MqttManager;
import com.ns.greg.library.mqtt_manager.external.Publishing;
import com.ns.greg.library.mqtt_manager.external.Subscription;

/**
 * @author Gregory
 * @since 2017/11/14
 */

public class TopicDoor extends DemoTopic implements Subscription, Publishing {

  public TopicDoor(String mqttTopic) {
    super(mqttTopic);
  }

  public TopicDoor(String mqttTopic, String message) {
    super(mqttTopic, message);
  }

  @Override public int getPublishingQoS() {
    return MqttManager.EXACTLY_ONCE;
  }

  @Override public int isRetained() {
    return MqttManager.RETAINED;
  }

  @Override public String getPublishingMessage() {
    return getMessage();
  }

  @Override public int getSubscriptionQoS() {
    return MqttManager.EXACTLY_ONCE;
  }
}
