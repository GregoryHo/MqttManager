package com.ns.greg.library.mqtt_manager.external

/**
 * @author gregho
 * @since 2019-06-20
 */
typealias MqttConnectionListener = (message: String, throwable: Throwable?) -> Unit
typealias MqttActionListener<T> = (topic: T, payload: ByteArray, throwable: Throwable?) -> Unit