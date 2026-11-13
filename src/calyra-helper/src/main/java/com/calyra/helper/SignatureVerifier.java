package com.calyra.helper;

import java.security.*;
import java.util.HexFormat;

/**
 * Verifies signed filter lists (Ed25519) if signature present.
 * No custom crypto — uses JDK/java libsodium.
 */
public class SignatureVerifier {
    // For demo: Ed25519 verification with JDK 21 (supported via EdDSA)
    public boolean verify(byte[] message, byte[] signature, PublicKey pub) throws Exception {
        Signature sig = Signature.getInstance("Ed25519");
        sig.initVerify(pub);
        sig.update(message);
        return sig.verify(signature);
    }

    public static String sha256Hex(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(md.digest(data));
    }
}
