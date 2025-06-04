package io.preboot.eventbus.tasks;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
public class HashUtils {
    @SneakyThrows
    public static <T> String getHash(Map<String, T> params) {
        if (params == null || params.isEmpty()) {
            return "-";
        }
        List<Map.Entry<String, T>> sortedEntries = new ArrayList<>(params.entrySet());
        sortedEntries.sort(Map.Entry.comparingByKey());
        String mapString =
                sortedEntries.stream().map(e -> e.getKey() + e.getValue()).collect(Collectors.joining());
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] hashBytes = digest.digest(mapString.getBytes());
        return bytesToHex(hashBytes);
    }

    private static String bytesToHex(byte[] hashInBytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : hashInBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
