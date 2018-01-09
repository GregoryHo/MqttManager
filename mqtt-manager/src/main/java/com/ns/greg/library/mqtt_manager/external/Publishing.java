package com.ns.greg.library.mqtt_manager.external;

import com.ns.greg.library.mqtt_manager.annotation.Retained;

/**
 * @author Gregory
 * @since 2017/11/13
 */

public interface Publishing {

  int getPublishingQoS();

  @Retained int isRetained();

  String getPublishingMessage();
}
