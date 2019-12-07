package com.ns.greg.library.mqtt_manager.internal.client

import com.ns.greg.library.mqtt_manager.ConnectionState
import com.ns.greg.library.mqtt_manager.ConnectionState.DISCONNECTED
import org.eclipse.paho.client.mqttv3.MqttAsyncClient

/**
 * @author gregho
 * @since 2019-06-19
 */
internal class AsyncClientWrapper(
  val client: MqttAsyncClient
) {
  @Volatile
  private var state = DISCONNECTED

  fun getState(): ConnectionState {
    synchronized(AsyncClientWrapper::class.java) {
      return state
    }
  }

  fun setState(state: ConnectionState) {
    synchronized(AsyncClientWrapper::class.java) {
      this.state = state
    }
  }
}