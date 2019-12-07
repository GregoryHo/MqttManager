package com.ns.greg.library.mqtt_manager.internal.action

import com.ns.greg.library.mqtt_manager.ConnectionState.CONNECTED
import com.ns.greg.library.mqtt_manager.ConnectionState.CONNECTING
import com.ns.greg.library.mqtt_manager.ConnectionState.DISCONNECTED
import com.ns.greg.library.mqtt_manager.ConnectionState.CLOSING
import com.ns.greg.library.mqtt_manager.external.LoggerLevel.ALL
import com.ns.greg.library.mqtt_manager.external.MqttConnectionListener
import com.ns.greg.library.mqtt_manager.internal.MqttLogger
import com.ns.greg.library.mqtt_manager.internal.client.AsyncClientWrapper
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import java.util.concurrent.CopyOnWriteArrayList

/**
 * @author gregho
 * @since 2019-06-19
 */
internal class ConnectAction @JvmOverloads constructor(
  clientWrapper: AsyncClientWrapper,
  retryCount: Int = 0,
  private val connectOptions: MqttConnectOptions
) : BaseMqttAction(clientWrapper, retryCount) {

  var mqttConnectionListener: MqttConnectionListener? = null
  private val subActions = CopyOnWriteArrayList<TopicAction<*>>()

  /**
   * Add sub action, usually is [SubscribeAction], [UnsubscribeAction], [PublishAction]
   */
  fun addSubAction(subAction: TopicAction<*>) {
    synchronized(ConnectAction::class.java) {
      subActions.add(subAction)
    }
  }

  fun clearSubActions() {
    synchronized(ConnectAction::class.java) {
      subActions.clear()
    }
  }

  private fun executeSubActions() {
    for (subAction in subActions) {
      subAction.execute()
      subActions.remove(subAction)
    }
  }

  private fun notifySubActionsFailure(exception: Throwable?) {
    for (subAction in subActions) {
      subAction.onFailure(null, exception)
      subActions.remove(subAction)
    }
  }

  @Throws(Exception::class)
  override fun execute() {
    with(clientWrapper) {
      if (getState() == DISCONNECTED) {
        try {
          setState(CONNECTING)
          /* debug */
          MqttLogger.log(
              "ConnectAction", "EXECUTE",
              "${client.clientId} connecting to the MQTT broker... (${client.serverURI})",
              level = ALL
          )
          client.connect(connectOptions, null, this@ConnectAction)
        } catch (e: Exception) {
          setState(DISCONNECTED)
          e.printStackTrace()
        }
      }
    }
  }

  override fun onSuccess(asyncActionToken: IMqttToken?) {
    with(clientWrapper) {
      when (getState()) {
        CLOSING -> {
          notifySubActionsFailure(InterruptedException("Closing the connection..."))
        }

        else -> {
          setState(CONNECTED)
          /* debug */
          MqttLogger.log(
              "ConnectAction", "ON_SUCCESS", "${client.clientId} MQTT broker connected", level = ALL
          )
          client.setBufferOpts(DisconnectedBufferOptions().apply {
            isBufferEnabled = true
          })

          mqttConnectionListener?.invoke("${client.clientId} connected", null)
          executeSubActions()
        }
      }
    }
  }

  override fun onFailure(
    asyncActionToken: IMqttToken?,
    exception: Throwable?
  ) {
    clientWrapper.setState(DISCONNECTED)
    /* debug */
    MqttLogger.log(
        "ConnectAction", "ON_FAILURE",
        "${clientWrapper.client.clientId} MQTT broker disconnected, exception: $exception",
        level = ALL
    )
    mqttConnectionListener?.invoke("", exception)
    notifySubActionsFailure(exception)
  }
}