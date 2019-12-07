package com.ns.greg.library.mqtt_manager.external.topic

import com.ns.greg.library.mqtt_manager.external.QoS

/**
 * @author gregho
 * @since 2019-06-19
 */
interface Subscription {

  fun getSubscriptionQos(): QoS
}