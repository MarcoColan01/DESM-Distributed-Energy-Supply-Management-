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
    private final Map<String, ManagedChannel> channels = new HashMap<>();

    public void connect(String id, String host, int port) {
        if (stubs.containsKey(id)) return; // già connesso
        ManagedChannel ch = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext().build();
        PlantServiceGrpc.PlantServiceStub stub = PlantServiceGrpc.newStub(ch);
        stubs.put(id, stub);
        channels.put(id, ch);
        System.out.printf("[GrpcClient] Connected to %s at %s:%d%n", id, host, port);
    }

    public void forwardElection(String idSucc, PlantNetwork.ElectionMsg msg) {
        PlantServiceGrpc.PlantServiceStub stub = stubs.get(idSucc);
        if (stub == null) {
            System.err.printf("[GrpcClient] No stub for %s%n", idSucc);
            return;
        }
        stub.forwardElection(msg, new StreamObserver<>() {
            public void onNext(com.google.protobuf.Empty ignore) {
                System.out.printf("[GrpcClient] forwardElection: response received from %s%n", idSucc);
            }
            public void onError(Throwable t) {
                System.err.printf("[GrpcClient] forwardElection: ERROR from %s: %s%n", idSucc, t);
                //t.printStackTrace();
            }
            public void onCompleted() {
                System.out.printf("[GrpcClient] forwardElection: completed with %s%n", idSucc);
            }
        });
    }

    public void announceJoinAll(PlantInfo me, Collection<PlantInfo> peers) {
        PlantNetwork.PlantInfoMsg self = PlantNetwork.PlantInfoMsg.newBuilder()
                .setId(me.getId()).setHost(me.getHost()).setPort(me.getGrpcPort()).build();
        for (PlantInfo p : peers) {
            connect(p.getId(), p.getHost(), p.getGrpcPort());
            PlantServiceGrpc.PlantServiceStub stub = stubs.get(p.getId());
            stub.announceJoin(self, new StreamObserver<>() {
                public void onNext(com.google.protobuf.Empty ignore) {
                    System.out.printf("[GrpcClient] announceJoin: response received from %s%n", p.getId());
                }
                public void onError(Throwable t) {
                    System.err.printf("[GrpcClient] announceJoin: ERROR from %s: %s%n", p.getId(), t);
                    //t.printStackTrace();
                }
                public void onCompleted() {
                    System.out.printf("[GrpcClient] announceJoin: completed with %s%n", p.getId());
                }
            });
        }
    }

    // Utilità: chiusura canali quando tutto è finito (da chiamare a fine programma)
    public void shutdown() {
        for (var ch : channels.values()) {
            ch.shutdownNow();
        }
        System.out.println("[GrpcClient] All channels shut down.");
    }
}
