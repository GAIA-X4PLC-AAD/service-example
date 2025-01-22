// SPDX-FileCopyrightText: 2024 tracetronic GmbH
//
// SPDX-License-Identifier: Apache-2.0

package de.tracetronic.edc.extension.DataProcessingService;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.services.spi.asset.AssetService;
import org.eclipse.edc.spi.monitor.Monitor;

/**
 * Utility class used throughout the project for verifying different information with the connector
 * Also used to interact with the filesystem, for verifying if files exists and for deleting them
 */
public class VerficationUtils {
    
    private static Boolean isInitialized = false;
    private static Monitor monitor;
    private static ContractNegotiationStore contractNegotiationStore;
    private static AssetService assetService;

    /**
     * Initializes the static class with useful objects 
     * @return true when successfully initialized and false if it was already initialized
     */
    public static Boolean initialize(Monitor monitor, ContractNegotiationStore contractNegotiationStore, AssetService assetService) {
        if (!isInitialized) {
            isInitialized = true;
            VerficationUtils.monitor = monitor;        
            VerficationUtils.contractNegotiationStore = contractNegotiationStore;
            VerficationUtils.assetService = assetService;
            return true;
        }
        return false;
    }

    /**
     * Verifies a contract agreement id by accessing the EDC contract agreement store
     * @return true when contract exists and false if it does not
     */
    public static boolean verifyContractAgreement(String contractAgreementId) {
        monitor.info("Verifying contract: " + contractAgreementId);
        if (DataStore.DEBUG_MODE) return true; //Debug setting
        ContractAgreement contractAgreement = contractNegotiationStore.findContractAgreement(contractAgreementId);
        if (contractAgreement == null) {
            monitor.severe("Contract agreement id is not valid");
            return false;
        }
        return true;
    }

    /**
     * Verifies if an asset exists by using the EDC asset service
     * @return true when asset exists and false if it doesnt exist
     */
    public static Boolean verifyAssetExists(String assetId) {
        Asset asset = assetService.findById(assetId);
        if (asset == null) {
            monitor.severe("Couldn't find asset with ID " + assetId);
            return false;
        }
        monitor.info("Registered Service to Asset: " + asset.getId());
        return true;
    }

    /**
     * Fetches the contract-agreement from an id and returns the asset string
     * @param contractAgreementId contract agreement id
     * @return the asset id corresponding to the contract agreement or null if the contract agreement id is invalid
     */
    public static String getAssetIdFromContractAgreement(String contractAgreementId) {
        ContractAgreement contractAgreement = contractNegotiationStore.findContractAgreement(contractAgreementId);
        if (contractAgreement != null) {
            return contractAgreement.getAssetId();
        }
        return null;
    }

    /**
     * Verifies if all file paths in the given list exist
     * @return true all files exist and files when one or more files don't exist
     */
    public static Boolean verifyFilePathsForTransfer(List<String> fileList) {
        Boolean filesExist = true;
        for (String filePath: fileList) {
            filesExist &= verifyFilePath(filePath);
        }
        return filesExist;
    }

    /**
     * Verifies a file path exists
     * @return true if the file path exists and false if it doesn't
     */
    public static Boolean verifyFilePath(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return false;
            }
        } catch (InvalidPathException invalidPathException) {
            monitor.severe("Couldn't find or parse the following filepath: " + filePath);
        }
        return true;
    }

    /**
     * Deletes all files that belong to the consumer, which are the initially transferred files and the resulting files
     * @param resultFileList the resulting files, which are provided when transferring them to the consumer
     * @param contractAgreementId contract agreement id
     * @return true if all files were successfully deleted, false otherwise
     */
    public static Boolean deleteConsumerFiles(List<String> resultFileList, String contractAgreementId) {
        for (String filePath : resultFileList) {
            Path path = Paths.get(filePath);
            try {
                Files.delete(path);
            } catch (IOException e) {
                monitor.severe("Error deleting the following file" + filePath);
                return false;
            }
        }
        Path dataStorageFolder = Paths.get(DataStore.getDataStorageFolderFromContractAgreement(contractAgreementId));
        if (Files.isDirectory(dataStorageFolder)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dataStorageFolder)) {
                for (Path filePath : stream) {
                    if(!Files.isDirectory(filePath)) {
                        Files.delete(filePath);
                    }
                }
            } catch (IOException e) {
                monitor.severe("Error deleting consumer data");
                return false;
            }
        }
        return true;
    }

}
