package com.ns.greg.library.mqtt_manager

/**
 * @author gregho
 * @since 2019-06-19
 */
enum class ConnectionState {

  /*INIT,*/
  CONNECTING,
  CONNECTED,
  DISCONNECTED,
  CLOSING,
  CLOSED
}