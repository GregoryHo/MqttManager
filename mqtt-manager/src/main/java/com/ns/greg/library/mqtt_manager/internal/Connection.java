package com.ns.greg.library.mqtt_manager.internal;

import android.content.Context;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import com.ns.greg.library.mqtt_manager.MqttManager;
import com.ns.greg.library.mqtt_manager.external.MqttTopic;
import com.ns.greg.library.mqtt_manager.external.Publishing;
import com.ns.greg.library.mqtt_manager.external.Subscription;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
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
import static com.ns.greg.library.mqtt_manager.MqttManager.RETAINED;

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
 *    b. Publish (Multiple)
 * </pre>
 *
 * @author Gregory
 * @since 2017/4/11
 */

public class Connection implements MqttCallbackExtended {

  // TCP connection
  private static final String TCP = "tcp://";
  // SSL connection
  private static final String SSL = "ssl://";
  // Connection status
  public static final int INIT = 1;
  public static final int CONNECTING = 1 << 1;
  public static final int CONNECTED = 1 << 2;
  public static final int DISCONNECTED = 1 << 3;
  public static final int LEAVE = 1 << 4;

  @IntDef(flag = true, value = { INIT, CONNECTING, CONNECTED, DISCONNECTED, LEAVE })
  @Retention(RetentionPolicy.SOURCE) @interface ConnectionStatus {

  }

  // Retry delay time
  private static final long RETRY_DELAY_TIME = 5000L;

  private final MqttAndroidClient client;
  private final String uri;
  private final String clientId;
  private boolean sslConnection;
  private int port;
  private final List<MqttActionListener> subActions = new CopyOnWriteArrayList<>();
  @ConnectionStatus private volatile int status = INIT;
  private MqttConnectOptions mqttConnectOptions;
  // Connection listener to send callback when connection is re-connect or not subscribe anything
  @Nullable private OnActionListener connectionListener;
  // Current subscriptionList of connection
  private final List<MqttTopic> subscriptionList = new ArrayList<>();
  // Current publishingList of connection
  private final List<MqttTopic> publishingList = new ArrayList<>();
  // The leaving flag
  private boolean leaving = false;

  private Connection(MqttAndroidClient client, String uri, String clientId, boolean sslConnection,
      int port) {
    this.client = client;
    this.client.setCallback(this);
    this.uri = uri;
    this.clientId = clientId;
    this.sslConnection = sslConnection;
    this.port = port;
  }

  public static Connection createConnection(Context context, String host, String clientId) {
    return createConnection(context, host, clientId, false, 1883);
  }

  public static Connection createConnection(Context context, String host, String clientId,
      boolean sslConnection, int port) {
    String uri;
    if (sslConnection) {
      uri = SSL + host + ":" + port;
    } else {
      uri = TCP + host + ":" + port;
    }

    MqttAndroidClient client = new MqttAndroidClient(context, uri, clientId);
    return new Connection(client, uri, clientId, sslConnection, port);
  }

  public static Connection createConnectionWithTimeStamp(Context context, String host,
      String clientId) {
    return createConnectionWithTimeStamp(context, host, clientId, false, 1883);
  }

  public static Connection createConnectionWithTimeStamp(Context context, String host,
      String clientId, boolean sslConnection, int port) {
    String timestampId = clientId + MqttManager.getInstance().getTimeStamp(clientId);
    String uri;
    if (sslConnection) {
      uri = SSL + host + ":" + port;
    } else {
      uri = TCP + host + ":" + port;
    }

    MqttAndroidClient client = new MqttAndroidClient(context, uri, timestampId);
    return new Connection(client, uri, timestampId, sslConnection, port);
  }

  @Override public String toString() {
    return "Client ID : " + clientId;
  }

