package com.ns.greg.library.mqtt_manager.annotation;

import android.support.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.ns.greg.library.mqtt_manager.MqttConstants.AT_LEAST_ONCE;
import static com.ns.greg.library.mqtt_manager.MqttConstants.AT_MOST_ONCE;
import static com.ns.greg.library.mqtt_manager.MqttConstants.EXACTLY_ONCE;

/**
 * @author Gregory
 * @since 2018/1/9
 */

@IntDef({ AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE }) @Retention(RetentionPolicy.SOURCE)
public @interface QoS {

}
