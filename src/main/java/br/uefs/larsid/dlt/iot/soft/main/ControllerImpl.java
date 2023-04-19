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

      int idSchema = ariesController.getSchemasCreated().size() + 11; // precisa automatizar o número baseado na
                                                                      // persistencia
      int idTag = 1;

      List<String> attributes = new ArrayList<>();
      attributes.add("idNode");

      /* Creating a schema */
      schema = new Schema(("Schema_" + idSchema), (idSchema++ + ".0"));
      schema.addAttributes(attributes);

      /* Creating a credential definition */
      Boolean revocable = false;
      credentialDefinition = new CredentialDefinition(("tag_" + idTag++), revocable, 1000, schema);
    } catch (IOException e) {
      e.printStackTrace();
    }

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

  /**
   * Sends the connection JSON to the gateway present in the Fog.
   * 
   * @param nodeUri - URI of the node want to connect.
   * @throws IOException
   * @throws WriterException
    */
  public void sendJSONInvitation(String nodeUri) throws IOException, WriterException {
    JsonObject jsonResult = this.createInvitation(nodeUri);

    printlnDebug("Send Invitation URL...");
    byte[] payload = jsonResult.toString().getBytes();
    this.MQTTClientUp.publish(CONNECT, payload, QOS);
  }

  public JsonObject createInvitation(String nodeUri) throws IOException, WriterException {
    int idConvite = ariesController.getConnections().size();
    return createInvitation(ariesController, ("Convite_" + idConvite++), nodeUri);
  }

  /**
   * Creating connection invitation.
   * 
   * @param ariesController - Aries controller with agent interaction methods.
   * @param label - Connection invite label.
   * @param nodeUri - URI of the node want to connect.
   * @return JsonObject
   * @throws IOException
   * @throws WriterException
    */
  private JsonObject createInvitation(AriesController ariesController, String label, String nodeUri)
      throws IOException, WriterException {
    printlnDebug("Criando convite de conexao...");

    CreateInvitationResponse createInvitationResponse = ariesController.createInvitation(label);

    String json = ariesController.getJsonInvitation(createInvitationResponse);

    printlnDebug("Json Invitation: " + json);

    printlnDebug("Invitation created!");

    JsonObject jsonInvitation = new Gson().fromJson(json, JsonObject.class);

    jsonInvitation.addProperty("nodeUri", nodeUri);

    printlnDebug("Final JSON: " + jsonInvitation.toString() + "\n");

    return jsonInvitation;
  }

  public String createCredentialDefinition() throws IOException {
    return createCredentialDefinition(ariesController, schema, credentialDefinition);
  }

  /**
   * Creating credential definition with attribute "idNode" that 
   * represents the IP address of the gateway.
   * 
   * @param ariesController - Aries controller with agent interaction methods.
   * @param schema - Schema to be created.
   * @param credentialDefinition - Credential Definition to be created.
   * @return String
   * @throws IOException
    */
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

  public void issueCredentialV1(JsonObject jsonProperties) throws IOException {
    issueCredentialV1(ariesController, jsonProperties);
  }

  /**
   * Issuing a credential to a connected gateway.
   * 
   * @param ariesController - Aries controller with agent interaction methods.
   * @param jsonProperties - JSON with the properties for sending the credential.
   * @throws IOException
    */
  private void issueCredentialV1(AriesController ariesController, JsonObject jsonProperties) throws IOException {
    String value = jsonProperties.get("value").getAsString();
    String connectionId = jsonProperties.get("connectionId").getAsString();

    CredentialDefinition credentialDef = ariesController.getCredentialDefinitionById(
      ariesController.getCredentialDefinitionsCreated().getCredentialDefinitionIds().get(0)); //TODO: colocar dinâmico

    // Collect values of attributes
    Map<String, String> values = new HashMap<>();

    for (String attr : credentialDefinition.getSchema().getAttributes()) {
        values.put(attr, value);
    }

    // Creating credencial
    Boolean autoRemove = false;
    Credential credential = new Credential(credentialDef, autoRemove);
    credential.addValues(values);

    // Issuing credential
    printlnDebug("Issuing credential...");
    printlnDebug("Connection Id: " + connectionId);
    printlnDebug("Value: " + value);

    ariesController.issueCredentialV1(connectionId, credential);

    printlnDebug("Issued Credential!\n");
  }

  /**
   * Makes the list of agent connections.
   * 
   * @param ariesController - Aries controller with agent interaction methods.
   * @throws IOException
    */
  private void listConnections(AriesController ariesController) throws IOException {
   printlnDebug("Consultando conexões ...");

    List<ConnectionRecord> connectionsRecords = ariesController.getConnections();

   printlnDebug("Listando conexões...");
    for (ConnectionRecord connectionRecord : connectionsRecords) {
     printlnDebug("\nConexão ID: " + connectionRecord.getConnectionId());
     printlnDebug("State: " + connectionRecord.getState());
     printlnDebug("RFC State: " + connectionRecord.getRfc23Sate());
     printlnDebug("Alias: " + connectionRecord.getAlias());
     printlnDebug("Invitation Key: " + connectionRecord.getInvitationKey());
     printlnDebug("Their Label: " + connectionRecord.getTheirLabel());
     printlnDebug("Their DID: " + connectionRecord.getTheirDid());
     printlnDebug("Created At: " + connectionRecord.getCreatedAt());
     printlnDebug("Updated At: " + connectionRecord.getUpdatedAt());
     printlnDebug("Msg error: " + connectionRecord.getErrorMsg());
    }

   printlnDebug("\nFim da lista de conexões!\n");
  }

  public void sendRequestPresentationRequest(String connectionId) throws IOException, InterruptedException {
    sendRequestPresentationRequest(ariesController, connectionId);
  }

  /**
   * Send presentation Request to gateways.
   * 
   * @param ariesController - Aries controller with agent interaction methods.
   * @param connectionId - Connection id to send the request
   * @throws IOException
   * @throws InterruptedException
    */
  private void sendRequestPresentationRequest(AriesController ariesController, String connectionId) throws IOException, InterruptedException {
    
    /* Creating a proof request */
    String name = "Prove que é um gateway válido?";
    String comment = "Essa é uma verificação de prova do larsid";
    String version = "1.0";
    String nameAttrRestriction = "";
    String nameRestriction = "cred_def_id";
    String propertyRestriction = "";

    CredentialDefinition credentialDef = ariesController.getCredentialDefinitionById(
      ariesController.getCredentialDefinitionsCreated().getCredentialDefinitionIds().get(0));
    nameAttrRestriction = credentialDef.getSchema().getAttributes().get(0);
    propertyRestriction = credentialDef.getId();

    AttributeRestriction attributeRestriction = new AttributeRestriction(nameAttrRestriction, nameRestriction, propertyRestriction);
    List<AttributeRestriction> attributesRestrictions = new ArrayList<>();
    attributesRestrictions.add(attributeRestriction);

    /* Start timestamp of test request */
    Timestamp timeSend = new Timestamp(System.currentTimeMillis());

    String presentationExchangeId =
    ariesController.sendRequestPresentationRequest(name, comment, version, connectionId, attributesRestrictions);

    printlnDebug("Submitting proof request...");

    PresentationExchangeRecord presentationExchangeRecord;

    do {
      presentationExchangeRecord = ariesController.getPresentation(presentationExchangeId);
      printlnDebug("UpdateAt: " + presentationExchangeRecord.getUpdatedAt());
      printlnDebug("Presentation: " + presentationExchangeRecord.getPresentation());
      printlnDebug("Verified: " + presentationExchangeRecord.isVerified());
      printlnDebug("State: " + presentationExchangeRecord.getState());
      printlnDebug("Auto Presentation: " + presentationExchangeRecord.getAutoPresent()+ "\n");
    } while
    (!presentationExchangeRecord.getState().equals(PresentationExchangeState.REQUEST_RECEIVED)
    &&
    !presentationExchangeRecord.getState().equals(PresentationExchangeState.VERIFIED));

    printlnDebug("Proof Request Received!\n");

    verifyProofPresentation(ariesController, presentationExchangeId);

    Timestamp timeReceive = new Timestamp(System.currentTimeMillis());
    printlnDebug("Calculate timestamp...");
    printlnDebug("Initial Time: " + timeSend);
    printlnDebug("Final Time: " + timeReceive);
    printlnDebug("Difference: " + (timeReceive.getTime() - timeSend.getTime()));
  }

  /**
   * Checking proof presentation.
   * 
   * @param ariesController - Aries controller with agent interaction methods.
   * @param presentationExchangeId - Id of the Presentation Exchange that was performed.
   * @throws IOException
   * @throws InterruptedException
    */
  private void verifyProofPresentation(AriesController ariesController, String presentationExchangeId) throws IOException, InterruptedException {
    printlnDebug("Checking Proof Request...");

    if (ariesController.getPresentation(presentationExchangeId).getVerified()) {
      printlnDebug("Credential Verified!\n");
    } else {
      System.err.println("Unverified Credential!\n");
    }
  }

  public void receiveInvitation(String nodeUri, JsonObject invitationJson) throws IOException {
    receiveInvitation(ariesController, nodeUri, invitationJson);
  }

  /**
   * Receiving connection invitation from another aries agent.
   * 
   * @param ariesController - Aries controller with agent interaction methods.
   * @param nodeUri - URI of the node want to connect.
   * @param invitationJson - JSON with connection properties.
   * @throws IOException
    */
  private void receiveInvitation(AriesController ariesController, String nodeUri, JsonObject invitationJson) throws IOException {
    Invitation invitationObj = new Invitation(invitationJson);

    printlnDebug("Receiving Connection Invitation...");

    ConnectionRecord connectionRecord = ariesController.receiveInvitation(invitationObj);

    /* Add new Connection Id of URI gateway */
    this.addConnectionIdNodes(nodeUri, connectionRecord.getConnectionId());

    printlnDebug("\n\nConnection:\n" + connectionRecord.toString() + "\n");
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
        "\n\n+---- Nodes URI Connected ----+\n" + 
        "        empty" + 
        "\n+----------------------------+\n"
      );
    } else {
      printlnDebug(
        "\n\n+---- Nodes URI Connected ----+\n" + 
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
