package br.uefs.larsid.dlt.iot.soft.mqtt;

import br.uefs.larsid.dlt.iot.soft.services.Controller;

import java.util.logging.Logger;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class ListenerResponse implements IMqttMessageListener {

  /*-------------------------Constantes---------------------------------------*/
  private static final String SENSORS_EDGE_RES = "SENSORS_EDGE_RES";
  private static final String N_DEVICES_EDGE_RES = "N_DEVICES_EDGE_RES";
  /*--------------------------------------------------------------------------*/

  private boolean debugModeValue;
  private Controller controllerImpl;
  private MQTTClient MQTTClientHost;
  private static final Logger logger = Logger.getLogger(ListenerResponse.class.getName());

  /**
   * Builder method.
   *
   * @param controllerImpl Controller - Controller that will make use of this Listener.
   * @param MQTTClientHost MQTTClient - Gateway's own MQTT client.
   * @param topics         String[] - Topics to be subscribed to.
   * @param qos            int - Quality of service of the topic that will be heard.
   * @param debugModeValue boolean - How to debug the code.
   */
  public ListenerResponse(
    Controller controllerImpl,
    MQTTClient MQTTClientHost,
    String[] topics,
    int qos,
    boolean debugModeValue
  ) {
    this.controllerImpl = controllerImpl;
    this.MQTTClientHost = MQTTClientHost;
    this.debugModeValue = debugModeValue;

    for (String topic : topics) {
      this.MQTTClientHost.subscribe(qos, this, topic);
    }
  }

  @Override
  public void messageArrived(String topic, MqttMessage message)
    throws Exception {
    final String[] params = topic.split("/");
    String messageContent = new String(message.getPayload());

    printlnDebug("Receiving requests from edge layer nodes...");

    switch (params[0]) {
      
      case N_DEVICES_EDGE_RES:
        this.controllerImpl.getNumberDevicesConnectedNodes().add(messageContent);

        printlnDebug("Number of Devices response received and add to the list!");

        /* Adding new request. */
        this.controllerImpl.updateResponse("numberOfDevices");

        break;
      case SENSORS_EDGE_RES:

        if(!messageContent.equals("")){
          JsonObject jsonResponse = new Gson().fromJson(messageContent, JsonObject.class);

          this.controllerImpl.putSensorsTypes(jsonResponse);

          printlnDebug("Sensors response received and add to the map!");
        }

        /* Adding new request. */
        this.controllerImpl.updateResponse("getSensors");

        break;
    }
  }

  private void printlnDebug(String str) {
    if (isDebugModeValue()) {
      logger.info(str);
    }
  }

  public boolean isDebugModeValue() {
    return debugModeValue;
  }

  public void setDebugModeValue(boolean debugModeValue) {
    this.debugModeValue = debugModeValue;
  }
}
