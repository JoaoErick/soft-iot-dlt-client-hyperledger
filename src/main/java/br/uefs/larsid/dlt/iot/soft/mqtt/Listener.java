package br.uefs.larsid.dlt.iot.soft.mqtt;

import java.util.Base64;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class Listener implements IMqttMessageListener {

  private static final int QOS = 1;

  /* -------------------------- Topic constants ---------------------------- */
  private static final String CREATE_INVITATION = "POST CREATE_INVITATION";
  private static final String ACCEPT_INVITATION = "POST ACCEPT_INVITATION";
  private static final String CREATE_CREDENTIAL_DEFINITIONS = "POST CREATE_CREDENTIAL_DEFINITIONS";
  private static final String ISSUE_CREDENTIAL = "POST ISSUE_CREDENTIAL";
  private static final String REQUEST_PROOF_CREDENTIAL = "POST REQUEST_PROOF_CREDENTIAL";
  /* ---------------------------------------------------------------------- */

  /*
   * -------------------------- Topic Res constants ----------------------------
   */
  private static final String CREATE_INVITATION_RES = "CREATE_INVITATION_RES";
  private static final String CREATE_CREDENTIAL_DEFINITIONS_RES = "CREATE_CREDENTIAL_DEFINITIONS_RES";
  private static final String ACCEPT_INVITATION_RES = "ACCEPT_INVITATION_RES";
  private static final String ISSUE_CREDENTIAL_RES = "ISSUE_CREDENTIAL_RES";
  private static final String REQUEST_PROOF_CREDENTIAL_RES = "REQUEST_PROOF_CREDENTIAL_RES";
  /* ---------------------------------------------------------------------- */

  private boolean debugModeValue;
  private MQTTClient mqttClient;

  /**
   * Método Construtor.
   *
   * @param mqttClient     MQTTClient - Cliente MQTT.
   * @param topics         String[] - Tópicos que serão assinados.
   * @param qos            int - Qualidade de serviço do tópico que será ouvido.
   * @param debugModeValue boolean - Modo para debugar o código.
   */
  public Listener(
      MQTTClient mqttClient,
      String[] topics,
      int qos,
      boolean debugModeValue) {
    this.mqttClient = mqttClient;
    this.debugModeValue = debugModeValue;

    for (String topic : topics) {
      this.mqttClient.subscribe(qos, this, topic);
    }
  }

  @Override
  public void messageArrived(String topic, MqttMessage message)
      throws Exception {
    String msg = new String(message.getPayload());

    printlnDebug("==== Receive Request ====");

    /* Verificar qual o tópico recebido. */
    switch (topic) {
      case CREATE_INVITATION:
        // JsonObject jsonResult = Main.createInvitation(msg);
        printlnDebug(">> Send Invitation URL...");

        // byte[] payload = jsonResult.toString().getBytes();
        // this.mqttClient.publish(CREATE_INVITATION_RES, payload, QOS);
        break;
      case ACCEPT_INVITATION: //Remover
        
        // JsonObject jsonInvitation = new Gson().fromJson(msg, JsonObject.class);
        // Main.receiveInvitation(jsonInvitation);

        // printlnDebug(">> Invitation Accepted!");
        // this.mqttClient.publish(ACCEPT_INVITATION_RES, "".getBytes(), QOS);

        break;
      case CREATE_CREDENTIAL_DEFINITIONS:
        printlnDebug("CREATE_CREDENTIAL_DEFINITIONS");
        // Main.createCredentialDefinition();

        // this.mqttClient.publish(CREATE_CREDENTIAL_DEFINITIONS_RES, "".getBytes(), QOS);

        break;
      case ISSUE_CREDENTIAL:
        // JsonObject jsonCredential = new Gson().fromJson(msg, JsonObject.class);
        // Main.issueCredentialV1(jsonCredential);

        // this.mqttClient.publish(ISSUE_CREDENTIAL_RES, "".getBytes(), QOS);

        break;
      case REQUEST_PROOF_CREDENTIAL:
        // JsonObject jsonRequestProof = new Gson().fromJson(msg, JsonObject.class);
        // Main.requestProofCredential(jsonRequestProof);

        // this.mqttClient.publish(REQUEST_PROOF_CREDENTIAL_RES, "".getBytes(), QOS);

        break;
      default:
        printlnDebug("Unrecognized topic!");
        break;
    }
  }

  private void printlnDebug(String str) {
    if (isDebugModeValue()) {
      System.out.println(str);
    }
  }

  public boolean isDebugModeValue() {
    return debugModeValue;
  }
}
