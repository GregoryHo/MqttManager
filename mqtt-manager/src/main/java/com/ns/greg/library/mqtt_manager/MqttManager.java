package com.ns.greg.library.mqtt_manager;

import com.ns.greg.library.mqtt_manager.internal.Connection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Gregory
 * @since 2017/4/11
 */

public class MqttManager {

  public static final String MQTT_TAG = "MqttManager";
  public static final boolean DEBUG = false;

  private static volatile MqttManager mqttManager;
  // List of {@link Connection} object
  private final Map<String, Connection> connections = new ConcurrentHashMap<>();
  // List of time stamp of connection
  private final Map<String, Long> timeStamps = new ConcurrentHashMap<>();

  private MqttManager() {

  }

  public static MqttManager getInstance() {
    if (mqttManager == null) {
      synchronized (MqttManager.class) {
        if (mqttManager == null) {
          mqttManager = new MqttManager();
        }
      }
    }

    return mqttManager;
  }

  public static void releaseInstance() {
    mqttManager = null;
  }

  /**
   * Should be called when activity is onResume,
   * this will update the time stamp according to the client id.
   */
  public void onResume(String clientId) {
    if (timeStamps.containsKey(clientId)) {
      timeStamps.remove(clientId);
    }

    timeStamps.put(clientId, System.currentTimeMillis());
  }

  /**
   * Gets current time stamp of client id
   *
   * @param clientId target client id
   * @return time stamp
   */
  public String getTimeStamp(String clientId) {
    return "_" + timeStamps.get(clientId);
  }

  /**
   * Adds connection to the connections
   *
   * @param connection mqtt connection
   * @return connection
   */
  public Connection addConnection(Connection connection) {
    connections.put(connection.getClientId(), connection);

    return connection;
  }

  /**
   * Removes specific connection
   *
   * @param clientId
   */
  public void removeConnection(String clientId) {
    if (connections.containsKey(clientId)) {
      connections.remove(clientId);
    }
  }

  /**
   * Clears connection
   */
  public void clearConnections() {
    connections.clear();
  }

  /**
   * Gets the connection with client id
   *
   * @param clientId target id
   * @return connection
   */
  public Connection getConnection(String clientId) {
    Connection connection = null;
    long currentTime = timeStamps.get(clientId);
    for (String key : connections.keySet()) {
      if (key.contains(clientId)) {
        long time = convertTime(key);
        // Get current time stamp connection
        if (time == currentTime) {
          connection = connections.get(key);
        } else {
          // release / disconnect the connection is out of date
          if (connections.get(key).getStatus() == Connection.LEAVE) {
            connections.remove(key);
          } else {
            connections.get(key).disconnect();
          }
        }
      }
    }

    return connection;
  }

  /**
   * The client key is compose by client id + time stamp, as below:
   * "clientId_timeStamp", e.g. cht_12345678.
   *
   * For getting the time stamp, the easy way is split the string by
   * "_", to get the time stamp of client id
   *
   * @param key client key
   * @return time stamp
   */
  private long convertTime(String key) {
    try {
      if (key.contains("_")) {
        String[] strings = key.split("_");
        return Long.parseLong(strings[strings.length - 1]);
      }

      return Long.parseLong(key);
    } catch (Exception e) {
      e.printStackTrace();
    }

    return 0;
  }
}