  /**
   * Adds connection options for mqtt
   */
  public Connection addConnectionOptions(ConnectOptionsBuilder builder) {
    mqttConnectOptions = builder.build();
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
  public String getUri() {
    return uri;
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
    synchronized (Connection.class) {
      return status;
    }
  }

  /**
   * Change current connection status {@link ConnectionStatus}
   */
  private void setStatus(@ConnectionStatus int status) {
    synchronized (Connection.class) {
      this.status = status;
    }
  }

  /**
   * Checks current status
   *
   * @param status the specific to checks
   * @return true if equals, otherwise false
   */
  public boolean checkStatus(@ConnectionStatus int status) {
    return getStatus() == status;
  }

  /**
   * Add action that you want do {@link MqttActionListener.Action}
   */
  public void addAction(MqttActionListener connectionListener) {
    MqttActionListener subMqttActionListener = connectionListener.getSubMqttActionListener();
    if (checkStatus(CONNECTED)) {
      if (subMqttActionListener != null) {
        doSubAction(subMqttActionListener.getTopic(), subMqttActionListener.getTopics(),
            subMqttActionListener, null);
      }
    } else if (!checkStatus(LEAVE)) {
      if (subMqttActionListener != null) {
        addSubAction(subMqttActionListener);
      }

      // Re-connect when connection is disconnected
      if (checkStatus(DISCONNECTED)) {
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
  private void subActions(@Nullable OnActionListener onActionListener, Throwable throwable) {
    if (onActionListener != null) {
      connectCallback(onActionListener, throwable);
    }

    synchronized (subActions) {
      Iterator<MqttActionListener> iterator = subActions.iterator();
      while (iterator.hasNext()) {
        MqttActionListener subMqttActionListener = iterator.next();
        doSubAction(subMqttActionListener.getTopic(), subMqttActionListener.getTopics(),
            subMqttActionListener, throwable);
        subActions.remove(subMqttActionListener);
      }
    }
  }

  private OnActionListener weakReferenceListener(@Nullable OnActionListener onActionListener) {
    if (onActionListener == null) {
      return null;
    }

    return new WeakReference<>(onActionListener).get();
  }

  /**
   * Connection callback
   *
   * @param connectActionListener callback listener
   * @param throwable mqtt exception
   */
  @SuppressWarnings("unchecked") private void connectCallback(
      @NonNull OnActionListener connectActionListener, Throwable throwable) {
    if (throwable == null) {
      connectActionListener.onSuccess(null, "connectCallback to server");
    } else {
      connectActionListener.onFailure(null, throwable);
    }
  }

  /**
   * Do sub-action
   *
   * @param topic the topic
   * @param topics the list of topic
   * @param subMqttActionListener sub mqtt action listener
   * @param throwable mqtt exception
   */
  private void doSubAction(MqttTopic topic, List<? extends MqttTopic> topics,
      @NonNull MqttActionListener subMqttActionListener, Throwable throwable) {
    switch (subMqttActionListener.getAction()) {
      case MqttActionListener.SUBSCRIBE:
        if (topic != null) {
          subscribeTopic(topic, subMqttActionListener, throwable);
        } else if (topics != null) {
          subscribeTopics(topics, subMqttActionListener, throwable);
        }

        break;

      case MqttActionListener.UN_SUBSCRIBE:
        if (topic != null) {
          unSubscribeTopic(topic, subMqttActionListener, throwable);
        }

        break;

      case MqttActionListener.PUBLISH:
        if (topic != null) {
          publishTopic(topic, subMqttActionListener, throwable);
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
  @SuppressWarnings("unchecked") public void connect(@Nullable OnActionListener onActionListener) {
    connectionListener = weakReferenceListener(onActionListener);
    if (checkStatus(CONNECTED)) {
      if (connectionListener != null) {
        connectionListener.onSuccess(null, clientId + " is connected");
      }
    } else if (!checkStatus(CONNECTING | LEAVE)) {
      MqttActionListener connectionListener =
          new MqttActionListener(MqttActionListener.CONNECT, this, this.connectionListener);
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
    if (checkStatus(CONNECTING)) {
      debugMessage(clientId + " [LEAVING]...");
      leaving = true;
    } else {
      debugMessage(clientId + " [DISCONNECT] to server (" + uri + ")");
      setStatus(LEAVE);
      try {
        connectionListener = null;
        client.disconnect();
        subscriptionList.clear();
        publishingList.clear();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Release the resource of the mqtt service
   */
  private void release() {
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
   * @param topic the subscription topic
   * @param onActionListener action callback listener
   * @param retryTime retry times
   */
  public <T extends MqttTopic> void subscribeTopic(@NonNull final T topic,
      @Nullable final OnActionListener<T> onActionListener, int retryTime) {
    // Save the subscribed topic of current connection
    synchronized (subscriptionList) {
      if (!subscriptionList.contains(topic)) {
        subscriptionList.add(topic);
      }
    }

    MqttActionListener subscribeListener =
        new MqttActionListener(MqttActionListener.SUBSCRIBE, this, topic,
            weakReferenceListener(onActionListener), retryTime);
    if (checkStatus(CONNECTED)) {
      subscribeTopic(topic, subscribeListener, null);
    } else {
      MqttActionListener connectionListener = new MqttActionListener(this, subscribeListener);
      addAction(connectionListener);
    }
  }

  /**
   * Add the {@link MqttActionListener#SUBSCRIBE} action to try subscribe the topics from MQTT
   * server, this will check the connection status, {@link Connection#addAction(MqttActionListener)}
   *
   * @param topics the list of subscription topic
   * @param onActionListener action callback listener
   * @param retryTime retry times
   */
  public <T extends MqttTopic> void subscribeTopics(@NonNull final List<T> topics,
      @Nullable final OnActionListener<T> onActionListener, int retryTime) {
    // Save the subscribed topic of current connection
    synchronized (subscriptionList) {
      for (MqttTopic topic : topics) {
        if (!subscriptionList.contains(topic)) {
          subscriptionList.add(topic);
        }
      }
    }

    MqttActionListener subscribeListener =
        new MqttActionListener(MqttActionListener.SUBSCRIBE, this, topics,
            weakReferenceListener(onActionListener), retryTime);
    if (checkStatus(CONNECTED)) {
      subscribeTopics(topics, subscribeListener, null);
    } else {
      MqttActionListener connectionListener = new MqttActionListener(this, subscribeListener);
      addAction(connectionListener);
    }
  }

  /**
   * Add the {@link} action to try unSubscribe the topic from MQTT server, this will
   * check the connection status, {@link Connection#addAction(MqttActionListener)}
   *
   * @param topic the un-subscription topic
   * @param onActionListener action callback listener
   */
  public <T extends MqttTopic> void unSubscribeTopic(@NonNull final T topic,
      @Nullable final OnActionListener<T> onActionListener) {
    MqttActionListener unSubscribeListener =
        new MqttActionListener(MqttActionListener.UN_SUBSCRIBE, this, topic,
            weakReferenceListener(onActionListener));
    if (checkStatus(CONNECTED)) {
      unSubscribeTopic(topic, unSubscribeListener, null);
    } else {
      MqttActionListener connectionListener = new MqttActionListener(this, unSubscribeListener);
      addAction(connectionListener);
    }
  }

  /**
   * Add the publish action to try publish the topic to MQTT server, this will
   * check the connection status, {@link Connection#addAction(MqttActionListener)}
   *
   * @param topic the publishing topic
   * @param onActionListener action callback listener
   */
  public <T extends MqttTopic> void publishTopic(@NonNull final T topic,
      @Nullable OnActionListener<T> onActionListener) {
    MqttActionListener publishListener =
        new MqttActionListener(MqttActionListener.PUBLISH, this, topic,
            weakReferenceListener(onActionListener));
    if (checkStatus(CONNECTED)) {
      publishTopic(topic, publishListener, null);
    } else {
      MqttActionListener connectionListener = new MqttActionListener(this, publishListener);
      addAction(connectionListener);
    }
  }

  /**
   * Add the publish action to try publish the topic to MQTT server, this will
   * check the connection status, {@link Connection#addAction(MqttActionListener)}
   *
   * @param topic the publishing topic
   * @param onActionListener action callback listener
   * @param retryTime retry times
   */
  public <T extends MqttTopic> void publishTopic(@NonNull final T topic,
      @Nullable OnActionListener<T> onActionListener, int retryTime) {
    // Save the subscribed topic of current connection
    synchronized (publishingList) {
      if (!publishingList.contains(topic)) {
        publishingList.add(topic);
      }
    }

    MqttActionListener publishListener =
        new MqttActionListener(MqttActionListener.PUBLISH, this, topic,
            weakReferenceListener(onActionListener), retryTime);
    if (checkStatus(CONNECTED)) {
      publishTopic(topic, publishListener, null);
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
      if (mqttConnectOptions == null) {
        throw new IllegalStateException("ConnectOptions is null");
      }

      setStatus(CONNECTING);
      debugMessage(clientId + " [CONNECTING]...");
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
  @SuppressWarnings("unchecked") void connection(final MqttActionListener mqttActionListener,
      @Nullable final OnActionListener onActionListener, Throwable throwable) {
    if (getStatus() < CONNECTED) {
      if (throwable == null) {
        debugMessage(clientId + " [CONNECT] to mqtt server (" + uri + ") succeeded.");
        setStatus(CONNECTED);
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

        subActions(onActionListener, null);
      } else {
        setStatus(DISCONNECTED);
        int retryTime = mqttActionListener.retryTime();
        debugMessage(clientId
            + " [CONNECT] to MQTT server failure, "
            + throwable
            + ", rest retry times: "
            + retryTime
            + ", sub action: "
            + mqttActionListener.getSubMqttActionListener());
        MqttActionListener subMqttActionListener = mqttActionListener.getSubMqttActionListener();
        if (subMqttActionListener != null) {
          subMqttActionListener.clearRetryTime();
        }

        subActions(onActionListener, throwable);
      }
    }
  }

  /**
   * Subscribe topic from MQTT server
   *
   * @param topic subscription topic
   * @param mqttActionListener mqtt action listener
   * @param throwable exception
   */
  private void subscribeTopic(@NonNull MqttTopic topic, MqttActionListener mqttActionListener,
      Throwable throwable) {
    Subscription subscription = (Subscription) topic;
    if (throwable == null && checkStatus(CONNECTED)) {
      try {
        client.subscribe(topic.getMqttTopic(), subscription.getSubscriptionQoS(), null,
            mqttActionListener);
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
   * @param topic subscription topic
   * @param onActionListener onSubscribe callback listener
   * @param throwable exception
   */
  @SuppressWarnings("unchecked") void subscribe(final MqttActionListener mqttActionListener,
      final MqttTopic topic, @Nullable final OnActionListener onActionListener,
      final Throwable throwable) {
    Subscription subscription = (Subscription) topic;
    if (throwable == null && checkStatus(CONNECTED)) {
      debugMessage("[SUBSCRIBE] " + topic.getMqttTopic() + " succeeded.");
      try {
        client.subscribe(topic.getMqttTopic(), subscription.getSubscriptionQoS(),
            (s, mqttMessage) -> {
              String message = new String(mqttMessage.getPayload());
              debugMessage("[RECEIVED] " + topic.getMqttTopic() + ", message : " + message);
              if (onActionListener != null) {
                onActionListener.onSuccess(topic, message);
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
      debugMessage("[SUBSCRIBE] " + topic.getMqttTopic() + " failure.");
      if (onActionListener != null) {
        onActionListener.onFailure(topic, throwable);
      }
    }
  }

  /**
   * Subscribe topic from MQTT server
   *
   * @param mqttActionListener mqtt action listener
   * @param topics list of subscription topic
   * @param mqttActionListener onSubscribe callback listener
   * @param throwable exception
   */
  private void subscribeTopics(@NonNull List<? extends MqttTopic> topics,
      MqttActionListener mqttActionListener, Throwable throwable) {
    if (throwable == null && checkStatus(CONNECTED)) {
      try {
        int size = topics.size();
        String[] topicArray = new String[size];
        int[] qos = new int[size];
        for (int i = 0; i < size; i++) {
          MqttTopic topic = topics.get(i);
          Subscription subscription = (Subscription) topic;
          topicArray[i] = topics.get(i).getMqttTopic();
          qos[i] = subscription.getSubscriptionQoS();
        }

        client.subscribe(topicArray, qos, null, mqttActionListener);
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
   * @param topics list of subscription topic
   * @param onActionListener onSubscribe callback listener
   * @param throwable exception
   */
  @SuppressWarnings("unchecked") void subscribes(final MqttActionListener mqttActionListener,
      final List<? extends MqttTopic> topics, @Nullable final OnActionListener onActionListener,
      final Throwable throwable) {
    if (throwable == null && checkStatus(CONNECTED)) {
      try {
        int size = topics.size();
        String[] topicArray = new String[size];
        int[] qos = new int[size];
        IMqttMessageListener[] listeners = new IMqttMessageListener[size];
        for (int i = 0; i < size; i++) {
          final MqttTopic topic = topics.get(i);
          final Subscription subscription = (Subscription) topic;
          topicArray[i] = topic.getMqttTopic();
          qos[i] = subscription.getSubscriptionQoS();
          debugMessage("[SUBSCRIBE] " + topicArray[i] + " succeeded.");
          listeners[i] = (s, mqttMessage) -> {
            String message = new String(mqttMessage.getPayload());
            debugMessage("[RECEIVED] " + topic.getMqttTopic() + ", message: " + message);
            if (onActionListener != null) {
              onActionListener.onSuccess(topic, message);
            }
          };
        }

        client.subscribe(topicArray, qos, listeners);
      } catch (MqttException e) {
        e.printStackTrace();
      } catch (IllegalArgumentException e) {
        e.printStackTrace();
      } catch (NullPointerException e) {
        e.printStackTrace();
      }
    } else {
      for (MqttTopic topic : topics) {
        debugMessage("[SUBSCRIBE] " + topic.getMqttTopic() + " failure, " + throwable);
        onActionListener.onFailure(topic, throwable);
      }
    }
  }

  /**
   * Un-subscribe topic form MQTT server
   *
   * @param topic un-subscription topic
   * @param mqttActionListener mqtt action listener
   * @param throwable exception
   */
  private void unSubscribeTopic(@NonNull MqttTopic topic, MqttActionListener mqttActionListener,
      Throwable throwable) {
    if (throwable == null && checkStatus(CONNECTED)) {
      try {
        client.unsubscribe(topic.getMqttTopic(), null, mqttActionListener);
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
   * Un-subscribe callback from MQTT server
   *
   * @param topic un-subscription topic
   * @param onActionListener action callback listener
   * @param throwable exception
   */
  @SuppressWarnings("unchecked") void unSubscribe(MqttTopic topic,
      @Nullable OnActionListener onActionListener, Throwable throwable) {
    if (throwable == null && checkStatus(CONNECTED)) {
      debugMessage("[UN-SUBSCRIBE] " + topic.getMqttTopic() + " succeeded.");
      if (onActionListener != null) {
        onActionListener.onSuccess(topic, null);
      }
    } else {
      debugMessage("[UN-SUBSCRIBE] " + topic.getMqttTopic() + " failure, " + throwable);
      if (onActionListener != null) {
        onActionListener.onFailure(topic, throwable);
      }
    }
  }

  /**
   * Publish topic to MQTT server
   *
   * @param topic publishing topic
   * @param mqttActionListener mqtt action listener
   * @param throwable exception
   */
  private void publishTopic(@NonNull MqttTopic topic, MqttActionListener mqttActionListener,
      Throwable throwable) {
    Publishing publishing = (Publishing) topic;
    if (throwable == null && checkStatus(CONNECTED)) {
      try {
        String message = publishing.getPublishingMessage();
        if (message == null) {
          message = "";
        }

        client.publish(topic.getMqttTopic(), message.getBytes(), publishing.getPublishingQoS(),
            publishing.isRetained() == RETAINED, null, mqttActionListener);
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
   * @param topic publishing topic
   * @param onActionListener action callback listener
   * @param throwable exception
   */
  @SuppressWarnings("unchecked") void publish(final MqttActionListener mqttActionListener,
      final MqttTopic topic, @Nullable OnActionListener onActionListener,
      final Throwable throwable) {
    Publishing publishing = (Publishing) topic;
    if (throwable == null && checkStatus(CONNECTED)) {
      debugMessage("[PUBLISH] "
          + topic.getMqttTopic()
          + ", message: "
          + publishing.getPublishingMessage()
          + " succeeded.");
      if (onActionListener != null) {
        onActionListener.onSuccess(topic, publishing.getPublishingMessage());
      }
    } else {
      debugMessage("[PUBLISH] " + topic.getMqttTopic() + " failure, " + throwable);
      if (onActionListener != null) {
        onActionListener.onFailure(topic, throwable);
      }
    }
  }

  @SuppressWarnings("unchecked") @Override
  public void connectComplete(boolean isReconnect, String s) {
    if (DEBUG) {
      Log.d(MQTT_TAG, "Connection complete, isReconnect = " + isReconnect);
    }

    // Set status to connected
    setStatus(CONNECTED);
    /// If leaving
    if (leaving) {
      disconnect();
    } else {
      if (connectionListener != null) {
        if (isReconnect || subscriptionList.isEmpty()) {
          connectionListener.onSuccess(null, TopicMessage.RECONNECT);
        }
      }
    }
  }

  @Override public void connectionLost(Throwable throwable) {
    if (DEBUG) {
      Log.d(MQTT_TAG, "Connection lost, throwable = " + throwable);
    }

    if (!checkStatus(LEAVE)) {
      setStatus(DISCONNECTED);
    } else {
      release();
    }
  }

  @Override public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {

  }

  @Override public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

  }

  private void debugMessage(String message) {
    if (DEBUG) {
      Log.d(MQTT_TAG, message);
    }
  }

  public static class ConnectOptionsBuilder {

    private String user = null;
    private String password = null;
    private int connectionTimeout = 30;
    private int keepAliveInterval = 60;
    private boolean autoReconnect = true;
    private boolean cleanSession = false;
    private SocketFactory socketFactory = null;

    public ConnectOptionsBuilder setUser(String user) {
      this.user = user;
      return this;
    }

    public ConnectOptionsBuilder setPassword(String password) {
      this.password = password;
      return this;
    }

    public ConnectOptionsBuilder setConnectionTimeout(int connectionTimeout) {
      this.connectionTimeout = connectionTimeout;
      return this;
    }

    public ConnectOptionsBuilder setKeppAliveInterval(int keepAliveInterval) {
      this.keepAliveInterval = keepAliveInterval;
      return this;
    }

    public ConnectOptionsBuilder setAutoReconnect(boolean autoReconnect) {
      this.autoReconnect = autoReconnect;
      return this;
    }

    public ConnectOptionsBuilder setCleanSession(boolean cleanSession) {
      this.cleanSession = cleanSession;
      return this;
    }

    public ConnectOptionsBuilder setSocketFactory(SSLSocketFactory socketFactory) {
      this.socketFactory = socketFactory;
      return this;
    }

    MqttConnectOptions build() {
      if (user == null) {
        throw new NullPointerException("User can't be null");
      }

      if (password == null) {
        throw new NullPointerException("Password can't be null");
      }

      MqttConnectOptions connectOptions = new MqttConnectOptions();
      connectOptions.setUserName(user);
      connectOptions.setPassword(password.toCharArray());
      connectOptions.setConnectionTimeout(connectionTimeout);
      connectOptions.setKeepAliveInterval(keepAliveInterval);
      connectOptions.setAutomaticReconnect(autoReconnect);
      connectOptions.setCleanSession(cleanSession);
      if (socketFactory != null) {
        connectOptions.setSocketFactory(socketFactory);
      }

      return connectOptions;
    }
  }
}
