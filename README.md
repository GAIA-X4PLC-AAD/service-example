# EDC Connector Data Processing Service Extension

This repository contains the source code for the data processing service extension for the EDC connector. Elements of the repository, such as the build files and resources, are taken from the eclipse-edc samples repository (https://github.com/eclipse-edc/Samples). The data processing extension was developed in the context of a bachelor's thesis by @ValentinFischerTT. The thesis was written in cooperation with tracetronic GmbH in the context of the Gaia-X 4 PLC-AAD research project.

## Getting Started

To build a connector which includes the extension, simply run:

```console
./gradlew clean build
```

To start a provider or a consumer connector, the provided shell scripts can be used:

```console
./startProvider.sh
```

```console
./startConsumer.sh
```

The started connectors include basic functionality like creating assets and negotiating contracts for these assets. The added functionality of adding and executing services can be used via the new endpoints that are shown in the provided [postman collection](data-processing-service-extension.postman_collection.json).

## Third-Party Software

The software makes use of third-party dependencies. A list of dependencies is provided in the [third-party-software.md](third-party-software.md) file. Note, that their software is only linked via the gradle build file and is not directly distributed by this work, since this work does not provide a compiled jar-file. The dependencies are downloaded and integrated when building the jar-file. This list is maintained manually and has to be adjusted appropriately if relevant changes occur.

## License

This project is licensed under the Apache License 2.0 - see [LICENSE](LICENSE) for details.

## Documentation

This Documentation assumes knowledge of basic EDC Connector functionality and detailes mainly the added service functionality.

The general workflow is detailed in the following [graphic](documentation/service-use-case.pdf).

The referenced REST endpoints are available in the [postman collection](data-processing-service-extension.postman_collection.json).

The complete workflow is directed by the REST requests, 

### Setup a Connector

Besides building and starting the connector with the extension, there are a few things to take care of:

- The extension also uses the ...configuration.properties file in the data-processing-service/resources`folder that is used with the normal connector. There are three added parameters that have to be configured before starting the server:
    - The port on which the server to receive data runs
    - The callback address (and port) on which the server is reachable
    - The path to a folder where the received data should be stored in. **IMPORTANT**: The path has to be an absolute linux path, such as /home/<user>/..., otherwise it wont work.

- The trigger_service.sh shell script is normally used to start the actual service. Of course, another shell script can also be used. This script provides the contractAgreementId and the path to the stored data as a parameter. This script has to be adjusted in such a way that it triggers the actual service (Examples: start an executable, run a python script, access a rest-api). Once the service is finished, the service would have to trigger the transfer of data using the REST endpoint of the service extension.

- The credentials.json file should contain the credentials to access the federate catalogue, if a service provider plans to create and upload the metadata using this extension.

### Postman Collection

In this following section, all entries in the Postman collection will be explained. Bold entries are for Endpoints of the data processing service extension. The other ones are used for normal EDC Endpoints. The entries are ordered sequentially, meaning the mostly depend one one another and should normally be executed in the given order.

1. Provider Asset Creation - Create an asset

2. Provider Policy Creation - Create a policy

3. Provider Contract Creation - Create a contract

4. **Provider Register Service to Asset** - register a shell script to an assetId.

5. Provider Get Contract Id - Get the contract id for a created asset (used to get the contract id for the metadata).

6. **Provider Create Service Metadata** - Create service metadata and upload it to the Federated Catalogue.

7. Consumer Get Dataset - Get the contract id from the provider, in case it is not easily visible in the metadata.

8. Consumer Contract Negotiation - start a contract negotiation with the service provider using the contract id.

9. Consumer Contract Agreement - using the id from the response above, retrieve the contract agreement id.

10. **Consumer Send Data** - input the files to send to the service provider along with the contract agreement id for verification.

11. **Provider Send Result** - similar to the above endpoint, the provider sends the result back to the consumer, also using the provided contract agreement id. The address of the consumer was saved and is chosen automatically, so it does not have to be provided in the request.

### Provider: Setup a Service

1. A provider can create a service by creating a normal EDC connector asset. The actual data that is provided to the asset is not important. Of course, a policy and a contract definition also have to be created for that asset. This is standard EDC functionality and is represented by the postman requests 1, 2 and 3.

2. The next step is to use the /register-service-to-asset REST endpoint provided by the service extension and to add a shell script to the service asset. That way, the script will be launched if a data transfer, that runs on a contractAgreementId which is linked to the assetId, finishes successfully. The script should then (as explained above) launch the actual service. This is represented by the Postman request 4.

3. For the next step, the /send-service-metadata-to-fc REST Endpoint has to be used. That REST Endpoint takes important metadata about the service as an input and creates a Gaia-X conform self-description with it, that is then uploaded to the federated catalogue. All the fields have to be filled properly. Most contains simple text that can be anything, but some require a specific format. Notable fields are: 
    - identifierPrefix: prefix for the metadata IDs.
    - legalParticipant, legalRegistration and termsAndConditions: Links to existing metadata of the Gaia-X participant.
    - serviceAccessPoint...: describes, how the EDC Connector can be reached.
    - serviceContractId: The contract of the service asset that can be used to start a contract negotiation. It can be found out with Postman request 5.
    - "specification" and "tooling" in required- and resultingFiles: Have to be valid URLs

4. Postman request 11 should not be executed manually, the according REST Endpoint should instead be triggered automatically once the service is finished, as mentioned above.

This is it everything for the setup of the service. If a consumer now wants to use the service, they can use the metadata in the federated catalogue to access the service-asset, negotiate a contract about that asset and send their data to the provider. If everything is set up correctly (see section above), the provider EDC connector will automatically start the service which will do the processing and trigger the transfer of the resulting files.

### Consumer: Use a Service

1. The first step to using a service is to negotiate a contract about the service-asset a service provider is offering. This is done using the provided contract-negotiation workflow of the EDC connector. After a successfull contract-negotiation, the resulting ContractAgreementId should be fetched and stored. The contract negotiation can be started with Postman request 8, using the contract id. The contract id is either available in the service metadata or can be be fetched using Postman request 7. With the id of the response from request 8, the contractAgreementId can be fetched using Postman request 9.
2. This ContractAgreementId can be passed into the /start-data-transfer REST Endpoint (provided by this extension) along with other required data such as the receiving connector and the paths of the data that should be sent. This ContractAgreementId is required to successfully send the data, without it the transfer will fail. This is represented by Postman request 10.
3. Once the data is sent, the service should be executed automatically on the side of the provider and once it is finished, the result will be sent to the consumer automatically, as long as the connector with the extension is still running.
