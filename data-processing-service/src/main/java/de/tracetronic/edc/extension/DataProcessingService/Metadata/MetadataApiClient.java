// SPDX-FileCopyrightText: 2024 tracetronic GmbH
//
// SPDX-License-Identifier: Apache-2.0

package de.tracetronic.edc.extension.DataProcessingService.Metadata;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.edc.spi.monitor.Monitor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import de.tracetronic.edc.extension.DataProcessingService.Requests.ServiceMetadataRequest;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * This class is used for accessing the REST-API Endpoints required for creating and uploading the service metadata
 */
class MetadataApiClient {

    // Endpoints for creating the metadata
    private final String CLAIM_COMPLIANCE_PROVIDER = "https://claim-compliance-provider.gxfs.gx4fm.org/v1/";
    private final String CCP_GEN_CLAIMS = "generate-claims";
    private final String CCP_SEND_CLAIMS = "send-claims";
    // Federated Catalogue Endpoints for authentication and uploading of self-descriptions
    private final String KEYCLOAK_GET_AUTH = "https://fc-keycloak.gxfs.gx4fm.org/realms/gaia-x/protocol/openid-connect/token";
    private final String FC_UPLOAD_VP = "https://fc-server.gxfs.gx4fm.org/self-descriptions";

    // Variable for saving the auth token 
    private String authToken = "";

    // Http Client used for sending the requests, longer timeout becuase of the CCP endpoints
    private final OkHttpClient CLIENT = new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS).build();
    // Mapper used for handling json
    private final ObjectMapper MAPPER = new ObjectMapper();

    private final Monitor monitor;

    MetadataApiClient (Monitor monitor) {
        this.monitor = monitor;
    }

    /** 
     * Calls the generate-claims endpoint of the CCP
     * @param serviceMetadataRequest Request object provided by the user
     * @return String formatted in JSON that serves as a blueprint of the claims
     */
    String generateClaims(ServiceMetadataRequest serviceMetadataRequest) {
        String requestBody;
        requestBody = MAPPER.createObjectNode()
            .put("identifierPrefix", serviceMetadataRequest.getIdentifierPrefix())
            .put("legalParticipantId", serviceMetadataRequest.getLegalParticipant())
            .put("physicalResourceLegalParticipantId", serviceMetadataRequest.getLegalParticipant())
            .toPrettyString();
        String mediaType = "application/json; charset=utf-8";
        String url = CLAIM_COMPLIANCE_PROVIDER + CCP_GEN_CLAIMS;
        Response response = postRequest(url, requestBody, mediaType);
        try {
            return response.body().string();
        } catch (IOException e) {
            monitor.severe("Failed to get generated claims from the response of the Claim Compliance Provider");
            return null;
        }
    }

    
    /** 
     * Retrieves the provided credentials via their links via simple GET requests
     * @param serviceMetadataRequest Request object provided by the user
     * @return List<String> that includes the credentials, the strings are formatted in JSON
     */
    List<String> loadCredentials(ServiceMetadataRequest serviceMetadataRequest) {
        List<String> credentialList = new ArrayList<>();
        for (String credentialLink : List.of(serviceMetadataRequest.getLegalParticipant(), serviceMetadataRequest.getLegalRegistration(), serviceMetadataRequest.getTermsAndConditions())) {
            Request request = new Request.Builder()
                .url(credentialLink)
                .get()
                .build();
            try {
                Response response = CLIENT.newCall(request).execute();
                credentialList.add(response.body().string());
            } catch (IOException e) {
                monitor.severe("Failed to fetch the following credential: " + credentialLink);
                return null;
            }
        }
        return credentialList;
    }

    
    /** 
     * Sends the adjusted claims to the CCP send-claims endpoint and retrieves the VPs
     * @param claims that were built by the MetadataBuilder
     * @return The three verifiable presentations in the form of an ArrayNode
     */
    ArrayNode sendClaims(JsonNode claims) {
        String url = CLAIM_COMPLIANCE_PROVIDER + CCP_SEND_CLAIMS;
        String requestBody = claims.toPrettyString();
        String mediaType = "application/json; charset=utf-8";
        Response response = postRequest(url, requestBody, mediaType);
        try {
            String stringVPs = response.body().string();
            ArrayNode jsonVPs = (ArrayNode) MAPPER.readTree(stringVPs);
            return jsonVPs;
        } catch (IOException e) {
            monitor.severe("Failed to extract or parse the verifiable presentations");
            return null;
        }
    }

    
    /** 
     * Retrieves an auth token from the federated catalogue, which is needed to interact with the it
     * @param username the username of the user
     * @param password the password of the user
     * @param clientSecret the client secret of the user
     * @return true if the authentication was successful, otherwise false
     */
    boolean getFCAuth(String username, String password, String clientSecret) {
        String url = KEYCLOAK_GET_AUTH;
        RequestBody formBody = new FormBody.Builder()
            .add("username", username)
            .add("password", password)
            .add("client_id", "federated-catalogue")
            .add("client_secret", clientSecret)
            .add("grant_type", "password")
            .add("scope", "openid")
            .build();
        Response response = postRequest(url, formBody);
        try {
            JsonNode responseNode = MAPPER.readTree(response.body().string());
            this.authToken = responseNode.get("access_token").asText();
            return true;
        } catch (IOException ioException) {
            monitor.severe("IOException occured when calling the authorization endpoint of the keycloak server");
            return false;
        }
    }

    
    
    /** 
     * Sends a verifiable presentation to the federated catalogue
     * @param VP verifiable presenation to send
     * @return true if successful, otherwise false
     */
    boolean sendVpToFC(String VP) {
        String url = FC_UPLOAD_VP;
        Response response = postRequest(url, VP, "application/json; charset=utf-8", "Bearer " + this.authToken);
        response.close();
        if (response.isSuccessful()) {
            return true;
        }
        return true;
    }

    /**
     * Wrapper for the postRequest Method
     */
    private Response postRequest(String url, String body, String mediaType) {
        RequestBody requestBody = RequestBody.create(body, MediaType.get(mediaType));
        return postRequest(url, requestBody, null);
    }

    /**
     * Wrapper for the postRequest Method
     */
    private Response postRequest(String url, RequestBody body) {
        return postRequest(url, body, null);
    }

    /**
     * Wrapper for the postRequest Method
     */
    private Response postRequest(String url, String body, String mediaType, String authHeader) {
        RequestBody requestBody = RequestBody.create(body, MediaType.get(mediaType));
        return postRequest(url, requestBody, authHeader);
    }

    /**
     * Helper method used for creating the REST requests
     * @param url url of the endpoint
     * @param body body/payload for the request
     * @param authHeader auth header for the federated catalogue if needed
     * @return the response if the response was successful, otherwise null
     */
    private Response postRequest(String url, RequestBody body, String authHeader) {
        Request request;
        if (authHeader != null) {
            request = new Request.Builder()
            .url(url)
            .header("Authorization", authHeader)
            .post(body)
            .build();
        } else {
            request = new Request.Builder()
            .url(url)
            .post(body)
            .build();
        }
        try {
            Response response = CLIENT.newCall(request).execute();
            if (!response.isSuccessful()) monitor.severe("Unexpected code " + response);
            return response;
        } catch (IOException ioException) {
            monitor.severe("Failed to reach the following url: " + url);
            return null;
        }
    }
    
}
