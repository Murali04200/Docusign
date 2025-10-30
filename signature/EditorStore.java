package com.example.Docusign.signature;

import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EditorStore {
    public static class Recipient {
        public String name;
        public String email;
        public String permission; // SIGN, VIEW, COPY
    }

    public static class TempEnvelope {
        public String id;
        public Path filePath;
        public String fileName;
        public String contentType;
        public Recipient[] recipients;
        public String placementsJson; // raw JSON from client (for demo)
    }

    private final Map<String, TempEnvelope> store = new ConcurrentHashMap<>();

    public String save(Path filePath, String fileName, String contentType, Recipient[] recipients) {
        TempEnvelope env = new TempEnvelope();
        env.id = UUID.randomUUID().toString();
        env.filePath = filePath;
        env.fileName = fileName;
        env.contentType = contentType;
        env.recipients = recipients;
        store.put(env.id, env);
        return env.id;
    }

    public TempEnvelope get(String id) {
        return store.get(id);
    }

    public void setPlacements(String id, String placementsJson) {
        TempEnvelope env = store.get(id);
        if (env != null) {
            env.placementsJson = placementsJson;
        }
    }

    public TempEnvelope remove(String id) {
        return store.remove(id);
    }
}
