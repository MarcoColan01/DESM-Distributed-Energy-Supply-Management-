package it.sdp2025.plant;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import it.sdp2025.PlantNetwork;
import it.sdp2025.PlantServiceGrpc;
import it.sdp2025.common.PlantInfo;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class GrpcClient {

    private final Map<String, PlantServiceGrpc.PlantServiceStub> stubs = new HashMap<>();

    public void connect(String id, String host, int port) {
        ManagedChannel ch = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext().build();
        stubs.put(id, PlantServiceGrpc.newStub(ch));
    }

    public void forwardElection(String idSucc, PlantNetwork.ElectionMsg msg) {
        PlantServiceGrpc.PlantServiceStub stub = stubs.get(idSucc);
        if (stub != null) stub.forwardElection(msg, new StreamObserver<>() {
            public void onNext(com.google.protobuf.Empty ignore) {}
            public void onError(Throwable t) { t.printStackTrace(); }
            public void onCompleted() {}
        });
    }

    public void announceJoinAll(PlantInfo me, Collection<PlantInfo> peers) {
        PlantNetwork.PlantInfoMsg self = PlantNetwork.PlantInfoMsg.newBuilder()
                .setId(me.getId()).setHost(me.getHost()).setPort(me.getGrpcPort()).build();
        for (PlantInfo p : peers) {
            connect(p.getId(), p.getHost(), p.getGrpcPort());
            stubs.get(p.getId()).announceJoin(self, new StreamObserver<>() {
                public void onNext(com.google.protobuf.Empty ignore) {}
                public void onError(Throwable t) {}
                public void onCompleted() {}
            });
        }
    }
}

