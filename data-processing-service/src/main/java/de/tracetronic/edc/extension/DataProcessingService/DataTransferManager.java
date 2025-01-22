// SPDX-FileCopyrightText: 2024 tracetronic GmbH
//
// SPDX-License-Identifier: Apache-2.0

package de.tracetronic.edc.extension.DataProcessingService;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.security.KeyStore;
import java.util.List;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.eclipse.edc.spi.monitor.Monitor;


/**
 * Class that manages the initiating of data transfers
 */
public class DataTransferManager {

    private Monitor monitor;

    public DataTransferManager(Monitor monitor) {
        this.monitor = monitor;
        if (DataStore.USE_HTTPS) {
            initializeHttps();
        }
    }

    /**
     * Initialize a https connection if needed. *Currently NOT TESTED*
     */
    private void initializeHttps() {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream("data-processing-service/resources/certs/cert.pfx")) {
                keyStore.load(fis, "123456".toCharArray());
            }

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, "123456".toCharArray());

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

            SSLContext.setDefault(sslContext);
        } catch (Exception exception) {
            monitor.severe("failed to setup Keystore");
        }
    }

    
    /** 
     * Wrapper method for starting the data transfer
     * @param contractAgreementId contrac agreement id
     * @param files list of files that should be sent
     * @param host the host that should receive the files
     * @return true if the data transfer was successful, false otherwise
     */
    public boolean initiateDataTransfer(String contractAgreementId, List<String> files, String host) {
        try {
            startDataTransfer(contractAgreementId, files, host);
            return true;
        } catch (IOException ioException) {
            monitor.severe("Failure when transferring data");
            return false;
        }
    }

    /**
     * Method that does the actual file transfer using a HTTP multipart/form-data request
     */
    private void startDataTransfer(String contractAgreementId, List<String> fileList, String host) throws IOException {
        String boundary = "===" + System.currentTimeMillis() + "===";
        String lineFeed = "\r\n";
        //Create the HTTP connection to the host and configure the headers
        HttpURLConnection httpConn = (HttpURLConnection) new URL("http://" + host + "/upload").openConnection();
        httpConn.setUseCaches(false);
        httpConn.setDoOutput(true);
        httpConn.setDoInput(true);
        httpConn.setRequestMethod("POST");
        httpConn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        httpConn.setRequestProperty("ContractAgreementId", contractAgreementId);
        httpConn.setRequestProperty("callbackAddress", DataStore.getConfiguredCallbackAddress());
        //Get output stream from the connection and start sending the files
        try (OutputStream outputStream = httpConn.getOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true)) {
            monitor.debug("established connection, sending files");
            //Send each file in a loop
            for (String filePath : fileList) {
                //Send the header for each file
                File file = new File(filePath);
                String fileName = file.getName();
                writer.append("--" + boundary).append(lineFeed);
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"").append(lineFeed);
                writer.append("Content-Type: " + Files.probeContentType(file.toPath())).append(lineFeed);
                writer.append("Content-Transfer-Encoding: binary").append(lineFeed);
                writer.append(lineFeed);
                writer.flush();
                //Read the actual file and send it
                try (FileInputStream inputStream = new FileInputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    outputStream.flush();
                }

                writer.append(lineFeed);
                writer.flush();
            }
            writer.append("--" + boundary + "--").append(lineFeed);
            //Close connection and check response from the receiver
            writer.close();

            int responseCode = httpConn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                monitor.info("Files uploaded successfully.");
            } else {
                monitor.severe("File upload failed. Server returned response code: " + responseCode);
            }
        }
    }

}