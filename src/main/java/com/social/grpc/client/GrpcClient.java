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
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class GrpcClient {
    private final ManagedChannel channel;
    private final HelloServiceGrpc.HelloServiceBlockingStub blockingStub;

    public GrpcClient(ManagedChannel channel, HelloServiceGrpc.HelloServiceBlockingStub blockingStub) {
        this.channel = channel;
        this.blockingStub = blockingStub;
    }

    private static SslContext buildSslContext() throws SSLException {
        SslContextBuilder builder = GrpcSslContexts.forClient();
        return builder.build();
    }


    public GrpcClient(String host,
                               int port,
                               SslContext sslContext) throws SSLException {

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

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    /**
     * Say hello to server.
     */
    public void greet(String name) {
        System.out.println("Will try to greet " + name + " ...");
        HelloRequest request = HelloRequest.newBuilder()
                .setFirstName("Rajesh")
                .setLastName("91Social")
                .build();
        HelloResponse response;
        try {
            response = blockingStub.hello(request);
        } catch (StatusRuntimeException e) {
            System.out.println(Level.WARNING + " RPC failed: "+e.getStatus());
            return;
        }
        System.out.println("Greeting: " + response.getGreeting());
    }

    public static void main(String[] args) throws SSLException, InterruptedException {
        System.out.println("gRPC client");

        GrpcClient client = new GrpcClient(args[0], Integer.parseInt(args[1]),
                buildSslContext());

        try {
            client.greet(args[0]);
        } finally {
            client.shutdown();
        }

    }
}
