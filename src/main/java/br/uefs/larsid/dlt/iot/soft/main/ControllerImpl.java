package br.uefs.larsid.dlt.iot.soft.main;

import br.uefs.larsid.dlt.iot.soft.controller.AriesController;
import br.uefs.larsid.dlt.iot.soft.model.AttributeRestriction;
import br.uefs.larsid.dlt.iot.soft.model.Credential;
import br.uefs.larsid.dlt.iot.soft.model.CredentialDefinition;
import br.uefs.larsid.dlt.iot.soft.model.Device;
import br.uefs.larsid.dlt.iot.soft.model.Invitation;
import br.uefs.larsid.dlt.iot.soft.model.Schema;
import br.uefs.larsid.dlt.iot.soft.model.Sensor;
import br.uefs.larsid.dlt.iot.soft.mqtt.ListenerConnection;
import br.uefs.larsid.dlt.iot.soft.mqtt.ListenerDeviceConnection;
import br.uefs.larsid.dlt.iot.soft.mqtt.ListenerDeviceRequest;
import br.uefs.larsid.dlt.iot.soft.mqtt.ListenerRequest;
import br.uefs.larsid.dlt.iot.soft.mqtt.ListenerResponse;
import br.uefs.larsid.dlt.iot.soft.mqtt.MQTTClient;
import br.uefs.larsid.dlt.iot.soft.services.Controller;
import br.uefs.larsid.dlt.iot.soft.utils.ClientIoTService;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.logging.Level;

import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.connection.CreateInvitationResponse;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRecord;
import org.hyperledger.aries.api.present_proof.PresentationExchangeState;
import org.hyperledger.aries.api.schema.SchemaSendResponse;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author João Erick Barbosa
 */
public class ControllerImpl implements Controller {

  /*------------------------------- Constants -------------------------------*/
  private static final int QOS = 1;

  private static final String CONNECT = "SYN_IDENTITY";
  private static final String DISCONNECT = "FIN_IDENTITY";

  private static final String DEV_CONNECTIONS = "dev/CONNECTIONS";

  private static final String GET_N_DEVICES = "GET_N_DEVICES";
  private static final String N_DEVICES_RES = "N_DEVICES_RES";
  private static final String N_DEVICES_EDGE = "N_DEVICES_EDGE";
  private static final String N_DEVICES_EDGE_RES = "N_DEVICES_EDGE_RES";

  private static final String GET_SENSORS = "GET_SENSORS";
  private static final String SENSORS_RES = "SENSORS_RES";
  private static final String SENSORS_EDGE = "SENSORS_EDGE";
  private static final String SENSORS_EDGE_RES = "SENSORS_EDGE_RES";

  private static final String AUTHENTICATED_DEVICES = "AUTHENTICATED_DEVICES";
  /*-------------------------------------------------------------------------*/

  /* ------------------------ Aries constants ------------------------ */
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
  private Map<String, String> connectionIdDeviceNodes = new LinkedHashMap<String, String>();
  private List<String> nodesUris;
  private List<Device> devices;
  private List<String> numberDevicesConnectedNodes;

  private Map<String, Integer> responseQueue = new LinkedHashMap<String, Integer>();
  private JsonObject sensorsTypesJSON = new JsonObject();
  private String urlAPI;

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
     * Configure Listeners MQTT.
    */
    String[] topicsDeviceConnection = { DEV_CONNECTIONS };
    if (hasNodes) {
      nodesUris = new ArrayList<>();
      numberDevicesConnectedNodes = new ArrayList<>();
      String[] topicsConnection = { CONNECT, DISCONNECT };
      String[] topicsRequest = { GET_N_DEVICES, GET_SENSORS };
      String[] topicsResponse = { N_DEVICES_EDGE_RES, SENSORS_EDGE_RES };

      new ListenerConnection(
          this,
          MQTTClientHost,
          topicsConnection,
          QOS,
          debugModeValue
      );

      new ListenerRequest(
        this,
        MQTTClientUp,
        MQTTClientHost,
        this.nodesUris,
        topicsRequest,
        QOS,
        debugModeValue
      );

      new ListenerResponse(
        this,
        MQTTClientHost,
        topicsResponse,
        QOS,
        debugModeValue
      );
    } else {
      String[] topicsRequest = { N_DEVICES_EDGE, SENSORS_EDGE };
      String[] topicsDeviceRequest = { AUTHENTICATED_DEVICES };

      new ListenerRequest(
        this,
        MQTTClientUp,
        MQTTClientHost,
        this.nodesUris,
        topicsRequest,
        QOS,
        debugModeValue
      );

      new ListenerDeviceRequest(
        this,
        MQTTClientUp,
        MQTTClientHost,
        topicsDeviceRequest,
        QOS,
        debugModeValue
      );
    }

    new ListenerDeviceConnection(
        this,
        MQTTClientHost,
        topicsDeviceConnection,
        QOS,
        debugModeValue
    );

    ariesController = new AriesController(AGENT_ADDR, AGENT_PORT);

