// SPDX-FileCopyrightText: 2024 tracetronic GmbH
//
// SPDX-License-Identifier: Apache-2.0

package de.tracetronic.edc.extension.DataProcessingService.Requests;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;

/*
 * Class for modelling a RegisterServiceRequest that is used in the REST API for registering a service to an asset
 */
public class RegisterServiceRequest {

    @JsonProperty("assetId")
    @NotNull(message = "assetId is required")
    private String assetId;

    @JsonProperty("serviceScriptPath")
    @NotNull(message = "serviceScriptPath is required")
    private String serviceScriptPath;

    public String getAssetId() {
        return assetId;
    }

    public String getServiceScriptPath() {
        return serviceScriptPath;
    }

}
