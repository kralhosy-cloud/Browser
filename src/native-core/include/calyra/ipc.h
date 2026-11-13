#pragma once
#include "core.h"
#include <cstdint>
#include <cstddef>
#include <string>

#define CALYRA_IPC_MAGIC 0x43414C59 // 'CALY'
#define CALYRA_IPC_VERSION 1
#define CALYRA_IPC_MAX_BODY 1048576 // 1 MiB

namespace calyra::ipc {

enum class MsgType : uint32_t {
  HELLO = 1,
  CHALLENGE = 2,
  HELLO_ACK = 3,
  UPDATE_FILTERS_REQ = 10,
  UPDATE_FILTERS_RESP = 11,
  REPORT_REQ = 12,
  REPORT_RESP = 13,
  CANCEL = 99,
};

struct Header {
  uint32_t magic;   // CALYRA_IPC_MAGIC
  uint32_t version; // 1
  uint32_t type;    // MsgType
  uint32_t length;  // body length
  uint32_t request_id;
  uint32_t reserved;
};

CalyraStatus frame_message(MsgType type, uint32_t request_id, const uint8_t* body, size_t body_len, std::string& out);
CalyraStatus parse_header(const uint8_t* data, size_t len, Header* out);

// Platform pipe/socket path helper
std::string helper_socket_path();

} // namespace calyra::ipc
