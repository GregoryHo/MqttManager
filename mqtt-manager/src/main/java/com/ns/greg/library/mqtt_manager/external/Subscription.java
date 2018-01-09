package com.ns.greg.library.mqtt_manager.external;

import com.ns.greg.library.mqtt_manager.annotation.QoS;

/**
 * @author Gregory
 * @since 2017/11/13
 */

public interface Subscription {

  @QoS int getSubscriptionQoS();
}
