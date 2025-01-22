// SPDX-FileCopyrightText: 2024 tracetronic GmbH
//
// SPDX-License-Identifier: Apache-2.0

package de.tracetronic.edc.extension.DataProcessingService;

import org.eclipse.edc.spi.monitor.Monitor;

import de.tracetronic.edc.extension.DataProcessingService.Metadata.ComplianceManager;
import de.tracetronic.edc.extension.DataProcessingService.Requests.DataTransferRequest;
import de.tracetronic.edc.extension.DataProcessingService.Requests.RegisterServiceRequest;
import de.tracetronic.edc.extension.DataProcessingService.Requests.ServiceMetadataRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * Api Controller that defines new endpoints for services
 */
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/service")
public class ServiceApiController {

    private Monitor monitor;
    private DataTransferManager dataTransferManager;
    private ComplianceManager complianceManager;
    //private ExecutorService executorService;

    public ServiceApiController(Monitor monitor, DataTransferManager dataTransferManager) {
        this.monitor = monitor;
        this.dataTransferManager = dataTransferManager;
        this.monitor.debug("Initialized ServiceApiController");
        this.complianceManager = new ComplianceManager(monitor);
        //this.executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * API Endpoint to register a service corresponding to an asset-id
     */
    @POST
    @Path("/register-service-to-asset")
    public Response registerServiceToAsset(RegisterServiceRequest request) {

        if (!VerficationUtils.verifyAssetExists(request.getAssetId()) | !VerficationUtils.verifyFilePath(request.getServiceScriptPath())) {
            monitor.severe("Registering service failed!");
            return Response.status(404).entity("Failed to register service").build();
        }

        DataStore.registerServiceToAsset(request.getAssetId(), request.getServiceScriptPath());

        return Response.ok().entity("Successfully registered service").build();
    }

    /**
     * API Endpoint to start the transfer of data to the provider
     */
    @POST
    @Path("/start-data-transfer")
    public Response startDataTransfer(DataTransferRequest request) {
        monitor.info("Received data transfer request for contract: " + request.getContractAgreementId());
        Boolean error = false;
        if (!VerficationUtils.verifyContractAgreement(request.getContractAgreementId())) {
            error = true;
        }
        if (!VerficationUtils.verifyFilePathsForTransfer(request.getFileList())) {
            error = true;
        }
        if (request.getCounterPartyAddress() == null) {
            monitor.severe("Missing CounterPartyAddress field!");
            error = true;
        }
        if (error) {
            monitor.severe("Aborting file transfer");
            return Response.status(Status.BAD_REQUEST).entity("File transfer aborted").build();
        }
        if (request.getReturnFolderPath() != null) {
            DataStore.registerResultFolderToContractAgreement(request.getContractAgreementId(), request.getReturnFolderPath());
        } else {
            monitor.info("No return folder path has been given, result will be saved to the default folder");
        }
        monitor.info("Starting data transfer to the provider");

        boolean transferSuccessful = dataTransferManager.initiateDataTransfer(request.getContractAgreementId(), request.getFileList(), request.getCounterPartyAddress());

        if (transferSuccessful) {
            monitor.info("Data transfer to the provider was successful");
            return Response.ok().entity("Data transfer was successful").build();
        }
        monitor.severe("Data transfer aborted");
        return Response.status(400).entity("Data transfer could not be initiated, see logs for the reason").build();
    }

    /**
     * API Endpoint to start the transfer of the result to the consumer
     */
    @POST
    @Path("/start-result-transfer")
    public Response startResultTransfer(DataTransferRequest request) {
        monitor.info("Received result data transfer request for contract: " + request.getContractAgreementId());
        Boolean error = false;
        if (!VerficationUtils.verifyContractAgreement(request.getContractAgreementId())) {
            error = true;
        }
        if (!VerficationUtils.verifyFilePathsForTransfer(request.getFileList())) {
            error = true;
        }
        String counterPartyAddress = DataStore.getCallbackAddressFromContractAgreementId(request.getContractAgreementId());
        if (counterPartyAddress == null) {
            monitor.severe("Failed to find callback address for the given contract agreement");
            error = true;
        }
        if (error) {
            monitor.severe("Aborting file transfer");
            return Response.status(Status.BAD_REQUEST).entity("File transfer aborted").build();
        }
        monitor.info("Starting result transfer to the consumer");

        boolean transferSuccessful = dataTransferManager.initiateDataTransfer(request.getContractAgreementId(), request.getFileList(), counterPartyAddress);

        if (transferSuccessful) {
            monitor.info("Result transfer to the consumer was successful");
            monitor.info("Deleting consumer files");
            if (VerficationUtils.deleteConsumerFiles(request.getFileList(), request.getContractAgreementId())) {
                monitor.info("Successfully deleted the consumer files");
            } else {
                monitor.severe("Error deleting the consumer files");
            }
            return Response.ok().entity("Result transfer to the consumer was successful").build();
        }
        monitor.severe("Data transfer aborted!");
        return Response.status(400).entity("Data transfer could not be initiated, see logs for the reason").build();
    }

    /**
     * API endpoint to trigger the creation of the metadata for the service
     */
    @POST
    @Path("/send-service-metadata-to-fc")
    public Response sendServiceMetadataToFederatedCatalogue(ServiceMetadataRequest request) {
        complianceManager.handleMetadataCreationAndFederatedCatalogue(request);
        return Response.ok().entity("Successfully sent metadata to the federated catalogue").build();
    }

}
