package br.uefs.larsid.dlt.iot.soft.main;

import br.uefs.larsid.dlt.iot.soft.controller.AriesController;
import br.uefs.larsid.dlt.iot.soft.model.AttributeRestriction;
import br.uefs.larsid.dlt.iot.soft.model.Credential;
import br.uefs.larsid.dlt.iot.soft.model.CredentialDefinition;
import br.uefs.larsid.dlt.iot.soft.model.Invitation;
import br.uefs.larsid.dlt.iot.soft.model.Schema;
import br.uefs.larsid.dlt.iot.soft.mqtt.ListenerConnection;
import br.uefs.larsid.dlt.iot.soft.mqtt.MQTTClient;
import br.uefs.larsid.dlt.iot.soft.services.Controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.zxing.WriterException;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.connection.CreateInvitationResponse;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRecord;
import org.hyperledger.aries.api.present_proof.PresentationExchangeState;
import org.hyperledger.aries.api.schema.SchemaSendResponse;

public class ControllerImpl implements Controller {

  /*------------------------------- Constants -------------------------------*/
  private static final int QOS = 1;

  private static final String CONNECT = "SYN";
  private static final String DISCONNECT = "FIN";
  /*-------------------------------------------------------------------------*/

  /* ------------------------ Aries Topic constants ------------------------ */
  private String AGENT_ADDR;
  private String AGENT_PORT;
  /* ----------------------------------------------------------------------- */

  private MQTTClient MQTTClientUp;
  private MQTTClient MQTTClientHost;

  private boolean debugModeValue;
  private boolean crendentialDefinitionIsConfigured = false;
  private int timeoutInSeconds;

  private boolean hasNodes;
  private Map<String, String> connectionIdNodes = new LinkedHashMap<String, String>();
  private List<String> nodesUris;
  private JsonObject sensorsTypesJSON = new JsonObject();

  public static AriesController ariesController;
  public static Schema schema;
  public static CredentialDefinition credentialDefinition;

  private static Logger log = Logger.getLogger(ControllerImpl.class.getName());

  public ControllerImpl() {
  }

  /**
   * Initialize the bundle.
   */
  public void start() {
    this.MQTTClientUp.connect();
    this.MQTTClientHost.connect();

    printlnDebug("Start Hyperledger Aries bundle!");

    /* 
     * Configure Listener to receive connections from gateways 
     * present at the Edge.
    */
    if (hasNodes) {
      nodesUris = new ArrayList<>();
      String[] topicsConnection = { CONNECT, DISCONNECT };

      new ListenerConnection(
          this,
          MQTTClientHost,
          MQTTClientUp,
          topicsConnection,
          QOS,
          debugModeValue);
    }

    ariesController = new AriesController(AGENT_ADDR, AGENT_PORT);

    /* Configure Schema and Credential */
    try {
      printlnDebug("EndPoint: " + ariesController.getEndPoint());
      /* TODO: Criar uma classe para servir de base para implementação de 
      credenciais especificas */

      int idSchema = ariesController.getSchemasCreated().size() + 11; // precisa automatizar o número baseado na
                                                                      // persistencia
      int idTag = 1;

      List<String> attributes = new ArrayList<>();
      attributes.add("id");

      schema = new Schema(("Schema_" + idSchema), (idSchema++ + ".0"));
      schema.addAttributes(attributes);

      Boolean revocable = false;
      int revocableSize = 1000;
      credentialDefinition = new CredentialDefinition(("tag_" + idTag++), revocable, 1000, schema);

      Boolean autoRemove = false;
      Credential credential = new Credential(credentialDefinition, autoRemove);
      Map<String, String> values = new HashMap<>();
      values.put("id", "10.10.10.10");
      credential.addValues(values);

    } catch (IOException e) {
      e.printStackTrace();
    }

    /* Creating a proof request */
    String name = "Prove que é um gateway válido?";
    String comment = "É um gateway válido?";
    String version = "1.0";
    String nameAttrRestriction = "id";
    String nameRestriction = "cred_def_id";
    String propertyRestriction = "JU1jTydsRztc8XvjPHboAn:3:CL:63882:tag_1";

    AttributeRestriction attributeRestriction = new AttributeRestriction(nameAttrRestriction, nameRestriction,
        propertyRestriction);
    List<AttributeRestriction> attributesRestrictions = new ArrayList<>();
    attributesRestrictions.add(attributeRestriction);

    /*
     * Fog: Configures the credential definition.
     * Edge: Creates and Sends the connection JSON to the gateway
     * present in the Fog.
     */
    if (hasNodes) {
      try {
        this.createCredentialDefinition();
      } catch (IOException e) {
        printlnDebug("\n(!) Error to configure Crendential Definition\n");
        e.printStackTrace();
      }
      this.setCrendentialDefinitionIsConfigured(true);
    } else {
      try {
        String nodeUri = String
            .format("%s:%s", MQTTClientHost.getIp(), MQTTClientHost.getPort());
        this.sendJSONInvitation(nodeUri);
      } catch (IOException | WriterException e) {
        printlnDebug("\n(!) Error to create JSON Connection Invitation\n");
        e.printStackTrace();
      }
    }

  }

