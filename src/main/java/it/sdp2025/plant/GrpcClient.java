package it.sdp2025.plant;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import it.sdp2025.PlantNetwork;
import it.sdp2025.PlantServiceGrpc;
import it.sdp2025.common.PlantInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class GrpcClient {
    private final Map<String, PlantServiceGrpc.PlantServiceStub> stubs = new HashMap<>();
    private final Map<String, ManagedChannel> channels = new HashMap<>();

    public void connect(@NotNull String id, @NotNull String host, int port) {
        if (stubs.containsKey(id)) return;
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        PlantServiceGrpc.PlantServiceStub stub = PlantServiceGrpc.newStub(channel);
        stubs.put(id, stub);
        channels.put(id, channel);
        System.out.printf("[GrpcClient] Connesso a %s at %s:%d%n", id, host, port);
    }

    public void forwardElection(@NotNull String idSucc, @NotNull PlantNetwork.ElectionMessage message) {
        PlantServiceGrpc.PlantServiceStub stub = stubs.get(idSucc);
        if (stub == null) {
            System.err.printf("[GrpcClient] No stub per %s%n", idSucc);
            return;
        }
        stub.forwardElection(message, new StreamObserver<>() {
            public void onNext(com.google.protobuf.Empty ignore) {
            }
            public void onError(Throwable t) {
                System.err.printf("[GrpcClient] forwardElection: ERROR from %s: %s%n", idSucc, t);
            }
            public void onCompleted() {
            }
        });
    }

    public void announceJoinAll(@NotNull PlantInfo plantInfo, @NotNull Collection<PlantInfo> peers) {
        PlantNetwork.PlantInfoMessage self = PlantNetwork.PlantInfoMessage.newBuilder()
                .setId(plantInfo.getId()).setHost(plantInfo.getHost()).setPort(plantInfo.getGrpcPort()).build();
        for (PlantInfo peer : peers) {
            connect(peer.getId(), peer.getHost(), peer.getGrpcPort());
            PlantServiceGrpc.PlantServiceStub stub = stubs.get(peer.getId());
            stub.announceJoin(self, new StreamObserver<>() {
                public void onNext(com.google.protobuf.Empty ignore) {
                    //System.out.printf("[GrpcClient] announceJoin: response received from %s%n", peer.getId());
                }
                public void onError(Throwable t) {
                    System.err.printf("[GrpcClient] announceJoin: ERROR from %s: %s%n", peer.getId(), t);
                }
                public void onCompleted() {
                    //
                }
            });
        }
    }

//    public void shutdown() {
//        for (var channel : channels.values()) {
//            channel.shutdownNow();
//        }
//        System.out.println("[GrpcClient] All channels shut down.");
//    }
}
