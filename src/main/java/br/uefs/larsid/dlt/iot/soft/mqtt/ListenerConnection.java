package br.uefs.larsid.dlt.iot.soft.mqtt;

import br.uefs.larsid.dlt.iot.soft.services.Controller;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.zxing.WriterException;

public class ListenerConnection implements IMqttMessageListener {

  /*-------------------------Constantes--------------------------------------*/
  private static final String CONNECT = "SYN";
  private static final String DISCONNECT = "FIN";
  /*-------------------------------------------------------------------------*/

  /* -------------------------- Aries Topic constants ----------------------- */
  private static final String ACCEPT_INVITATION = "POST ACCEPT_INVITATION";
  /* ----------------------------------------------------------------------- */

  private static final int QOS = 1;

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

  // public void initialize(){
  // try {
  // controllerImpl.createInvitation(this.MQTTClientHost.getIp()+":"+this.MQTTClientHost.getPort());
  // } catch (IOException | WriterException e) {
  // // TODO Auto-generated catch block
  // e.printStackTrace();
  // }
  // }

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

        String nodeUri = jsonProperties.get("nodeUri").getAsString();
        String connectionId = jsonProperties.get("connectionId").getAsString();

        this.controllerImpl.addConnectionIdNodes(nodeUri, connectionId);

        this.controllerImpl.addNodeUri(nodeUri);

        jsonProperties.remove("nodeUri");
        jsonProperties.remove("connectionId");
        this.controllerImpl.receiveInvitation(jsonProperties);

        printlnDebug(">> Invitation Accepted!");
        // this.mqttClient.publish(ACCEPT_INVITATION_RES, "".getBytes(), QOS);

        // sendToControllerAries(ACCEPT_INVITATION, jsonProperties.toString());

        break;
      case DISCONNECT:
        printlnDebug("DISCONNECT...");
        this.controllerImpl.removeNodeUri(msg);

        break;
    }
  }

  private void sendToControllerAries(String topic, String message) {
    byte[] payload = message.getBytes();
    this.MQTTClientHost.publish(topic, payload, QOS);
  }

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
      System.out.println(str);
    }
  }

  public boolean isDebugModeValue() {
    return debugModeValue;
  }

  public void setDebugModeValue(boolean debugModeValue) {
    this.debugModeValue = debugModeValue;
  }
}
