package it.sdp2025.plant;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import it.sdp2025.PlantNetwork;
import it.sdp2025.PlantServiceGrpc;
import org.jetbrains.annotations.NotNull;

public class GrpcServer extends PlantServiceGrpc.PlantServiceImplBase {
    private final ElectionManager electionManager;
    private final TopologyManager topologyManager;
    private final GrpcClient client;

    public GrpcServer(@NotNull ElectionManager electionManager, @NotNull TopologyManager topologyManager,
                      @NotNull GrpcClient client) {
        this.electionManager = electionManager;
        this.topologyManager = topologyManager;
        this.client = client;
    }

    @Override
    public void forwardElection(@NotNull PlantNetwork.ElectionMessage message,
                                @NotNull StreamObserver<Empty> responseObserver) {
        try {
            //System.out.printf("[GrpcServer] Received forwardElection from %s%n", message.getInitiatorId());
            electionManager.handleElection(message);
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
            //System.out.println("[GrpcServer] forwardElection: response sent and completed.");
        } catch (Exception e) {
            responseObserver.onError(e);
            System.err.println("[GrpcServer] forwardElection: error occurred.");
            e.printStackTrace();
        }
    }

    @Override
    public void announceJoin(@NotNull PlantNetwork.PlantInfoMessage message,
                             @NotNull StreamObserver<Empty> responseObserver) {
        try {
            //System.out.printf("[GrpcServer] Received announceJoin from %s%n", request.getId());
            topologyManager.addPlant(message.getId());
            client.connect(message.getId(), message.getHost(), message.getPort());
            System.out.printf("[%s] RING (post-join) → %s%n", topologyManager.getMyId(), topologyManager.getPlants());
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
            //System.out.println("[GrpcServer] announceJoin: response sent and completed.");
        } catch (Exception e) {
            responseObserver.onError(e);
            //System.err.println("[GrpcServer] announceJoin: error occurred.");
            //e.printStackTrace();
        }
    }
}
