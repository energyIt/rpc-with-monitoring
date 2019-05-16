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

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import io.opencensus.contrib.grpc.metrics.RpcViews;
import io.opencensus.exporter.stats.prometheus.PrometheusStatsCollector;
import io.prometheus.client.exporter.HTTPServer;
import me.dinowernli.grpc.prometheus.Configuration;
import me.dinowernli.grpc.prometheus.MonitoringServerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server that manages startup/shutdown of a {@code TradeEnquiryService}.
 */
public class TradingEnquiryServer {

    private static final Logger LOG = LoggerFactory.getLogger(TradingEnquiryServer.class);

    private Server server;
    private HTTPServer monitoringServer;

    private void start() throws IOException, InterruptedException {
        setupMonitoringAndExporters();

        /* The port on which the server should run */
        int port = 50051;
        server = ServerBuilder.forPort(port)
                .addService(createService())
                .build()
                .start();
        LOG.info("Server started, listening on {}", port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                TradingEnquiryServer.this.stop();
                System.err.println("*** server shut down");
            }
        });
        server.awaitTermination(5, TimeUnit.SECONDS);
    }

    private ServerServiceDefinition createService() {
        MonitoringServerInterceptor monitoringInterceptor =
                MonitoringServerInterceptor.create(Configuration.allMetrics().withLatencyBuckets(new double[]{0.001, 0.002, 0.005, 0.01, 0.02, 0.05, 0.1, 0.2, 0.5, 1, 2, 5}));
        return ServerInterceptors.intercept(new TradeEnquiryService(), monitoringInterceptor);
    }

    private void setupMonitoringAndExporters() throws IOException {
        // Register all the gRPC views and enable stats
        RpcViews.registerAllGrpcViews();

        // Register the Prometheus exporter
        PrometheusStatsCollector.createAndRegister();

        // Run the server as a daemon on address "localhost:8888"
        monitoringServer = new HTTPServer("localhost", 8888, true);
    }

    private void stop() {
        if (server != null) {
            server.shutdown();
        }
        if (monitoringServer != null) {
            monitoringServer.stop();
        }
        LOG.info("stopped");
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    /**
     * Main launches the server from the command line.
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        final TradingEnquiryServer server = new TradingEnquiryServer();
        server.start();
        server.blockUntilShutdown();
    }
}
