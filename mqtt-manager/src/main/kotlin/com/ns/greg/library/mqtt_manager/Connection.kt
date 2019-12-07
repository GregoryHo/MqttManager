package com.ns.greg.library.mqtt_manager

import com.ns.greg.library.mqtt_manager.ConnectionState.CONNECTED
import com.ns.greg.library.mqtt_manager.ConnectionState.DISCONNECTED
import com.ns.greg.library.mqtt_manager.ConnectionState.CLOSED
import com.ns.greg.library.mqtt_manager.ConnectionState.CLOSING
import com.ns.greg.library.mqtt_manager.external.LoggerLevel
import com.ns.greg.library.mqtt_manager.external.LoggerLevel.ALL
import com.ns.greg.library.mqtt_manager.external.LoggerLevel.STATUS
import com.ns.greg.library.mqtt_manager.external.MqttActionListener
import com.ns.greg.library.mqtt_manager.external.MqttConnectionListener
import com.ns.greg.library.mqtt_manager.external.topic.MqttTopic
import com.ns.greg.library.mqtt_manager.internal.MqttLogger
import com.ns.greg.library.mqtt_manager.internal.action.ConnectAction
import com.ns.greg.library.mqtt_manager.internal.action.PublishAction
import com.ns.greg.library.mqtt_manager.internal.action.SubscribeAction
import com.ns.greg.library.mqtt_manager.internal.action.TopicAction
import com.ns.greg.library.mqtt_manager.internal.action.UnsubscribeAction
import com.ns.greg.library.mqtt_manager.internal.client.AsyncClientWrapper
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttClientPersistence
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence

/**
 * @author gregho
 * @since 2019-06-19
 */

/**
 * Support feature:
 * a. Connect
 * b. Subscribe (Single)
 * c. Subscribe (Multiple)
 * d. Unsubscribe (Single)
 * e. Unsubscribe (Multiple)
 * f. Publish (Single)
 * h. Disconnect
 */
