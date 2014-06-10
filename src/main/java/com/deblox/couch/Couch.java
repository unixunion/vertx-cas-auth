package com.deblox.couch;

import com.deblox.utils.Util;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by keghol on 6/10/14.
 */
public class Couch {

    public static JsonObject Get(String id) {
        JsonObject request = new JsonObject()
                .putString("op", "GET")
                .putString("key", id)
                .putBoolean("ack", true);
        return request;
    }

    public static JsonObject Set(String id, Object value) {
        JsonObject request = new JsonObject()
                .putString("op", "SET")
                .putString("key", id)
                .putString("value", Util.encode(value))
                .putBoolean("ack", true);
        return request;
    }

}
