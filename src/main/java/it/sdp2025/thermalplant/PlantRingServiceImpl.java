package it.sdp2025.thermalplant;

import it.sdp2025.proto.*;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;

public class PlantRingServiceImpl extends PlantRingGrpc.PlantRingImplBase {

    private final ElectionManager election;
    private final PlantTopologyManager topology;

    public PlantRingServiceImpl(ElectionManager election, PlantTopologyManager topology) {
        this.election  = election;
        this.topology  = topology;
    }

    @Override
    public void hello(HelloMsg request, StreamObserver<Empty> responseObserver) {
        topology.addPeer(request.getId(), request.getHost(), request.getPort());
        topology.buildRing();

        System.out.printf("[gRPC] Ricevuto hello da %s (%s:%d)%n",
                request.getId(), request.getHost(), request.getPort());

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    /* ---------- Stream bidirezionale: ringStream ---------- */
    @Override
    public StreamObserver<ElectionEnvelope> ringStream(
            StreamObserver<ElectionEnvelope> responseObserver) {

        return new StreamObserver<>() {
            @Override
            public void onNext(ElectionEnvelope env) {
                if (env.hasElection()) {
                    ElectionMsg m = env.getElection();
                    election.onElection(m.getCandidateId(), m.getPrice());
                } else if (env.hasElected()) {
                    election.onElected(env.getElected().getCoordinatorId());
                }
                /* Nessun forward qui: sarà l’ElectionManager, tramite il suo
                   GrpcClient, a decidere quando inoltrare al successore. */
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("[gRPC-stream] errore: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }
}
