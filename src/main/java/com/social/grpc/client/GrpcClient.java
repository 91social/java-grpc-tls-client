package com.social.grpc.client;

import com.social.grpc.HelloRequest;
import com.social.grpc.HelloResponse;
import com.social.grpc.HelloServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import javax.net.ssl.SSLException;
import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class GrpcClient {
    private final ManagedChannel channel;
    private final HelloServiceGrpc.HelloServiceBlockingStub blockingStub;

    public GrpcClient(ManagedChannel channel, HelloServiceGrpc.HelloServiceBlockingStub blockingStub) {
        this.channel = channel;
        this.blockingStub = blockingStub;
    }

    public GrpcClient(String host,
                      int port,
                      SslContext sslContext) {

        this(NettyChannelBuilder.forAddress(host, port)
                //.overrideAuthority("foo.test.google.fr")  /* Only for using provided test certs. */
                .sslContext(sslContext)
                .build());
    }


    /**
     * Construct client for accessing RouteGuide server using the existing channel.
     */
    GrpcClient(ManagedChannel channel) {
        this.channel = channel;
        blockingStub = HelloServiceGrpc.newBlockingStub(channel);
    }

    private static SslContext buildSslContext(String trustCertCollectionFilePath,
                                              String clientCertChainFilePath,
                                              String clientPrivateKeyFilePath) throws SSLException {
        SslContextBuilder builder = GrpcSslContexts.forClient();
        if (trustCertCollectionFilePath != null) {
            builder.trustManager(new File(trustCertCollectionFilePath));
        }
        if (clientCertChainFilePath != null && clientPrivateKeyFilePath != null) {
            builder.keyManager(new File(clientCertChainFilePath), new File(clientPrivateKeyFilePath));
        }
        return builder.build();
    }

    public static void main(String[] args) throws SSLException, InterruptedException {
        System.out.println("gRPC client");
        if (args.length < 2 || args.length == 4 || args.length > 5) {
            System.out.println("USAGE: GrpcClient host port [trustCertCollectionFilePath " +
                    "[clientCertChainFilePath clientPrivateKeyFilePath]]\n  Note: clientCertChainFilePath and " +
                    "clientPrivateKeyFilePath are only needed if mutual auth is desired.");
            System.exit(0);
        }

        GrpcClient client;

        switch (args.length) {
            case 2:
                /* Use default CA. Only for real server certificates. */
                client = new GrpcClient(args[0], Integer.parseInt(args[1]),
                        buildSslContext(null, null, null));
                break;
            case 3:
                client = new GrpcClient(args[0], Integer.parseInt(args[1]),
                        buildSslContext(args[2], null, null));
                break;
            default:
                client = new GrpcClient(args[0], Integer.parseInt(args[1]),
                        buildSslContext(args[2], args[3], args[4]));
        }

        try {
            client.greet(args[0]);
        } finally {
            client.shutdown();
        }

    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    /**
     * Say hello to server.
     */
    public void greet(String name) {
        System.out.println("Will try to greet " + name + " ...");
        HelloRequest request = HelloRequest.newBuilder()
                .setFirstName("91Social")
                .setLastName("Awesome")
                .build();
        HelloResponse response;
        try {
            response = blockingStub.hello(request);
        } catch (StatusRuntimeException e) {
            System.out.println(Level.WARNING + " RPC failed: " + e.getStatus());
            return;
        }
        System.out.println("Greeting: " + response.getGreeting());
    }
}
