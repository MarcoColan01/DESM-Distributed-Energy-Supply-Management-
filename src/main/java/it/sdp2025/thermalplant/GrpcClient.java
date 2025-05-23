package it.sdp2025.thermalplant;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import it.sdp2025.proto.*;

import java.util.HashMap;
import java.util.Map;

public class GrpcClient {
    private final PlantConfig config;
    private final PlantTopologyManager topology;
    private final Map<String, ManagedChannel> channels = new HashMap<>();
    private StreamObserver<ElectionEnvelope> outStream; //bidirezionale

    public GrpcClient(PlantConfig config, PlantTopologyManager topology){
        this.config = config;
        this.topology = topology;
    }

    public void connectToSuccessor(){
        PeerInfo successor = topology.getSuccessor();
        if(successor == null){
            System.out.println("GRPC CLIENT: nessun successore noto");
            return;
        }
        openChannelIfAbsent(successor);
        initRingStream(successor);
    }

    public void reconnectToNewSuccessor(){
        PeerInfo successor = topology.getSuccessor();
        if(successor == null) return;
        initRingStream(successor);
    }

    public void sendElectionToSuccessor(String candidateId){
        ensureStream();
        ElectionEnvelope envelope = ElectionEnvelope.newBuilder()
                .setElection(ElectionMsg.newBuilder().setCandidateId(candidateId))
                .build();
        outStream.onNext(envelope);
        System.out.printf("[gRPC-client] → Election(%s)%n", candidateId);
    }

    public void sendElectedToSuccessor(String coordinatorId){
        ensureStream();
        ElectionEnvelope envelope = ElectionEnvelope.newBuilder()
                .setElected(ElectedMsg.newBuilder()
                        .setCoordinatorId(coordinatorId).build()).build();
        outStream.onNext(envelope);
        System.out.printf("[gRPC-client] → Elected(%s)%n", coordinatorId);
    }

    public void helloToPeers(){
        for(PeerInfo peer: topology.getPeers()){
            if(peer.getId().equals(config.getId())) continue;
            sendHello(peer);
        }
    }

    private ManagedChannel openChannelIfAbsent(PeerInfo peer){
        return channels.computeIfAbsent(peer.getId(), id ->
                ManagedChannelBuilder.forAddress(peer.getHost(), peer.getPort()).usePlaintext().build());
    }

    private void initRingStream(PeerInfo successor) {
        ManagedChannel channel = openChannelIfAbsent(successor);
        PlantRingGrpc.PlantRingStub stub = PlantRingGrpc.newStub(channel);

        if (outStream != null) {
            try { outStream.onCompleted(); } catch (Exception ignored) {}
        }

        outStream = stub.ringStream(new StreamObserver<>() {
            public void onNext(ElectionEnvelope envelope) {}
            public void onError(Throwable throwable) {
                System.err.println("[gRPC-client] errore stream: "+throwable.getMessage());
            }
            public void onCompleted() {}
        });

        System.out.printf("[gRPC-client] Stream verso successore %s aperto%n", successor.getId());
    }

    private void sendHello(PeerInfo peer) {
        ManagedChannel channel = openChannelIfAbsent(peer);
        PlantRingGrpc.PlantRingStub stub = PlantRingGrpc.newStub(channel);

        HelloMsg msg = HelloMsg.newBuilder()
                .setId(config.getId())
                .setHost(config.getHost())
                .setPort(config.getPort())
                .build();

        stub.hello(msg, new StreamObserver<>() {
            public void onNext(com.google.protobuf.Empty empty) {}
            public void onError(Throwable t) {
                System.err.printf("[gRPC-client] hello a %s fallito: %s%n",
                        peer.getId(), t.getMessage());
            }
            public void onCompleted() {
                System.out.printf("[gRPC-client] hello completato con %s%n", peer.getId());
            }
        });
    }

    private void ensureStream() {
        if (outStream == null) {
            reconnectToNewSuccessor();
            if (outStream == null)
                throw new IllegalStateException("Stream non inizializzato");
        }
    }
}

