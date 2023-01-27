package br.uefs.larsid.dlt.iot.soft.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import br.uefs.larsid.dlt.iot.soft.services.Controller;

public class ListenerCredentialDefinition implements IMqttMessageListener {

  /* -------------------------- Aries Topic Res constants ------------------ */
  private static final String CREDENTIAL_DEFINITIONS_RES = "CREDENTIAL_DEFINITIONS_RES";
  /* ----------------------------------------------------------------------- */

  private static final int QOS = 1;

  private boolean debugModeValue;
  private Controller controllerImpl;
  private MQTTClient MQTTClientHost;

  /**
   * Método Construtor.
   *
   * @param controllerImpl Controller - Controller que fará uso desse Listener.
   * @param MQTTClientHost MQTTClient - Cliente MQTT do gateway inferior.
   * @param topics         String[] - Tópicos que serão assinados.
   * @param qos            int - Qualidade de serviço do tópico que será ouvido.
   * @param debugModeValue boolean - Modo para debugar o código.
   */
  public ListenerCredentialDefinition(
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

    printlnDebug("==== Receive Crendential Definition Request ====");

    /* Verificar qual o tópico recebido. */
    switch (params[0]) {
      case CREDENTIAL_DEFINITIONS_RES:
        printlnDebug("CREDENTIAL_DEFINITIONS_RES...");

        printlnDebug("Crendential Definition configured!");

        this.controllerImpl.setCrendentialDefinitionIsConfigured(true);

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
