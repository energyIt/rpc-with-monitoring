/*
 * Copyright 2015 The gRPC Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tech.energyit.trading.api;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.google.common.primitives.Longs;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.NameResolver;
import io.grpc.StatusRuntimeException;
import io.prometheus.client.exporter.HTTPServer;
import me.dinowernli.grpc.prometheus.Configuration;
import me.dinowernli.grpc.prometheus.MonitoringClientInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple client that requests trades via grpc.
 */
public class TradingEnquiryClient {

    private static final Logger LOG = LoggerFactory.getLogger(TradingEnquiryClient.class);

    private final ManagedChannel channel;
    private final TradeEnquiryServiceGrpc.TradeEnquiryServiceBlockingStub blockingStub;

    /**
     * Construct client connecting to HelloWorld server at {@code host:port}.
     */
    public TradingEnquiryClient(String... hostPorts) {
        this.channel = ManagedChannelBuilder.forTarget(hostPorts[0])
                // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                // needing certificates.
                .nameResolverFactory(new FixedAddressNameResolver.FixedAddressNameResolverProvider(Arrays.asList(hostPorts)))
                .defaultLoadBalancingPolicy("round_robin")//("pick_first")//("round_robin")
                .usePlaintext()
                .intercept(MonitoringClientInterceptor.create(Configuration
                        .allMetrics()
                        .withLatencyBuckets(TradingEnquiryServer.LATENCY_BUCKETS)))
                .build();
        this.blockingStub = TradeEnquiryServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public void getTrades(long epochFrom, long epochTo) {
        LOG.info("get trades for {} - {}", epochFrom, epochTo);
        TradeRequest request = TradeRequest.newBuilder()
                .setTimestampFrom(epochFrom)
                .setTimestampTo(epochTo)
                .build();
        TradeResponse response;
        try {
            response = blockingStub.getTrades(request);
            LOG.trace("trades received : \n{}", response);
        } catch (StatusRuntimeException e) {
            LOG.error("RPC failed: {}", e.getStatus());
        }
    }

    public void cancelTrades(long... tradeIds) {
        LOG.info("canceling trades : {}", Arrays.toString(tradeIds));
        CancelTradeRequest request = CancelTradeRequest.newBuilder()
                .addAllTradeId(Longs.asList(tradeIds))
                .build();
        TradeResponse response;
        try {
            response = blockingStub.cancelTrades(request);
            LOG.trace("trades canceled : \n{}", response);
        } catch (StatusRuntimeException e) {
            LOG.error("RPC failed: {}", e.getStatus());
        }
    }

    /**
     * Greet server. If provided, the first element of {@code args} is the name to use in the
     * greeting.
     */
    public static void main(String[] args) throws Exception {
        TradingEnquiryClient client = new TradingEnquiryClient("localhost:50051", "localhost:50052");
        HTTPServer monitoringServer = new HTTPServer("localhost", 9889, true);
        try {
            for (int i = 0; i < 5; i++) {
                client.getTrades(123456L + i, 1234999L + i);
                client.getTrades(123456L + i, 1234999L + i);
                client.cancelTrades(123L + i, 1234L + i, 123456L + i);
            }
            System.in.read();
        } finally {
            client.shutdown();
            monitoringServer.stop();
        }
    }
}
