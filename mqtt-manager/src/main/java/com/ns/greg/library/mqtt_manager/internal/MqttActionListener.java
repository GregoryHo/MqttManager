package com.ns.greg.library.mqtt_manager.internal;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.ns.greg.library.mqtt_manager.external.MqttTopic;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;

/**
 * @author Gregory
 * @since 2017/4/12
 */

public class MqttActionListener implements IMqttActionListener {

  // Always retry flag
  public static final int ALWAYS_RETRY = -1;

  // Client status
  static final int CONNECT = 0;
  static final int SUBSCRIBE = 1;
  static final int UN_SUBSCRIBE = 2;
  static final int PUBLISH = 3;

  @IntDef({ CONNECT, SUBSCRIBE, UN_SUBSCRIBE, PUBLISH }) @Retention(RetentionPolicy.SOURCE)
  public @interface Action {

  }

  // Action
  @Action private int action;
  // Mqtt connection
  private Connection connection;
  // Subscription
  @Nullable private MqttTopic topic;
  // Subscriptions
  @Nullable private List<? extends MqttTopic> topics;
  // The callback listener for mqtt action listener
  @Nullable private OnActionListener onActionListener;
  // The sub mqtt action listener
  @Nullable private MqttActionListener subMqttActionListener;
  // The mqtt action retry time
  private int retryTime;

  /**
   * Constructor for [CONNECT] action listener with retry mechanism
   *
   * @param action any action defined in {@link Action}
   * @param connection mqtt connection
   * @param onActionListener action listener
   * @param retryTime retry times for action
   */
  MqttActionListener(@Action int action, @NonNull Connection connection,
      @Nullable OnActionListener onActionListener, int retryTime) {
    this.action = action;
    this.connection = connection;
    this.onActionListener = onActionListener;
    this.retryTime = retryTime;
  }

  /**
   * Constructor for [CONNECT] action listener with retry mechanism
   *
   * @param action any action defined in {@link Action}
   * @param connection mqtt connection
   * @param onActionListener action listener
   */
  MqttActionListener(@Action int action, @NonNull Connection connection,
      @Nullable OnActionListener onActionListener) {
    this(action, connection, onActionListener, 0);
  }

  /**
   * Constructor for [SUBSCRIBE/UN-SUBSCRIBE/PUBLISH] action listener with retry
   * mechanism
   *
   * @param action any action defined in {@link Action}
   * @param connection mqtt connection
   * @param topic topic
   * @param onActionListener action listener
   * @param retryTime retry times for action
   */
  MqttActionListener(@Action int action, @NonNull Connection connection, @NonNull MqttTopic topic,
      @Nullable OnActionListener onActionListener, int retryTime) {
    this.action = action;
    this.connection = connection;
    this.topic = topic;
    this.onActionListener = onActionListener;
    this.retryTime = retryTime;
  }

  /**
   * Constructor for [SUBSCRIBE/UN-SUBSCRIBE/PUBLISH] action listener without retry
   * mechanism
   *
   * @param action any action defined in {@link Action}
   * @param connection mqtt connection
   * @param topic topic
   * @param onActionListener action listener
   */
  MqttActionListener(@Action int action, @NonNull Connection connection, @NonNull MqttTopic topic,
      @Nullable OnActionListener onActionListener) {
    this(action, connection, topic, onActionListener, 0);
  }

  /**
   * Constructor for [SUBSCRIBE/UN-SUBSCRIBE] action listener with retry mechanism
   *
   * @param action any action defined in {@link Action}
   * @param connection mqtt connection
   * @param topics topics
   * @param onActionListener action listener
   * @param retryTime retry times for action
   */
  MqttActionListener(@Action int action, @NonNull Connection connection,
      @NonNull List<? extends MqttTopic> topics, @Nullable OnActionListener onActionListener, int retryTime) {
    this.action = action;
    this.connection = connection;
    this.topics = topics;
    this.onActionListener = onActionListener;
    this.retryTime = retryTime;
  }

  /**
   * Constructor for [SUBSCRIBE/UN-SUBSCRIBE] action listener without retry mechanism
   *
   * @param action any action defined in {@link Action}
   * @param connection mqtt connection
   * @param topics topics
   * @param onActionListener action listener
   */
  MqttActionListener(@Action int action, @NonNull Connection connection,
      @NonNull List<? extends MqttTopic> topics, @Nullable OnActionListener onActionListener) {
    this(action, connection, topics, onActionListener, 0);
  }

