package com.ns.greg.library.mqtt_manager.internal.action

import com.ns.greg.library.mqtt_manager.external.MqttActionListener
import com.ns.greg.library.mqtt_manager.external.topic.MqttTopic
import com.ns.greg.library.mqtt_manager.internal.client.AsyncClientWrapper

/**
 * @author gregho
 * @since 2019-06-20
 */
internal abstract class TopicAction<T : MqttTopic>(
  clientWrapper: AsyncClientWrapper,
  retries: Int
) : BaseMqttAction(clientWrapper, retries) {

  var mqttActionListener: MqttActionListener<T>? = null
}