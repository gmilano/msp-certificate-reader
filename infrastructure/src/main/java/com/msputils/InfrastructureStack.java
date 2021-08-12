package com.msputils;

import java.util.Arrays;
import java.util.List;

import software.amazon.awscdk.core.BundlingOptions;
import software.amazon.awscdk.core.CfnOutput;
import software.amazon.awscdk.core.CfnOutputProps;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.DockerVolume;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.apigatewayv2.AddRoutesOptions;
import software.amazon.awscdk.services.apigatewayv2.HttpApi;
import software.amazon.awscdk.services.apigatewayv2.HttpApiProps;
import software.amazon.awscdk.services.apigatewayv2.HttpMethod;
import software.amazon.awscdk.services.apigatewayv2.PayloadFormatVersion;
import software.amazon.awscdk.services.apigatewayv2.integrations.LambdaProxyIntegration;
import software.amazon.awscdk.services.apigatewayv2.integrations.LambdaProxyIntegrationProps;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.s3.assets.AssetOptions;

import static java.util.Collections.singletonList;
import static software.amazon.awscdk.core.BundlingOutput.ARCHIVED;

public class InfrastructureStack extends Stack {
    public InfrastructureStack(final Construct parent, final String id) {
        this(parent, id, null);
    }

    public InfrastructureStack(final Construct parent, final String id, final StackProps props) {
        super(parent, id, props);

        List<String> functionOnePackagingInstructions = Arrays.asList(
                "/bin/sh",
                "-c",
                "cd FunctionOne " +
                "&& mvn clean install " +
                "&& cp /asset-input/FunctionOne/target/functionone.jar /asset-output/"
        );

     

        BundlingOptions.Builder builderOptions = BundlingOptions.builder()
                .command(functionOnePackagingInstructions)
                .image(Runtime.JAVA_11.getBundlingImage())
                .volumes(singletonList(
                        // Mount local .m2 repo to avoid download all the dependencies again inside the container
                        DockerVolume.builder()
                                .hostPath(System.getProperty("user.home") + "/.m2/")
                                .containerPath("/root/.m2/")
                                .build()
                ))
                .user("root")
                .outputType(ARCHIVED);

        Function functionOne = new Function(this, "FunctionOne", FunctionProps.builder()
                .runtime(Runtime.JAVA_11)
                .code(Code.fromAsset("../software/", AssetOptions.builder()
                        .bundling(builderOptions
                                .command(functionOnePackagingInstructions)
                                .build())
                        .build()))
                .handler("msputils.App")
                .memorySize(1024)
                .timeout(Duration.seconds(10))
                .logRetention(RetentionDays.ONE_WEEK)
                .build());

       

        HttpApi httpApi = new HttpApi(this, "gx-msp-api", HttpApiProps.builder()
                .apiName("gx-msp-api")
                .build());

        httpApi.addRoutes(AddRoutesOptions.builder()
                .path("/validate")
                .methods(singletonList(HttpMethod.POST))
                .integration(new LambdaProxyIntegration(LambdaProxyIntegrationProps.builder()
                        .handler(functionOne)
                        .payloadFormatVersion(PayloadFormatVersion.VERSION_2_0)
                        .build()))
                .build());

        new CfnOutput(this, "HttApi", CfnOutputProps.builder()
                .description("Url for Http Api")
                .value(httpApi.getApiEndpoint())
                .build());
    }
}
