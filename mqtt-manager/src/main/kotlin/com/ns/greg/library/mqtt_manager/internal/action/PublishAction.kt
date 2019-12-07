package com.ns.greg.library.mqtt_manager.internal.action

import com.ns.greg.library.mqtt_manager.external.LoggerLevel.ALL
import com.ns.greg.library.mqtt_manager.internal.MqttLogger
import com.ns.greg.library.mqtt_manager.external.topic.MqttTopic
import com.ns.greg.library.mqtt_manager.external.topic.Publication
import com.ns.greg.library.mqtt_manager.internal.client.AsyncClientWrapper
import org.eclipse.paho.client.mqttv3.IMqttToken

/**
 * @author gregho
 * @since 2019-06-19
 */
internal class PublishAction<T : MqttTopic>(
  clientWrapper: AsyncClientWrapper,
  retries: Int = 0,
  private val mqttTopic: T
) : TopicAction<T>(clientWrapper, retries) {

  private val publication = mqttTopic as Publication

  override fun execute() {
    /* debug */
    MqttLogger.log(
        "PublishAction", "EXECUTE",
        "${clientWrapper.client.clientId} publishing topic... (T: $mqttTopic, M: ${publication.getPayload()}, Q: ${publication.getPublishingQos()}, R: ${publication.isRetained()})",
        level = ALL
    )
    clientWrapper.client.publish(
        mqttTopic.topic, publication.getPayload().toByteArray(),
        publication.getPublishingQos().value,
        publication.isRetained(), null, this
    )
  }

  override fun onSuccess(asyncActionToken: IMqttToken?) {
    /* debug */
    MqttLogger.log(
        "PublishAction", "ON_SUCCESS",
        "${clientWrapper.client.clientId} publication published (T: $mqttTopic, M: ${publication.getPayload()}, Q: ${publication.getPublishingQos()}, R: ${publication.isRetained()})",
        level = ALL
    )
    mqttActionListener?.invoke(mqttTopic, publication.getPayload().toByteArray(), null)
  }

  override fun onFailure(
    asyncActionToken: IMqttToken?,
    exception: Throwable?
  ) {
    /* debug */
    MqttLogger.log(
        "PublishAction", "ON_FAILURE",
        "${clientWrapper.client.clientId} $mqttTopic publish failed (T: $mqttTopic, M: ${publication.getPayload()}, Q: ${publication.getPublishingQos()}, R: ${publication.isRetained()}), exception: $exception",
        level = ALL
    )
    mqttActionListener?.invoke(mqttTopic, byteArrayOf(), exception)
  }
}