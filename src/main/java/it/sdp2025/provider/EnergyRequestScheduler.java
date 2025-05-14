package it.sdp2025.provider;

import it.sdp2025.proto.EnergyRequestOuterClass;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
@Slf4j
public class EnergyRequestScheduler {
    private final MqttPublisher mqttPublisher;

    @Scheduled(fixedRate = MqttConstants.PUBLISH_INTERVAL_MS)
    public void publishEnergyRequest(){
        int kwhQty = ThreadLocalRandom.current().nextInt(5000, 15000+1);
        EnergyRequestOuterClass.EnergyRequest request = EnergyRequestOuterClass.EnergyRequest.newBuilder()
                .setRequestId(UUID.randomUUID().toString())
                .setQuantityKwh(kwhQty)
                .setTimestampMs(System.currentTimeMillis())
                .build();
        mqttPublisher.publish(request.toByteArray());
        System.out.println("Published new request "  + request.getRequestId() + " of " + kwhQty);
    }
}
