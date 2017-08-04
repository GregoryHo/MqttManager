package com.ns.greg.library.mqtt_manager.internal;

import android.content.Context;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import com.ns.greg.library.mqtt_manager.MqttManager;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import static com.ns.greg.library.mqtt_manager.MqttManager.DEBUG;
import static com.ns.greg.library.mqtt_manager.MqttManager.MQTT_TAG;

/**
 * <pre>
 *   Support feature:
 *    a. Connect
 *    b. Disconnect
 *    c. Subscribe (Single)
 *    d. Subscribe (Multiple)
 *    e. Un-Subscribe (Single)
 *    f. Publish (Single)
 *
 *   Future feature:
 *    a. Un-Subscribe (Multiple)
 * </pre>
 *
 * @author Gregory
 * @since 2017/4/11
 */

public class Connection implements MqttCallbackExtended {

  public static final int NONE = 0;
  public static final int CONNECTING = 1;
  public static final int CONNECTED = 2;
  public static final int DISCONNECTED = 3;
  public static final int LEAVE = 4;

  @IntDef({ NONE, CONNECTING, CONNECTED, DISCONNECTED, LEAVE }) @Retention(RetentionPolicy.SOURCE)
  public @interface ConnectionStatus {

  }

  private final MqttAndroidClient client;
  private final String serverUri;
  private final String clientId;
  private final List<MqttActionListener> subActions = new ArrayList<>();
  @ConnectionStatus private volatile int status = NONE;
  private MqttConnectOptions mqttConnectOptions;
  // Connection listener to send callback when connection is re-connect or not subscribe anything
  @Nullable private MqttActionListener.OnActionListener connectionListener;
  // Current subscriptionList of connection
  private final List<Subscription> subscriptionList = new ArrayList<>();

  private Connection(MqttAndroidClient client, String serverUri, String clientId) {
    this.client = client;
    this.serverUri = serverUri;
    this.clientId = clientId;
    client.setCallback(this);
  }

  public static Connection createConnection(Context context, String serverUri, String clientId) {
    MqttAndroidClient client = new MqttAndroidClient(context, serverUri, clientId);
    return new Connection(client, serverUri, clientId);
  }

  public static Connection createConnectionWithTimeStamp(Context context, String serverUri,
      String clientId) {
    String id = clientId + MqttManager.getInstance().getTimeStamp(clientId);
    MqttAndroidClient client = new MqttAndroidClient(context, serverUri, id);
    return new Connection(client, serverUri, id);
  }

  @Override public String toString() {
    return "Client ID : " + clientId;
  }

  /**
   * Adds connection options for mqtt
   */
  public Connection addConnectionOptions(String userName, char[] mqttPassword,
      int connectionTimeout, int keepAliveInterval, boolean autoReconnect, boolean cleanSession) {
    mqttConnectOptions = new MqttConnectOptions();
    mqttConnectOptions.setUserName(userName);
    mqttConnectOptions.setPassword(mqttPassword);
    mqttConnectOptions.setConnectionTimeout(connectionTimeout);
    mqttConnectOptions.setKeepAliveInterval(keepAliveInterval);
    mqttConnectOptions.setAutomaticReconnect(autoReconnect);
    mqttConnectOptions.setCleanSession(cleanSession);

    return this;
  }

  /**
   * Return current connection client
   */
  public MqttAndroidClient getClient() {
    return client;
  }

  /**
   * Return current connection server uri
   */
  public String getServerUri() {
    return serverUri;
  }

  /**
   * Return current connection client id
   */
  public String getClientId() {
    return clientId;
  }

  /**
   * Return current connection options
   */
  public MqttConnectOptions getMqttConnectOptions() {
    return mqttConnectOptions;
  }

  /**
   * Return current connection status
   */
  public int getStatus() {
    return status;
  }

  /**
   * Change current connection status {@link ConnectionStatus}
   */
  private void setStatus(@ConnectionStatus int status) {
    this.status = status;
  }

