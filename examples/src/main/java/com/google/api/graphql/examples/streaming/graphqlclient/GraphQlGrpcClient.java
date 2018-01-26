package com.google.api.graphql.examples.streaming.graphqlclient;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.examples.graphql.GraphQlRequest;
import io.grpc.examples.graphql.GraphQlResponse;
import io.grpc.examples.graphql.GraphQlServiceGrpc;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GraphQlGrpcClient {
  private static final Logger logger = Logger.getLogger(GraphQlGrpcClient.class.getName());

  private final ManagedChannel channel;
  private final GraphQlServiceGrpc.GraphQlServiceStub stub;
  private final CountDownLatch ON_COMPLETE = new CountDownLatch(1);

  public GraphQlGrpcClient(String host, int port) {
    this(ManagedChannelBuilder.forAddress(host, port).usePlaintext(true).build());
  }

  GraphQlGrpcClient(ManagedChannel channel) {
    this.channel = channel;
    stub = GraphQlServiceGrpc.newStub(channel);
  }

  public void shutdown() throws InterruptedException {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
  }

  public void query(String query) {
    GraphQlRequest request =
        GraphQlRequest.newBuilder().build().newBuilder().setQuery(query).build();

    stub.execute(
        request,
        new StreamObserver<GraphQlResponse>() {
          @Override
          public void onNext(GraphQlResponse value) {
            logger.info("onNext: " + value);
          }

          @Override
          public void onError(Throwable t) {
            logger.log(Level.WARNING, t, () -> "onError");
            ON_COMPLETE.countDown();
          }

          @Override
          public void onCompleted() {
            logger.info("onCompleted");
            ON_COMPLETE.countDown();
          }
        });
  }

  public static void main(String[] args) throws Exception {
    GraphQlGrpcClient client = new GraphQlGrpcClient("localhost", 8888);
    try {
      String query =
          "{ sayHello(input: {name: \"world\"}) { message } hiNick: sayHello(input: {name: \"nick\"}) { message }  }";
      client.query(query);
      logger.info("waiting for ON_COMPLETE");
      client.ON_COMPLETE.await(10, TimeUnit.SECONDS);
      logger.info("main() is done");
    } finally {
      client.shutdown();
    }
  }
}
