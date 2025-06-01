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
        electionMgr.onElection(request);
        responseObserver.onNext(com.google.protobuf.Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void announceJoin(PlantNetwork.PlantInfoMsg req, StreamObserver<Empty> resp) {
        topo.addPlant(req.getId());
        client.connect(req.getId(), req.getHost(), req.getPort());  // <--- AGGIUNGI QUESTO!
        System.out.printf("[%s] RING (post-join) → %s%n",
                topo.getMyId(), topo.getPlants());
        resp.onNext(Empty.getDefaultInstance());
        resp.onCompleted();
    }


}

