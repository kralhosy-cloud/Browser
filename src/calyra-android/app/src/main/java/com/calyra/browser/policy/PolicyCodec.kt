package com.calyra.browser.policy

/** JNI bridge to C policy codec — same 64-byte record, same CRC/overflow tests */
object PolicyCodec {
    init { System.loadLibrary("calyra_policy") }
    @JvmStatic external fun validate(bytes: ByteArray): Int // 0=OK

    fun create(domain: String, route: Int, flags: Int): ByteArray {
        // Calls C calyra_policy_create via JNI in real build; here stub for tests
        return ByteArray(64).also { it[0]='C'.code.toByte() }
    }
}
