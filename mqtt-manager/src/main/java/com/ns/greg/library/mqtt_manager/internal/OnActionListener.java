package com.ns.greg.library.mqtt_manager.internal;

import com.ns.greg.library.mqtt_manager.external.MqttTopic;

/**
 * @author Gregory
 * @since 2017/11/13
 */

public interface OnActionListener<T extends MqttTopic> {

  void onSuccess(T mqttTopic, String message);

  void onFailure(T mqttTopic, Throwable throwable);
}
