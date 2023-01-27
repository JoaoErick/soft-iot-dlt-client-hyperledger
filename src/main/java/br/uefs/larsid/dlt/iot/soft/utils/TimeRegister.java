/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package br.uefs.larsid.dlt.iot.soft.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.sql.Timestamp;

/**
 *
 * @author Emers
 */
public class TimeRegister {
    private Timestamp timeInit;
    private Timestamp timeEnd;

    public TimeRegister(Timestamp timeInit, Timestamp timeEnd) {
        this.timeInit = timeInit;
        this.timeEnd = timeEnd;
    }

    public Timestamp getTimeInit() {
        return timeInit;
    }

    public void setTimeInit(Timestamp timeInit) {
        this.timeInit = timeInit;
    }

    public Timestamp getTimeEnd() {
        return timeEnd;
    }

    public void setTimeEnd(Timestamp timeEnd) {
        this.timeEnd = timeEnd;
    }
    
    public long getDuration(){
        return timeEnd.getTime() - timeInit.getTime();
    }
    
    public JsonObject getJson(){
        JsonElement timeRegister = new Gson().toJsonTree(this);
        JsonObject timeRegisterJson = timeRegister.getAsJsonObject();
        timeRegisterJson.addProperty("Duration", getDuration());
        return timeRegisterJson;
    }
    
}
