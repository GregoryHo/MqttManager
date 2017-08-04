package com.ns.greg.library.mqtt_manager.internal;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;

import static com.ns.greg.library.mqtt_manager.MqttManager.DEBUG;
import static com.ns.greg.library.mqtt_manager.MqttManager.MQTT_TAG;

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

  public interface OnActionListener {

    <T extends Subscription> void onSuccess(T subscription, String message);

    void onFailure(Subscription subscription, Throwable throwable);
  }

  // Action
  @Action private int action;

  // Mqtt connection
  private Connection connection;

  // Subscription
  @Nullable private Subscription subscription;

  // Subscriptions
  @Nullable private List<Subscription> subscriptions;

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
   * Constructor for [SUBSCRIBE/UN-SUBSCRIBE/PUBLISH] subscription action listener with retry
   * mechanism
   *
   * @param action any action defined in {@link Action}
   * @param connection mqtt connection
   * @param subscription subscription
   * @param onActionListener action listener
   * @param retryTime retry times for action
   */
  MqttActionListener(@Action int action, @NonNull Connection connection,
      @NonNull Subscription subscription, @Nullable OnActionListener onActionListener,
      int retryTime) {
    this.action = action;
    this.connection = connection;
    this.subscription = subscription;
    this.onActionListener = onActionListener;
    this.retryTime = retryTime;
  }

  /**
   * Constructor for [SUBSCRIBE/UN-SUBSCRIBE/PUBLISH] subscription action listener without retry
   * mechanism
   *
   * @param action any action defined in {@link Action}
   * @param connection mqtt connection
   * @param subscription subscription
   * @param onActionListener action listener
   */
  MqttActionListener(@Action int action, @NonNull Connection connection,
      @NonNull Subscription subscription, @Nullable OnActionListener onActionListener) {
    this(action, connection, subscription, onActionListener, 0);
  }

  /**
   * Constructor for [SUBSCRIBE/UN-SUBSCRIBE] subscriptions action listener with retry mechanism
   *
   * @param action any action defined in {@link Action}
   * @param connection mqtt connection
   * @param subscriptions subscriptions
   * @param onActionListener action listener
   * @param retryTime retry times for action
   */
  MqttActionListener(@Action int action, @NonNull Connection connection,
      @NonNull List<Subscription> subscriptions, @Nullable OnActionListener onActionListener,
      int retryTime) {
    this.action = action;
    this.connection = connection;
    this.subscriptions = subscriptions;
    this.onActionListener = onActionListener;
    this.retryTime = retryTime;
  }

  /**
   * Constructor for [SUBSCRIBE/UN-SUBSCRIBE] subscriptions action listener without retry mechanism
   *
   * @param action any action defined in {@link Action}
   * @param connection mqtt connection
   * @param subscriptions subscriptions
   * @param onActionListener action listener
   */
  MqttActionListener(@Action int action, @NonNull Connection connection,
      @NonNull List<Subscription> subscriptions, @Nullable OnActionListener onActionListener) {
    this(action, connection, subscriptions, onActionListener, 0);
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

  /**
   * Returns current action
   *
   * @return action
   */
  public int getAction() {
    return action;
  }

  /**
   * Returns subscription, might be null
   *
   * @return subscription
   */
  @Nullable public Subscription getSubscription() {
    return subscription;
  }

  /**
   * Returns subscriptions, might be null
   *
   * @return subscription
   */
  public List<Subscription> getSubscriptions() {
    return subscriptions;
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

  int restRetryTime() {
    if (retryTime == ALWAYS_RETRY) {
      return retryTime;
    } else if (retryTime > 0) {
      retryTime--;
    }

    return retryTime;
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
    if (subscription != null) {
      if (DEBUG) {
        Log.d(MQTT_TAG, "Subscribe topic : " + subscription.getSubscriptionTopic() + " succeeded.");
      }

      connection.subscribe(this, subscription, onActionListener, null);
    } else if (subscriptions != null) {
      if (DEBUG) {
        for (Subscription subscription : subscriptions) {
          Log.d(MQTT_TAG,
              "Subscribe topic : " + subscription.getSubscriptionTopic() + " succeeded.");
        }
      }

      connection.subscribes(this, subscriptions, onActionListener, null);
    }
  }

  /**
   * When un-onSubscribe success
   */
  private void unSubscribeSuccess() {
    if (subscription != null) {
      if (DEBUG) {
        Log.d(MQTT_TAG,
            "UnSubscribe topic : " + subscription.getSubscriptionTopic() + " succeeded.");
      }

      connection.unSubscribe(subscription, onActionListener, null);
    }
  }

  /**
   * When publish success
   */
  private void publishSuccess() {
    if (subscription != null && subscription instanceof Publishing) {
      Publishing publishing = (Publishing) subscription;
      if (DEBUG) {
        Log.d(MQTT_TAG, "Publish topic : "
            + publishing.getSubscriptionTopic()
            + ", message : "
            + publishing.getPublishMessage()
            + " succeeded.");
      }

      connection.publish(publishing, onActionListener, null);
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
    if (DEBUG) {
      Log.d(MQTT_TAG, "Connect to mqtt server failure, " + throwable);
    }

    connection.connection(this, onActionListener, throwable);
  }

  /**
   * When onSubscribe failure
   *
   * @param throwable mqtt exception
   */
  private void subscribeFailure(Throwable throwable) {
    if (subscription != null) {
      if (DEBUG) {
        Log.d(MQTT_TAG,
            "Subscribe topic : " + subscription.getSubscriptionTopic() + " failure, " + throwable);
      }

      connection.subscribe(this, subscription, onActionListener, throwable);
    } else if (subscriptions != null) {
      if (DEBUG) {
        for (Subscription subscription : subscriptions) {
          Log.d(MQTT_TAG,
              "Subscribe topic : " + subscription.getSubscriptionTopic() + " succeeded.");
        }
      }

      connection.subscribes(this, subscriptions, onActionListener, throwable);
    }
  }

  /**
   * When un-onSubscribe failure
   *
   * @param throwable mqtt exception
   */
  private void unSubscribeFailure(Throwable throwable) {
    if (subscription != null) {
      if (DEBUG) {
        Log.d(MQTT_TAG, "UnSubscribe topic : "
            + subscription.getSubscriptionTopic()
            + " failure , "
            + throwable);
      }

      connection.unSubscribe(subscription, onActionListener, throwable);
    }
  }

  /**
   * When publish failure
   *
   * @param throwable mqtt exception
   */
  private void publishFailure(Throwable throwable) {
    if (subscription != null && subscription instanceof Publishing) {
      Publishing publishing = (Publishing) subscription;
      if (DEBUG) {
        Log.d(MQTT_TAG,
            "Publish topic : " + publishing.getSubscriptionTopic() + " failure, " + throwable);
      }

      connection.publish(publishing, onActionListener, throwable);
    }
  }
}
