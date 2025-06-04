package it.sdp2025.plant;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import it.sdp2025.PlantNetwork;
import it.sdp2025.PlantServiceGrpc;
import org.jetbrains.annotations.NotNull;

public class GrpcServer extends PlantServiceGrpc.PlantServiceImplBase {
    private final ElectionManager electionMgr;
    private final TopologyManager topo;
    private final GrpcClient client;

    public GrpcServer(@NotNull ElectionManager e, @NotNull TopologyManager t, @NotNull GrpcClient client) {
        this.electionMgr = e;
        this.topo = t;
        this.client = client;
    }

    @Override
    public void forwardElection(@NotNull PlantNetwork.ElectionMessage request, @NotNull StreamObserver<Empty> responseObserver) {
        try {
            System.out.printf("[GrpcServer] Received forwardElection from %s%n", request.getInitiatorId());
            electionMgr.handleElection(request);
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
            System.out.println("[GrpcServer] forwardElection: response sent and completed.");
        } catch (Exception e) {
            responseObserver.onError(e);
            System.err.println("[GrpcServer] forwardElection: error occurred.");
            e.printStackTrace();
        }
    }

    @Override
    public void announceJoin(@NotNull PlantNetwork.PlantInfoMessage request, @NotNull StreamObserver<Empty> responseObserver) {
        try {
            System.out.printf("[GrpcServer] Received announceJoin from %s%n", request.getId());
            topo.addPlant(request.getId());
            client.connect(request.getId(), request.getHost(), request.getPort());
            System.out.printf("[%s] RING (post-join) → %s%n", topo.getMyId(), topo.getPlants());
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
            System.out.println("[GrpcServer] announceJoin: response sent and completed.");
        } catch (Exception e) {
            responseObserver.onError(e);
            System.err.println("[GrpcServer] announceJoin: error occurred.");
            //e.printStackTrace();
        }
    }
}
