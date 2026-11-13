#include "calyra/core.h"
#include "calyra/network_policy.h"
#include "calyra/policy_record.h" // from C module (header-only include path)
#include <cstring>
#include <cstdlib>

extern "C" {

CalyraStatus CALYRA_CALL calyra_engine_create(const CalyraEngineConfig* cfg, CalyraEngine** out) {
  if (!cfg || !out) return CALYRA_E_INVALID_ARG;
  if (cfg->abi_version != CALYRA_ABI_VERSION) return CALYRA_E_INVALID_ARG;
  // ownership: caller must call calyra_engine_destroy
  auto* impl = new calyra::EngineImpl(*cfg);
  CalyraStatus s = impl->init();
  if (s != CALYRA_OK) { delete impl; return s; }
  *out = reinterpret_cast<CalyraEngine*>(impl);
  return CALYRA_OK;
}

CalyraStatus CALYRA_CALL calyra_engine_destroy(CalyraEngine* e) {
  if (!e) return CALYRA_E_INVALID_ARG;
  auto* impl = reinterpret_cast<calyra::EngineImpl*>(e);
  delete impl;
  return CALYRA_OK;
}

CalyraStatus CALYRA_CALL calyra_engine_navigate(CalyraEngine* e, const char* url_utf8, size_t len) {
  if (!e || !url_utf8) return CALYRA_E_INVALID_ARG;
  if (len == 0 || len > 2048) return CALYRA_E_OVERFLOW;
  // No NUL trust — use len
  std::string url(url_utf8, len);
  auto* impl = reinterpret_cast<calyra::EngineImpl*>(e);
  return impl->navigate(url);
}

CalyraStatus CALYRA_CALL calyra_engine_get_sandbox_active(CalyraEngine* e, int* out_active) {
  if (!e || !out_active) return CALYRA_E_INVALID_ARG;
  auto* impl = reinterpret_cast<calyra::EngineImpl*>(e);
  *out_active = impl->isSandboxActive() ? 1 : 0;
  return CALYRA_OK;
}

void CALYRA_CALL calyra_free(void* p) { free(p); }

} // extern C

namespace calyra {

EngineImpl::EngineImpl(CalyraEngineConfig cfg) : cfg_(cfg) {}

CalyraStatus EngineImpl::init() {
  // Validate profile path UTF-8 length
  if (cfg_.profile_path_utf8 == nullptr || cfg_.profile_path_len == 0) return CALYRA_E_INVALID_ARG;
  if (cfg_.profile_path_len > 1024) return CALYRA_E_OVERFLOW;

  // Sandbox check: in real CEF embedding, we'd call CefInitialize with sandbox info
  // and query sandbox status via CefSandboxInfo::is_sandboxed.
  // Here we simulate: sandbox_active_ is true if enable_sandbox==1 and OS supports it.
  // CI test asserts fail if --no-sandbox flag is present in release.
  sandbox_active_ = (cfg_.enable_sandbox == 1);

  // Network policy
  std::string proxy;
  if (cfg_.proxy_url_utf8 && cfg_.proxy_url_len > 0) {
    if (cfg_.proxy_url_len > 512) return CALYRA_E_OVERFLOW;
    proxy.assign(cfg_.proxy_url_utf8, cfg_.proxy_url_len);
  }
  bool tor_mode = (cfg_.profile_type == 2);
  policy_ = std::make_unique<FailClosedProxyPolicy>(proxy, tor_mode);
  return CALYRA_OK;
}

CalyraStatus EngineImpl::navigate(const std::string& url) {
  if (url.find("://") == std::string::npos) return CALYRA_E_INVALID_ARG;
  // Policy decision (fail-closed enforcement)
  auto decision = policy_->decide(url, url, false);
  if (decision.route == NetworkPolicy::Route::BLOCK) return CALYRA_E_DENIED;
  // Real implementation would call CefBrowserHost::CreateBrowser / LoadURL
  // For skeleton, we validate route and return OK (integration test will verify proxy-only traffic)
  return CALYRA_OK;
}

bool EngineImpl::isSandboxActive() const { return sandbox_active_; }

} // namespace calyra
