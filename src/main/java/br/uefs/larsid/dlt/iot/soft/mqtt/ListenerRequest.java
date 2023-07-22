package br.uefs.larsid.dlt.iot.soft.mqtt;

import br.uefs.larsid.dlt.iot.soft.services.Controller;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.List;
import java.util.logging.Logger;

public class ListenerRequest implements IMqttMessageListener {

  /*-------------------------Constants---------------------------------------*/
  private static final String GET_N_DEVICES = "GET_N_DEVICES";
  private static final String N_DEVICES_EDGE = "N_DEVICES_EDGE";
  private static final String N_DEVICES_EDGE_RES = "N_DEVICES_EDGE_RES";

  private static final String GET_SENSORS = "GET_SENSORS";
  private static final String SENSORS_RES = "SENSORS_RES";
  private static final String SENSORS_EDGE = "SENSORS_EDGE";
  private static final String SENSORS_EDGE_RES = "SENSORS_EDGE_RES";

  private static final int QOS = 1;
  /*--------------------------------------------------------------------------*/

  private boolean debugModeValue;
  private MQTTClient MQTTClientUp;
  private MQTTClient MQTTClientHost;
  private List<String> nodesUris;
  private Controller controllerImpl;
  private static final Logger logger = Logger.getLogger(ListenerRequest.class.getName());

  /**
   * Builder method.
   *
   * @param controllerImpl Controller - Controller that will make use of this Listener.
   * @param MQTTClientUp   MQTTClient - Upper Gateway MQTT Client.
   * @param MQTTClientHost MQTTClient - Gateway's own MQTT client.
   * @param nodesUris      List<String> - URI List.
   * @param topics         String[] - Topics to be subscribed to.
   * @param qos            int - Quality of service of the topic that will be heard.
   * @param debugModeValue boolean - How to debug the code.
   */
  public ListenerRequest(
    Controller controllerImpl,
    MQTTClient MQTTClientUp,
    MQTTClient MQTTClientHost,
    List<String> nodesUris,
    String[] topics,
    int qos,
    boolean debugModeValue
  ) {
    this.MQTTClientUp = MQTTClientUp;
    this.MQTTClientHost = MQTTClientHost;
    this.nodesUris = nodesUris;
    this.controllerImpl = controllerImpl;
    this.debugModeValue = debugModeValue;

    if (controllerImpl.hasNodes()) {
      for (String topic : topics) {
        this.MQTTClientUp.subscribe(qos, this, topic);
      }
    } else {
      for (String topic : topics) {
        this.MQTTClientHost.subscribe(qos, this, topic);
      }
    }
  }

  @Override
  public void messageArrived(String topic, MqttMessage message)
    throws Exception {

    switch (topic) {
      case GET_N_DEVICES:

        printlnDebug("Requesting number of connected devices...");

        /**
         * Requesting the devices that are connected to the node itself.
         */
        this.controllerImpl.loadConnectedDevices();

        this.controllerImpl.getNumberDevicesConnectedNodes().add(
          String.format(
            "gateway: %s | %s devices connected", 
            MQTTClientHost.getIp(), 
            this.controllerImpl.getDevices().size()
          )
        );

        /* Creating a new key, in the request map */
        this.controllerImpl.addResponse("numberOfDevices");

        this.publishToDown(N_DEVICES_EDGE, "".getBytes());

        /* Waits for responses from lower layer nodes connected to it; 
         * and publishes to the upper layer.
         */
        this.controllerImpl.publishNumberDevicesConnected();

        break;

      case N_DEVICES_EDGE:

        printlnDebug("Requesting number of connected devices...");

        /**
         * Requesting the devices that are connected to the node itself.
         */
        this.controllerImpl.loadConnectedDevices();

        byte[] payloadNumberDevices = String.format(
            "gateway: %s | %s devices connected", 
            MQTTClientHost.getIp(), 
            this.controllerImpl.getDevices().size()
          ).getBytes();

        MQTTClientUp.publish(N_DEVICES_EDGE_RES, payloadNumberDevices, 1);

        break;

      case GET_SENSORS:
        printlnDebug("Requesting device sensors...");

        /**
         * Requesting the devices that are connected to the node itself.
         */
        this.controllerImpl.loadConnectedDevices();

        /**
         * If there are devices connected to the node itself.
         */
        if (this.controllerImpl.getDevices().size() > 0) {
          JsonObject jsonGetSensors = new JsonObject();
          String deviceListJson = new Gson()
          .toJson(this.controllerImpl.loadSensorsTypes());

          jsonGetSensors.addProperty("sensors", deviceListJson);

          byte[] payload = jsonGetSensors
            .toString()
            .replace("\\", "")
            .getBytes();

          MQTTClientUp.publish(SENSORS_RES, payload, 1);
        } else {
          this.controllerImpl.getSensorsTypesJSON()
            .addProperty("sensors", "[]");

          /* Creating a new key, in the request map */
          this.controllerImpl.addResponse("getSensors");

          this.publishToDown(SENSORS_EDGE, "".getBytes());

          /* Waits for responses from lower layer nodes connected to it; 
           * and publishes to the upper layer.
           */
          this.controllerImpl.publishSensorType();
        }

        break;
      case SENSORS_EDGE:
        byte[] payload;

        printlnDebug("Requesting device sensors...");

        /**
         * Requesting the devices that are connected to the node itself.
         */
        this.controllerImpl.loadConnectedDevices();

        /**
         * If there are devices connected to the node itself.
         */
        if (this.controllerImpl.getDevices().size() > 0) {
          JsonObject jsonGetSensors = new JsonObject();
          String deviceListJson = new Gson()
          .toJson(this.controllerImpl.loadSensorsTypes());

          jsonGetSensors.addProperty("sensors", deviceListJson);

          payload = jsonGetSensors.toString().getBytes();

        } else {
          payload = "".getBytes();
        }

        MQTTClientUp.publish(SENSORS_EDGE_RES, payload, 1);

        break;
      
      default:
        String responseMessage = String.format(
          "\nOops! the request isn't recognized...\nTry one of the options below:\n- %s\n",
          GET_SENSORS
        );

        printlnDebug(responseMessage);

        break;
    }
  }

  /**
   * Publish the request to the child nodes.
   *
   * @param topicDown String - Topic.
   * @param messageDown byte[] - Message that will be sent.
   */
  private void publishToDown(String topicDown, byte[] messageDown) {
    String user = this.MQTTClientUp.getUserName();
    String password = this.MQTTClientUp.getPassword();

    for (String nodeUri : this.nodesUris) {
      String uri[] = nodeUri.split(":");

      MQTTClient MQTTClientDown = new MQTTClient(
        this.debugModeValue,
        uri[0],
        uri[1],
        user,
        password
      );

      MQTTClientDown.connect();
      MQTTClientDown.publish(topicDown, messageDown, QOS);
      MQTTClientDown.disconnect();
    }
  }

  private void printlnDebug(String str) {
    if (debugModeValue) {
      logger.info(str);
    }
  }
}
