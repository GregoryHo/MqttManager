package com.ns.greg.library.mqtt_manager.internal;

import android.support.annotation.NonNull;

/**
 * @author Gregory
 * @since 2017/4/11
 */

public class Subscription {

  private static final String SLASH = "/";

  private final String subscriptionTopic;

  private String topic;

  public Subscription(String topic) {
    this.topic = topic;

    subscriptionTopic = getTopicString(topic);
  }

  @Override public String toString() {
    return "Subscription, topic: " + topic;
  }

  @Override public boolean equals(Object o) {
    if (o == this) {
      return true;
    }

    if (!(o instanceof Subscription)) {
      return false;
    }

    Subscription subscription = (Subscription) o;
    return subscription.getSubscriptionTopic().equals(subscriptionTopic);
  }

  public String getTopic() {
    return topic;
  }

  @NonNull private String getTopicString(String topic) {
    return SLASH + topic;
  }

  public String getSubscriptionTopic() {
    return subscriptionTopic;
  }
}