class Connection private constructor(
  connectionOptions: MqttConnectOptions,
  private val clientWrapper: AsyncClientWrapper
) : MqttCallbackExtended {

  companion object Constants {

    private const val TCP = "tcp://"
    private const val SSL = "ssl://"

    @JvmStatic
    fun createConnection(
      mqttConnectOptions: MqttConnectOptions,
      uri: String,
      clientId: String,
      mqttClientPersistence: MqttClientPersistence = MqttDefaultFilePersistence()
    ): Connection {
      return Connection(
          mqttConnectOptions,
          AsyncClientWrapper(MqttAsyncClient(uri, clientId, mqttClientPersistence))
      )
    }
  }

  private val connectAction = ConnectAction(clientWrapper, 0, connectionOptions)
  private var connectionListener: MqttConnectionListener? = null

  init {
    clientWrapper.client.setCallback(this)
  }

  fun setLogLevel(level: LoggerLevel) {
    MqttLogger.setLevel(level)
  }

  fun getState(): ConnectionState {
    return clientWrapper.getState()
  }

  fun connect(listener: MqttConnectionListener? = null) {
    this.connectionListener = listener
    when (getState()) {
      CONNECTED -> {
        connectionListener?.invoke("", null)
      }

      CLOSING, CLOSED -> {
        /* ignored, or throw exception? */
      }

      else -> {
        connectAction.apply {
          mqttConnectionListener = listener
        }
            .execute()
      }
    }
  }

  fun <T : MqttTopic> subscribe(
    topic: T,
    listener: MqttActionListener<T>? = null,
    retries: Int = 0
  ) {
    val subscribeAction = SubscribeAction(clientWrapper, retries, topic).apply {
      mqttActionListener = listener
    }
    when (getState()) {
      CONNECTED -> {
        subscribeAction.execute()
      }

      else -> {
        connectAction.wrapSubAction(subscribeAction)
      }
    }
  }

  fun <T : MqttTopic> subscribe(
    topicList: List<T>,
    listener: MqttActionListener<T>? = null,
    retries: Int = 0
  ) {
    val subscribeAction = SubscribeAction(clientWrapper, retries, topicList).apply {
      mqttActionListener = listener
    }
    when (getState()) {
      CONNECTED -> {
        subscribeAction.execute()
      }

      else -> {
        connectAction.wrapSubAction(subscribeAction)
      }
    }
  }

  fun <T : MqttTopic> unsubscribe(
    topic: T,
    listener: MqttActionListener<T>? = null,
    retries: Int = 0
  ) {
    val unsubscribeAction = UnsubscribeAction(clientWrapper, retries, topic).apply {
      mqttActionListener = listener
    }
    when (getState()) {
      CONNECTED -> {
        unsubscribeAction.execute()
      }

      else -> {
        connectAction.wrapSubAction(unsubscribeAction)
      }
    }
  }

  fun <T : MqttTopic> unsubscribe(
    topicList: List<T>,
    listener: MqttActionListener<T>? = null,
    retries: Int = 0
  ) {
    val unsubscribeAction = UnsubscribeAction(clientWrapper, retries, topicList).apply {
      mqttActionListener = listener
    }
    when (getState()) {
      CONNECTED -> {
        unsubscribeAction.execute()
      }

      else -> {
        connectAction.wrapSubAction(unsubscribeAction)
      }
    }
  }

  fun <T : MqttTopic> publish(
    topic: T,
    listener: MqttActionListener<T>? = null,
    retries: Int = 0
  ) {
    val publishAction = PublishAction(clientWrapper, retries, topic).apply {
      mqttActionListener = listener
    }
    when (getState()) {
      CONNECTED -> {
        publishAction.execute()
      }

      else -> {
        connectAction.wrapSubAction(publishAction)
      }
    }
  }

  fun disconnect() {
    when (getState()) {
      CONNECTED, CLOSING -> {
        try {
          connectionListener = null
          clientWrapper.client.disconnect()
          MqttLogger.log("Connection", "DISCONNECT", level = ALL)
          close()
        } catch (e: Exception) {
          e.printStackTrace()
        }
      }
      else -> {
        /* ignored */
      }
    }

    if (getState() != CLOSED) {
      MqttLogger.log("Connection", "CLOSING", level = ALL)
      setState(CLOSING)
    }
  }

  /*--------------------------------
   * Private function
   *-------------------------------*/

  private fun setState(state: ConnectionState) {
    clientWrapper.setState(state)
  }

  private fun close() {
    MqttLogger.log("Connection", "CLOSE", level = STATUS)
    setState(CLOSED)
    try {
      connectAction.clearSubActions()
      clientWrapper.client.close()
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  private fun ConnectAction.wrapSubAction(subAction: TopicAction<*>) {
    addSubAction(subAction)
    execute()
  }

  override fun connectComplete(
    reconnect: Boolean,
    serverURI: String?
  ) {
    MqttLogger.log("Connection", "CONNECT_COMPLETE, reconnect: $reconnect", level = STATUS)
    when (getState()) {
      CLOSING -> disconnect()

      CLOSED -> {
        /* ignored */
      }

      else -> {
        if (reconnect) {
          setState(CONNECTED)
          connectionListener?.invoke("", null)
        }
      }
    }
  }

  override fun messageArrived(
    topic: String?,
    message: MqttMessage?
  ) {
    /* ignored */
  }

  override fun connectionLost(cause: Throwable?) {
    MqttLogger.log("Connection", "CONNECTION_LOST, cause: $cause", level = STATUS)
    when (getState()) {
      CLOSING, CLOSED -> {
        /* ignored */
      }

      else -> {
        setState(DISCONNECTED)
      }
    }
  }

  override fun deliveryComplete(token: IMqttDeliveryToken?) {
    /* ignored */
  }
}