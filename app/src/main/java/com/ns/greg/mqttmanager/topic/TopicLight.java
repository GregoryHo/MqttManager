package com.ns.greg.mqttmanager.topic;

import com.ns.greg.library.mqtt_manager.MqttManager;
import com.ns.greg.library.mqtt_manager.external.Subscription;

/**
 * @author Gregory
 * @since 2017/11/14
 */

public class TopicLight extends DemoTopic implements Subscription {

  public TopicLight(String mqttTopic) {
    super(mqttTopic);
  }

  @Override public int getSubscriptionQoS() {
    return MqttManager.AT_LEAST_ONCE;
  }
}
