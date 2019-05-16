package tech.energyit.trading.api;

import io.grpc.stub.StreamObserver;

public class TradeEnquiryService extends TradeEnquiryServiceGrpc.TradeEnquiryServiceImplBase {

    @Override
    public void getTrades(TradeRequest request, StreamObserver<TradeResponse> responseObserver) {
        TradeResponse resp = buildTradeResponse();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }

    private TradeResponse buildTradeResponse() {
        return TradeResponse.newBuilder()
                    .addTrade(TradeData.newBuilder()
                            .setContractId(123)
                            .setId(123456L)
                            .setQuantity(100)
                            .setRevision(5)
                            .setPrice(Price.newBuilder().setCent(10000).setShift(2).build())
                            .build())
                    .addTrade(TradeData.newBuilder()
                            .setContractId(1234)
                            .setId(1234567L)
                            .setQuantity(100)
                            .setRevision(5)
                            .setPrice(Price.newBuilder().setCent(10000).setShift(2).build())
                            .build())
                    .addTrade(TradeData.newBuilder()
                            .setContractId(12345)
                            .setId(12345678L)
                            .setQuantity(100)
                            .setRevision(5)
                            .setPrice(Price.newBuilder().setCent(10000).setShift(2).build())
                            .build())
                    .build();
    }

    @Override
    public void cancelTrades(CancelTradeRequest request, StreamObserver<TradeResponse> responseObserver) {
        TradeResponse resp = buildTradeResponse();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }
}
