#pragma once
#include <cstdint>
#include <cstddef>
#include <memory>
#include <string>
#include <functional>

#define CALYRA_ABI_VERSION 1
#define CALYRA_CALL

#ifdef _WIN32
  #define CALYRA_EXPORT __declspec(dllexport)
#else
  #define CALYRA_EXPORT __attribute__((visibility("default")))
#endif

extern "C" {

typedef enum {
  CALYRA_OK = 0,
  CALYRA_E_INVALID_ARG = 1,
  CALYRA_E_OVERFLOW = 2,
  CALYRA_E_NOT_FOUND = 3,
  CALYRA_E_DENIED = 4,
  CALYRA_E_TIMEOUT = 5,
  CALYRA_E_INTERNAL = 6,
} CalyraStatus;

typedef struct CalyraEngine CalyraEngine;
typedef struct CalyraTab CalyraTab;

typedef struct {
  uint32_t abi_version; // must be CALYRA_ABI_VERSION
  const char* profile_path_utf8;
  size_t profile_path_len;
  uint32_t enable_sandbox; // 1 = require sandbox, 0 = fail if sandbox unavailable
  uint32_t profile_type;   // 0=Normal,1=Enhanced,2=Tor
  const char* proxy_url_utf8; // nullable
  size_t proxy_url_len;
} CalyraEngineConfig;

CALYRA_EXPORT CalyraStatus CALYRA_CALL calyra_engine_create(const CalyraEngineConfig* cfg, CalyraEngine** out);
CALYRA_EXPORT CalyraStatus CALYRA_CALL calyra_engine_destroy(CalyraEngine* e);
CALYRA_EXPORT CalyraStatus CALYRA_CALL calyra_engine_navigate(CalyraEngine* e, const char* url_utf8, size_t len);
CALYRA_EXPORT CalyraStatus CALYRA_CALL calyra_engine_get_sandbox_active(CalyraEngine* e, int* out_active);
CALYRA_EXPORT void CALYRA_CALL calyra_free(void* p);

} // extern "C"

// C++ internal API (not exposed via C ABI)
namespace calyra {

class NetworkPolicy {
public:
  enum class Route { DIRECT=0, PROXY=1, TOR=2, BLOCK=3 };
  struct Decision { Route route; std::string proxy_url; bool remote_dns; };
  virtual Decision decide(const std::string& url, const std::string& initiator, bool is_third_party) = 0;
  virtual ~NetworkPolicy() = default;
};

class EngineImpl {
public:
  explicit EngineImpl(CalyraEngineConfig cfg);
  CalyraStatus init();
  CalyraStatus navigate(const std::string& url);
  bool isSandboxActive() const;
private:
  CalyraEngineConfig cfg_;
  bool sandbox_active_ = false;
  std::unique_ptr<NetworkPolicy> policy_;
};

} // namespace calyra
