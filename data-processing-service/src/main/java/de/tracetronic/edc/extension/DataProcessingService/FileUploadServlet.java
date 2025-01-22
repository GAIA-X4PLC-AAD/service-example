// SPDX-FileCopyrightText: 2024 tracetronic GmbH
//
// SPDX-License-Identifier: Apache-2.0

package de.tracetronic.edc.extension.DataProcessingService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.File;
import java.io.IOException;

import org.eclipse.edc.spi.monitor.Monitor;

/**
 * Servlet that contains the endpoint for receiving file uploads
 */
@WebServlet("/upload")
public class FileUploadServlet extends HttpServlet {

    private Monitor monitor;
    private final String UPLOAD_DIR;

    public FileUploadServlet(Monitor monitor) {
        this.monitor = monitor;
        this.UPLOAD_DIR = DataStore.getDefaultFileDir();
        initDirectory();
    }

    /**
     * Make sure the directory in which the uploaded files should be stored exists
     */
    private void initDirectory() {
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) {
            monitor.debug("upload folder path doesnt exist yet");
            Boolean created = uploadDir.mkdirs();
            monitor.debug("Folder created:" + created);
        } else {
            monitor.debug("dir exists");
        }
    }

    
    /** 
     * Helper method for creating a directory
     */
    private void createDirectoryForCurrentContractAgreement(String folderPath) {
        File dir = new File(folderPath);
        dir.mkdirs();
    }

    /**
     * Endpoint for the file transfer
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String contractAgreementId = request.getHeader("ContractAgreementId");
        if (!VerficationUtils.verifyContractAgreement(contractAgreementId)) {
            response.getWriter().println("ContractAgreementId is invalid");
            return;
        }
        String callbackAddress = request.getHeader("callbackAddress");
        if (callbackAddress != null) {
            DataStore.saveCallbackAddress(contractAgreementId, callbackAddress);
        }
        String folderPath;
        if (DataStore.getResultFolderFromContractAgreementId(contractAgreementId) != null) {
            folderPath = DataStore.getResultFolderFromContractAgreementId(contractAgreementId);
        } else {
            folderPath = UPLOAD_DIR + File.separator + contractAgreementId;
        }
        DataStore.registerDataStorageFolderToContractAgreement(contractAgreementId, folderPath);
        createDirectoryForCurrentContractAgreement(folderPath);
        //Receive each file
        try {
            for (Part part : request.getParts()) {
                String fileName = extractFileName(part);
                monitor.debug("Receiving file: " + fileName);
                String filePath = folderPath + File.separator + fileName;
                if (new File(filePath).exists()) {
                    monitor.debug("Overwriting existing file");
                    new File(filePath).delete();
                }
                monitor.debug("writing file to: " + filePath);
                part.write(filePath);
            }
        } catch (Exception e) {
            monitor.debug("Exception occurred during file transfer process");
            e.printStackTrace();
            return;
        }
        monitor.debug("Received all files");

        response.getWriter().println("Files uploaded successfully.");
        
        String assetId = VerficationUtils.getAssetIdFromContractAgreement(contractAgreementId);
        String serviceScriptPath = DataStore.getServiceFromAssetId(assetId);
        //Check if theres a service for that asset, if yes execute the service
        if (serviceScriptPath != null) {
            triggerService(serviceScriptPath, contractAgreementId, folderPath);

        }
    }

    /**
     * Utility method to extract the file name from the currently transferring file
     * @param part part that gets received by the http file transfer request
     * @return the file name of the part (file)
     */
    private String extractFileName(Part part) {
        String contentDisposition = part.getHeader("content-disposition");
        for (String token : contentDisposition.split(";")) {
            if (token.trim().startsWith("filename")) {
                return token.substring(token.indexOf("=") + 2, token.length() - 1);
            }
        }
        return "";
    }
    
    /**
     * Trigger the service corresponding to the contract agreement
     * @param contractAgreementId
     */
    private void triggerService(String serviceScriptPath, String contractAgreementId, String folderPath) {
        monitor.info("Executing the following service script: " + serviceScriptPath);
        String command = "/bin/bash " + serviceScriptPath + " " + contractAgreementId + " " + folderPath;
        try {
            Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}