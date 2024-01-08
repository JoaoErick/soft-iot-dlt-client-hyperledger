package br.uefs.larsid.dlt.iot.soft.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.logging.Logger;

import com.google.gson.JsonObject;

import br.uefs.larsid.dlt.iot.soft.model.Node;
import br.uefs.larsid.dlt.iot.soft.services.Controller;

public class CheckDevicesCredentialProofTask extends TimerTask {
    /*-------------------------Constants-------------------------------------*/
    private static final String TOPIC = "AUTHENTICATED_DEVICES";
    private static final int TIMEOUT_IN_SECONDS = 15;
    /*-----------------------------------------------------------------------*/

    private final Node node;
    private Controller controllerImpl;
    private boolean debugModeValue;

    private static final Logger logger = Logger.getLogger(
            CheckDevicesCredentialProofTask.class.getName());

    /**
     * Método construtor.
     *
     * @param node           NodeType - Nó que verificará os dispositivos que estão
     *                       conectados.
     * @param controllerImpl Controller - Controller that will make use of this
     *                       task.
     * @param debugModeValue boolean - How to debug the code.
     */
    public CheckDevicesCredentialProofTask(
            Node node,
            Controller controllerImpl,
            boolean debugModeValue) {
        this.node = node;
        this.controllerImpl = controllerImpl;
        this.debugModeValue = debugModeValue;
    }

    @Override
    public void run() {
        logger.info("+----- (Hyperledger Bundle) Checking devices credential proofs -----+\n");

        try {
            this.node.loadConnectedDevices();

            List<String> deviceIdsAuths = new ArrayList<>();
            JsonObject jsonResponse = new JsonObject();

            /* Send request for presentation */
            for (Map.Entry<String, String> connectionIdDeviceNode : this.node
                    .getConnectionIdDeviceNodes().entrySet()) {
                String deviceId = connectionIdDeviceNode.getKey();

                printlnDebug("| Device ID: " + deviceId);

                this.controllerImpl.sendRequestPresentationRequest(connectionIdDeviceNode.getValue());

                long start = System.currentTimeMillis();
                long end = start + TIMEOUT_IN_SECONDS * 1000;

                /*
                 * Enquanto o dispositivo ainda não provou que possui uma
                 * credencial verificável e o tempo configurado não terminou.
                 */
                while (!this.controllerImpl.isProofOfCredentialReceived() &&
                        System.currentTimeMillis() < end) {
                }

                this.controllerImpl.setProofOfCredentialReceived(false);

                deviceIdsAuths.add(deviceId);
            }

            printlnDebug(
                    "(Hyperledger Bundle) Amount Autheticated Devices: " +
                            deviceIdsAuths.size() + " of " + 
                            this.node.getDevices().size() +
                            "\n");

            printlnDebug("+-------------------------------------------------------------------+\n");

            jsonResponse.addProperty("authDevices", deviceIdsAuths.toString());
            this.node.getController().getMQTTClientHost().publish(TOPIC, jsonResponse.toString().getBytes(), 1);
        } catch (Exception e) {
            logger.severe("!Error when publishing to send authenticated devices!");
            logger.severe(e.getMessage());
            logger.severe(e.getStackTrace().toString());
            this.cancel();
        }
    }

    private void printlnDebug(String str) {
        if (debugModeValue) {
            logger.info(str);
        }
    }
}
