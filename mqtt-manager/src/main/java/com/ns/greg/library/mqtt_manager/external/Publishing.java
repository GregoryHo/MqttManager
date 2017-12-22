package com.ns.greg.library.mqtt_manager.external;

import com.ns.greg.library.mqtt_manager.MqttManager;

/**
 * @author Gregory
 * @since 2017/11/13
 */

public interface Publishing extends Qos {

  @Override int getQoS();

  @MqttManager.Retained int isRetained();

  String getPublishingMessage();
}
