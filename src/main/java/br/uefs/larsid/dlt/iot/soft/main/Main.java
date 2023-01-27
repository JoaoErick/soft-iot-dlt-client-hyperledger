// /*
//  * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
//  * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
//  */
// package br.uefs.larsid.dlt.iot.soft.model;

// import br.uefs.larsid.dlt.iot.soft.aries.AriesController;
// import br.uefs.larsid.dlt.iot.soft.entity.AttributeRestriction;
// import br.uefs.larsid.dlt.iot.soft.entity.Credential;
// import br.uefs.larsid.dlt.iot.soft.entity.CredentialDefinition;
// import br.uefs.larsid.dlt.iot.soft.entity.Schema;
// import br.uefs.larsid.dlt.iot.soft.mqtt.Listener;
// import br.uefs.larsid.dlt.iot.soft.mqtt.MQTTClient;
// import br.uefs.larsid.dlt.iot.soft.util.CLI;

// import com.google.gson.Gson;
// import com.google.gson.JsonObject;
// import com.google.zxing.WriterException;
// import java.io.IOException;
// import java.io.InputStream;
// import java.util.ArrayList;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
// import java.util.Properties;
// import java.util.Scanner;
// import org.hyperledger.aries.api.connection.ConnectionRecord;
// import org.hyperledger.aries.api.connection.CreateInvitationResponse;
// import org.hyperledger.aries.api.schema.SchemaSendResponse;

// /**
//  *
//  * @author Emers
//  */
// public class Main {

//     public static boolean DEBUG_MODE = true;

//     /* -------------------------- Topic constants ---------------------------- */
//     private static final String CREATE_INVITATION = "POST CREATE_INVITATION";
//     private static final String ACCEPT_INVITATION = "POST ACCEPT_INVITATION";
//     private static final String CREDENTIAL_DEFINITIONS = "POST CREDENTIAL_DEFINITIONS";
//     private static final String ISSUE_CREDENTIAL = "POST ISSUE_CREDENTIAL";
//     /* ---------------------------------------------------------------------- */

//     /* -------------------------- MQTT constants ---------------------------- */
//     private static final int MQTT_QOS = 1;
//     /* ---------------------------------------------------------------------- */

//     /* ---------------------- Controller properties ------------------------- */
//     private static String AGENT_END_POINT;
//     private static String CRED_DEF_ID;
//     private static String BROKER_IP;
//     private static String BROKER_PORT;
//     private static String BROKER_USR;
//     private static String BROKER_PW;
//     private static String AGENT_ADDR;
//     private static String AGENT_PORT;
//     /* ---------------------------------------------------------------------- */

//     public static AriesController ariesController;
//     public static Schema schema;
//     public static CredentialDefinition credentialDefinition;
//     private static MQTTClient mqttClient;

//     public static void main(String[] args) throws IOException, WriterException {
//         readProperties(args);

//         mqttClient = new MQTTClient(DEBUG_MODE, BROKER_IP, BROKER_PORT, BROKER_USR, BROKER_PW);

//         mqttClient.connect();

//         String[] topics = {
//                 CREATE_INVITATION,
//                 ACCEPT_INVITATION,
//                 CREDENTIAL_DEFINITIONS,
//                 ISSUE_CREDENTIAL
//         };

//         new Listener(
//                 mqttClient,
//                 topics,
//                 MQTT_QOS,
//                 DEBUG_MODE);

//         // final String AGENT_ADDR = "localhost";
//         // final String AGENT_PORT = "8021";
//         // final String AGENT_END_POINT = "https://2f8e-177-99-172-106.sa.ngrok.io";

//         ariesController = new AriesController(AGENT_ADDR, AGENT_PORT, AGENT_END_POINT);

//         /*Base to create issuer class*/
        
//         /*Criar uma classe para servir de base para implementação de credenciais especificas*/
        
//         /*Implementar demais métodos, verificação, recepção, ...*/
        
//         int idSchema = ariesController.getSchemasCreated().size() + 11; //precisa automatizar o número baseado na persistencia
//         int idTag = 1;

//         List<String> attributes = new ArrayList<>();
//         attributes.add("id");

//         schema = new Schema(("Schema_" + idSchema), (idSchema++ + ".0"));
//         schema.addAttributes(attributes);

//         Boolean revocable = false;
//         int revocableSize = 1000;
//         credentialDefinition = new CredentialDefinition(("tag_" + idTag++), revocable, 1000, schema);

//         Boolean autoRemove = false;
//         Credential credential = new Credential(credentialDefinition, autoRemove);
//         Map<String, String> values = new HashMap<>();
//         values.put("id", "10.10.10.10");
       
//         credential.addValues(values);

