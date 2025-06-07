package it.sdp2025.plant;

import it.sdp2025.common.EnergyRequest;

import java.util.LinkedList;
import java.util.Queue;

public class RequestBuffer {
    private final Queue<EnergyRequest> pendingRequests = new LinkedList<>();
    private final Object lock = new Object();

    public void addRequest(EnergyRequest request) {
        synchronized (lock) {
            pendingRequests.offer(request);
            System.out.printf("[BUFFER] Richiesta %d accodata (buffer size: %d)%n",
                    request.getTimestamp(), pendingRequests.size());
        }
    }

    public EnergyRequest getNextRequest() {
        synchronized (lock) {
            EnergyRequest request = pendingRequests.poll();
            if (request != null) {
                System.out.printf("[BUFFER] Richiesta %d estratta dal buffer (buffer size: %d)%n",
                        request.getTimestamp(), pendingRequests.size());
            }
            return request;
        }
    }

    public boolean isEmpty() {
        synchronized (lock) {
            return pendingRequests.isEmpty();
        }
    }

    public EnergyRequest nextRequest() {
        synchronized (lock) {
            return pendingRequests.peek();
        }
    }

}