package br.uefs.larsid.dlt.iot.soft.services;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.zxing.WriterException;

import br.uefs.larsid.dlt.iot.soft.model.Device;

public interface Controller {

  /**
   * Returns the number of connected nodes.
   *
   * @return String
   */
  int getNodes();

  /**
   * Returns the connected devices.
   *
   * @return String
   */
  List<Device> getDevices();

  /**
   * Returns the number of devices connected to the nodes.
   *
   * @return String
   */
  List<String> getNumberDevicesConnectedNodes();

  /**
   * Creates a new key in the children's response map.
   *
   * @param id String - Request Id.
   */
  void addResponse(String key);

  /**
   * Updates the number of responses.
   *
   * @param id String - Request Id.
   */
  void updateResponse(String key);

  /**
   * Removes a specific reply from the reply queue.
   *
   *@param id String - Request Id.
   */
  void removeSpecificResponse(String key);

  /**
   * Adds the devices that were requested to the device list.
   */
  void loadConnectedDevices();

  /**
   * Adds a URI to the URI list.
   *
   * @param uri String - URI you want to add.
   */
  public void addNodeUri(String uri);

  /**
   * Removes a URI from the URI list.
   *
   * @param uri String - URI you want to remove.
   */
  public void removeNodeUri(String uri);

  /**
   * Checks if the gateway has children.
   *
   * @return boolean
   */
  public boolean hasNodes();

  /**
   * Returns the list of URIs of connected nodes.
   *
   * @return List
   */
  public List<String> getNodeUriList();

  /**
   * Displays the URI of nodes that are connected.
   */
  public void showNodesConnected();

  /**
   * Checks if the credential definition is already configured.
   * 
   * @return boolean
   */
  public boolean crendentialDefinitionIsConfigured();

  /**
   * Changes the indicator that tells you whether the credential definition 
   * is already configured or not.
   * 
   * @param crendentialDefinitionIsConfigured
   */
  public void setCrendentialDefinitionIsConfigured(boolean crendentialDefinitionIsConfigured);

  /**
   * Returns a map containing the URI and connection id pair of 
   * connected nodes.
   * 
   * @return Map<String, String>
   */
  public Map<String, String> getConnectionIdNodes();

  /**
   * Adds the URI and connection id of the connected node.
   * 
   * @param nodeUri - URI of the node want to connect.
   * @param connectionId - Node connection id
   */
  public void addConnectionIdNodes(String nodeUri, String connectionId);

  /**
   * Returns a map containing device id and connection id of 
   * device connected nodes.
   * 
   * @return Map<String, String>
   */
  public Map<String, String> getConnectionIdDeviceNodes();

  /**
   * Adds the device id and connection id of the connected node.
   * 
   * @param deviceId - Id of the device want to connect.
   * @param connectionId - Node connection id
   */
  public void addConnectionIdDeviceNodes(String deviceId, String connectionId);

  /**
   * Publishes the number of devices connected to nodes to the upper layer.
   */
  public void publishNumberDevicesConnected();

  /**
   * Publishes sensor types to the top layer.
   */
  public void publishSensorType();

  /**
   * Adds the sensors in a JSON to send to the upper layer.
   *
   * @param jsonReceived JsonObject - JSON containing sensor types.
   */
  public void putSensorsTypes(JsonObject jsonReceived);

  /**
   * Returns a JSON containing the available sensor types.
   *
   * @return JsonObject
   */
  public JsonObject getSensorsTypesJSON();

  /**
   * Requests sensor types from a connected device.
   *
   * @return List<String>
   */
  public List<String> loadSensorsTypes();

  /**
   * Creating connection invitation.
   * 
   * @param nodeUri - URI of the node want to connect.
   * @return JsonObject
   * @throws IOException
   * @throws WriterException
   */
  public JsonObject createInvitation(String nodeUri) throws IOException, WriterException;

  /**
   * Receiving connection invitation from another aries agent.
   * 
   * @param nodeUri - URI of the node want to connect.
   * @param invitationJson - JSON with connection properties.
   * @throws IOException
   */
  public void receiveInvitation(String nodeUri, JsonObject invitationJson) throws IOException;

  /**
   * Receiving device connection invitation from another aries agent.
   * 
   * @param deviceId - Id of the device want to connect.
   * @param invitationJson - JSON with connection properties.
   * @throws IOException
   */
  public String receiveDeviceInvitation(String deviceId, JsonObject invitationJson) throws IOException;

  /**
   * Issuing a credential to a connected gateway.
   * 
   * @param jsonProperties - JSON with the properties for sending the credential.
   * @throws IOException
   */
  public void issueCredentialV1(JsonObject jsonProperties) throws IOException;

  /**
   * Send presentation Request to gateways.
   * 
   * @param connectionId - Connection id to send the request
   * @throws IOException
   * @throws InterruptedException
   */
  public void sendRequestPresentationRequest(String connectionId) throws IOException, InterruptedException;
}