//         //criando solicitação de prova
//         String name = "Prove que você é aluno";
//         String comment = "Você é um aluno?";
//         String version = "1.0";
//         String nameAttrRestriction = "nome";
//         String nameRestriction = "cred_def_id";
//         String propertyRestriction = "NwYTntJoJUgPVmcyBpq6oo:3:CL:59496:tag_1";

//         AttributeRestriction attributeRestriction = new AttributeRestriction(nameAttrRestriction, nameRestriction, propertyRestriction);
//         List<AttributeRestriction> attributesRestrictions = new ArrayList<>();
//         attributesRestrictions.add(attributeRestriction);

//         // final Scanner scan = new Scanner(System.in);

//         // boolean menuControl = true;

//         // do {
//             // System.out.println("Menu Aries Agent\n");
//             // System.out.println("1  - Gerar url de Conexão");
//             // System.out.println("2  - Criar Definição de credencial");
//             // System.out.println("3  - Emitir Credencial");
//             // System.out.println("4  - Exibir Schemas Criados");
//             // System.out.println("5  - Exibir schema por ID");
//             // System.out.println("6  - Listar Conexões");
//             // System.out.println("7  - Listar Definições de credenciais");
//             // System.out.println("8  - Solicitar prova de credenciais");
//             // System.out.println("9  - Verificar apresentação de prova de credenciais");
//             // System.out.println("10 - Aceitar Conexão");
//             // System.out.println("11 - Listar Credenciais Recebidas");
//             // System.out.println("12 - Revogar Credencial Emitida");
//             // System.out.println("0  - Exit\n");

//             // switch (scan.nextInt()) {
//             //     case 1: //Cria um convite de conexão
//             //         createInvitation(ariesController, ("Convite_" + idConvite++));
//             //         break;
//             //     case 2: //Cria uma definição de credencial
//             //         createCredentialDefinition(ariesController, schema, credentialDefinition);
//             //         break;
//             //     case 3: // Envia uma credencial pelo método V 1.0
//             //         ConnectionRecord connectionRecord = ariesController.getConnections().get(2);
//             //         issueCredentialV1(ariesController, credential, connectionRecord);
//             //         break;
//             //     case 4: //Lista os id dos schemas criados
//             //         listSchemas(ariesController);
//             //         break;
//             //     case 5: //Lista os schemas através de ID
//             //         listSchemaById(ariesController, schema.getId());
//             //         break;
//             //     case 6: //Lista as conexões realizadas
//             //         listConnections(ariesController);
//             //         break;
//             //     case 7:

//             //         break;
//             //     case 8:
//             //         sendRequestPresentationRequest(ariesController, name, comment, version, ariesController.getConnections().get(2), attributesRestrictions);
//             //         break;
//             //     case 9:

//             //         break;
//             //     case 10:

//             //         break;
//             //     case 11:

//             //         break;
//             //     case 12:

//             //         break;
//             //     case 0:
//             //         menuControl = false;
//             //         break;
//             //     default:
//             //         break;
//         //     }
//         // } while (menuControl);
//     }

//     /**
//      * Realiza leitura das propriedades passadas por parâmetro ou resgata 
//      * valores presentes no arquivo de propriedade. 
//      * 
//      * @param args String[] - Dados passados na execução do projeto.
//      */
//     public static void readProperties(String[] args) {
//         try (InputStream input = AriesAgent.class.getResourceAsStream("ariesController.properties")) {
//             if (input == null) {
//                 printlnDebug("Sorry, unable to find ariesController.properties.");
//                 return;
//             }
//             Properties props = new Properties();
//             props.load(input);

//             AGENT_END_POINT = CLI.getEndpoint(args)
//                     .orElse(props.getProperty("AGENT_END_POINT"));

//             CRED_DEF_ID = CLI.getCredentialDefinitionId(args)
//                     .orElse(props.getProperty("CRED_DEF_ID"));

//             BROKER_IP = CLI.getBrokerIp(args)
//                     .orElse(props.getProperty("BROKER_IP"));

//             BROKER_PORT = CLI.getBrokerPort(args)
//                     .orElse(props.getProperty("BROKER_PORT"));

//             BROKER_PW = CLI.getBrokerPassword(args)
//                     .orElse(props.getProperty("BROKER_PW"));

//             BROKER_USR = CLI.getBrokerUsername(args)
//                     .orElse(props.getProperty("BROKER_USR"));

//             AGENT_ADDR = CLI.getAgentIp(args)
//                     .orElse(props.getProperty("AGENT_ADDR"));

//             AGENT_PORT = CLI.getAgentPort(args)
//                     .orElse(props.getProperty("AGENT_PORT"));

//         } catch (IOException ex) {
//             printlnDebug("Sorry, unable to find sensors.json or not create pesistence file.");
//         }
//     }

//     public static JsonObject createInvitation(String nodeUri) throws IOException, WriterException {
//         int idConvite = ariesController.getConnections().size();
//         return createInvitation(ariesController, ("Convite_" + idConvite++), nodeUri);
//     }

