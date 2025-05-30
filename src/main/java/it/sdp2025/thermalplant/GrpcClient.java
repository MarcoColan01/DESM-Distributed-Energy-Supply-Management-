package it.sdp2025.thermalplant;

import it.sdp2025.common.PlantInfo;
import it.sdp2025.proto.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.ArrayDeque;
import java.util.Queue;

public class GrpcClient {

    private final PlantConfig cfg;
    private final ElectionManager election;

    /* sincronizzati su `this` */
    private ManagedChannel channel;
    private StreamObserver<ElectionEnvelope> stream;
    private String currentSuccId = null;
    private final Queue<ElectionEnvelope> pending = new ArrayDeque<>();

    public GrpcClient(PlantConfig cfg, ElectionManager election) {
        this.cfg = cfg;
        this.election = election;
    }

    /* ------------------------------------------------------------------ */
    /** (ri)apre il canale verso il successore indicato. */
    public synchronized void connect(PlantInfo succ) {
        if (succ == null) return;
        if (succ.getId().equals(currentSuccId)) return;              // già connesso

        /* 1) costruiamo **prima** il nuovo canale (+ stream) ----------- */
        ManagedChannel newCh = ManagedChannelBuilder
                .forAddress(succ.getHost(), succ.getPort())
                .usePlaintext()
                .build();

        StreamObserver<ElectionEnvelope> newStream =
                PlantRingGrpc.newStub(newCh).ringStream(new RingInbound());

        /* 2) aggiorniamo gli handler atomici --------------------------- */
        ManagedChannel oldCh = this.channel;
        this.channel        = newCh;
        this.stream         = newStream;
        this.currentSuccId  = succ.getId();

        System.out.printf("[gRPC-client] Stream verso %s aperto%n", succ.getId());

        /* 3) ora che lo stream nuovo è attivo, chiudiamo il vecchio ---- */
        if (oldCh != null && !oldCh.isShutdown()) oldCh.shutdownNow();
        while (!pending.isEmpty()) stream.onNext(pending.poll());
    }

    public synchronized void sendElection(String candId, double price) {
        ElectionMsg m = ElectionMsg.newBuilder()
                .setCandidateId(candId)
                .setPrice(price)
                .build();
        ElectionEnvelope env = ElectionEnvelope.newBuilder().setElection(m).build();

        if (stream == null) {
            System.out.printf("[gRPC-client] stream nullo → metto in coda (id=%s)%n", candId);
            pending.add(env);
            return;
        }
        stream.onNext(env);
    }

    public synchronized void sendElected(String coordId) {
        if (stream == null) return;
        ElectedMsg m = ElectedMsg.newBuilder().setCoordinatorId(coordId)
                .build();
        stream.onNext(ElectionEnvelope.newBuilder().setElected(m).build());
    }

    /* ------------------------------------------------------------------ */
    private class RingInbound implements StreamObserver<ElectionEnvelope> {
        public void onNext(ElectionEnvelope env) {
            if (env.hasElection())
                election.onElection(env.getElection().getCandidateId(),
                        env.getElection().getPrice());
            else if (env.hasElected())
                election.onElected(env.getElected().getCoordinatorId());
        }
        public void onError(Throwable t) {
            System.err.println("[gRPC-stream] errore: " + t.getMessage());
        }
        public void onCompleted() {/* il successore si è spento */ }
    }
}
