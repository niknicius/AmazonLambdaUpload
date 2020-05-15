package com.potter.serverless.utils;

import com.potter.serverless.models.LambdaFunction;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.apigatewayv2.ApiGatewayV2Client;
import software.amazon.awssdk.services.apigatewayv2.model.*;

public class ApiGateway {

    private LambdaFunction lambdaFunction;
    private ApiGatewayV2Client apiGatewayClient;
    private Region region;

    public ApiGateway(Region region, LambdaFunction lambdaFunction) {
        this.region = region;
        this.lambdaFunction = lambdaFunction;
        this.apiGatewayClient = ApiGatewayV2Client.builder().region(this.region).build();
    }

    public CreateApiResponse createApi(String functionArn){
        return this.apiGatewayClient.createApi(
                CreateApiRequest.builder()
                        .name(this.lambdaFunction.getName() + "API")
                        .corsConfiguration(
                                Cors.builder().allowOrigins("*").build())
                        .protocolType(ProtocolType.HTTP)
                        .target(functionArn)
                        .build()
        );
    }

    public CreateStageResponse createStage(String apiId, String stage){
        return this.apiGatewayClient.createStage(CreateStageRequest.builder()
                .apiId(apiId)
                .stageName(stage)
                .autoDeploy(true)
                .build());
    }

    public CreateIntegrationResponse createIntegration(String apiId, String functionArn){
        return this.apiGatewayClient.createIntegration(CreateIntegrationRequest.builder()
                .apiId(apiId)
                .connectionType(ConnectionType.INTERNET)
                .integrationMethod("POST")
                .integrationType(IntegrationType.AWS_PROXY)
                .integrationUri(generateUri(functionArn))
                .payloadFormatVersion("1.0")
                .build());
    }

    public CreateRouteResponse createRoute(String apiId){
        return this.apiGatewayClient.createRoute(CreateRouteRequest.builder()
                .apiId(apiId)
                .authorizationType(AuthorizationType.NONE)
                .build());
    }

    private String generateUri(String functionArn){
        return "arn:aws:apigateway:" + this.region.toString() + ":lambda:path/2015-03-31/functions/" + functionArn + "/invocations";
    }

}
