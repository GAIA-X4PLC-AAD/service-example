// SPDX-FileCopyrightText: 2024 tracetronic GmbH
//
// SPDX-License-Identifier: Apache-2.0

package de.tracetronic.edc.extension.DataProcessingService;
import java.util.HashMap;
import java.util.Map;

/*
 * Static class to store data needed for services in-memory
 */
public class DataStore {

    /**
     * Simple variables and maps used for storing data
     */
    private static boolean isInitialized = false;
    private static String port;
    private static String defaultFileDir;
    private static String callbackAddress;
    public final static boolean DEBUG_MODE = false;
    public final static boolean USE_HTTPS = false;
    private static Map<String, String> ASSET_SERVICE_MAP;
    private static Map<String, String> RESULT_SAVE_FOLDER_MAP;
    private static Map<String, String> DATA_SAVE_FOLDER_MAP;
    private static Map<String, String> CALLBACK_ADDRESS_MAP;

    /**
     * Initialize the maps
     */
    static {
        ASSET_SERVICE_MAP = new HashMap<>();
        RESULT_SAVE_FOLDER_MAP = new HashMap<>();
        DATA_SAVE_FOLDER_MAP = new HashMap<>();
        CALLBACK_ADDRESS_MAP = new HashMap<>();
    }

    /**
     * Initializes the data store with the given params, only works the first time
     * @param port port where the server should be hosted on
     * @param defaultFileDir directory where the received files should be stored
     * @return true it has been initialized and false if it has been initialized before
     */
    public static Boolean initialize(String port, String defaultFileDir, String callbackAddress) {
        if (!isInitialized) {
            isInitialized = true;
            DataStore.port = port;
            DataStore.defaultFileDir = defaultFileDir;
            DataStore.callbackAddress = callbackAddress;
            return true;
        }
        return false;
    }

    /**
     * register a service script to an already existing asset id. Overwrites existing service scripts if one has already been registered to the given asset id
     * @param assetId id of an already registered asset which should function as a service
     * @param serviceScriptPath the path to a bash script that is used to trigger the actual service
     */
    public static void registerServiceToAsset(String assetId, String serviceScriptPath) {
        ASSET_SERVICE_MAP.put(assetId, serviceScriptPath);
    }

    /**
     * Get the service script which is registered to the given asset id
     * @param assetId id of an asset which has a service registered to it
     * @return the file path of the service shell script or null if there is no service registered to the asset
     */
    public static String getServiceFromAssetId(String assetId) {
        return ASSET_SERVICE_MAP.get(assetId);
    }

    /**
     * Define a directory in which the results of the service execution should be saved (service consumer side)
     * @param contractAgreementId Contract agreement id 
     * @param folderPath path to a directory where the resulting files should be saved
    */
    public static void registerResultFolderToContractAgreement(String contractAgreementId, String folderPath) {
        RESULT_SAVE_FOLDER_MAP.put(contractAgreementId, folderPath);
    }

    /**
     * Get the directory for the result which was previously registered to a contract agreement id (service consumer side)
     * @param contractAgreementId Contract agreement id
     * @return the path to the directory or null if there is no directory for that contract agreement id
     */
    public static String getResultFolderFromContractAgreementId(String contractAgreementId) {
        return RESULT_SAVE_FOLDER_MAP.get(contractAgreementId);
    }

    /**
     * Save the directory in which the data of the consumer is stored in (service provider side)
     * @param contractAgreementId Contract agreement id 
     * @param folderPath path to a directory where the consumer data should be saved
     */
    public static void registerDataStorageFolderToContractAgreement(String contractAgreementId, String folderPath) {
        DATA_SAVE_FOLDER_MAP.put(contractAgreementId, folderPath);
    }

    /**
     * Get the directory that contains the consumer data, which was previously registered to a contract agreement id (service provider side)
     * @param contractAgreementId Contract agreement id
     * @return the path to the directory or null if there is no directory for that contract agreement id
     */
    public static String getDataStorageFolderFromContractAgreement(String contractAgreementId) {
        return DATA_SAVE_FOLDER_MAP.get(contractAgreementId);
    }

    /**
     * Save the callback address to the service consumer connector, mapped to the contract agreement id
     * @param contractAgreementId contract agreement id
     * @param callbackAddress callback address of service consumer
     */
    public static void saveCallbackAddress(String contractAgreementId, String callbackAddress) {
        CALLBACK_ADDRESS_MAP.put(contractAgreementId, callbackAddress);
    }

    /**
     * Get the callback address that was saved to a contract agreement id
     * @param contractAgreementId Contract agreement id
     * @return Callback address of service consumer or null if there is none registered
     */
    public static String getCallbackAddressFromContractAgreementId(String contractAgreementId) {
        return CALLBACK_ADDRESS_MAP.get(contractAgreementId);
    }

    /**
     * Get the set port to host the server on
     * @return the port which is set in the config
     */
    public static String getPort() {
        return port;
    }

    /**
     * Get the directory in which received files normally should be saved in
     * @return the default directory for received files which is set in the config
     */
    public static String getDefaultFileDir() {
        return defaultFileDir;
    }

    /**
     * Get the callback address to this connector
     * @return the callback address set in the config
     */
    public static String getConfiguredCallbackAddress() {
        return callbackAddress;
    }


}
