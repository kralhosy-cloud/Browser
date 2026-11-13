#include "calyra/policy_record.h"
#include <assert.h>
#include <string.h>
#include <stdio.h>

int main(void) {
    CalyraPolicyRecord rec;
    assert(calyra_policy_create("example.com", 11, CALYRA_ROUTE_PROXY, 0x3, &rec) == CALYRA_STATUS_OK);
    assert(calyra_policy_validate(&rec) == CALYRA_STATUS_OK);
    uint8_t buf[64];
    assert(calyra_policy_encode(&rec, buf) == CALYRA_STATUS_OK);
    CalyraPolicyRecord out;
    assert(calyra_policy_decode(buf, &out) == CALYRA_STATUS_OK);
    assert(out.domain_hash == rec.domain_hash);
    // corrupt CRC
    buf[10] ^= 0xFF;
    assert(calyra_policy_decode(buf, &out) == CALYRA_STATUS_CORRUPT);
    // overflow domain_len
    CalyraPolicyRecord bad = rec;
    bad.domain_len = 500;
    bad.crc32 = calyra_crc32((uint8_t*)&bad, 60);
    assert(calyra_policy_validate(&bad) == CALYRA_STATUS_OVERFLOW);
    // bad magic
    memcpy(bad.magic, "BAD!", 4);
    bad.crc32 = calyra_crc32((uint8_t*)&bad, 60);
    assert(calyra_policy_validate(&bad) == CALYRA_STATUS_BAD_MAGIC);
    printf("All policy tests passed.\n");
    return 0;
}
