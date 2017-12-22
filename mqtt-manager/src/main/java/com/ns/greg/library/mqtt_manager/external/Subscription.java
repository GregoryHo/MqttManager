package com.ns.greg.library.mqtt_manager.external;

/**
 * @author Gregory
 * @since 2017/11/13
 */

public interface Subscription extends Qos {

  @Override int getQoS();
}
