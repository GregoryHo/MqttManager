# MQTT Manager
A connection / subscribe / publish handler of MQTT

[![license](https://img.shields.io/github/license/mashape/apistatus.svg)](https://github.com/GregoryHo/MqttManager/blob/master/LICENSE)

### Create Connection
```java
Connection connection = Connection.createConnection(context, "endpoint", "clientId")
                                  .addConnectionOptions(new Connection.ConnectOptionsBuilder().setUser("test")
                                      .setPassword("1234")
                                      .setConnectionTimeout(30)
                                      .setKeppAliveInterval(10));
connection.connect(new OnActionListener() {
  @Override public void onSuccess(MqttTopic mqttTopic, String message) {
    /* do something when connected */
  }

  @Override public void onFailure(MqttTopic mqttTopic, Throwable throwable) {
    /* some error handle when failure */
  }
});
````
### Create Topic
```java
/* demo topic, create every topic with prefix /DEMO/ */
public class DemoTopic extends MqttTopic {
  
  private final String topic;
  private String message;
  
  public DemoTopic(String topic) {
    this(topic, "");
  }
  
  public DemoTopic(String mqttTopic, String message) {
    super("/DEMO/" + topic);
    this.topic = topic;
    this.message = message;
  }
  
  public String getTopic() {
    return topic;
  }
}
/* topic for subscribe */
public class SubscribeTopic extends DemoTopic implements Subscription {
  
  public SubscribeTopic(String topic) {
    super(topic);
  }
  
  @Override public int getSubscriptionQoS() {
    return MqttConstants.AT_LEAST_ONCE;
  }
}
/* topic for publish */
public class PublishTopic extends DemoTopic implements Publishing {
  
  public PublishTopic(String topic) {
    super(topic);
  }
  
  public PublishTopic(String topic, String message) {
    super(topic, message);
  }
  
  @Override public int getPublishingQoS() {
    return MqttConstants.EXACTLY_ONCE;
  }
  
  @Override public int isRetained() {
    return MqttConstants.RETAINED;
  }
  
  @Override public String getPublishingMessage() {
    return getMessage();
  }
}
```
### Subscribe Topic
The connection will check current state,
if connected, it will just subscribe the topic,
otherwise it will automatic connect to server and subscribe when connected.
### Single
```java
connection.subscribeTopic(topic, new OnActionListener<TopicDoor>() {
  @Override public void onSuccess(SubscribeTopic topic, String message) {
    /* subscribe topic succeeded */
  }

  @Override public void onFailure(SubscribeTopic topic, Throwable throwable) {
    /* subscribe topic failure */
  }
});
```
### Multiple
```java
List<DemoTopic> subscriptionList = new ArrayList<>();
subscriptionList.add(topic);
subscriptionList.add(topic2);
connection.subscribeTopics(subscriptionList, new OnActionListener<TopicDoor>() {
  @Override public void onSuccess(SubscribeTopic topic, String message) {
    /* subscribe topic succeeded */
  }

  @Override public void onFailure(SubscribeTopic topic, Throwable throwable) {
    /* subscribe topic failure */
  }
});
```
### Publish Topic
The connection will check current state,
if connected, it will just publish the topic,
otherwise it will automatic connect to server and publish when connected.
### Single
```java
topic.setMessage("some message");
/* doesn't care about callback */
connection.publishTopic(topic, null);
connection.publishTopic(topic, new OnActionListener<TopicDoor>() {
  @Override public void onSuccess(TopicDoor mqttTopic, String message) {
    /* publish topic succeeded */
  }
                               
  @Override public void onFailure(TopicDoor mqttTopic, Throwable throwable) {
    /* publish topic failure */
  }
})
```

