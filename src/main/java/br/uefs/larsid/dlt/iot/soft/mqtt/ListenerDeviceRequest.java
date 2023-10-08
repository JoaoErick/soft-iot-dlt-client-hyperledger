package br.uefs.larsid.dlt.iot.soft.mqtt;

import br.uefs.larsid.dlt.iot.soft.services.Controller;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class ListenerDeviceRequest implements IMqttMessageListener {
  /*-------------------------Constants---------------------------------------*/
  private static final String AUTHENTICATED_DEVICES = "AUTHENTICATED_DEVICES";
  private static final String AUTHENTICATED_DEVICES_RES = "AUTHENTICATED_DEVICES_RES";
  
  private static final int QOS = 1;
  /*-------------------------------------------------------------------------*/

  private boolean debugModeValue;
  private MQTTClient MQTTClientUp;
  private MQTTClient MQTTClientHost;
  private Controller controllerImpl;
  private static final Logger logger = Logger.getLogger(ListenerDeviceRequest.class.getName());
  /**
   * Builder method.
   *
   * @param controllerImpl Controller - Controller that will make use of this Listener.
   * @param MQTTClientUp   MQTTClient - Upper Gateway MQTT Client.
   * @param MQTTClientHost MQTTClient - Gateway's own MQTT client.
   * @param topics         String[] - Topics to be subscribed to.
   * @param qos            int - Quality of service of the topic that will be heard.
   * @param debugModeValue boolean - How to debug the code.
   */
  public ListenerDeviceRequest(
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

    for (String topic : topics) {
        this.MQTTClientHost.subscribe(qos, this, topic);
    }
  }

  @Override
  public void messageArrived(String topic, MqttMessage message)
    throws Exception {

    switch (topic) {
        case AUTHENTICATED_DEVICES:

          printlnDebug("Requesting proof of credential for connected devices...");

          List<String> deviceIdsAuths = new ArrayList<>();

          /* Send request for presentation */
          for (Map.Entry<String, String> connectionIdDeviceNode : this.controllerImpl.getConnectionIdDeviceNodes().entrySet()) {
            String deviceId = connectionIdDeviceNode.getKey();
            printlnDebug("Device ID: " + deviceId);
            this.controllerImpl.sendRequestPresentationRequest(connectionIdDeviceNode.getValue());
            deviceIdsAuths.add(deviceId);
          }

          printlnDebug("Autheticated Devices: " + deviceIdsAuths);
          MQTTClientHost.publish(AUTHENTICATED_DEVICES_RES, deviceIdsAuths.toString().getBytes(), QOS);

          break;
        default:
          String responseMessage = String.format(
            "\nOops! the request isn't recognized...\nTry one of the options below:\n- %s\n",
            AUTHENTICATED_DEVICES
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
