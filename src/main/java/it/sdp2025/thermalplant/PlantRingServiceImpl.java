package it.sdp2025.thermalplant;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import it.sdp2025.proto.ElectionEnvelope;
import it.sdp2025.proto.HelloMsg;
import it.sdp2025.proto.PlantRingGrpc;

public class PlantRingServiceImpl extends PlantRingGrpc.PlantRingImplBase {
    private final ElectionManager electionManager;
    private final PlantTopologyManager topology;

    public PlantRingServiceImpl(ElectionManager electionManager, PlantTopologyManager topology){
        this.electionManager = electionManager;
        this.topology = topology;
    }

    @Override
    public StreamObserver<ElectionEnvelope> ringStream(StreamObserver<ElectionEnvelope> responseObserver){
        return new StreamObserver<ElectionEnvelope>() {
            @Override
            public void onNext(ElectionEnvelope msg) {
                switch(msg.getPayloadCase()){
                    case ELECTION -> {
                        String candidate = msg.getElection().getCandidateId();
                        System.out.printf("[gRPC] Ricevuto messaggio Election: %s%n", candidate);
                        electionManager.onElection(candidate);
                    }
                    case ELECTED -> {
                        String coordinator = msg.getElected().getCoordinatorId();
                        System.out.printf("[gRPC] Ricevuto messaggio Elected: %s%n", coordinator);
                        electionManager.onElected(coordinator);
                    }
                    default -> {System.out.println("[gRPC] Envelope ricevuto senza payload valido");}
                }
            }

            @Override
            public void onError(Throwable throwable) {
                System.err.println("[gRPC] Errore nello stream: " + throwable.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("[gRPC] Stream chiuso dal peer");
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public void hello(HelloMsg msg, StreamObserver<Empty> responseObserver) {
        System.out.printf("[gRPC] Ricevuto hello da %s (%s:%d)%n",
                msg.getId(), msg.getHost(), msg.getPort());

        topology.addPeer(msg.getId(), msg.getHost(), msg.getPort());

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}
