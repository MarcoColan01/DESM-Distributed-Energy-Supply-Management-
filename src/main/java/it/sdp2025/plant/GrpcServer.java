package it.sdp2025.plant;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import it.sdp2025.PlantNetwork;
import it.sdp2025.PlantServiceGrpc;

public class GrpcServer extends PlantServiceGrpc.PlantServiceImplBase {

    private final ElectionManager electionMgr;
    private final TopologyManager topo;

    public GrpcServer(ElectionManager e, TopologyManager t) {
        this.electionMgr = e; this.topo = t;
    }

    @Override
    public void forwardElection(PlantNetwork.ElectionMsg request,
                                StreamObserver<Empty> responseObserver) {
        electionMgr.onElectionMsg(request);
        responseObserver.onNext(com.google.protobuf.Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void announceJoin(PlantNetwork.PlantInfoMsg request,
                             StreamObserver<com.google.protobuf.Empty> responseObserver) {
        topo.addPlant(request.getId());
        responseObserver.onNext(com.google.protobuf.Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}

