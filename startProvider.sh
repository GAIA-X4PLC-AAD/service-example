keystorePath="data-processing-service/resources/certs/cert.pfx"
keystorePassword="123456"
consumerVaultPath="data-processing-service/resources/configuration/consumer-vault.properties"
consumerConfigPath="data-processing-service/resources/configuration/consumer-configuration.properties"
providerVaultPath="data-processing-service/resources/configuration/provider-vault.properties"
providerConfigPath="data-processing-service/resources/configuration/provider-configuration.properties"
jarPath="data-processing-service/build/libs/connector.jar"

providerArguments="-Dedc.keystore=$keystorePath
-Dedc.keystore.password=$keystorePassword
-Dedc.vault=$providerVaultPath
-Dedc.fs.config=$providerConfigPath
-jar $jarPath"

java $providerArguments