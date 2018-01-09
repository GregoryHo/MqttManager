package com.ns.greg.library.mqtt_manager.annotation;

import android.support.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.ns.greg.library.mqtt_manager.MqttConstants.RETAINED;
import static com.ns.greg.library.mqtt_manager.MqttConstants.UN_RETAINED;

/**
 * @author Gregory
 * @since 2018/1/9
 */

@IntDef({ UN_RETAINED, RETAINED }) @Retention(RetentionPolicy.SOURCE) public @interface Retained {

}