//     public static JsonObject createInvitation(AriesController ariesController, String label, String nodeUri) throws IOException, WriterException {
//         System.out.println("\nCriando convite de conexão ...");

//         CreateInvitationResponse createInvitationResponse = ariesController.createInvitation(label);

//         String url = ariesController.getURLInvitation(createInvitationResponse);

//         System.out.println("\nUrl: " + url);

//         String json = ariesController.getJsonInvitation(createInvitationResponse);

//         System.out.println("Json Invitation: " + json);

//         // System.out.println("\nGerando QR Code ...");

//         // ariesController.generateQRCodeInvitation(createInvitationResponse);

//         System.out.print("\nConvite Criado!\n");

//         JsonObject jsonInvitation = new Gson().fromJson(json, JsonObject.class);

//         jsonInvitation.addProperty("connectionId", createInvitationResponse.getConnectionId());
//         jsonInvitation.addProperty("nodeUri", nodeUri);

//         printlnDebug("Final JSON: " + jsonInvitation.toString());

//         return jsonInvitation;
//     }

//     public static String createCredentialDefinition() throws IOException {
//         return createCredentialDefinition(ariesController, schema, credentialDefinition);
//     }

//     private static String createCredentialDefinition(AriesController ariesController, Schema schema, CredentialDefinition credentialDefinition) throws IOException {
//         System.out.println("\nCriando Schema ...");

//         SchemaSendResponse schemaSendResponse = ariesController.createSchema(schema);
//         schema.setId(schemaSendResponse.getSchemaId());

//         System.out.println("\nSchema ID: " + schema.getId());

//         System.out.println("\nSchema Criado!");

//         System.out.println("\nCriando definição de credencial ...");

//         ariesController.createCredendentialDefinition(credentialDefinition);

//         System.out.println("\nDefinição de Credencial ID: " + credentialDefinition.getId());

//         System.out.println("\nDefinição de Credencial Criada!\n");

//         return credentialDefinition.getId();
//     }

//     private static void issueCredentialV1(AriesController ariesController, Credential credential, ConnectionRecord connectionRecord) throws IOException {
//         System.out.println("\nEmitindo Credencial ...");

//         ariesController.issueCredentialV1(connectionRecord.getConnectionId(), credential);

//         System.out.println("Credencial ID: " + credential.getId());

//         System.out.println("\nCredencial Emitinda!\n");
//     }

//     private static void listSchemas(AriesController ariesController) throws IOException {
//         System.out.println("\nConsultando schemas ...");

//         List<String> schemas = ariesController.getSchemasCreated();

//         System.out.println("\nListando schemas ...");

//         for (String schema : schemas) {
//             System.out.println("Schema: " + schema);
//         }

//         System.out.println("\nFim da lista de schemas!\n");
//     }

//     private static void listSchemaById(AriesController ariesController, String schemaId) throws IOException {
//         System.out.println("\nConsultando schemas ...");

//         SchemaSendResponse.Schema schemaResponse = ariesController.getSchemaById(schemaId);

//         System.out.println("\nListando schema ...");

//         System.out.println("Name: " + schemaResponse.getName());
//         System.out.println("Version: " + schemaResponse.getVersion());
//         System.out.println("Attributes: " + schemaResponse.getAttrNames());

//         System.out.println("\nFim da lista de schemas!\n");

//     }

//     private static void listConnections(AriesController ariesController) throws IOException {
//         System.out.println("\nConsultando conexões ...");

//         List<ConnectionRecord> connectionsRecords = ariesController.getConnections();

//         System.out.println("\nListando conexões...");
//         for (ConnectionRecord connectionRecord : connectionsRecords) {
//             System.out.println("\nConexão ID: " + connectionRecord.getConnectionId());
//             System.out.println("State: " + connectionRecord.getState());
//             System.out.println("RFC State: " + connectionRecord.getRfc23Sate());
//             System.out.println("Alias: " + connectionRecord.getAlias());
//             System.out.println("Invitation Key: " + connectionRecord.getInvitationKey());
//             System.out.println("Their Label: " + connectionRecord.getTheirLabel());
//             System.out.println("Their DID: " + connectionRecord.getTheirDid());
//             System.out.println("Created At: " + connectionRecord.getCreatedAt());
//             System.out.println("Updated At: " + connectionRecord.getUpdatedAt());
//             System.out.println("Msg error: " + connectionRecord.getErrorMsg());
//         }

//         System.out.println("\nFim da lista de conexões!\n");
//     }

//     private static void printlnDebug(String str) {
//         if (isDebugModeValue()) {
//             System.out.println(str);
//         }
//     }

//     public static boolean isDebugModeValue() {
//         return DEBUG_MODE;
//     }

// }
