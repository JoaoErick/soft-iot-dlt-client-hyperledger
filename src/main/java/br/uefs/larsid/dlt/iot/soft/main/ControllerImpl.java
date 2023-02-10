package br.uefs.larsid.dlt.iot.soft.main;

import br.uefs.larsid.dlt.iot.soft.controller.AriesController;
import br.uefs.larsid.dlt.iot.soft.model.AttributeRestriction;
import br.uefs.larsid.dlt.iot.soft.model.Credential;
import br.uefs.larsid.dlt.iot.soft.model.CredentialDefinition;
import br.uefs.larsid.dlt.iot.soft.model.Invitation;
import br.uefs.larsid.dlt.iot.soft.model.Schema;
import br.uefs.larsid.dlt.iot.soft.mqtt.ListenerConnection;
import br.uefs.larsid.dlt.iot.soft.mqtt.ListenerCredentialDefinition;
import br.uefs.larsid.dlt.iot.soft.mqtt.ListenerInvitation;
import br.uefs.larsid.dlt.iot.soft.mqtt.MQTTClient;
import br.uefs.larsid.dlt.iot.soft.services.Controller;
import br.uefs.larsid.dlt.iot.soft.utils.File;
import br.uefs.larsid.dlt.iot.soft.utils.TimeRegister;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.zxing.WriterException;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

  /*--------------------------Constants-------------------------------------*/
  private static final int QOS = 1;

  private static final String CONNECT = "SYN";
  private static final String DISCONNECT = "FIN";
  /*-------------------------------------------------------------------------*/

  /* -------------------------- Aries Topic constants ---------------------- */
  private String AGENT_ADDR;
  private String AGENT_PORT;
  /* ----------------------------------------------------------------------- */

  /* -------------------------- Aries Topic constants ---------------------- */
  private static final String CREDENTIAL_DEFINITIONS = "POST CREDENTIAL_DEFINITIONS";
  private static final String CREATE_INVITATION = "POST CREATE_INVITATION";
  /* ----------------------------------------------------------------------- */

  /* -------------------------- Aries Topic Res constants ------------------ */
  private static final String CREATE_INVITATION_RES = "CREATE_INVITATION_RES";
  private static final String ACCEPT_INVITATION_RES = "ACCEPT_INVITATION_RES";
  private static final String CREDENTIAL_DEFINITIONS_RES = "CREDENTIAL_DEFINITIONS_RES";
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
   * Inicializa o bundle.
   */
  public void start() {
    this.MQTTClientUp.connect();
    this.MQTTClientHost.connect();
    
    printlnDebug("Start Hyperledger bundle!");

    if (hasNodes) {
      nodesUris = new ArrayList<>();
      String[] topicsConnection = { CONNECT, DISCONNECT };
      String[] topicsCredentialDefinition = { CREDENTIAL_DEFINITIONS_RES };
      String[] topicsInvitation = { ACCEPT_INVITATION_RES };

      new ListenerConnection(
          this,
          MQTTClientHost,
          MQTTClientUp,
          topicsConnection,
          QOS,
          debugModeValue);

      new ListenerCredentialDefinition(
          this,
          MQTTClientHost,
          topicsCredentialDefinition,
          QOS,
          debugModeValue);

      new ListenerInvitation(
          this,
          MQTTClientHost,
          MQTTClientUp,
          topicsInvitation,
          QOS,
          debugModeValue);

      byte[] payload = "".getBytes();

      this.MQTTClientHost.publish(CREDENTIAL_DEFINITIONS, payload, QOS);

    } else {
      String[] topics = { CREATE_INVITATION_RES };

      new ListenerInvitation(
          this,
          MQTTClientHost,
          MQTTClientUp,
          topics,
          QOS,
          debugModeValue);
    }

    ariesController = new AriesController(AGENT_ADDR, AGENT_PORT);

    try {
      printlnDebug("End Point: " + ariesController.getEndPoint());
      /* Base to create issuer class */
      /* Criar uma classe para servir de base para implementação de credenciais especificas */
      /* Implementar demais métodos, verificação, recepção, ... */
      
      int idSchema = ariesController.getSchemasCreated().size() + 11; //precisa automatizar o número baseado na persistencia
      int idTag = 1;

      List<String> attributes = new ArrayList<>();
      attributes.add("nome");
      attributes.add("email");
      attributes.add("matricula");

      Schema schema = new Schema(("Schema_" + idSchema), (idSchema++ + ".0"));
      schema.addAttributes(attributes);

      Boolean revocable = false;
      int revocableSize = 1000;
      CredentialDefinition credentialDefinition = new CredentialDefinition(("tag_" + idTag++), revocable, 1000, schema);

      Boolean autoRemove = false;
      Credential credential = new Credential(credentialDefinition, autoRemove);
      Map<String, String> values = new HashMap<>();
      values.put("nome", "fulano");
      values.put("email", "fulano@gmail.com");
      values.put("matricula", "12345");
      credential.addValues(values);

    } catch (IOException e) {
      e.printStackTrace();
    } 

    //criando solicitação de prova
    String name = "Prove que você é aluno";
    String comment = "Você é um aluno?";
    String version = "1.0";
    String nameAttrRestriction = "nome";
    String nameRestriction = "cred_def_id";
    String propertyRestriction = "JU1jTydsRztc8XvjPHboAn:3:CL:63882:tag_1";

    AttributeRestriction attributeRestriction = new AttributeRestriction(nameAttrRestriction, nameRestriction, propertyRestriction);
    List<AttributeRestriction> attributesRestrictions = new ArrayList<>();
    attributesRestrictions.add(attributeRestriction);

    if (!hasNodes) {
      try {
        String nodeUri = String
          .format("%s:%s", MQTTClientHost.getIp(), MQTTClientHost.getPort());
        this.sendJSONInvitation(nodeUri);
      } catch (IOException | WriterException e) {
        e.printStackTrace();
      }
    }
    
  }

  /**
   * Finaliza o bundle.
   */
  public void stop() {
    if (!this.hasNodes) {
      byte[] payload = String
          .format("%s:%s", MQTTClientHost.getIp(), MQTTClientHost.getPort())
          .getBytes();

      this.MQTTClientUp.publish(DISCONNECT, payload, QOS);
      this.MQTTClientHost.unsubscribe(CREATE_INVITATION_RES);

    } else {
      this.MQTTClientUp.unsubscribe(CONNECT);
      this.MQTTClientUp.unsubscribe(DISCONNECT);
      this.MQTTClientHost.unsubscribe(CREDENTIAL_DEFINITIONS_RES);
      this.MQTTClientHost.unsubscribe(ACCEPT_INVITATION_RES);
    }

    this.MQTTClientHost.disconnect();
    this.MQTTClientUp.disconnect();
  }

  public void sendJSONInvitation(String nodeUri) throws IOException, WriterException {
    JsonObject jsonResult = this.createInvitation(nodeUri);
    
    printlnDebug(">> Send Invitation URL...");
    byte[] payload = jsonResult.toString().getBytes();
    this.MQTTClientUp.publish(CONNECT, payload, QOS);
  }

  public JsonObject createInvitation(String nodeUri) throws IOException, WriterException {
    int idConvite = ariesController.getConnections().size();
    return createInvitation(ariesController, ("Convite_" + idConvite++), nodeUri);
  }

  private JsonObject createInvitation(AriesController ariesController, String label, String nodeUri)
      throws IOException, WriterException {
    printlnDebug("\nCriando convite de conexão ...");

    CreateInvitationResponse createInvitationResponse = ariesController.createInvitation(label);

    String url = ariesController.getURLInvitation(createInvitationResponse);

    printlnDebug("\nUrl: " + url);

    String json = ariesController.getJsonInvitation(createInvitationResponse);

    printlnDebug("Json Invitation: " + json);

    printlnDebug("\nConvite Criado!\n");

    JsonObject jsonInvitation = new Gson().fromJson(json, JsonObject.class);

    jsonInvitation.addProperty("connectionId", createInvitationResponse.getConnectionId());
    jsonInvitation.addProperty("nodeUri", nodeUri);

    printlnDebug("Final JSON: " + jsonInvitation.toString());

    return jsonInvitation;
  }

  public static String createCredentialDefinition() throws IOException {
    return createCredentialDefinition(ariesController, schema, credentialDefinition);
  }

  private static String createCredentialDefinition(AriesController ariesController, Schema schema,
      CredentialDefinition credentialDefinition) throws IOException {
    System.out.println("\nCriando Schema ...");

    SchemaSendResponse schemaSendResponse = ariesController.createSchema(schema);
    schema.setId(schemaSendResponse.getSchemaId());

    System.out.println("\nSchema ID: " + schema.getId());

    System.out.println("\nSchema Criado!");

    System.out.println("\nCriando definição de credencial ...");

    ariesController.createCredendentialDefinition(credentialDefinition);

    System.out.println("\nDefinição de Credencial ID: " + credentialDefinition.getId());

    System.out.println("\nDefinição de Credencial Criada!\n");

    return credentialDefinition.getId();
  }

  private static void issueCredentialV1(AriesController ariesController, Credential credential,
      ConnectionRecord connectionRecord) throws IOException {
    System.out.println("\nEmitindo Credencial ...");

    ariesController.issueCredentialV1(connectionRecord.getConnectionId(), credential);

    System.out.println("Credencial ID: " + credential.getId());

    System.out.println("\nCredencial Emitinda!\n");
  }

  private static void listSchemas(AriesController ariesController) throws IOException {
    System.out.println("\nConsultando schemas ...");

    List<String> schemas = ariesController.getSchemasCreated();

    System.out.println("\nListando schemas ...");

    for (String schema : schemas) {
      System.out.println("Schema: " + schema);
    }

    System.out.println("\nFim da lista de schemas!\n");
  }

  private static void listSchemaById(AriesController ariesController, String schemaId) throws IOException {
    System.out.println("\nConsultando schemas ...");

    SchemaSendResponse.Schema schemaResponse = ariesController.getSchemaById(schemaId);

    System.out.println("\nListando schema ...");

    System.out.println("Name: " + schemaResponse.getName());
    System.out.println("Version: " + schemaResponse.getVersion());
    System.out.println("Attributes: " + schemaResponse.getAttrNames());

    System.out.println("\nFim da lista de schemas!\n");

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

  // private static void sendRequestPresentationRequest(AriesController ariesController) throws IOException, InterruptedException {
  //   Scanner scan = new Scanner(System.in);
  //   String name = "Prove que você é você";
  //   String comment = "Essa é uma verificação de prova do larsid";
  //   String version = "1.0";
  //   String nameAttrRestriction = "";
  //   String nameRestriction = "cred_def_id";
  //   String propertyRestriction = "";

  //   //listando definições de credenciais
  //   listCredentialDefinitionsCreated(ariesController);
  //   System.out.println("Número da definição de credencial: ");
  //   int numberCredentialDefinition = scan.nextInt();

  //   CredentialDefinition credentialDefinition = ariesController.getCredentialDefinitionById(
  //           ariesController.getCredentialDefinitionsCreated().getCredentialDefinitionIds().get(numberCredentialDefinition));

  //   nameAttrRestriction = credentialDefinition.getSchema().getAttributes().get(0);
  //   propertyRestriction = credentialDefinition.getId();

  //   AttributeRestriction attributeRestriction = new AttributeRestriction(nameAttrRestriction, nameRestriction, propertyRestriction);
  //   List<AttributeRestriction> attributesRestrictions = new ArrayList<>();
  //   attributesRestrictions.add(attributeRestriction);

  //   //listando conexões
  //   listConnections(ariesController);
  //   System.out.println("Número da conexão: ");
  //   int numberConnection = scan.nextInt();
  //   ConnectionRecord connectionRecord = ariesController.getConnections().get(numberConnection);

  //   //Guardando timestamp do inicio da solicitação de prova
  //   Timestamp timeSend = new Timestamp(System.currentTimeMillis());

  //   String presentationExchangeId = ariesController.sendRequestPresentationRequest(name, comment, version, connectionRecord.getConnectionId(), attributesRestrictions);

  //   System.out.println("\nEnviando solicitação de prova ...");

  //   PresentationExchangeRecord presentationExchangeRecord;

  //   do {
  //       presentationExchangeRecord = ariesController.getPresentation(presentationExchangeId);
  //       System.out.println("UpdateAt: " + presentationExchangeRecord.getUpdatedAt());
  //       System.out.println("Presentation: " + presentationExchangeRecord.getPresentation());
  //       System.out.println("Verificada: " + presentationExchangeRecord.isVerified());
  //       System.out.println("State: " + presentationExchangeRecord.getState());
  //       System.out.println("Auto Presentation: " + presentationExchangeRecord.getAutoPresent());
  //       //Thread.sleep(2 * 1000);
  //   } while (!presentationExchangeRecord.getState().equals(PresentationExchangeState.REQUEST_RECEIVED) && !presentationExchangeRecord.getState().equals(PresentationExchangeState.VERIFIED));

  //   System.out.println("\nSolicitação de prova recebida!\n");

  //   verifyProofPresentation(ariesController, presentationExchangeId);

  //   System.out.println("\nCalculando time stamp ...\n");

  //   Timestamp timeReceive = new Timestamp(System.currentTimeMillis());
  //   System.out.println("Calculando time stamp ...");
  //   System.out.println("Tempo Inicial: " + timeSend);
  //   System.out.println("Tempo Final: " + timeReceive);
  //   System.out.println("Diferença: " + (timeReceive.getTime() - timeSend.getTime()));
  // }

  // private static void verifyProofPresentation(AriesController ariesController, String presentationExchangeId) throws IOException, InterruptedException {
  //     System.out.println("\nVerificando solicitação de prova ...");

  //     //Thread.sleep(5 * 1000);
  //     if (ariesController.getPresentation(presentationExchangeId).getVerified()) {
  //         System.out.println("\nCredencial verificada!\n");
  //     } else {
  //         System.err.println("\nCredencial não verificada!\n");
  //     }
  // }

  public void receiveInvitation(JsonObject invitationJson) throws IOException {
    receiveInvitation(ariesController, invitationJson);
  }

  private void receiveInvitation(AriesController ariesController, JsonObject invitationJson) throws IOException {
      Invitation invitationObj = new Invitation(invitationJson);

      System.out.println("\nRecebendo convite de conexão ...");

      ConnectionRecord connectionRecord = ariesController.receiveInvitation(invitationObj);

      System.out.println("\nConexão:\n" + connectionRecord.toString());
  }

  // private static void saveTimeRegister(String data) throws IOException {
  //     String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH'h'mm'min'ss's'"));
  //     File.write("times_"+dateTime, "json", data);
  // }

  // private static void testTimeRegister() throws InterruptedException, IOException {
  //     Timestamp t1 = new Timestamp(System.currentTimeMillis());
  //     Thread.sleep(2 * 1000);
  //     Timestamp t2 = new Timestamp(System.currentTimeMillis());

  //     TimeRegister tr = new TimeRegister(t1, t2);

  //     JsonArray timeRegisters = new JsonArray();
  //     timeRegisters.add(tr.getJson());

  //     saveTimeRegister(timeRegisters.toString());
  // }

  // private static void sendRequestPresentationRequests(AriesController ariesController) throws IOException, InterruptedException {
  //     Scanner scan = new Scanner(System.in);
  //     String name = "Prove que você é você";
  //     String comment = "Essa é uma verificação de prova do larsid";
  //     String version = "1.0";
  //     String nameAttrRestriction = "";
  //     String nameRestriction = "cred_def_id";
  //     String propertyRestriction = "";

  //     //listando definições de credenciais
  //     // listCredentialDefinitionsCreated(ariesController);
  //     System.out.println("Número da definição de credencial: ");
  //     int numberCredentialDefinition = scan.nextInt();

  //     CredentialDefinition credentialDefinition = ariesController.getCredentialDefinitionById(
  //             ariesController.getCredentialDefinitionsCreated().getCredentialDefinitionIds().get(numberCredentialDefinition));

  //     nameAttrRestriction = credentialDefinition.getSchema().getAttributes().get(0);
  //     propertyRestriction = credentialDefinition.getId();

  //     AttributeRestriction attributeRestriction = new AttributeRestriction(nameAttrRestriction, nameRestriction, propertyRestriction);
  //     List<AttributeRestriction> attributesRestrictions = new ArrayList<>();
  //     attributesRestrictions.add(attributeRestriction);

  //     //listando conexões
  //     listConnections(ariesController);
  //     System.out.println("Número da conexão: ");
  //     int numberConnection = scan.nextInt();
  //     ConnectionRecord connectionRecord = ariesController.getConnections().get(numberConnection);

  //     //Guardando timestamp do inicio da solicitação de prova
  //     Timestamp timeSend = null;
  //     Timestamp timeReceive = null;
  //     String presentationExchangeId = null;
  //     JsonArray timeRegisters = new JsonArray();

  //     System.out.print("Numero de verificações de provas: ");
  //     int limit = scan.nextInt();

  //     System.out.println("\nEnviando " + limit + " solicitações de prova ...");

  //     for (int i = 0; i < limit; i++) {
  //         timeSend = new Timestamp(System.currentTimeMillis());
  //         presentationExchangeId = ariesController.sendRequestPresentationRequest(name, comment, version, connectionRecord.getConnectionId(), attributesRestrictions);

  //         PresentationExchangeRecord presentationExchangeRecord;

  //         do {
  //             presentationExchangeRecord = ariesController.getPresentation(presentationExchangeId);
  //         } while (!presentationExchangeRecord.getState().equals(PresentationExchangeState.REQUEST_RECEIVED) && !presentationExchangeRecord.getState().equals(PresentationExchangeState.VERIFIED));

  //         verifyProofPresentation(ariesController, presentationExchangeId);

  //         timeReceive = new Timestamp(System.currentTimeMillis());
          
  //         timeRegisters.add(new TimeRegister(timeSend, timeReceive).getJson());
  //         Thread.sleep(10 * 1000); //evitar cache
  //     }
      
  //     System.out.println("\n" + limit + " solicitações de prova enviadas!");
  //     System.out.println("\nSalvando " + limit + " solicitações de provas ...");
  //     saveTimeRegister(timeRegisters.toString());
  //     System.out.println("\n" + limit + " solicitações de prova salvas!");

  // }

  /**
   * Adiciona um URI na lista de URIs.
   *
   * @param uri String - URI que deseja adicionar.
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
   * Remove uma URI na lista de URIs.
   *
   * @param uri String - URI que deseja remover.
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
   * Retorna a posição de um URI na lista de URIs
   *
   * @param uri String - URI que deseja a posição.
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
   * Retorna a lista de URIs dos nós conectados.
   *
   * @return List
   */
  @Override
  public List<String> getNodeUriList() {
    return this.nodesUris;
  }

  /**
   * Retorna a quantidade de nós conectados.
   *
   * @return String
   */
  @Override
  public int getNodes() {
    return this.nodesUris.size();
  }

  /**
   * Exibe a URI dos nós que estão conectados.
   */
  public void showNodesConnected() {
    printlnDebug("+---- Nodes URI Connected ----+");
    for (String nodeIp : this.getNodeUriList()) {
      printlnDebug("     " + nodeIp);
    }

    if (this.getNodeUriList().size() == 0) {
      printlnDebug("        empty");
    }
    printlnDebug("+----------------------------+");
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
   * Verifica se o gateway possui filhos.
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
