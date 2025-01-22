// SPDX-FileCopyrightText: 2024 tracetronic GmbH
//
// SPDX-License-Identifier: Apache-2.0

package de.tracetronic.edc.extension.DataProcessingService.Requests;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;

/**
 * Class for modelling a ServiceMetadataRequest which is used in the REST API for generating the Gaia-X compliant metadata
 */
public class ServiceMetadataRequest {

    @JsonProperty("identifierPrefix")
    @NotNull(message = "identifierPrefix is required")
    private String identifierPrefix;

    @JsonProperty("legalParticipant")
    @NotNull(message = "legalParticipant is required")
    private String legalParticipant;

    @JsonProperty("legalRegistration")
    @NotNull(message = "legalRegistration is required")
    private String legalRegistration;

    @JsonProperty("termsAndConditions")
    @NotNull(message = "termsAndConditions is required")
    private String termsAndConditions;

    @JsonProperty("physicalResourceParticipant")
    private String physResParticipant;
    
    @JsonProperty("physicalResourceLocation")
    @NotNull(message = "physicalResourceLocation is required")
    private String physResLocation;

    @JsonProperty("physicalResourceName")
    @NotNull(message = "physicalResourceName is required")
    private String physResName;

    @JsonProperty("physicalResourceDescription")
    @NotNull(message = "physicalResourceDescription is required")
    private String physResDescription;

    @JsonProperty("softwareResourceName")
    @NotNull(message = "softwareResourceName is required")
    private String softResName;

    @JsonProperty("softwareResourceDescription")
    @NotNull(message = "softwareResourceDescription is required")
    private String softResDescription;

    @JsonProperty("serviceAccessPointName")
    @NotNull(message = "serviceAccessPointName is required")
    private String seAcPoName;

    @JsonProperty("serviceAccessPointHost")
    @NotNull(message = "serviceAccessPointHost is required")    
    private String seAcPoHost;

    @JsonProperty("serviceAccessPointProtocol")
    @NotNull(message = "serviceAccessPointProtocol is required")
    private String seAcPoProtocol;

    @JsonProperty("serviceAccessPointVersion")
    @NotNull(message = "serviceAccessPointVersion is required")
    private String seAcPoVersion;

    @JsonProperty("serviceAccessPointPort")
    @NotNull(message = "serviceAccessPointPort is required")
    private String seAcPoPort;

    @JsonProperty("serviceAccessPointOpenAPI")
    @NotNull(message = "serviceAccessPointOpenAPI is required")
    private String seAcPoOpenAPI;

    @JsonProperty("serviceName")
    @NotNull(message = "serviceName is required")
    private String serviceName;

    @JsonProperty("serviceDescription")
    @NotNull(message = "serviceDescription is required")
    private String serviceDescription;

    @JsonProperty("serviceContractId")
    @NotNull(message = "serviceContractId is required")
    private String serviceContractId;

    @JsonProperty("requiredFiles")
    @NotNull(message = "requiredFiles is required")
    private List<RequiredFile> requiredFiles;

    @JsonProperty("resultingFiles")
    @NotNull(message = "resultingFiles is required")
    private List<ResultingFile> resultingFiles;


    public String getIdentifierPrefix() {
        return identifierPrefix;
    }

    public String getLegalParticipant() {
        return legalParticipant;
    }

    public String getLegalRegistration() {
        return legalRegistration;
    }

    public String getTermsAndConditions() {
        return termsAndConditions;
    }

    public String getPhysResParticipant() {
        return physResParticipant;
    }

    public String getPhysResLocation() {
        return physResLocation;
    }

    public String getPhysResName() {
        return physResName;
    }

    public String getPhysResDescription() {
        return physResDescription;
    }

    public String getSoftResName() {
        return softResName;
    }

    public String getSoftResDescription() {
        return softResDescription;
    }

    public String getSeAcPoName() {
        return seAcPoName;
    }

    public String getSeAcPoHost() {
        return seAcPoHost;
    }

    public String getSeAcPoProtocol() {
        return seAcPoProtocol;
    }

    public String getSeAcPoVersion() {
        return seAcPoVersion;
    }

    public String getSeAcPoPort() {
        return seAcPoPort;
    }

    public String getSeAcPoOpenAPI() {
        return seAcPoOpenAPI;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getServiceDescription() {
        return serviceDescription;
    }

    public String getServiceContractId() {
        return serviceContractId;
    }

    public List<RequiredFile> getRequiredFiles() {
        return requiredFiles;
    }

    public List<ResultingFile> getResultingFiles() {
        return resultingFiles;
    }

    public static class RequiredFile {
        @JsonProperty("description")
        @NotNull(message = "description is required")
        private String description;

        @JsonProperty("specification")
        private String specification;

        @JsonProperty("tooling")
        private String tooling;

        public String getDescription() {
            return description;
        }

        public String getSpecification() {
            return specification;
        }

        public String getTooling() {
            return tooling;
        }
    }

    public static class ResultingFile {
        @JsonProperty("description")
        @NotNull(message = "description is required")
        private String description;

        @JsonProperty("specification")
        @NotNull(message = "specification is required")
        private String specification;

        public String getDescription() {
            return description;
        }

        public String getSpecification() {
            return specification;
        }
    }

}
