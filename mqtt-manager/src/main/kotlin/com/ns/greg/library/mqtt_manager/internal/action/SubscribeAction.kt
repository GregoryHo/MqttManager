package com.ns.greg.library.mqtt_manager.internal.action

import com.ns.greg.library.mqtt_manager.external.LoggerLevel.ALL
import com.ns.greg.library.mqtt_manager.internal.MqttLogger
import com.ns.greg.library.mqtt_manager.external.topic.MqttTopic
import com.ns.greg.library.mqtt_manager.external.topic.Subscription
import com.ns.greg.library.mqtt_manager.internal.client.AsyncClientWrapper
import org.eclipse.paho.client.mqttv3.IMqttMessageListener
import org.eclipse.paho.client.mqttv3.IMqttToken

/**
 * @author gregho
 * @since 2019-06-19
 */
internal class SubscribeAction<T : MqttTopic> : TopicAction<T> {

  private val mqttTopicList = mutableListOf<T>()
  private val topic: Array<String>
  private val qos: IntArray
  private val messageListener: Array<IMqttMessageListener?>

  @JvmOverloads
  constructor(
    clientWrapper: AsyncClientWrapper,
    retries: Int = 0,
    mqttTopic: T
  ) : super(clientWrapper, retries) {
    this.mqttTopicList.add(mqttTopic)
    this.topic = arrayOf(mqttTopic.topic)
    this.qos = intArrayOf((mqttTopic as Subscription).getSubscriptionQos().value)
    this.messageListener = arrayOf(IMqttMessageListener { topic, message ->
      /* debug */
      MqttLogger.log(
          "SubscribeAction", "ON_MESSAGE",
          "${clientWrapper.client.clientId} subscription received (T: $topic, M: $message)",
          level = ALL
      )
      mqttActionListener?.invoke(mqttTopic, message?.payload ?: byteArrayOf(), null)
    })
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
    this.qos = IntArray(size)
    this.messageListener = Array(size) { null }
    for (i in 0 until size) {
      val item = this.mqttTopicList[i]
      topic[i] = item.topic
      qos[i] = (item as Subscription).getSubscriptionQos()
          .value
      messageListener[i] = IMqttMessageListener { topic, message ->
        /* debug */
        MqttLogger.log(
            "SubscribeAction", "ON_MESSAGE",
            "${clientWrapper.client.clientId} subscription received (T: $topic, M: $message)",
            level = ALL
        )
        mqttActionListener?.invoke(item, message?.payload ?: byteArrayOf(), null)
      }
    }
  }

  override fun execute() {
    try {
      clientWrapper.client.subscribe(topic, qos, null, this)
      val size = mqttTopicList.size
      for (i in 0 until size) {
        /* debug */
        MqttLogger.log(
            "SubscribeAction", "EXECUTE",
            "${clientWrapper.client.clientId} subscribing topic... (T: ${topic[i]}, Q: ${qos[i]})",
            level = ALL
        )
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  override fun onSuccess(asyncActionToken: IMqttToken?) {
    clientWrapper.client.subscribe(topic, qos, messageListener)
    val size = mqttTopicList.size
    for (i in 0 until size) {
      /* debug */
      MqttLogger.log(
          "SubscribeAction", "EXECUTE",
          "${clientWrapper.client.clientId} subscribed (T: ${topic[i]}, Q: ${qos[i]})",
          level = ALL
      )
    }
  }

  override fun onFailure(
    asyncActionToken: IMqttToken?,
    exception: Throwable?
  ) {
    mqttActionListener?.run {
      val size = mqttTopicList.size
      for (i in 0 until size) {
        /* debug */
        MqttLogger.log(
            "SubscribeAction", "ON_FAILURE",
            "${clientWrapper.client.clientId} subscribe failed (T: ${topic[i]}, Q: ${qos[i]}), exception: $exception",
            level = ALL
        )
        invoke(mqttTopicList[i], byteArrayOf(), exception)
      }
    }
  }
}