package br.uefs.larsid.dlt.iot.soft.services;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.zxing.WriterException;

import br.uefs.larsid.dlt.iot.soft.controller.AriesController;

public interface Controller {

  /**
   * Retorna a quantidade de nós conectados.
   *
   * @return String
   */
  int getNodes();

  /**
   * Adiciona um URI na lista de URIs.
   *
   * @param uri String - URI que deseja adicionar.
   */
  public void addNodeUri(String uri);

  /**
   * Remove uma URI na lista de URIs.
   *
   * @param uri String - URI que deseja remover.
   */
  public void removeNodeUri(String uri);

  /**
   * Verifica se o gateway possui filhos.
   *
   * @return boolean
   */
  public boolean hasNodes();

  /**
   * Retorna a lista de URIs dos nós conectados.
   *
   * @return List
   */
  public List<String> getNodeUriList();

  /**
   * Exibe a URI dos nós que estão conectados.
   */
  public void showNodesConnected();

  /**
   * Verifica se a definição de credencial já está configurada.
   * @return boolean
   */
  public boolean crendentialDefinitionIsConfigured();

  /**
   * Altera o indicador que informa se a definição de credencial já está 
   * configurada ou não.
   */
  public void setCrendentialDefinitionIsConfigured(boolean crendentialDefinitionIsConfigured);

  public Map<String, String> getConnectionIdNodes();

  public void addConnectionIdNodes(String nodeUri, String connectionId);
  /**
   * Adiciona os sensores em um JSON para enviar para a camada superior.
   *
   * @param jsonReceived JsonObject - JSON contendo os tipos dos sensores.
   */
  // public void putSensorsTypes(JsonObject jsonReceived);

  /**
   * Retorna um JSON contendo os tipos de sensores disponíveis.
   *
   * @return JsonObject
   */
  // public JsonObject getSensorsTypesJSON();

  /**
   * Requisita os tipos de sensores de um dispositivo conectado.
   *
   * @return List<String>
   */
  // public List<String> loadSensorsTypes();

  public JsonObject createInvitation(String nodeUri) throws IOException, WriterException;

  /**
   * @param invitationJson
   */
  public void receiveInvitation(JsonObject invitationJson) throws IOException;

  /**
   * 
   * @param jsonProperties
   * @throws IOException 
   * 
   */
  public void issueCredentialV1(JsonObject jsonProperties) throws IOException;

  /**
   * 
   * @param connectionId
   * @throws IOException
   * @throws InterruptedException 
   * 
   */
  public void sendRequestPresentationRequest(String connectionId) throws IOException, InterruptedException;
}
