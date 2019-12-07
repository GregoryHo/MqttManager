package com.ns.greg.library.mqtt_manager.internal.action

import com.ns.greg.library.mqtt_manager.external.LoggerLevel.ALL
import com.ns.greg.library.mqtt_manager.internal.MqttLogger
import com.ns.greg.library.mqtt_manager.external.topic.MqttTopic
import com.ns.greg.library.mqtt_manager.internal.client.AsyncClientWrapper
import org.eclipse.paho.client.mqttv3.IMqttToken

/**
 * @author gregho
 * @since 2019-06-19
 */
internal class UnsubscribeAction<T : MqttTopic> : TopicAction<T> {

  private val mqttTopicList = mutableListOf<T>()
  private val topic: Array<String>

  @JvmOverloads
  constructor(
    clientWrapper: AsyncClientWrapper,
    retries: Int = 0,
    mqttTopic: T
  ) : super(clientWrapper, retries) {
    this.mqttTopicList.add(mqttTopic)
    this.topic = arrayOf(mqttTopic.topic)
  }

  @JvmOverloads
  constructor(
    clientWrapper: AsyncClientWrapper,
    retries: Int = 0,
    mqttTopicList: List<T>
  ) : super(clientWrapper, retries) {
    this.mqttTopicList.addAll(mqttTopicList)
    val size = mqttTopicList.size
    this.topic = Array(size) { "" }
    for (i in 0 until size) {
      val item = this.mqttTopicList[i]
      topic[i] = item.topic
    }
  }

  override fun execute() {
    try {
      clientWrapper.client.unsubscribe(topic, null, this)
      val size = mqttTopicList.size
      for (i in 0 until size) {
        /* debug */
        MqttLogger.log(
            "UnsubscribeAction", "EXECUTE",
            "${clientWrapper.client.clientId} unsubscribing topic... (T: ${topic[i]})",
            level = ALL
        )
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  override fun onSuccess(asyncActionToken: IMqttToken?) {
    for (mqttTopic in mqttTopicList) {
      MqttLogger.log(
          "UnsubscribeAction", "ON_SUCCESS",
          "${clientWrapper.client.clientId} unsubscribed (T: $mqttTopic)",
          level = ALL
      )

      mqttActionListener?.invoke(mqttTopic, byteArrayOf(), null)
    }
  }

  override fun onFailure(
    asyncActionToken: IMqttToken?,
    exception: Throwable?
  ) {
    for (mqttTopic in mqttTopicList) {
      /* debug */
      MqttLogger.log(
          "UnsubscribeAction", "ON_FAILURE",
          "${clientWrapper.client.clientId} unsubscribe failed (T: $mqttTopic), exception: $exception",
          level = ALL
      )
      mqttActionListener?.invoke(mqttTopic, byteArrayOf(), exception)
    }
  }
}