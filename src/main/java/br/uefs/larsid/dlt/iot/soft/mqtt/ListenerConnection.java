package br.uefs.larsid.dlt.iot.soft.mqtt;

import br.uefs.larsid.dlt.iot.soft.services.Controller;

import java.util.List;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.logging.Logger;

public class ListenerConnection implements IMqttMessageListener {

  /*------------------------------ Constantes ------------------------------*/
  private static final String CONNECT = "SYN";
  private static final String DISCONNECT = "FIN";
  private static final int TIMEOUT_IN_SECONDS = 10;
  private static final int QOS = 1;
  private static Logger log = Logger.getLogger(ListenerConnection.class.getName());
  /*-------------------------------------------------------------------------*/

  private boolean debugModeValue;
  private Controller controllerImpl;
  private MQTTClient MQTTClientHost;
  private MQTTClient MQTTClientUp;

  /**
   * Método Construtor.
   *
   * @param controllerImpl Controller - Controller que fará uso desse Listener.
   * @param MQTTClientHost MQTTClient - Cliente MQTT do gateway inferior.
   * @param MQTTClientUp   MQTTClient - Cliente MQTT do gateway superior.
   * @param topics         String[] - Tópicos que serão assinados.
   * @param qos            int - Qualidade de serviço do tópico que será ouvido.
   * @param debugModeValue boolean - Modo para debugar o código.
   */
  public ListenerConnection(
      Controller controllerImpl,
      MQTTClient MQTTClientHost,
      MQTTClient MQTTClientUp,
      String[] topics,
      int qos,
      boolean debugModeValue) {
    this.controllerImpl = controllerImpl;
    this.MQTTClientHost = MQTTClientHost;
    this.MQTTClientUp = MQTTClientUp;
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

    printlnDebug("==== Receive Connect Request ====");

    /* Verificar qual o tópico recebido. */
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

  /* Envia uma mensagem para um tópico especificado em que os nós inferiores estão assinados. */
  private void publishToDown(String topicDown, byte[] messageDown) {
    List<String> nodesUris = this.controllerImpl.getNodeUriList();
    String user = this.MQTTClientUp.getUserName();
    String password = this.MQTTClientUp.getPassword();

    String uri[] = nodesUris.get(nodesUris.size() - 1).split(":");

    MQTTClient MQTTClientDown = new MQTTClient(
        this.debugModeValue,
        uri[0],
        uri[1],
        user,
        password);

    MQTTClientDown.connect();
    MQTTClientDown.publish(topicDown, messageDown, QOS);
    MQTTClientDown.disconnect();
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
