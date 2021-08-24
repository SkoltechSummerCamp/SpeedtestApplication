# ClientApi

All URIs are relative to *http://localhost:8080/Skoltech_OpenRAN_5G/iperf_load_balancer/0.1.0*

Method | HTTP request | Description
------------- | ------------- | -------------
[**clientObtainIp**](ClientApi.md#clientObtainIp) | **GET** /addr | obtain iperf server ip list to connect to

<a name="clientObtainIp"></a>
# **clientObtainIp**
> kotlin.Array&lt;ServerAddr&gt; clientObtainIp()

obtain iperf server ip list to connect to

Return servers ip list

### Example
```kotlin
// Import classes:
//import io.swagger.client.infrastructure.*
//import io.swagger.client.models.*;

val apiInstance = ClientApi()
try {
    val result : kotlin.Array<ServerAddr> = apiInstance.clientObtainIp()
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ClientApi#clientObtainIp")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ClientApi#clientObtainIp")
    e.printStackTrace()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**kotlin.Array&lt;ServerAddr&gt;**](ServerAddr.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

