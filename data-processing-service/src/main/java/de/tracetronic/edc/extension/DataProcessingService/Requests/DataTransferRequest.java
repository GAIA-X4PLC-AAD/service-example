// SPDX-FileCopyrightText: 2024 tracetronic GmbH
//
// SPDX-License-Identifier: Apache-2.0

package de.tracetronic.edc.extension.DataProcessingService.Requests;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;

/*
 * Class for modelling a DataTransferRequest that is used in the REST API to initiate a file transfer
 */
public class DataTransferRequest {

    @JsonProperty("contractAgreementId")
    @NotNull(message = "contractAgreementId is required")
    private String contractAgreementId;

    @JsonProperty("counterPartyAddress")
    private String adress;

    @JsonProperty("filesToSend")
    @NotNull(message = "filesToSend is required")
    private List<String> fileList;

    @JsonProperty("returnFolderPath")
    private String returnFolderPath;


    public String getContractAgreementId() {
        return contractAgreementId;
    }

    public String getCounterPartyAddress() {
        return adress;
    }

    public List<String> getFileList() {
        return fileList;
    }

    public String getReturnFolderPath() {
        return returnFolderPath;
    }

    
}
