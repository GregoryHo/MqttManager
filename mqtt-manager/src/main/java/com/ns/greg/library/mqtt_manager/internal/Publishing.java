package com.ns.greg.library.mqtt_manager.internal;

/**
 * @author Gregory
 * @since 2017/4/11
 */

public class Publishing extends Subscription {

  private String publishMessage;

  public Publishing(String topic, String publishMessage) {
    super(topic);
    this.publishMessage = publishMessage;
  }

  public String getPublishMessage() {
    return publishMessage;
  }

  public boolean isRetained() {
    return true;
  }
}
