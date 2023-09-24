package br.uefs.larsid.dlt.iot.soft.mqtt;

import br.uefs.larsid.dlt.iot.soft.main.ControllerImpl;
import br.uefs.larsid.dlt.iot.soft.services.Controller;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.logging.Logger;

/**
 *
 * @author João Erick Barbosa
 */
public class ListenerDeviceConnection implements IMqttMessageListener {

  /*------------------------------ Constants ------------------------------*/
  private static final String DEV_CONNECTIONS = "dev/CONNECTIONS";
  
  private static Logger log = Logger.getLogger(ListenerConnection.class.getName());
  /*-------------------------------------------------------------------------*/

  private boolean debugModeValue;
  private Controller controllerImpl;
  private MQTTClient MQTTClientHost;

  /**
   * Constructor Method.
   *
   * @param controllerImpl Controller - Controller that will make use of this Listener.
   * @param MQTTClientHost MQTTClient - Bottom Gateway MQTT Client.
   * @param topics         String[] - Topics that will be subscribed.
   * @param qos            int - Quality of service of the topic that will be heard.
   * @param debugModeValue boolean - Mode to debug the code.
   */
  public ListenerDeviceConnection(
      Controller controllerImpl,
      MQTTClient MQTTClientHost,
      String[] topics,
      int qos,
      boolean debugModeValue) {
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
    String msg = new String(message.getPayload());

    /* Check which topic was received. */
    switch (topic) {
      case DEV_CONNECTIONS:
        printlnDebug("CONNECT DEVICE...");
        String deviceIp = "";
        String connectionId = "";

        try {
          
          String jsonDevice = msg.replace("CONNECT VALUE BROKER ", "");
          
          printlnDebug(jsonDevice);
  
          JsonObject jsonProperties = new Gson().fromJson(jsonDevice, JsonObject.class);
  
          /* Extract data */
          JsonObject jsonInvitation = jsonProperties.get("ARIES_CONNECTION").getAsJsonObject();
          deviceIp = jsonProperties.get("HEADER").getAsJsonObject().get("SOURCE_IP").getAsString();
  
          connectionId = this.controllerImpl.receiveDeviceInvitation(jsonInvitation);
          printlnDebug("Invitation Accepted!");
  
          printlnDebug("Received Connection Id: " + connectionId);

          /* Ensuring that connection between agents has been completed */
          String state;
          boolean flag = false;
          printlnDebug("Waiting for the connection to become active...");
          while (flag == false) {
            state = ControllerImpl.ariesController.getConnection(connectionId).getState().toString();
            if (state.equals("ACTIVE")) {
              flag = true;
            }
          }
          printlnDebug("Connection established!");
        } catch (Exception e) {
          printlnDebug("[!Error receiving connection from new device!]");
          e.printStackTrace();
        }

        if (!deviceIp.isEmpty() && !connectionId.isEmpty()) {
          try {
            
            /* Create JSON to Issue Credential */
            JsonObject jsonIssueCredential = new JsonObject();
            jsonIssueCredential.addProperty("value", deviceIp);
            jsonIssueCredential.addProperty("connectionId", connectionId);
    
            /* Issue Credential */
            this.controllerImpl.issueCredentialV1(jsonIssueCredential);
          } catch (Exception e) {
            printlnDebug("[!Error when trying to issue the credential for the new device!]");
            e.printStackTrace();
          }
        }

        break;
    }
  }

  private void printlnDebug(String str) {
    if (isDebugModeValue()) {
      log.info(str);
    }
  }

  public boolean isDebugModeValue() {
    return debugModeValue;
  }

  public void setDebugModeValue(boolean debugModeValue) {
    this.debugModeValue = debugModeValue;
  }
}
