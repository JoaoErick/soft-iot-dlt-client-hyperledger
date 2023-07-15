package br.uefs.larsid.dlt.iot.soft.mqtt;

import br.uefs.larsid.dlt.iot.soft.services.Controller;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import java.util.logging.Logger;

public class ListenerRequest implements IMqttMessageListener {

  /*-------------------------Constants---------------------------------------*/
  private static final String SENSORS_FOG = "SENSORS_FOG";
  private static final String SENSORS_FOG_RES = "SENSORS_FOG_RES";
  private static final String SENSORS_EDGE = "SENSORS_EDGE";
  private static final String SENSORS_EDGE_RES = "SENSORS_EDGE_RES";
  private static final int QOS = 1;
  /*--------------------------------------------------------------------------*/

  private boolean debugModeValue;
  private MQTTClient MQTTClientUp;
  private MQTTClient MQTTClientHost;
  private Controller controllerImpl;
  private static final Logger logger = Logger.getLogger(ListenerRequest.class.getName());

  /**
   * Builder method.
   *
   * @param controllerImpl Controller - Controller that will make use of this Listener.
   * @param MQTTClientUp   MQTTClient - Upper Gateway MQTT Client.
   * @param MQTTClientHost   MQTTClient - Gateway's own MQTT client.
   * @param topics          String[] - Topics to be subscribed to.
   * @param qos            int - Quality of service of the topic that will be heard.
   * @param debugModeValue boolean - How to debug the code.
   */
  public ListenerRequest(
    Controller controllerImpl,
    MQTTClient MQTTClientUp,
    MQTTClient MQTTClientHost,
    String[] topics,
    int qos,
    boolean debugModeValue
  ) {
    this.MQTTClientUp = MQTTClientUp;
    this.MQTTClientHost = MQTTClientHost;
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
      case SENSORS_FOG:

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

          MQTTClientUp.publish(SENSORS_FOG_RES, payload, QOS);
        } else {
          byte[] payload = "Sorry, there are no devices connected!".getBytes();

          MQTTClientUp.publish(SENSORS_FOG_RES, payload, QOS);
        }

        break;
      case SENSORS_EDGE:

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

          MQTTClientHost.publish(SENSORS_EDGE_RES, payload, QOS);
        } else {
          byte[] payload = "Sorry, there are no devices connected!".getBytes();

          MQTTClientHost.publish(SENSORS_EDGE_RES, payload, QOS);
        }

        break;
      default:
        String responseMessage = String.format(
          "\nOops! the request isn't recognized...\nTry one of the options below:\n- %s\n",
          SENSORS_FOG
        );

        printlnDebug(responseMessage);

        break;
    }
  }

  private void printlnDebug(String str) {
    if (debugModeValue) {
      logger.info(str);
    }
  }
}
