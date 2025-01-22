// SPDX-FileCopyrightText: 2024 tracetronic GmbH
//
// SPDX-License-Identifier: Apache-2.0

package de.tracetronic.edc.extension.DataProcessingService;

import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.services.spi.asset.AssetService;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;

/*
 * This is the starting point of the DataProcessingService Extension. This extension is integrated into the EDC connector and loaded on startup.
 */
public class DataProcessingServiceExtension implements ServiceExtension {

    //Constants for accessing Configurations
    private static final String SERVER_PORT_STRING = "web.http.server.service.port";
    private static final String SERVER_DEFAULT_SAVE_FOLDER_STRING = "web.http.server.service.folder.default";
    private static final String DEFAULT_CALLBACK_ADDRESS = "web.http.server.service.callback.address";

    public static final String USE_POLICY = "use-eu";

    //Inject Monitor for status updates and debugging
    @Inject
    private Monitor monitor;

    //Inject WebService for registering new REST-APIs
    @Inject
    private WebService webService;

    //Inject ContractNegotiationStore to access ContractAgreements
    @Inject 
    private ContractNegotiationStore contractNegotiationStore;
    
    //Inject AssetService to access registered Assets
    @Inject 
    private AssetService assetService;

    //Declare DataTransferManager 
    private DataTransferManager dataTransferManager;

    
    /** 
     * This method gets called on EDC connector startup, it is used initialize the extension
     * @param context provided by the EDC connector
     */
    @Override
    public void initialize(ServiceExtensionContext context) {

        //Load configurations and save them in the data store
        DataStore.initialize(
            context.getSetting(SERVER_PORT_STRING, "12345"), 
            context.getSetting(SERVER_DEFAULT_SAVE_FOLDER_STRING, "./"), 
            context.getSetting(DEFAULT_CALLBACK_ADDRESS, ""));
        if (DataStore.getConfiguredCallbackAddress() == "") {
            monitor.severe("No valid callback address has been set in the config");
        }
        //Initialize static helper class
        VerficationUtils.initialize(monitor, contractNegotiationStore, assetService);

        //Instantiate DataTransferManager
        this.dataTransferManager = new DataTransferManager(monitor);

        //Register ServiceApiController as a new REST-API endpoint
        webService.registerResource(new ServiceApiController(monitor, dataTransferManager));
        
        //Start FileReceiver
        new Thread(new FileUploadServer(monitor)).start();
        monitor.info("Data Processing Service Extension Initialized!");
    }

}
