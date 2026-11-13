#include "calyra/policy_record.h"
#include <string.h>
#include <time.h>

static uint32_t crc32_table[256];
static bool crc_table_inited = false;

static void init_crc_table(void) {
    if (crc_table_inited) return;
    for (uint32_t i = 0; i < 256; i++) {
        uint32_t c = i;
        for (int j = 0; j < 8; j++) c = (c & 1) ? (0xEDB88320U ^ (c >> 1)) : (c >> 1);
        crc32_table[i] = c;
    }
    crc_table_inited = true;
}

uint32_t calyra_crc32(const uint8_t* data, size_t len) {
    init_crc_table();
    uint32_t crc = 0xFFFFFFFFu;
    for (size_t i = 0; i < len; i++) crc = crc32_table[(crc ^ data[i]) & 0xFF] ^ (crc >> 8);
    return crc ^ 0xFFFFFFFFu;
}

uint32_t calyra_fnv1a32(const char* data, size_t len) {
    uint32_t h = 2166136261u;
    for (size_t i = 0; i < len; i++) {
        h ^= (uint8_t)data[i];
        h *= 16777619u;
    }
    return h;
}

CalyraPolicyStatus calyra_policy_validate(const CalyraPolicyRecord* rec) {
    if (!rec) return CALYRA_STATUS_INVALID_ARG;
    if (memcmp(rec->magic, CALYRA_POLICY_MAGIC, 4) != 0) return CALYRA_STATUS_BAD_MAGIC;
    if (rec->version != CALYRA_POLICY_VERSION) return CALYRA_STATUS_UNSUPPORTED_VERSION;
    if (rec->route > CALYRA_ROUTE_TOR) return CALYRA_STATUS_CORRUPT;
    if (rec->domain_len > CALYRA_MAX_DOMAIN_LEN) return CALYRA_STATUS_OVERFLOW;
    // reserved must be 0
    if (rec->reserved0 != 0) return CALYRA_STATUS_CORRUPT;
    for (int i = 0; i < 7; i++) if (rec->reserved1[i] != 0) return CALYRA_STATUS_CORRUPT;
    // CRC
    uint32_t got = rec->crc32;
    uint32_t calc = calyra_crc32((const uint8_t*)rec, 60);
    if (got != calc) return CALYRA_STATUS_CORRUPT;
    // expiry
    if (rec->expires_at_unix != 0 && rec->expires_at_unix < rec->created_at_unix) return CALYRA_STATUS_CORRUPT;
    return CALYRA_STATUS_OK;
}

CalyraPolicyStatus calyra_policy_encode(const CalyraPolicyRecord* rec, uint8_t out[64]) {
    if (!rec || !out) return CALYRA_STATUS_INVALID_ARG;
    CalyraPolicyStatus s = calyra_policy_validate(rec);
    if (s != CALYRA_STATUS_OK) return s;
    memcpy(out, rec, 64);
    return CALYRA_STATUS_OK;
}

CalyraPolicyStatus calyra_policy_decode(const uint8_t in[64], CalyraPolicyRecord* out) {
    if (!in || !out) return CALYRA_STATUS_INVALID_ARG;
    memcpy(out, in, 64);
    return calyra_policy_validate(out);
}

CalyraPolicyStatus calyra_policy_create(const char* domain_utf8, size_t domain_len,
                                        CalyraRoute route, uint32_t flags,
                                        CalyraPolicyRecord* out) {
    if (!out) return CALYRA_STATUS_INVALID_ARG;
    if (domain_utf8 == NULL && domain_len != 0) return CALYRA_STATUS_INVALID_ARG;
    if (domain_len > CALYRA_MAX_DOMAIN_LEN) return CALYRA_STATUS_OVERFLOW;
    if (route > CALYRA_ROUTE_TOR) return CALYRA_STATUS_INVALID_ARG;

    memset(out, 0, sizeof(*out));
    memcpy(out->magic, CALYRA_POLICY_MAGIC, 4);
    out->version = CALYRA_POLICY_VERSION;
    out->route = (uint32_t)route;
    out->flags = flags;
    out->created_at_unix = (uint32_t)time(NULL);
    out->expires_at_unix = 0;
    out->domain_len = (uint16_t)domain_len;
    if (domain_len > 0) {
        out->domain_hash = calyra_fnv1a32(domain_utf8, domain_len);
    } else {
        out->domain_hash = 0;
    }
    out->crc32 = calyra_crc32((const uint8_t*)out, 60);
    return CALYRA_STATUS_OK;
}
