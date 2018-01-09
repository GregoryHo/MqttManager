package com.ns.greg.library.mqtt_manager;

/**
 * @author Gregory
 * @since 2018/1/9
 */

public class MqttConstants {

  // [QOS]
  public static final int AT_MOST_ONCE = 0;
  public static final int AT_LEAST_ONCE = 1;
  public static final int EXACTLY_ONCE = 2;

  // [TOPIC TYPE]
  public static final int UN_RETAINED = 0;
  public static final int RETAINED = 1;
}
