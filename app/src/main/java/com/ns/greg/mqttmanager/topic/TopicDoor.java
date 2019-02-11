package com.ns.greg.mqttmanager.topic;

import com.ns.greg.library.mqtt_manager.MqttConstants;
import com.ns.greg.library.mqtt_manager.external.Publishing;
import com.ns.greg.library.mqtt_manager.external.Subscription;

/**
 * @author Gregory
 * @since 2017/11/14
 */

public class TopicDoor extends DemoTopic implements Subscription, Publishing {

  public TopicDoor(String topic) {
    super(topic);
  }

  public TopicDoor(String topic, String message) {
    super(topic, message);
  }

  @Override public int getPublishingQoS() {
    return MqttConstants.EXACTLY_ONCE;
  }

  @Override public int isRetained() {
    return MqttConstants.RETAINED;
  }

  @Override public String getPublishingMessage() {
    return getMessage();
  }

  @Override public int getSubscriptionQoS() {
    return MqttConstants.EXACTLY_ONCE;
  }
}
