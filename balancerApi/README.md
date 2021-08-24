# io.swagger.client - Kotlin client library for Balancer

## Requires

* Kotlin 1.4.30
* Gradle 5.3

## Build

First, create the gradle wrapper script:

```
gradle wrapper
```

Then, run:

```
./gradlew check assemble
```

This runs all tests and packages the library.

## Features/Implementation Notes

* Supports JSON inputs/outputs, File inputs, and Form inputs.
* Supports collection formats for query parameters: csv, tsv, ssv, pipes.
* Some Kotlin and Java types are fully qualified to avoid conflicts with types defined in Swagger definitions.
* Implementation of ApiClient is intended to reduce method counts, specifically to benefit Android targets.

<a name="documentation-for-api-endpoints"></a>
## Documentation for API Endpoints

All URIs are relative to *http://localhost:8080/Skoltech_OpenRAN_5G/iperf_load_balancer/0.1.0*

Class | Method | HTTP request | Description
------------ | ------------- | ------------- | -------------
*ClientApi* | [**clientObtainIp**](docs/ClientApi.md#clientobtainip) | **GET** /addr | obtain iperf server ip list to connect to
*ServerApi* | [**serverDeleteIp**](docs/ServerApi.md#serverdeleteip) | **POST** /addr_del | delete server IP
*ServerApi* | [**serverPostIp**](docs/ServerApi.md#serverpostip) | **POST** /addr | post self ip to balancer

<a name="documentation-for-models"></a>
## Documentation for Models

 - [io.swagger.client.models.InlineResponse200](docs/InlineResponse200.md)
 - [io.swagger.client.models.ServerAddr](docs/ServerAddr.md)

<a name="documentation-for-authorization"></a>
## Documentation for Authorization

All endpoints do not require authorization.
