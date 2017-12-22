package com.ns.greg.library.mqtt_manager.external;

import com.ns.greg.library.mqtt_manager.MqttManager;

/**
 * @author Gregory
 * @since 2017/12/22
 */

public interface Qos {

  @MqttManager.QoS int getQoS();
}