  /**
   * Stop bundle.
   */
  public void stop() {
    if (!this.hasNodes) {
      byte[] payload = String
          .format("%s:%s", MQTTClientHost.getIp(), MQTTClientHost.getPort())
          .getBytes();

      this.MQTTClientUp.publish(DISCONNECT, payload, QOS);

    } else {
      this.MQTTClientUp.unsubscribe(CONNECT);
      this.MQTTClientUp.unsubscribe(DISCONNECT);
    }

    this.MQTTClientHost.disconnect();
    this.MQTTClientUp.disconnect();
  }

  /* 
   * Sends the connection JSON to the gateway present in the Fog.
   */
  public void sendJSONInvitation(String nodeUri) throws IOException, WriterException {
    JsonObject jsonResult = this.createInvitation(nodeUri);

    printlnDebug("Send Invitation URL...");
    byte[] payload = jsonResult.toString().getBytes();
    this.MQTTClientUp.publish(CONNECT, payload, QOS);
  }

  /* 
   * Creating connection invitation.
   */
  public JsonObject createInvitation(String nodeUri) throws IOException, WriterException {
    int idConvite = ariesController.getConnections().size();
    return createInvitation(ariesController, ("Convite_" + idConvite++), nodeUri);
  }

  private JsonObject createInvitation(AriesController ariesController, String label, String nodeUri)
      throws IOException, WriterException {
    printlnDebug("Criando convite de conexao...");

    CreateInvitationResponse createInvitationResponse = ariesController.createInvitation(label);

    String json = ariesController.getJsonInvitation(createInvitationResponse);

    printlnDebug("Json Invitation: " + json);

    printlnDebug("Invitation created!");

    JsonObject jsonInvitation = new Gson().fromJson(json, JsonObject.class);

    jsonInvitation.addProperty("connectionId", createInvitationResponse.getConnectionId());
    jsonInvitation.addProperty("nodeUri", nodeUri);

    printlnDebug("Final JSON: " + jsonInvitation.toString() + "\n");

    return jsonInvitation;
  }

  /* 
   * Creating credential definition with attribute "id" that 
   * represents the IP address of the gateway.
   */
  public String createCredentialDefinition() throws IOException {
    return createCredentialDefinition(ariesController, schema, credentialDefinition);
  }

  private String createCredentialDefinition(AriesController ariesController, Schema schema,
      CredentialDefinition credentialDefinition) throws IOException {
    printlnDebug("Creating Schema...");

    SchemaSendResponse schemaSendResponse = ariesController.createSchema(schema);
    schema.setId(schemaSendResponse.getSchemaId());

    printlnDebug("Schema ID: " + schema.getId());

    printlnDebug("Schema Created!");

    printlnDebug("Creating Credential Definition ...");

    ariesController.createCredendentialDefinition(credentialDefinition);

    printlnDebug("Credential Definition ID: " + credentialDefinition.getId());

    printlnDebug("Crendential Definition configured!\n");

    return credentialDefinition.getId();
  }

  /* 
   * Issuing a credential to a connected gateway.
   */
  public void issueCredentialV1(JsonObject jsonProperties) throws IOException {
    issueCredentialV1(ariesController, jsonProperties);
  }

  private void issueCredentialV1(AriesController ariesController, JsonObject jsonProperties) throws IOException {
    String value = jsonProperties.get("value").getAsString();
    String connectionId = jsonProperties.get("connectionId").getAsString();

    // Collecting attribute values
    Map<String, String> values = new HashMap<>();
    values.put("id", value);

    // Creating credencial
    Boolean autoRemove = false;
    Credential credential = new Credential(credentialDefinition, autoRemove);
    credential.addValues(values);

    ConnectionRecord connectionRecord = ariesController.getConnection(connectionId);

    // Issuing credential
    printlnDebug("\nIssuing credential...");

    ariesController.issueCredentialV1(connectionRecord.getConnectionId(), credential);

    printlnDebug("Credential ID: " + credential.getId());

    printlnDebug("\nIssued Credential!\n");
  }

