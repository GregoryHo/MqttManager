package com.ns.greg.library.mqtt_manager.internal.action

import com.ns.greg.library.mqtt_manager.internal.client.AsyncClientWrapper
import org.eclipse.paho.client.mqttv3.IMqttActionListener

/**
 * @author gregho
 * @since 2019-06-19
 */
internal abstract class BaseMqttAction constructor(
  val clientWrapper: AsyncClientWrapper,
  val retries: Int = 0
) : IMqttActionListener {

  private var realRetries = retries

  abstract fun execute()

  @Deprecated("not ready to implement")
  fun getRetryCount(): Int {
    if (retries == -1) {
      return 1
    } else if (realRetries > 0) {
      realRetries--
    }

    return realRetries
  }

  @Deprecated("not ready to implement")
  fun stopRetry() {
    realRetries = 0
  }
}