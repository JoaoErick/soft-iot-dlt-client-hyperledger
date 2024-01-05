package br.uefs.larsid.dlt.iot.soft.services;

import java.util.List;
import java.util.Map;

import br.uefs.larsid.dlt.iot.soft.model.Device;

public interface INode {
    /**
     * Adiciona os dispositivos que foram requisitados na lista de dispositivos.
     *
     * @param strDevices String - Dispositivos requisitados.
     */
    public void loadConnectedDevices();

    public List<Device> getDevices();

    public void setDevices(List<Device> devices);

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
     * @param deviceId     - Id of the device want to connect.
     * @param connectionId - Node connection id
     */
    public void addConnectionIdDeviceNodes(String deviceId, String connectionId);

    public int getCheckDevicesCredentialProofTaskTime();

    public void setCheckDevicesCredentialProofTaskTime(int checkDevicesCredentialProofTaskTime);

    public String getDeviceAPIAddress();

    public void setDeviceAPIAddress(String deviceAPIAddress);

    public Controller getController();

    public void setController(Controller controller);

    public boolean isDebugModeValue();

    public void setDebugModeValue(boolean debugModeValue);
}