  private static void listConnections(AriesController ariesController) throws IOException {
    System.out.println("\nConsultando conexões ...");

    List<ConnectionRecord> connectionsRecords = ariesController.getConnections();

    System.out.println("\nListando conexões...");
    for (ConnectionRecord connectionRecord : connectionsRecords) {
      System.out.println("\nConexão ID: " + connectionRecord.getConnectionId());
      System.out.println("State: " + connectionRecord.getState());
      System.out.println("RFC State: " + connectionRecord.getRfc23Sate());
      System.out.println("Alias: " + connectionRecord.getAlias());
      System.out.println("Invitation Key: " + connectionRecord.getInvitationKey());
      System.out.println("Their Label: " + connectionRecord.getTheirLabel());
      System.out.println("Their DID: " + connectionRecord.getTheirDid());
      System.out.println("Created At: " + connectionRecord.getCreatedAt());
      System.out.println("Updated At: " + connectionRecord.getUpdatedAt());
      System.out.println("Msg error: " + connectionRecord.getErrorMsg());
    }

    System.out.println("\nFim da lista de conexões!\n");
  }

  /* 
   * Send presentation Request to gateways.
   */
  public void sendRequestPresentationRequest(String connectionId) throws IOException, InterruptedException {
    sendRequestPresentationRequest(ariesController, connectionId);
  }

  private void sendRequestPresentationRequest(AriesController ariesController, String connectionId) throws IOException, InterruptedException {
    
    /* Creating a proof request */
    String name = "Prove que é um gateway válido?";
    String comment = "Essa é uma verificação de prova do larsid";
    String version = "1.0";
    String nameAttrRestriction = "";
    String nameRestriction = "cred_def_id";
    String propertyRestriction = "";

    nameAttrRestriction = credentialDefinition.getSchema().getAttributes().get(0);
    propertyRestriction = credentialDefinition.getId();

    AttributeRestriction attributeRestriction = new AttributeRestriction(nameAttrRestriction, nameRestriction, propertyRestriction);
    List<AttributeRestriction> attributesRestrictions = new ArrayList<>();
    attributesRestrictions.add(attributeRestriction);

    ConnectionRecord connectionRecord = ariesController.getConnection(connectionId);

    /* Start timestamp of test request */
    Timestamp timeSend = new Timestamp(System.currentTimeMillis());

    String presentationExchangeId =
    ariesController.sendRequestPresentationRequest(name, comment, version, connectionRecord.getConnectionId(), attributesRestrictions);

    printlnDebug("\nSubmitting proof request...");

    PresentationExchangeRecord presentationExchangeRecord;

    do {
      presentationExchangeRecord = ariesController.getPresentation(presentationExchangeId);
      printlnDebug("\nUpdateAt: " + presentationExchangeRecord.getUpdatedAt());
      printlnDebug("Presentation: " + presentationExchangeRecord.getPresentation());
      printlnDebug("Verified: " + presentationExchangeRecord.isVerified());
      printlnDebug("State: " + presentationExchangeRecord.getState());
      printlnDebug("Auto Presentation: " + presentationExchangeRecord.getAutoPresent());
    } while
    (!presentationExchangeRecord.getState().equals(PresentationExchangeState.REQUEST_RECEIVED)
    &&
    !presentationExchangeRecord.getState().equals(PresentationExchangeState.VERIFIED));

    printlnDebug("\nProof Request Received!\n");

    verifyProofPresentation(ariesController, presentationExchangeId);

    Timestamp timeReceive = new Timestamp(System.currentTimeMillis());
    printlnDebug("\nCalculate timestamp...");
    printlnDebug("Initial Time: " + timeSend);
    printlnDebug("Final Time: " + timeReceive);
    printlnDebug("Difference: " + (timeReceive.getTime() -
    timeSend.getTime()));
  }

  private void verifyProofPresentation(AriesController ariesController, String presentationExchangeId) throws IOException, InterruptedException {
    printlnDebug("\nChecking Proof Request...");

    if (ariesController.getPresentation(presentationExchangeId).getVerified()) {
      printlnDebug("\nCredential Verified!\n");
    } else {
      System.err.println("\nUnverified Credential!\n");
    }
  }

  /* 
   * Receiving connection invitation from another aries agent.
   */
  public void receiveInvitation(JsonObject invitationJson) throws IOException {
    receiveInvitation(ariesController, invitationJson);
  }

  private void receiveInvitation(AriesController ariesController, JsonObject invitationJson) throws IOException {
    Invitation invitationObj = new Invitation(invitationJson);

    printlnDebug("\nReceiving Connection Invitation...");

    ConnectionRecord connectionRecord = ariesController.receiveInvitation(invitationObj);

    printlnDebug("\nConnection:\n" + connectionRecord.toString());
  }

