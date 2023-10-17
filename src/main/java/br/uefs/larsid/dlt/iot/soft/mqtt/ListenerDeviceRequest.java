package br.uefs.larsid.dlt.iot.soft.mqtt;

import br.uefs.larsid.dlt.iot.soft.services.Controller;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class ListenerDeviceRequest implements IMqttMessageListener {
  /*-------------------------Constants---------------------------------------*/
  private static final String AUTHENTICATED_DEVICES = "AUTHENTICATED_DEVICES";
  private static final String AUTHENTICATED_DEVICES_RES = "AUTHENTICATED_DEVICES_RES";

  private static final int TIMEOUT_IN_SECONDS = 30;
  
  private static final int QOS = 1;
  /*-------------------------------------------------------------------------*/

  private boolean debugModeValue;
  private MQTTClient MQTTClientHost;
  private Controller controllerImpl;
  private String nodeUri;
  private static final Logger logger = Logger.getLogger(ListenerDeviceRequest.class.getName());
  /**
   * Builder method.
   *
   * @param controllerImpl Controller - Controller that will make use of this Listener.
   * @param MQTTClientHost MQTTClient - Gateway's own MQTT client.
   * @param topics         String[] - Topics to be subscribed to.
   * @param qos            int - Quality of service of the topic that will be heard.
   * @param debugModeValue boolean - How to debug the code.
   */
  public ListenerDeviceRequest(
    Controller controllerImpl,
    MQTTClient MQTTClientHost,
    String nodeUri,
    String[] topics,
    int qos,
    boolean debugModeValue
  ) {
    this.MQTTClientHost = MQTTClientHost;
    this.controllerImpl = controllerImpl;
    this.debugModeValue = debugModeValue;
    this.nodeUri = nodeUri;

    for (String topic : topics) {
        this.MQTTClientHost.subscribe(qos, this, topic);
    }
  }

  @Override
  public void messageArrived(String topic, MqttMessage message)
    throws Exception {
    final String mqttMessage = new String(message.getPayload());

    switch (topic) {
        case AUTHENTICATED_DEVICES:
          printlnDebug("==== Fog gateway -> Bottom gateway  ====");
          printlnDebug("(Hyperledger Bundle) Requesting proof of credential for connected devices...\n");

          List<String> deviceIdsAuths = new ArrayList<>();
          JsonObject jsonGetTopKDown = new Gson()
            .fromJson(mqttMessage, JsonObject.class);

          try {
            /* Send request for presentation */
            for (Map.Entry<String, String> connectionIdDeviceNode : this.controllerImpl.getConnectionIdDeviceNodes().entrySet()) {
              String deviceId = connectionIdDeviceNode.getKey();

              printlnDebug("------------- Proof of Credential -------------");
              printlnDebug("Device ID: " + deviceId);
              printlnDebug("-----------------------------------------------\n");

              this.controllerImpl.sendRequestPresentationRequest(connectionIdDeviceNode.getValue());

              long start = System.currentTimeMillis();
              long end = start + TIMEOUT_IN_SECONDS * 1000;

              /*
                * Enquanto o dispositivo ainda não provou que possui uma 
                * credencial verificável e o tempo configurado não terminou.
                */
              while (
                !this.controllerImpl.isProofOfCredentialReceived() &&
                System.currentTimeMillis() < end
              ) {}

              this.controllerImpl.setProofOfCredentialReceived(false);

              deviceIdsAuths.add(deviceId);
            }
          } catch (Exception e) {
            printlnDebug("!Error when making proof requests for devices!");
          }


          printlnDebug(
            "(Hyperledger Bundle) Autheticated Devices = " + 
            deviceIdsAuths + 
            "\n"
          );

          try {
            jsonGetTopKDown.addProperty("authDevices", deviceIdsAuths.toString());
            this.MQTTClientHost.publish(AUTHENTICATED_DEVICES_RES, jsonGetTopKDown.toString().getBytes(), QOS);
          } catch (Exception e) {
            printlnDebug("!Error when publishing to send authenticated devices!");

            String user = "karaf";
            String password = "karaf";
            String uri[] = this.nodeUri.split(":");

            MQTTClient MQTTClient= new MQTTClient(
              this.debugModeValue,
              uri[0],
              uri[1],
              user,
              password
            );

            MQTTClient.connect();
            MQTTClient.publish(AUTHENTICATED_DEVICES_RES, deviceIdsAuths.toString().getBytes(), QOS);
            //MQTTClient.disconnect();
          }

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
