package com.deblox.tasks;

import com.deblox.utils.Util;
import org.vertx.java.core.json.JsonObject;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Created by keghol on 6/9/14.
 */
public class Task {

    public String taskName;
    public String dataDocumentId;
    public String utid; // unique task ID
    public String uaid; // parent application ID who is spawning the task
    public List<Stage> stages; // array of stages
    public int currentStage; // current stage index id
    public long currentStageStartTime;
    public long currentStageTimeout;
    public String[] history; // past transgressions
    public boolean locked = false;

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getDataDocumentId() {
        return dataDocumentId;
    }

    public void setDataDocumentId(String dataDocumentId) {
        this.dataDocumentId = dataDocumentId;
    }

    public String getUtid() {
        return utid;
    }

    public void setUtid(String utid) {
        this.utid = utid;
    }

    public String getUaid() {
        return uaid;
    }

    public void setUaid(String uaid) {
        this.uaid = uaid;
    }

    public List<Stage> getStages() {
        return stages;
    }

    public void setStages(List<Stage> stages) {
        this.stages = stages;
    }

    public Stage getCurrentStage() {
        return stages.get(currentStage);
    }

    public void setCurrentStage(int currentStage) {
        this.currentStage = currentStage;
    }

    // set and return the next stage
    public Stage nextStage() {
        if (currentStage<stages.size()) {
            setCurrentStage(currentStage + 1);
            return getCurrentStage();

        } else {
            setCurrentStage(stages.size());
            return getCurrentStage();
        }
    }

    public long getCurrentStageStartTime() {
        return currentStageStartTime;
    }

    public void setCurrentStageStartTime(long currentStageStartTime) {
        this.currentStageStartTime = currentStageStartTime;
    }

    public long getCurrentStageTimeout() {
        return currentStageTimeout;
    }

    public void setCurrentStageTimeout(long currentStageTimeout) {
        this.currentStageTimeout = currentStageTimeout;
    }

    public String[] getHistory() {
        return history;
    }

    public void setHistory(String[] history) {
        this.history = history;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }


    /**
     * New task instance
     *
     * accepts some values and populates stages by reading a json file from resources.
     *
     * @param taskName
     * @param dataDocumentId
     * @param uaid
     */
    public Task(String taskName, String dataDocumentId, String uaid) throws Exception {
        this.uaid = uaid;
        this.dataDocumentId = dataDocumentId;
        this.taskName = taskName;

        // generate a taskId
        this.utid = UUID.randomUUID().toString();
        System.out.println("utid: " + this.utid);
        JsonObject stagesConf;

        // load the template JSON from couchbase or somefile
        try {
            stagesConf = Util.loadConfig(this, "/tasks/" + taskName + ".json");
        } catch (IOException e) {
            throw new Exception("Unable to find the json template for " + taskName);
        }
        System.out.println(stagesConf.toString());

        // for stage in stages, re-instantiate
        stages = Stage.buildStages(stagesConf.getArray("stages"));
        System.out.println("Stages: " + stages.toString());

    }

    /**
     *
     * deserialize a JsonObject of a task, ignore the stages in the message for security
     * reasons and load from json file in resources instead.
     *
     * { "value": {"map": { key: value, key:value }}
     *
     * @param task
     */
    public static Task deserialize(JsonObject task) {
        // the map is in the value
        JsonObject map = new JsonObject(task.getString("value"));
        Task t = (Task)Util.decode(map.getObject("map").toString(), Task.class);
        return t;
    }

    /**
     * serialize an instance to a String
     * @param task
     * @return
     */
    public static String serialize(Task task) {
        return Util.encode(task);
    }

    /**
     * serialize to Json
     * @param task
     * @return
     */
    public static JsonObject toJson(Task task) {
        return new JsonObject(Task.serialize(task));
    }


    @Override
    public String toString() {
        return "TASK [taskName=" + taskName +
                ", dataDocumentId=" + dataDocumentId +
                ", stages=" + stages +
                ", currentStage=" + currentStage +
                "]";
    }

    /**
     * read up the 1st stage and kick it off
     *
     *

     */
    public void start() {

//        Stage stage = new Stage(stages[0]);

    }

}
