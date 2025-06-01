package it.sdp2025.plant;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import it.sdp2025.PlantNetwork;
import it.sdp2025.PlantServiceGrpc;

public class GrpcServer extends PlantServiceGrpc.PlantServiceImplBase {

    private final ElectionManager electionMgr;
    private final TopologyManager topo;
    private final GrpcClient client;

    public GrpcServer(ElectionManager e, TopologyManager t, GrpcClient client) {
        this.electionMgr = e;
        this.topo = t;
        this.client = client;
    }

    @Override
    public void forwardElection(PlantNetwork.ElectionMsg request,
                                StreamObserver<Empty> responseObserver) {
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
    public void announceJoin(PlantNetwork.PlantInfoMsg req, StreamObserver<Empty> resp) {
        try {
            System.out.printf("[GrpcServer] Received announceJoin from %s%n", req.getId());
            topo.addPlant(req.getId());
            client.connect(req.getId(), req.getHost(), req.getPort());
            System.out.printf("[%s] RING (post-join) → %s%n", topo.getMyId(), topo.getPlants());
            resp.onNext(Empty.getDefaultInstance());
            resp.onCompleted();
            System.out.println("[GrpcServer] announceJoin: response sent and completed.");
        } catch (Exception e) {
            resp.onError(e);
            System.err.println("[GrpcServer] announceJoin: error occurred.");
            e.printStackTrace();
        }
    }
}
