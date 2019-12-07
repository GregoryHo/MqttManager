package com.ns.greg.library.mqtt_manager.internal

import com.ns.greg.library.mqtt_manager.external.LoggerLevel
import java.util.logging.Level
import java.util.logging.Logger

/**
 * @author gregho
 * @since 2019-06-20
 */
internal object MqttLogger {

  private const val LEFT_BRACKET = "["
  private const val RIGHT_BRACKET = "]"
  private const val SIGN = "-->"
  private const val LINK = "└--"
  private val logger = Logger.getLogger(MqttLogger::class.java.simpleName)
      .apply {
        level = Level.ALL
      }
  private var logLevel = LoggerLevel.NONE

  @JvmStatic
  fun setLevel(level: LoggerLevel) {
    this.logLevel = level
  }

  @JvmStatic
  @JvmOverloads
  fun log(
    className: String = "",
    functionName: String = "",
    message: String = "",
    level: LoggerLevel
  ) {
    if (logLevel.compare(level)) {
      if (className.isNotEmpty()) {
        logger.info("$LEFT_BRACKET$className$RIGHT_BRACKET")
      }

      if (functionName.isNotEmpty()) {
        logger.info("$SIGN $functionName")
      }

      if (message.isNotEmpty()) {
        logger.info("$LINK $message")
      }
    }
  }
}