  /**
   * Constructor for connect action to wrapper the other action listener
   *
   * @param connection mqtt connection
   * @param subMqttActionListener sub action listener
   */
  MqttActionListener(@NonNull Connection connection,
      @NonNull MqttActionListener subMqttActionListener) {
    this.action = CONNECT;
    this.connection = connection;
    this.subMqttActionListener = subMqttActionListener;
    this.retryTime = subMqttActionListener.getRetryTime();
  }

  @Override public String toString() {
    switch (action) {
      case CONNECT:
        return "Connect";

      case SUBSCRIBE:
        return "Subscribe";

      case UN_SUBSCRIBE:
        return "UnSubscribe";

      case PUBLISH:
        return "Publish";

      default:
        return "Unknown";
    }
  }

  /**
   * Returns current action
   *
   * @return action
   */
  public int getAction() {
    return action;
  }

  /**
   * Returns topic, might be null
   *
   * @return topic
   */
  @Nullable public MqttTopic getTopic() {
    return topic;
  }

  /**
   * Returns topics, might be null
   *
   * @return topic
   */
  @Nullable List<? extends MqttTopic> getTopics() {
    return topics;
  }

  /**
   * Returns sub-MqttActionListener, might be null
   *
   * @return sub action listener
   */
  @Nullable MqttActionListener getSubMqttActionListener() {
    return subMqttActionListener;
  }

  private int getRetryTime() {
    return retryTime;
  }

  int retryTime() {
    if (retryTime == ALWAYS_RETRY) {
      return 777;
    } else if (retryTime > 0) {
      retryTime--;
    }

    return retryTime;
  }

  void clearRetryTime() {
    retryTime = 0;
  }

  @Override public void onSuccess(IMqttToken iMqttToken) {
    switch (action) {
      case CONNECT:
        connectionSuccess();
        break;

      case SUBSCRIBE:
        subscribeSuccess();
        break;

      case UN_SUBSCRIBE:
        unSubscribeSuccess();
        break;

      case PUBLISH:
        publishSuccess();
        break;

      default:
        break;
    }
  }

  /**
   * When connection success
   */
  private void connectionSuccess() {
    connection.connection(this, onActionListener, null);
  }

  /**
   * When onSubscribe success
   */
  private void subscribeSuccess() {
    if (topic != null) {
      connection.subscribe(this, topic, onActionListener, null);
    } else if (topics != null) {
      connection.subscribes(this, topics, onActionListener, null);
    }
  }

  /**
   * When un-onSubscribe success
   */
  private void unSubscribeSuccess() {
    if (topic != null) {
      connection.unSubscribe(topic, onActionListener, null);
    }
  }

  /**
   * When publish success
   */
  private void publishSuccess() {
    if (topic != null) {
      connection.publish(this, topic, onActionListener, null);
    }
  }

  @Override public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
    switch (action) {
      case CONNECT:
        connectionFailure(throwable);
        break;

      case SUBSCRIBE:
        subscribeFailure(throwable);
        break;

      case UN_SUBSCRIBE:
        unSubscribeFailure(throwable);
        break;

      case PUBLISH:
        publishFailure(throwable);
        break;

      default:
        break;
    }
  }

  /**
   * When connection failure
   *
   * @param throwable mqtt exception
   */
  private void connectionFailure(Throwable throwable) {
    connection.connection(this, onActionListener, throwable);
  }

  /**
   * When onSubscribe failure
   *
   * @param throwable mqtt exception
   */
  private void subscribeFailure(Throwable throwable) {
    if (topic != null) {
      connection.subscribe(this, topic, onActionListener, throwable);
    } else if (topics != null) {
      connection.subscribes(this, topics, onActionListener, throwable);
    }
  }

  /**
   * When un-onSubscribe failure
   *
   * @param throwable mqtt exception
   */
  private void unSubscribeFailure(Throwable throwable) {
    if (topic != null) {
      connection.unSubscribe(topic, onActionListener, throwable);
    }
  }

  /**
   * When publish failure
   *
   * @param throwable mqtt exception
   */
  private void publishFailure(Throwable throwable) {
    if (topic != null) {
      connection.publish(this, topic, onActionListener, throwable);
    }
  }
}
