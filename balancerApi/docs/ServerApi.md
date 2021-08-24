# ServerApi

All URIs are relative to *http://localhost:8080/Skoltech_OpenRAN_5G/iperf_load_balancer/0.1.0*

Method | HTTP request | Description
------------- | ------------- | -------------
[**serverDeleteIp**](ServerApi.md#serverDeleteIp) | **POST** /addr_del | delete server IP
[**serverPostIp**](ServerApi.md#serverPostIp) | **POST** /addr | post self ip to balancer

<a name="serverDeleteIp"></a>
# **serverDeleteIp**
> kotlin.Array&lt;InlineResponse200&gt; serverDeleteIp(body)

delete server IP

Send by server during shutdown.

### Example
```kotlin
// Import classes:
//import io.swagger.client.infrastructure.*
//import io.swagger.client.models.*;

val apiInstance = ServerApi()
val body : ServerAddr =  // ServerAddr | port of iperf server. Ip and time could be emply
try {
    val result : kotlin.Array<InlineResponse200> = apiInstance.serverDeleteIp(body)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ServerApi#serverDeleteIp")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ServerApi#serverDeleteIp")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**ServerAddr**](ServerAddr.md)| port of iperf server. Ip and time could be emply | [optional]

### Return type

[**kotlin.Array&lt;InlineResponse200&gt;**](InlineResponse200.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/Json

<a name="serverPostIp"></a>
# **serverPostIp**
> serverPostIp(body)

post self ip to balancer

When server makes free, post ip to balancer

### Example
```kotlin
// Import classes:
//import io.swagger.client.infrastructure.*
//import io.swagger.client.models.*;

val apiInstance = ServerApi()
val body : ServerAddr =  // ServerAddr | port of iperf server. Ip and time could be emply
try {
    apiInstance.serverPostIp(body)
} catch (e: ClientException) {
    println("4xx response calling ServerApi#serverPostIp")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ServerApi#serverPostIp")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**ServerAddr**](ServerAddr.md)| port of iperf server. Ip and time could be emply | [optional]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: Not defined