    /* Configure Schema and Credential */
    try {
      printlnDebug("EndPoint: " + ariesController.getEndPoint() + "\n");

      int idSchema = ariesController.getSchemasCreated().size() + 1; 
      int idTag = idSchema;

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
     * Configures the credential definition.
     */
    try {
      String credentialDefinitionId = this.createCredentialDefinition();
      credentialDefinition.setId(credentialDefinitionId);
    } catch (IOException e) {
      printlnDebug("\n(!) Error to configure Crendential Definition\n");
      e.printStackTrace();
    }
    this.setCrendentialDefinitionIsConfigured(true);

    /**
     * Edge: Creates and Sends the connection JSON to the gateway
     * present in the Fog.
      */
    if (!hasNodes) {
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

      this.MQTTClientHost.unsubscribe(N_DEVICES_EDGE);
      this.MQTTClientHost.unsubscribe(SENSORS_EDGE);

    } else {
      this.MQTTClientHost.unsubscribe(CONNECT);
      this.MQTTClientHost.unsubscribe(DISCONNECT);
      this.MQTTClientUp.unsubscribe(GET_N_DEVICES);
      this.MQTTClientUp.unsubscribe(GET_SENSORS);
      this.MQTTClientHost.unsubscribe(N_DEVICES_EDGE_RES);
      this.MQTTClientHost.unsubscribe(SENSORS_EDGE_RES);
    }
    this.MQTTClientHost.unsubscribe(DEV_CONNECTIONS);

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
    printlnDebug("Creating connection invitation...");

    CreateInvitationResponse createInvitationResponse = ariesController.createInvitation(label);

    String json = ariesController.getJsonInvitation(createInvitationResponse);

    printlnDebug("Json Invitation: " + json);

    printlnDebug("Invitation created!\n");

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

    printlnDebug("Schema Created!\n");

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

    CredentialDefinition credentialDef = ariesController.getCredentialDefinitionById(credentialDefinition.getId());

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

    CredentialDefinition credentialDef = ariesController.getCredentialDefinitionById(credentialDefinition.getId());
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
    printlnDebug("Difference: " + (timeReceive.getTime() - timeSend.getTime()) + " ms\n");
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

  public String receiveDeviceInvitation(String deviceId, JsonObject invitationJson) throws IOException {
    return receiveDeviceInvitation(ariesController, deviceId, invitationJson);
  }

  /**
   * Receiving device connection invitation from another aries agent.
   * 
   * @param ariesController - Aries controller with agent interaction methods.
   * @param deviceIp - IP of the node want to connect.
   * @param invitationJson - JSON with connection properties.
   * @return String
   * @throws IOException
    */
  private String receiveDeviceInvitation(AriesController ariesController, String deviceId, JsonObject invitationJson) throws IOException {
    Invitation invitationObj = new Invitation(invitationJson);

    printlnDebug("Receiving Connection Invitation...");

    ConnectionRecord connectionRecord = ariesController.receiveInvitation(invitationObj);

    this.addConnectionIdDeviceNodes(deviceId, connectionRecord.getConnectionId());

    printlnDebug("\n\nConnection:\n" + connectionRecord.toString() + "\n");

    return connectionRecord.getConnectionId();
  }

  /**
   * Adds the sensors in a JSON to send to the upper layer.
   *
   * @param jsonReceived JSONObject - JSON containing the sensor types.
   */
  @Override
  public void putSensorsTypes(JsonObject jsonReceived) {
    if (this.sensorsTypesJSON.get("sensors").getAsString().equals("[]")) {
      sensorsTypesJSON = jsonReceived;
    }
  }

  /**
   * Creates a new key in the children's response map.
   *
   * @param id String - Request Id.
   */
  @Override
  public void addResponse(String id) {
    responseQueue.put(id, 0);
  }

  /**
   * Updates the number of responses.
   *
   * @param id String - Request Id.
   */
  @Override
  public void updateResponse(String id) {
    int temp = responseQueue.get(id);
    responseQueue.put(id, ++temp);
  }

  /**
   * Removes a specific reply from the reply queue.
   *
   * @param id String - Request Id.
   */
  @Override
  public void removeSpecificResponse(String id) {
    responseQueue.remove(id);
  }

  /**
   * Adds the devices that were requested to the device list.
   */
  @Override
  public void loadConnectedDevices() {
    this.loadConnectedDevices(ClientIoTService.getApiIot(this.urlAPI));
  }

  /**
   * Adds the devices that were requested to the device list.
   *
   * @param strDevices String - Required devices.
   */
  private void loadConnectedDevices(String strDevices) {
    List<Device> devicesTemp = new ArrayList<Device>();

    try {
      printlnDebug("JSON load:");
      printlnDebug(strDevices);

      JSONArray jsonArrayDevices = new JSONArray(strDevices);

      for (int i = 0; i < jsonArrayDevices.length(); i++) {
        JSONObject jsonDevice = jsonArrayDevices.getJSONObject(i);
        ObjectMapper mapper = new ObjectMapper();
        Device device = mapper.readValue(jsonDevice.toString(), Device.class);

        devicesTemp.add(device);

        List<Sensor> tempSensors = new ArrayList<Sensor>();
        JSONArray jsonArraySensors = jsonDevice.getJSONArray("sensors");

        for (int j = 0; j < jsonArraySensors.length(); j++) {
          JSONObject jsonSensor = jsonArraySensors.getJSONObject(j);
          Sensor sensor = mapper.readValue(jsonSensor.toString(), Sensor.class);
          sensor.setUrlAPI(urlAPI);
          tempSensors.add(sensor);
        }

        device.setSensors(tempSensors);
      }
    } catch (JsonParseException e) {
      printlnDebug(
        "Verify the correct format of 'DevicesConnected' property in configuration file."
      );
      log.log(Level.SEVERE, null, e);
    } catch (JsonMappingException e) {
      printlnDebug(
        "Verify the correct format of 'DevicesConnected' property in configuration file."
      );
      log.log(Level.SEVERE, null, e);
    } catch (IOException e) {
      log.log(Level.SEVERE, null, e);
    }

    this.devices = devicesTemp;

    printlnDebug("Amount of devices connected: " + this.devices.size());
  }

  /**
   * Publishes the number of devices connected to nodes to the upper layer.
   */
  @Override
  public void publishNumberDevicesConnected() {
    printlnDebug("Waiting for Gateway nodes to send number of connected devices");

    long start = System.currentTimeMillis();
    long end = start + this.timeoutInSeconds * 1000;

    /*
     * As long as the number of request responses is less than 
     * the number of child nodes.
     */
    while (
      this.responseQueue.get("numberOfDevices") < this.nodesUris.size() &&
      System.currentTimeMillis() < end
    ) {}

    String numberDevicesFormatted = "\n+---- Number of devices connected to the nodes ----+\n";
    for (String numberDevicesNode : this.numberDevicesConnectedNodes) {
      numberDevicesFormatted += numberDevicesNode + "\n";
    }
    numberDevicesFormatted += "+--------------------------------------------------+\n";

    byte[] payload = numberDevicesFormatted.getBytes();

    MQTTClientUp.publish(N_DEVICES_RES, payload, 1);

    printlnDebug("Result sent successfully!");

    this.numberDevicesConnectedNodes.clear();

    this.removeSpecificResponse("numberOfDevices");
  }

  /**
   * Publishes sensor types to the top layer.
   */
  @Override
  public void publishSensorType() {
    printlnDebug("Waiting for Gateway nodes to send their sensors types");

    long start = System.currentTimeMillis();
    long end = start + this.timeoutInSeconds * 1000;

    /*
     * As long as the number of request responses is less than 
     * the number of child nodes.
     */
    while (
      this.responseQueue.get("getSensors") < this.nodesUris.size() &&
      System.currentTimeMillis() < end
    ) {}

    byte[] payload = sensorsTypesJSON.toString().replace("\\", "").getBytes();

    MQTTClientUp.publish(SENSORS_RES, payload, 1);

    printlnDebug("Result sent successfully!");

    this.removeSpecificResponse("getSensors");
  }

  /**
   * Requests sensor types from a connected device.
   *
   * @return List<String>
   */
  public List<String> loadSensorsTypes() {
    List<String> sensorsList = new ArrayList<>();

    for (Sensor sensor : this.getDevices().get(0).getSensors()) {
      sensorsList.add(sensor.getType());
    }

    return sensorsList;
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

  public List<Device> getDevices() {
    return devices;
  }

  public void setDevices(List<Device> devices) {
    this.devices = devices;
  }

  public String getUrlAPI() {
    return urlAPI;
  }

  public void setUrlAPI(String urlAPI) {
    this.urlAPI = urlAPI;
  }

  public List<String> getNodesUris() {
    return nodesUris;
  }

  public void setNodesUris(List<String> nodesUris) {
    this.nodesUris = nodesUris;
  }

  public List<String> getNumberDevicesConnectedNodes() {
    return numberDevicesConnectedNodes;
  }

  public void setNumberDevicesConnectedNodes(List<String> numberDevicesConnectedNodes) {
    this.numberDevicesConnectedNodes = numberDevicesConnectedNodes;
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

  public Map<String, String> getConnectionIdDeviceNodes() {
    return connectionIdDeviceNodes;
  }

  public void addConnectionIdDeviceNodes(String deviceId, String connectionId) {
    this.connectionIdDeviceNodes.put(deviceId, connectionId);
  }

  public int getTimeoutInSeconds() {
    return timeoutInSeconds;
  }

  public void setTimeoutInSeconds(int timeoutInSeconds) {
    this.timeoutInSeconds = timeoutInSeconds;
  }

  /**
   * Returns a JSON containing the available sensor types.
   *
   * @return JsonObject
   */
  @Override
  public JsonObject getSensorsTypesJSON() {
    return sensorsTypesJSON;
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
