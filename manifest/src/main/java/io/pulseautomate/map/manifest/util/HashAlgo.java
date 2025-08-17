package io.pulseautomate.map.manifest.util;

import java.security.MessageDigest;

public enum HashAlgo {
    SHA256("SHA-256");

    public final String jca;
    HashAlgo(String jca) { this.jca = jca; }

    public MessageDigest newDigest() {
        try { return MessageDigest.getInstance(jca); }
        catch (Exception e) { throw new RuntimeException("Missing JCA digest: " + jca, e); }
    }
}
