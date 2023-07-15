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
public class ListenerConnection implements IMqttMessageListener {

  /*------------------------------ Constants ------------------------------*/
  private static final String CONNECT = "SYN";
  private static final String DISCONNECT = "FIN";
  private static final int TIMEOUT_IN_SECONDS = 10;
  
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
  public ListenerConnection(
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
    final String[] params = topic.split("/");
    String msg = new String(message.getPayload());

    printlnDebug("\n==== Receive Connect Request ====");

    /* Check which topic was received. */
    switch (params[0]) {
      case CONNECT:
        printlnDebug("CONNECT...");
        JsonObject jsonProperties = new Gson().fromJson(msg, JsonObject.class);

        /* Add new URI gateway */
        String nodeUri = jsonProperties.get("nodeUri").getAsString();
        this.controllerImpl.addNodeUri(nodeUri);

        /* Receive Connection Invitation */
        jsonProperties.remove("nodeUri");

        this.controllerImpl.receiveInvitation(nodeUri, jsonProperties);
        printlnDebug("Invitation Accepted!");

        /* Get Connection Id */
        String connectionId = this.controllerImpl.getConnectionIdNodes().get(nodeUri);
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
        
        /* Create JSON to Issue Credential */
        JsonObject jsonIssueCredential = new JsonObject();
        jsonIssueCredential.addProperty("value", nodeUri);
        jsonIssueCredential.addProperty("connectionId", connectionId);

        /* Issue Credential */
        this.controllerImpl.issueCredentialV1(jsonIssueCredential);

        /* Waiting time for the credential to be received */
        long end = System.currentTimeMillis() + TIMEOUT_IN_SECONDS * 1000;

        while (System.currentTimeMillis() < end) {}

        /* Send request for presentation */
        this.controllerImpl.sendRequestPresentationRequest(connectionId);

        break;
      case DISCONNECT:
        printlnDebug("DISCONNECT...");
        this.controllerImpl.removeNodeUri(msg);

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
