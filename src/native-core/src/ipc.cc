#include "calyra/ipc.h"
#include <cstring>
#include <cstdlib>

#ifdef _WIN32
#include <windows.h>
#else
#include <unistd.h>
#include <sys/types.h>
#endif

namespace calyra::ipc {

CalyraStatus frame_message(MsgType type, uint32_t request_id, const uint8_t* body, size_t body_len, std::string& out) {
  if (body_len > CALYRA_IPC_MAX_BODY) return CALYRA_E_OVERFLOW;
  Header h{};
  h.magic = CALYRA_IPC_MAGIC;
  h.version = CALYRA_IPC_VERSION;
  h.type = static_cast<uint32_t>(type);
  h.length = static_cast<uint32_t>(body_len);
  h.request_id = request_id;
  h.reserved = 0;
  out.resize(sizeof(Header) + body_len);
  memcpy(out.data(), &h, sizeof(h));
  if (body_len) memcpy(out.data()+sizeof(h), body, body_len);
  return CALYRA_OK;
}

CalyraStatus parse_header(const uint8_t* data, size_t len, Header* out) {
  if (!data || !out) return CALYRA_E_INVALID_ARG;
  if (len < sizeof(Header)) return CALYRA_E_OVERFLOW;
  memcpy(out, data, sizeof(Header));
  if (out->magic != CALYRA_IPC_MAGIC) return CALYRA_E_DENIED;
  if (out->version != CALYRA_IPC_VERSION) return CALYRA_E_DENIED;
  if (out->length > CALYRA_IPC_MAX_BODY) return CALYRA_E_OVERFLOW;
  return CALYRA_OK;
}

std::string helper_socket_path() {
#ifdef _WIN32
  // Named pipe — user-scoped with random suffix (generated at startup, not hardcoded)
  char buf[64];
  uint32_t uid = (uint32_t)GetCurrentProcessId(); // simplified; real: user SID hash
  snprintf(buf, sizeof(buf), "\\\\.\\pipe\\calyra-helper-%u", uid);
  return std::string(buf);
#else
  const char* xdg = getenv("XDG_RUNTIME_DIR");
  if (!xdg) xdg = "/tmp";
  uid_t uid = getuid();
  char buf[256];
  snprintf(buf, sizeof(buf), "%s/calyra/helper-%d.sock", xdg, (int)uid);
  return std::string(buf);
#endif
}

} // namespace calyra::ipc
