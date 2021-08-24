/**
 * Balancer
 * Simple iperf load balancer
 *
 * OpenAPI spec version: 0.1.0
 * Contact: vinogradov.alek@gmail.com
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */
package io.swagger.client.apis

import io.swagger.client.models.InlineResponse200
import io.swagger.client.models.ServerAddr

import io.swagger.client.infrastructure.*

class ServerApi(basePath: kotlin.String = "http://localhost:8080/Skoltech_OpenRAN_5G/iperf_load_balancer/0.1.0") : ApiClient(basePath) {

    /**
     * delete server IP
     * Send by server during shutdown.
     * @param body port of iperf server. Ip and time could be emply (optional)
     * @return kotlin.Array<InlineResponse200>
     */
    @Suppress("UNCHECKED_CAST")
    fun serverDeleteIp(body: ServerAddr? = null): kotlin.Array<InlineResponse200> {
        val localVariableBody: kotlin.Any? = body
        
        val localVariableConfig = RequestConfig(
                RequestMethod.POST,
                "/addr_del"
        )
        val response = request<kotlin.Array<InlineResponse200>>(
                localVariableConfig, localVariableBody
        )

        return when (response.responseType) {
            ResponseType.Success -> (response as Success<*>).data as kotlin.Array<InlineResponse200>
            ResponseType.Informational -> TODO()
            ResponseType.Redirection -> TODO()
            ResponseType.ClientError -> throw ClientException((response as ClientError<*>).body as? String ?: "Client error")
            ResponseType.ServerError -> throw ServerException((response as ServerError<*>).message ?: "Server error")
        }
    }
    /**
     * post self ip to balancer
     * When server makes free, post ip to balancer
     * @param body port of iperf server. Ip and time could be emply (optional)
     * @return void
     */
    fun serverPostIp(body: ServerAddr? = null): Unit {
        val localVariableBody: kotlin.Any? = body
        
        val localVariableConfig = RequestConfig(
                RequestMethod.POST,
                "/addr"
        )
        val response = request<Any?>(
                localVariableConfig, localVariableBody
        )

        return when (response.responseType) {
            ResponseType.Success -> Unit
            ResponseType.Informational -> TODO()
            ResponseType.Redirection -> TODO()
            ResponseType.ClientError -> throw ClientException((response as ClientError<*>).body as? String ?: "Client error")
            ResponseType.ServerError -> throw ServerException((response as ServerError<*>).message ?: "Server error")
        }
    }
}