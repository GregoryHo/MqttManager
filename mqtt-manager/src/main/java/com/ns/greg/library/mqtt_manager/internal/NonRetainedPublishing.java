package com.ns.greg.library.mqtt_manager.internal;

/**
 * @author Gregory
 * @since 2017/11/1
 */

public class NonRetainedPublishing extends Publishing {

  public NonRetainedPublishing(String topic, String publishMessage) {
    super(topic, publishMessage);
  }

  @Override public boolean isRetained() {
    return false;
  }
}
