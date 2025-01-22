// SPDX-FileCopyrightText: 2024 tracetronic GmbH
//
// SPDX-License-Identifier: Apache-2.0

package de.tracetronic.edc.extension.DataProcessingService.Metadata;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.edc.spi.monitor.Monitor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import de.tracetronic.edc.extension.DataProcessingService.Requests.ServiceMetadataRequest;

/**
 * This class handles the creation of the Gaia-X compliant metadata for the service. The class mainly makes use of two other classes, to orchestrate the creation.
 */
public class ComplianceManager {

    private final Monitor monitor;
    private final MetadataApiClient metadataApiClient;
    private final MetadataBuilder metadataBuilder;

    private String username;
    private String password;
    private String clientSecret;

    public ComplianceManager(Monitor monitor) {
        this.monitor = monitor;
        this.metadataApiClient = new MetadataApiClient(monitor);
        this.metadataBuilder = new MetadataBuilder(monitor);
    }

    /** 
     * This method acts as an overview for the metadata creation process. It calls all other relevant methods which
     * call the needed endpoints and adjust the data
     * @param serviceMetadataRequest The request that is send by the user to the endpoint
     */
    public void handleMetadataCreationAndFederatedCatalogue(ServiceMetadataRequest serviceMetadataRequest) {
        if (!loadCredentials()) {
            monitor.severe("Failed to load credentials, aborting the loading of credentials");
        }
        String generatedClaims = metadataApiClient.generateClaims(serviceMetadataRequest);
        if (generatedClaims == null) {
            monitor.severe("Failed to generate claims from the Claim Compliance Provider 'generate-claims' endpoint");
            return;
        }
        List<String> credentialList = metadataApiClient.loadCredentials(serviceMetadataRequest); 
        if (credentialList == null) {
            monitor.severe("Failed to fetch given credentials");
            return;
        }
        JsonNode adjustedClaims = metadataBuilder.adjustClaims(serviceMetadataRequest, generatedClaims, credentialList);
        if (adjustedClaims == null) {
            monitor.severe("Failed to adjust the given claims");
            return;
        }
        ArrayNode jsonVPs = metadataApiClient.sendClaims(adjustedClaims);
        if (jsonVPs == null) {
            monitor.severe("Failed to generate Verifiable Presentation from the claims using the Claim Compliance Provider 'send-claims' endpoint");
            return;
        }
        if (!metadataApiClient.getFCAuth(this.username, this.password, this.clientSecret)) {
            monitor.severe("Failed to receive Authorization for the Federated Catalogue");
        }
        if (!metadataApiClient.sendVpToFC(jsonVPs.get(0).toPrettyString())) {
            monitor.severe("Failed to send the first Verifiable Presentation to the Catalogue");
        }
        if (!metadataApiClient.sendVpToFC(jsonVPs.get(1).toPrettyString())) {
            monitor.severe("Failed to send the second Verifiable Presentation to the Catalogue");
        }
        monitor.info("Metadata was successfully uploaded into the Federated Catalogue!");
    }

    /**
     * Loads the credentials from the /resources/credentials/credentials.json file
     * @return true if the credentials could be loaded successfully, otherwise false
     */
    private boolean loadCredentials() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode jsonNode = mapper.readTree(new File("data-processing-service/resources/credentials/fc_credentials.json"));
            username = jsonNode.get("username").asText();
            password = jsonNode.get("password").asText();
            clientSecret = jsonNode.get("client_secret").asText();
            if (username.isEmpty() || username.isBlank() || password.isEmpty() || password.isBlank() || clientSecret.isEmpty() || clientSecret.isBlank()) {
                monitor.severe("Credentials were (partially) empty, please input the credentials for the federated catalogue in the fc_credentials.json file under ./resources/credentials/");
                return false;
            }
            return true;
        } catch (IOException e) {
            monitor.severe("Failed to load the ./resources/credentials/fc_credentials.json file, please input your credentials in the correct JSON format");
            return false;
        }
    }
    
}
