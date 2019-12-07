package com.ns.greg.library.mqtt_manager.external.topic

/**
 * @author gregho
 * @since 2019-06-19
 */
open class MqttTopic @JvmOverloads constructor(
  val topic: String = ""
) {

  override fun equals(other: Any?): Boolean {
    if (other == this) {
      return true
    }

    if (other !is MqttTopic) {
      return false
    }

    return topic == other.topic
  }

  override fun hashCode(): Int {
    return topic.hashCode()
  }

  override fun toString(): String {
    return topic
  }
}