package br.uefs.larsid.dlt.iot.soft.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.logging.Logger;

import br.uefs.larsid.dlt.iot.soft.model.Node;
import br.uefs.larsid.dlt.iot.soft.services.Controller;

public class CheckDevicesCredentialProofTask extends TimerTask {
    private static final int TIMEOUT_IN_SECONDS = 15;

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
        logger.info("+----- Checking devices credential proofs -----+\n");

        try {
            this.node.loadConnectedDevices();

            printlnDebug("Devices: " + this.node.getDevices().size() + "\n");

            List<String> deviceIdsAuths = new ArrayList<>();

            /* Send request for presentation */
            for (Map.Entry<String, String> connectionIdDeviceNode : this.node
                    .getConnectionIdDeviceNodes().entrySet()) {
                String deviceId = connectionIdDeviceNode.getKey();

                // printlnDebug("------------- Proof of Credential -------------");
                printlnDebug("| Device ID: " + deviceId);
                // printlnDebug("-----------------------------------------------\n");

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
                    "(Hyperledger Bundle) Autheticated Devices = " +
                            deviceIdsAuths +
                            "\n");
        } catch (Exception e) {
            logger.severe("Unable to update device list.");
            logger.severe(e.getMessage());
            logger.severe(e.getStackTrace().toString());
            this.cancel();
        }

        printlnDebug("+----------------------------------------------+\n");
    }

    private void printlnDebug(String str) {
        if (debugModeValue) {
            logger.info(str);
        }
    }
}
