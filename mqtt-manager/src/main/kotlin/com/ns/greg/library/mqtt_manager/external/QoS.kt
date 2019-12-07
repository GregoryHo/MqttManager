package com.ns.greg.library.mqtt_manager.external

/**
 * @author gregho
 * @since 2019-06-20
 */
enum class QoS(val value: Int) {

  AT_MOST_ONCE(0),
  AT_LEAST_ONCE(1),
  EXACTLY_ONCE(2)
}