  /**
   * Add action that you want do {@link MqttActionListener.Action}
   */
  public void addAction(MqttActionListener connectionListener) {
    MqttActionListener subMqttActionListener = connectionListener.getSubMqttActionListener();

    if (status == CONNECTED) {
      if (subMqttActionListener != null) {
        doSubAction(subMqttActionListener.getSubscription(),
            subMqttActionListener.getSubscriptions(), subMqttActionListener, null);
      }
    } else {
      if (subMqttActionListener != null) {
        addSubAction(subMqttActionListener);
      }

      if (status == NONE || status == DISCONNECTED) {
        connect(connectionListener);
      }
    }
  }

  /**
   * Queue for sub actions
   */
  private void addSubAction(MqttActionListener subMqttActionListener) {
    synchronized (subActions) {
      subActions.add(subMqttActionListener);
    }
  }

  /**
   * Checks if has sub actions
   *
   * @param onActionListener connection action listener
   * @param throwable mqtt exception
   */
  private void subActions(@Nullable MqttActionListener.OnActionListener onActionListener,
      Throwable throwable) {
    if (onActionListener != null) {
      connectCallback(onActionListener, throwable);
    }

    synchronized (subActions) {
      Iterator<MqttActionListener> iterator = subActions.iterator();
      while (iterator.hasNext()) {
        MqttActionListener subMqttActionListener = iterator.next();
        doSubAction(subMqttActionListener.getSubscription(),
            subMqttActionListener.getSubscriptions(), subMqttActionListener, throwable);
        iterator.remove();
      }
    }
  }

  /**
   * Connection callback
   *
   * @param connectActionListener callback listener
   * @param throwable mqtt exception
   */
  private void connectCallback(@NonNull MqttActionListener.OnActionListener connectActionListener,
      Throwable throwable) {
    if (throwable == null) {
      connectActionListener.onSuccess(null, clientId + " connectCallback to server");
    } else {
      connectActionListener.onFailure(null, throwable);
    }
  }

  /**
   * Do sub-action
   *
   * @param subscription the wrapper for subscription topic
   * @param subscriptions the wrapper list for subscription
   * @param subMqttActionListener sub mqtt action listener
   * @param throwable mqtt exception
   */
  private void doSubAction(Subscription subscription, List<Subscription> subscriptions,
      @NonNull MqttActionListener subMqttActionListener, Throwable throwable) {
    switch (subMqttActionListener.getAction()) {
      case MqttActionListener.SUBSCRIBE:
        if (subscription != null) {
          subscribeTopic(subscription, subMqttActionListener, throwable);
        } else if (subscriptions != null) {
          subscribeTopics(subscriptions, subMqttActionListener, throwable);
        }

        break;

      case MqttActionListener.UN_SUBSCRIBE:
        if (subscription != null) {
          unSubscribeTopic(subscription.getSubscriptionTopic(), subMqttActionListener, throwable);
        }

        break;

      case MqttActionListener.PUBLISH:
        if (subscription != null && subscription instanceof Publishing) {
          Publishing publishing = (Publishing) subscription;
          publishTopic(publishing.getSubscriptionTopic(), publishing.getPublishMessage(),
              subMqttActionListener, null);
        }

        break;

      default:
        break;
    }
  }

  /**
   * Connect to the mqtt server
   *
   * @param onActionListener action callback listener
   */
  public void connect(@Nullable MqttActionListener.OnActionListener onActionListener) {
    connectionListener = onActionListener;

    if (status == CONNECTED) {
      if (onActionListener != null) {
        onActionListener.onSuccess(null, clientId + " is connected");
      }
    } else {
      MqttActionListener connectionListener =
          new MqttActionListener(MqttActionListener.CONNECT, this, onActionListener);
      connect(connectionListener);
    }
  }

