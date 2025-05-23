package it.sdp2025.tests;

import it.sdp2025.proto.Co2Average;
import it.sdp2025.proto.Co2AverageList;

import java.io.FileOutputStream;
import java.util.List;

public class TestServer {
    public static void main(String[] args) throws Exception {
        Co2AverageList list = Co2AverageList.newBuilder()
                .setPlantId("plant1")
                .setCreationTimestamp(System.currentTimeMillis())
                .addAllAvgs(List.of(
                        Co2Average.newBuilder().setAvg(42.0).setTimestamp(System.currentTimeMillis() - 10000).build(),
                        Co2Average.newBuilder().setAvg(55.0).setTimestamp(System.currentTimeMillis()).build()
                )).build();

        try (FileOutputStream out = new FileOutputStream("data.bin")) {
            list.writeTo(out);
        }

        System.out.println("data.bin written.");
    }
}
