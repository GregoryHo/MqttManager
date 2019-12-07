package com.ns.greg.library.mqtt_manager.external

/**
 * @author gregho
 * @since 2019-06-28
 */
enum class LoggerLevel(private val value: Int) {

  NONE(0),
  STATUS(1),
  ALL(2);

  fun compare(logLevel: LoggerLevel): Boolean {
    return value >= logLevel.value
  }
}