  /**
   * Disconnect from the mqtt server
   *
   * [NOTICED] if u disconnected the client, it can't re-connect back us same
   * client id, must create a new connection with new client id
   */
  public void disconnect() {
    setStatus(LEAVE);
    try {
      client.disconnect();
      subscriptionList.clear();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Release the resource of the mqtt service
   */
  public void release() {
    try {
      client.unregisterResources();
      client.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Add the {@link MqttActionListener#SUBSCRIBE} action to try subscribe the topic from MQTT
   * server, this will check the connection status, {@link Connection#addAction(MqttActionListener)}
   *
   * @param subscription the wrapper class for subscription topic
   * @param onActionListener action callback listener
   * @param retryTime retry times
   */
  public void subscribeTopic(@NonNull final Subscription subscription,
      @Nullable final MqttActionListener.OnActionListener onActionListener, int retryTime) {
    // Save the subscribed topic of current connection
    synchronized (subscriptionList) {
      if (!subscriptionList.contains(subscription)) {
        subscriptionList.add(subscription);
      }
    }

    MqttActionListener subscribeListener =
        new MqttActionListener(MqttActionListener.SUBSCRIBE, this, subscription, onActionListener,
            retryTime);
    if (status == CONNECTED) {
      subscribeTopic(subscription, subscribeListener, null);
    } else {
      MqttActionListener connectionListener = new MqttActionListener(this, subscribeListener);
      addAction(connectionListener);
    }
  }

  /**
   * Add the {@link MqttActionListener#SUBSCRIBE} action to try subscribe the topics from MQTT
   * server, this will check the connection status, {@link Connection#addAction(MqttActionListener)}
   *
   * @param subscriptions the wrapper class for subscription topic
   * @param onActionListener action callback listener
   * @param retryTime retry times
   */
  public void subscribeTopics(@NonNull final List<Subscription> subscriptions,
      @Nullable final MqttActionListener.OnActionListener onActionListener, int retryTime) {
    // Save the subscribed topic of current connection
    synchronized (subscriptionList) {
      for (Subscription subscription : subscriptions) {
        if (!subscriptionList.contains(subscription)) {
          subscriptionList.add(subscription);
        }
      }
    }

    MqttActionListener subscribeListener =
        new MqttActionListener(MqttActionListener.SUBSCRIBE, this, subscriptions, onActionListener,
            retryTime);
    if (status == CONNECTED) {
      subscribeTopics(subscriptions, subscribeListener, null);
    } else {
      MqttActionListener connectionListener = new MqttActionListener(this, subscribeListener);
      addAction(connectionListener);
    }
  }

  /**
   * Add the {@link} action to try unSubscribe the topic from MQTT server, this will
   * check the connection status, {@link Connection#addAction(MqttActionListener)}
   *
   * @param unSubscription the wrapper class for unSubscription topic
   * @param onActionListener action callback listener
   */
  public void unSubscribeTopic(@NonNull final Subscription unSubscription,
      @Nullable final MqttActionListener.OnActionListener onActionListener) {
    MqttActionListener unSubscribeListener =
        new MqttActionListener(MqttActionListener.UN_SUBSCRIBE, this, unSubscription,
            onActionListener);
    if (status == CONNECTED) {
      unSubscribeTopic(unSubscription.getSubscriptionTopic(), unSubscribeListener, null);
    } else {
      MqttActionListener connectionListener = new MqttActionListener(this, unSubscribeListener);
      addAction(connectionListener);
    }
  }

  /**
   * Add the publish action to try publish the topic to MQTT server, this will
   * check the connection status, {@link Connection#addAction(MqttActionListener)}
   *
   * @param publishing the wrapper class for unSubscription topic
   * @param onActionListener action callback listener
   */
  public void publishTopic(@NonNull final Publishing publishing,
      @Nullable MqttActionListener.OnActionListener onActionListener) {
    MqttActionListener publishListener =
        new MqttActionListener(MqttActionListener.PUBLISH, this, publishing, onActionListener);
    if (status == CONNECTED) {
      publishTopic(publishing.getSubscriptionTopic(), publishing.getPublishMessage(),
          publishListener, null);
    } else {
      MqttActionListener connectionListener = new MqttActionListener(this, publishListener);
      addAction(connectionListener);
    }
  }

  /**
   * Connect to the server
   *
   * @param connectionListener connection listener
   */
  private void connect(MqttActionListener connectionListener) {
    try {
      setStatus(CONNECTING);
      if (mqttConnectOptions == null) {
        throw new IllegalStateException("ConnectOptions is null");
      }
      client.connect(mqttConnectOptions, null, connectionListener);
    } catch (MqttException e) {
      e.printStackTrace();
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    } catch (NullPointerException e) {
      e.printStackTrace();
    }
  }

  /**
   * Connection callback to MQTT server
   *
   * @param mqttActionListener mqtt action listener
   * @param onActionListener action callback listener
   * @param throwable exception
   */
  void connection(final MqttActionListener mqttActionListener,
      @Nullable final MqttActionListener.OnActionListener onActionListener, Throwable throwable) {
    if (status < CONNECTED) {
      if (throwable == null) {
        DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
        disconnectedBufferOptions.setBufferEnabled(true);
        disconnectedBufferOptions.setBufferSize(100);
        disconnectedBufferOptions.setPersistBuffer(false);
        disconnectedBufferOptions.setDeleteOldestMessages(false);
        try {
          client.setBufferOpts(disconnectedBufferOptions);
        } catch (IllegalArgumentException e) {
          e.printStackTrace();
        } catch (NullPointerException e) {
          e.printStackTrace();
        }

        setStatus(CONNECTED);
        subActions(onActionListener, null);
      } else {
        if (DEBUG) {
          Log.d(MQTT_TAG, clientId + " connect to MQTT server failed");
        }

        if (mqttActionListener.restRetryTime() > 0) {
          new Thread(new Runnable() {
            @Override public void run() {
              try {
                // 500ms delay time for retry
                Thread.sleep(500);

                // Checking status
                while (status != CONNECTED && status < LEAVE) {
                  Thread.sleep(100);
                }
              } catch (InterruptedException e) {
                e.printStackTrace();
              }

              connect(mqttActionListener);
            }
          }).start();
        } else {
          subActions(onActionListener, throwable);
        }
      }
    }
  }

  /**
   * Subscribe topic from MQTT server
   *
   * @param subscription subscription
   * @param mqttActionListener mqtt action listener
   * @param throwable exception
   */
  private void subscribeTopic(@NonNull Subscription subscription,
      MqttActionListener mqttActionListener, Throwable throwable) {
    if (throwable == null && status == CONNECTED) {
      try {
        client.subscribe(subscription.getSubscriptionTopic(), 0, null, mqttActionListener);
      } catch (MqttException e) {
        e.printStackTrace();
      } catch (IllegalArgumentException e) {
        e.printStackTrace();
      } catch (NullPointerException e) {
        e.printStackTrace();
      }
    } else {
      mqttActionListener.onFailure(null, throwable);
    }
  }

  /**
   * [SINGLE] Subscribe message callback from MQTT server
   *
   * @param mqttActionListener mqtt action listener
   * @param subscription subscription
   * @param onActionListener onSubscribe callback listener
   * @param throwable exception
   */
  void subscribe(final MqttActionListener mqttActionListener, final Subscription subscription,
      @Nullable final MqttActionListener.OnActionListener onActionListener,
      final Throwable throwable) {
    if (throwable == null && status == CONNECTED) {
      try {
        client.subscribe(subscription.getSubscriptionTopic(), 0, new IMqttMessageListener() {
          @Override public void messageArrived(String s, final MqttMessage mqttMessage)
              throws Exception {
            String message = new String(mqttMessage.getPayload());
            if (DEBUG) {
              Log.d(MQTT_TAG, "topic : "
                  + subscription.getSubscriptionTopic()
                  + ", message arrived : "
                  + message);
            }

            if (onActionListener != null) {
              onActionListener.onSuccess(subscription, message);
            }
          }
        });
      } catch (MqttException e) {
        e.printStackTrace();
      } catch (IllegalArgumentException e) {
        e.printStackTrace();
      } catch (NullPointerException e) {
        e.printStackTrace();
      }
    } else {
      if (DEBUG) {
        Log.d(MQTT_TAG,
            clientId + " onSubscribe " + subscription.getSubscriptionTopic() + " failed");
      }

      if (mqttActionListener.restRetryTime() > 0) {
        new Thread(new Runnable() {
          @Override public void run() {
            try {
              // 500ms delay time for retry
              Thread.sleep(500);

              // Checking status
              while (status != CONNECTED && status < LEAVE) {
                Thread.sleep(100);
              }
            } catch (InterruptedException e) {
              e.printStackTrace();
            }

            subscribeTopic(subscription, mqttActionListener, throwable);
          }
        }).start();
      } else if (onActionListener != null) {
        onActionListener.onFailure(subscription, throwable);
      }
    }
  }

  private void subscribeTopics(@NonNull List<Subscription> subscriptions,
      MqttActionListener mqttActionListener, Throwable throwable) {
    if (throwable == null && status == CONNECTED) {
      try {
        int size = subscriptions.size();
        String[] topics = new String[size];
        int[] qos = new int[size];
        for (int i = 0; i < size; i++) {
          topics[i] = subscriptions.get(i).getSubscriptionTopic();
          qos[i] = 0;
        }

        client.subscribe(topics, qos, null, mqttActionListener);
      } catch (MqttException e) {
        e.printStackTrace();
      } catch (IllegalArgumentException e) {
        e.printStackTrace();
      } catch (NullPointerException e) {
        e.printStackTrace();
      }
    } else {
      mqttActionListener.onFailure(null, throwable);
    }
  }

  /**
   * [MULTIPLE] Subscribe message callback from MQTT server
   *
   * @param mqttActionListener mqtt action listener
   * @param subscriptions subscriptions
   * @param onActionListener onSubscribe callback listener
   * @param throwable exception
   */
  void subscribes(final MqttActionListener mqttActionListener,
      final List<Subscription> subscriptions,
      @Nullable final MqttActionListener.OnActionListener onActionListener,
      final Throwable throwable) {
    if (throwable == null && status == CONNECTED) {
      try {
        int size = subscriptions.size();
        String[] topics = new String[size];
        int[] qos = new int[size];
        IMqttMessageListener[] listeners = new IMqttMessageListener[size];

        for (int i = 0; i < size; i++) {
          final Subscription subscription = subscriptions.get(i);
          topics[i] = subscription.getSubscriptionTopic();
          qos[i] = 0;
          listeners[i] = new IMqttMessageListener() {
            @Override public void messageArrived(String s, MqttMessage mqttMessage)
                throws Exception {
              String message = new String(mqttMessage.getPayload());
              if (DEBUG) {
                Log.d(MQTT_TAG, "Topic: "
                    + subscription.getSubscriptionTopic()
                    + ", message arrived: "
                    + message);
              }

              if (onActionListener != null) {
                onActionListener.onSuccess(subscription, message);
              }
            }
          };
        }

        client.subscribe(topics, qos, listeners);
      } catch (MqttException e) {
        e.printStackTrace();
      } catch (IllegalArgumentException e) {
        e.printStackTrace();
      } catch (NullPointerException e) {
        e.printStackTrace();
      }
    } else {
      if (DEBUG) {
        Log.d(MQTT_TAG, clientId + " onSubscribe multiple failed");
      }

      if (mqttActionListener.restRetryTime() > 0) {
        new Thread(new Runnable() {
          @Override public void run() {
            try {
              // 500ms delay time for retry
              Thread.sleep(500);

              // Checking status
              while (status != CONNECTED && status < LEAVE) {
                Thread.sleep(100);
              }
            } catch (InterruptedException e) {
              e.printStackTrace();
            }

            subscribeTopics(subscriptions, mqttActionListener, throwable);
          }
        }).start();
      } else if (onActionListener != null) {
        for (Subscription subscription : subscriptions) {
          onActionListener.onFailure(subscription, throwable);
        }
      }
    }
  }

  /**
   * Un-onSubscribe topic form MQTT server
   *
   * @param unSubscriptionTopic un-subscription topic
   * @param mqttActionListener mqtt action listener
   * @param throwable exception
   */
  private void unSubscribeTopic(@NonNull String unSubscriptionTopic,
      MqttActionListener mqttActionListener, Throwable throwable) {
    if (throwable == null && status == CONNECTED) {
      try {
        client.unsubscribe(unSubscriptionTopic, null, mqttActionListener);
      } catch (MqttException e) {
        e.printStackTrace();
      } catch (IllegalArgumentException e) {
        e.printStackTrace();
      } catch (NullPointerException e) {
        e.printStackTrace();
      }
    } else {
      mqttActionListener.onFailure(null, throwable);
    }
  }

  /**
   * Un-onSubscribe callback from MQTT server
   *
   * @param unSubscriptionTopic un-subscription topic
   * @param onActionListener action callback listener
   * @param throwable exception
   */
  void unSubscribe(Subscription unSubscriptionTopic,
      @Nullable MqttActionListener.OnActionListener onActionListener, Throwable throwable) {
    if (onActionListener != null) {
      if (throwable == null && status == CONNECTED) {
        onActionListener.onSuccess(unSubscriptionTopic, null);
      } else {
        onActionListener.onFailure(unSubscriptionTopic, throwable);
      }
    }
  }

  /**
   * Publish topic to MQTT server
   *
   * @param publishTopic publish topic
   * @param publishMessage publish message
   * @param mqttActionListener mqtt action listener
   * @param throwable exception
   */
  private void publishTopic(@NonNull String publishTopic, String publishMessage,
      MqttActionListener mqttActionListener, Throwable throwable) {
    if (throwable == null && status == CONNECTED) {
      try {
        client.publish(publishTopic, publishMessage.getBytes(), 0, true, null, mqttActionListener);
      } catch (MqttException e) {
        e.printStackTrace();
      } catch (IllegalArgumentException e) {
        e.printStackTrace();
      } catch (NullPointerException e) {
        e.printStackTrace();
      }
    } else {
      mqttActionListener.onFailure(null, throwable);
    }
  }

  /**
   * Publish callback from MQTT server
   *
   * @param publishing publishing
   * @param onActionListener action callback listener
   * @param throwable exception
   */
  void publish(Publishing publishing,
      @Nullable MqttActionListener.OnActionListener onActionListener, Throwable throwable) {
    if (onActionListener != null) {
      if (throwable == null && status == CONNECTED) {
        onActionListener.onSuccess(publishing, publishing.getPublishMessage());
      } else {
        onActionListener.onFailure(publishing, throwable);
      }
    }
  }

  @Override public void connectComplete(boolean isReconnect, String s) {
    if (DEBUG) {
      Log.d(MQTT_TAG, clientId + " connection complete, isReconnect = " + isReconnect);
    }

    if (connectionListener != null) {
      if (isReconnect || subscriptionList.isEmpty()) {
        connectionListener.onSuccess(null, TopicMessage.RECONNECT);
      }
    }

    setStatus(CONNECTED);
  }

  @Override public void connectionLost(Throwable throwable) {
    if (DEBUG) {
      Log.d(MQTT_TAG, clientId + " connection lost, throwable = " + throwable);
    }

    if (status != LEAVE) {
      setStatus(DISCONNECTED);
    } else {
      release();
    }
  }

  @Override public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {

  }

  @Override public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

  }
}
