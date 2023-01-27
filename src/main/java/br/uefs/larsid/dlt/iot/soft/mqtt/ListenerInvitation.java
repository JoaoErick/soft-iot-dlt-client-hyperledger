package br.uefs.larsid.dlt.iot.soft.mqtt;

import java.net.InetAddress;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import br.uefs.larsid.dlt.iot.soft.services.Controller;

public class ListenerInvitation implements IMqttMessageListener {

  /* -------------------------- Aries Topic constants ----------------------- */
  private static final String ISSUE_CREDENTIAL = "POST ISSUE_CREDENTIAL";
  /* ----------------------------------------------------------------------- */

  /* -------------------------- Aries Topic Res constants ------------------ */
  private static final String CREATE_INVITATION_RES = "CREATE_INVITATION_RES";
  private static final String ACCEPT_INVITATION_RES = "ACCEPT_INVITATION_RES";
  /* ----------------------------------------------------------------------- */

  /* -------------------------- Nodes Topic constants ----------------------- */
  private static final String CONNECT = "SYN";
  /* ----------------------------------------------------------------------- */

  private static final int QOS = 1;
  private static final int TIMEOUT_IN_SECONDS = 25;

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
  public ListenerInvitation(
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

    printlnDebug("==== Receive Invitation Request ====");

    /* Verificar qual o tópico recebido. */
    switch (params[0]) {

      case CREATE_INVITATION_RES:
        printlnDebug("CREATE_INVITATION_RES...");
        printlnDebug(msg);

        JsonObject jsonProperties = new Gson().fromJson(msg, JsonObject.class);

        this.MQTTClientUp.publish(CONNECT, jsonProperties.toString().getBytes(), QOS);

        break;

      case ACCEPT_INVITATION_RES:
        printlnDebug("ACCEPT_INVITATION_RES...");

        String json = "{" +
            "\"value\":\"" + msg.split(":")[0] + "\"," +
            "\"connectionId\":\"" + this.controllerImpl.getConnectionIdNodes().get(msg) + "\"" +
        "}";

        printlnDebug("\nReceived connection id: " + this.controllerImpl.getConnectionIdNodes().get(msg));

        long start = System.currentTimeMillis();
        long end = start + TIMEOUT_IN_SECONDS * 1000;

        /*
         * Aguarda um tempo necessário para que a conexão entre os agentes
         * esteja realmente pronta.
         */
        while (System.currentTimeMillis() < end) {
        }

        printlnDebug("\nJSON of credential issuance: ");
        printlnDebug(json);

        sendToControllerAries(ISSUE_CREDENTIAL, json);

        break;
    }
  }

  private void sendToControllerAries(String topic, String message) {
    byte[] payload = message.getBytes();
    this.MQTTClientHost.publish(topic, payload, QOS);
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
