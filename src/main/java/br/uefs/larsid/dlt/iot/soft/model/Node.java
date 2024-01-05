package br.uefs.larsid.dlt.iot.soft.model;

import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import br.uefs.larsid.dlt.iot.soft.services.Controller;
import br.uefs.larsid.dlt.iot.soft.services.INode;
import br.uefs.larsid.dlt.iot.soft.tasks.CheckDevicesCredentialProofTask;
import br.uefs.larsid.dlt.iot.soft.utils.ClientIoTService;

public class Node implements INode {
    private List<Device> devices;
    private Map<String, String> connectionIdDeviceNodes = new LinkedHashMap<String, String>();

    private int checkDevicesCredentialProofTaskTime;
    private Timer checkDevicesCredentialProofTimer;

    private String deviceAPIAddress;

    private Controller controller;
    private boolean debugModeValue;
    private static final Logger logger = Logger.getLogger(Node.class.getName());

    public Node() {
    }

    public void start() {
        this.devices = new ArrayList<>();
        this.checkDevicesCredentialProofTimer = new Timer();
    }

    /**
     * Executa o que foi definido na função quando o bundle for finalizado.
     */
    public void stop() {
        if (this.checkDevicesCredentialProofTimer != null) {
            this.checkDevicesCredentialProofTimer.cancel();
        }
    }

    /**
     * Adiciona os dispositivos que foram requisitados na lista de dispositivos.
     *
     * @param strDevices String - Dispositivos requisitados.
     */
    public void loadConnectedDevices() {
        List<Device> devicesTemp = new ArrayList<Device>();

        try {
            JSONArray jsonArrayDevices = new JSONArray(
                    ClientIoTService.getApiIot(this.deviceAPIAddress));

            for (int i = 0; i < jsonArrayDevices.length(); i++) {
                JSONObject jsonDevice = jsonArrayDevices.getJSONObject(i);
                ObjectMapper mapper = new ObjectMapper();
                Device device = mapper.readValue(
                        jsonDevice.toString(),
                        Device.class);

                devicesTemp.add(device);

                List<Sensor> tempSensors = new ArrayList<Sensor>();
                JSONArray jsonArraySensors = jsonDevice.getJSONArray(
                        "sensors");

                for (int j = 0; j < jsonArraySensors.length(); j++) {
                    JSONObject jsonSensor = jsonArraySensors.getJSONObject(j);
                    Sensor sensor = mapper.readValue(
                            jsonSensor.toString(),
                            Sensor.class);
                    sensor.setDeviceAPIAddress(deviceAPIAddress);
                    tempSensors.add(sensor);
                }

                device.setSensors(tempSensors);
            }
        } catch (JsonParseException e) {
            printlnDebug("Verify the correct format of 'DevicesConnected' property in configuration file.");
            logger.log(Level.SEVERE, null, e);
        } catch (JsonMappingException e) {
            printlnDebug(
                    "Verify the correct format of 'DevicesConnected' property in configuration file.");
            logger.log(Level.SEVERE, null, e);
        } catch (IOException e) {
            logger.log(Level.SEVERE, null, e);
        }

        this.devices = devicesTemp;
    }

    public List<Device> getDevices() {
        return devices;
    }

    public void setDevices(List<Device> devices) {
        this.devices = devices;
    }

    public Map<String, String> getConnectionIdDeviceNodes() {
        return connectionIdDeviceNodes;
    }
    
    public void addConnectionIdDeviceNodes(String deviceId, String connectionId) {
        this.connectionIdDeviceNodes.put(deviceId, connectionId);
    }

    public int getCheckDevicesCredentialProofTaskTime() {
        return checkDevicesCredentialProofTaskTime;
    }

    public void setCheckDevicesCredentialProofTaskTime(int checkDevicesCredentialProofTaskTime) {
        this.checkDevicesCredentialProofTaskTime = checkDevicesCredentialProofTaskTime;
    }

    public String getDeviceAPIAddress() {
        return deviceAPIAddress;
    }

    public void setDeviceAPIAddress(String deviceAPIAddress) {
        this.deviceAPIAddress = deviceAPIAddress;
    }

    public Controller getController() {
        return controller;
    }

    public void setController(Controller controller) {
        this.controller = controller;

        this.checkDevicesCredentialProofTimer.scheduleAtFixedRate(
                new CheckDevicesCredentialProofTask(this, controller, debugModeValue),
                0,
                this.checkDevicesCredentialProofTaskTime * 1000);
    }

    public boolean isDebugModeValue() {
        return this.debugModeValue;
    }

    public void setDebugModeValue(boolean debugModeValue) {
        this.debugModeValue = debugModeValue;
    }

    private void printlnDebug(String str) {
        if (debugModeValue) {
            logger.info(str);
        }
    }
}
