// Minimal fuzz harness for policy codec — to be run with libFuzzer/AFL++
#include "calyra/policy_record.h"
#include <stdint.h>
#include <stddef.h>

int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size) {
    if (size != 64) return 0;
    CalyraPolicyRecord rec;
    // Must not crash, must not over-allocate, must return deterministic status
    CalyraPolicyStatus s = calyra_policy_decode(data, &rec);
    (void)s;
    // Re-encode if valid
    if (s == CALYRA_STATUS_OK) {
        uint8_t out[64];
        calyra_policy_encode(&rec, out);
    }
    return 0;
}
// For non-fuzzer builds
#ifdef FUZZ_STANDALONE
#include <stdio.h>
int main(){ uint8_t b[64]={0}; LLVMFuzzerTestOneInput(b,64); printf("fuzz smoke ok\n"); return 0;}
#endif
