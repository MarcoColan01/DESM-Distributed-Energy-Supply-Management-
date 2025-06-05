package it.sdp2025.plant;

import it.sdp2025.common.EnergyRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Buffer thread-safe per accodare le richieste di energia che non possono
 * essere immediatamente processate perché tutte le centrali sono occupate.
 *
 * Implementazione senza java.util.concurrent come richiesto dal progetto.
 */
public class RequestBuffer {
    private final Queue<EnergyRequest> pendingRequests = new LinkedList<>();
    private final Object lock = new Object();

    /**
     * Aggiunge una richiesta al buffer
     */
    public void addRequest(@NotNull EnergyRequest request) {
        synchronized (lock) {
            pendingRequests.offer(request);
            System.out.printf("[BUFFER] Richiesta %d accodata (buffer size: %d)%n",
                    request.getTimestamp(), pendingRequests.size());
        }
    }

    /**
     * Rimuove e restituisce la prima richiesta dal buffer, o null se vuoto
     */
    @Nullable
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

    /**
     * Verifica se il buffer è vuoto
     */
    public boolean isEmpty() {
        synchronized (lock) {
            return pendingRequests.isEmpty();
        }
    }

    /**
     * Restituisce il numero di richieste in coda
     */
    public int size() {
        synchronized (lock) {
            return pendingRequests.size();
        }
    }

    /**
     * Restituisce la prima richiesta senza rimuoverla dal buffer
     */
    @Nullable
    public EnergyRequest peekNext() {
        synchronized (lock) {
            return pendingRequests.peek();
        }
    }

    /**
     * Rimuove tutte le richieste che soddisfano una condizione
     */
    public void removeIf(java.util.function.Predicate<EnergyRequest> condition) {
        synchronized (lock) {
            pendingRequests.removeIf(condition);
        }
    }
}