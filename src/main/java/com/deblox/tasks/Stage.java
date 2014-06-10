package com.deblox.tasks;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by keghol on 6/9/14.
 */
public class Stage {

    public String app;
    public String task;
    public String callback;

    public Stage(JsonObject stage) {
        this.app = stage.getString("app");
        this.task = stage.getString("task");
        this.callback = stage.getString("callback");
    }

    /**
     * convert jsonArray of stages into list of Stage instances
     * @param stages, JsonArray of stages {"app": "someapp", "task":"some task name", "callback":"callback object"}
     * @return
     */

    public static List<Stage> buildStages(JsonArray stages) {

        List<Stage> result = new ArrayList<Stage>();

        Iterator s = stages.iterator();
        while ( s.hasNext()) {
            Stage ts = new Stage((JsonObject)s.next());
            result.add(ts);
        }

        return result;
    }

}