  /**
   * Adds a URI to the URI list.
   *
   * @param uri String - URI you want to add.
   */
  @Override
  public void addNodeUri(String uri) {
    if (!this.nodesUris.contains(uri)) {
      this.nodesUris.add(uri);
    }

    printlnDebug(String.format("URI: %s added in the nodesIps list.", uri));
    this.showNodesConnected();
  }

  /**
   * Removes a URI from the URI list.
   *
   * @param uri String - URI you want to remove.
   */
  @Override
  public void removeNodeUri(String uri) {
    int pos = this.findNodeUri(uri);

    if (pos != -1) {
      this.nodesUris.remove(pos);

      printlnDebug(String.format("URI: %s removed in the nodesIps list.", uri));

      this.showNodesConnected();
    } else {
      printlnDebug("Error, the desired node was not found.");
    }
  }

  /**
   * Returns the position of a URI in the list of URIs.
   *
   * @param uri String - URI you want the position.
   * @return int
   */
  private int findNodeUri(String uri) {
    for (int pos = 0; pos < this.nodesUris.size(); pos++) {
      if (this.nodesUris.get(pos).equals(uri)) {
        return pos;
      }
    }

    return -1;
  }

  /**
   * Returns the list of URIs of connected nodes.
   *
   * @return List
   */
  @Override
  public List<String> getNodeUriList() {
    return this.nodesUris;
  }

  /**
   * Returns the number of connected nodes.
   *
   * @return String
   */
  @Override
  public int getNodes() {
    return this.nodesUris.size();
  }

  /**
   * Displays the URI of nodes that are connected.
   */
  public void showNodesConnected() {
    if (this.getNodeUriList().size() == 0) {
      printlnDebug(
        "\n+---- Nodes URI Connected ----+\n" + 
        "        empty" + 
        "\n+----------------------------+\n"
      );
    } else {
      printlnDebug(
        "\n+---- Nodes URI Connected ----+\n" + 
        this.getNodeUriList() + 
        "\n+----------------------------+\n"
      );
    }
  }

  private void printlnDebug(String str) {
    if (debugModeValue) {
      log.info(str);
    }
  }

  public boolean isDebugModeValue() {
    return this.debugModeValue;
  }

  public void setDebugModeValue(boolean debugModeValue) {
    this.debugModeValue = debugModeValue;
  }

  public MQTTClient getMQTTClientUp() {
    return this.MQTTClientUp;
  }

  public void setMQTTClientUp(MQTTClient MQTTClientUp) {
    this.MQTTClientUp = MQTTClientUp;
  }

  public MQTTClient getMQTTClientHost() {
    return this.MQTTClientHost;
  }

  public void setMQTTClientHost(MQTTClient mQTTClientHost) {
    this.MQTTClientHost = mQTTClientHost;
  }

  public List<String> getNodesUris() {
    return nodesUris;
  }

  public void setNodesUris(List<String> nodesUris) {
    this.nodesUris = nodesUris;
  }

  /**
   * Checks if the gateway has children.
   *
   * @return boolean
   */
  @Override
  public boolean hasNodes() {
    return hasNodes;
  }

  public void setHasNodes(boolean hasNodes) {
    this.hasNodes = hasNodes;
  }

  public boolean crendentialDefinitionIsConfigured() {
    return crendentialDefinitionIsConfigured;
  }

  public void setCrendentialDefinitionIsConfigured(boolean crendentialDefinitionIsConfigured) {
    this.crendentialDefinitionIsConfigured = crendentialDefinitionIsConfigured;
  }

  public Map<String, String> getConnectionIdNodes() {
    return connectionIdNodes;
  }

  public void addConnectionIdNodes(String nodeUri, String connectionId) {
    this.connectionIdNodes.put(nodeUri, connectionId);
  }

  public int getTimeoutInSeconds() {
    return timeoutInSeconds;
  }

  public void setTimeoutInSeconds(int timeoutInSeconds) {
    this.timeoutInSeconds = timeoutInSeconds;
  }

  public String getAGENT_ADDR() {
    return AGENT_ADDR;
  }

  public void setAGENT_ADDR(String aGENT_ADDR) {
    AGENT_ADDR = aGENT_ADDR;
  }

  public String getAGENT_PORT() {
    return AGENT_PORT;
  }

  public void setAGENT_PORT(String aGENT_PORT) {
    AGENT_PORT = aGENT_PORT;
  }
}
