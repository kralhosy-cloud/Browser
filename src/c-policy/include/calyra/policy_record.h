#ifndef CALYRA_POLICY_RECORD_H
#define CALYRA_POLICY_RECORD_H

#include <stdint.h>
#include <stddef.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

#define CALYRA_POLICY_MAGIC "CLRA"
#define CALYRA_POLICY_MAGIC_LEN 4
#define CALYRA_POLICY_VERSION 1
#define CALYRA_POLICY_RECORD_SIZE 64
#define CALYRA_MAX_DOMAIN_LEN 253
#ifdef __cplusplus
#define CALYRA_STATIC_ASSERT static_assert
#else
#define CALYRA_STATIC_ASSERT _Static_assert
#endif

typedef enum {
    CALYRA_ROUTE_DIRECT = 0,
    CALYRA_ROUTE_PROXY  = 1,
    CALYRA_ROUTE_TOR    = 2,
} CalyraRoute;

typedef enum {
    CALYRA_STATUS_OK = 0,
    CALYRA_STATUS_INVALID_ARG = 1,
    CALYRA_STATUS_OVERFLOW = 2,
    CALYRA_STATUS_BAD_MAGIC = 3,
    CALYRA_STATUS_UNSUPPORTED_VERSION = 4,
    CALYRA_STATUS_CORRUPT = 5,
} CalyraPolicyStatus;

/* Fixed-size 64-byte policy record. Naturally aligned, no packing.
 * Layout is versioned and extensible via flags/reserved.
 */
typedef struct {
    char     magic[4];          // "CLRA"
    uint32_t version;           // 1
    uint32_t route;             // CalyraRoute
    uint32_t flags;             // bitmask (bit0=thirdPartyCookiesBlocked, bit1=webrtcBlocked, etc.)
    uint32_t domain_hash;       // FNV-1a 32 of canonical domain (0 = wildcard)
    uint32_t created_at_unix;   // seconds since epoch
    uint32_t expires_at_unix;   // 0 = no expiry
    uint16_t domain_len;        // if inline domain stored separately (not in this fixed record)
    uint16_t reserved0;
    uint32_t reserved1[7];      // must be 0
    uint32_t crc32;             // CRC32 of first 60 bytes (magic..reserved1)
} CalyraPolicyRecord;

CALYRA_STATIC_ASSERT(sizeof(CalyraPolicyRecord) == 64, "CalyraPolicyRecord must be 64 bytes");

/* Validation */
CalyraPolicyStatus calyra_policy_validate(const CalyraPolicyRecord* rec);

/* CRC32 (IEEE) */
uint32_t calyra_crc32(const uint8_t* data, size_t len);

/* FNV-1a 32 */
uint32_t calyra_fnv1a32(const char* data, size_t len);

/* Safe codec: encode to 64-byte buffer, decode from buffer.
 * Ownership: caller owns buffer. No allocation.
 */
CalyraPolicyStatus calyra_policy_encode(const CalyraPolicyRecord* rec, uint8_t out[64]);
CalyraPolicyStatus calyra_policy_decode(const uint8_t in[64], CalyraPolicyRecord* out);

/* Helper: create record for domain */
CalyraPolicyStatus calyra_policy_create(const char* domain_utf8, size_t domain_len,
                                        CalyraRoute route, uint32_t flags,
                                        CalyraPolicyRecord* out);

#ifdef __cplusplus
}
#endif

#endif /* CALYRA_POLICY_RECORD_H */
