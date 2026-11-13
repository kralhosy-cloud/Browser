#include "calyra/network_policy.h"

namespace calyra {

FailClosedProxyPolicy::FailClosedProxyPolicy(const std::string& proxy_url, bool tor_mode)
  : proxy_url_(proxy_url), tor_mode_(tor_mode) {}

NetworkPolicy::Decision FailClosedProxyPolicy::decide(const std::string& url,
                                                      const std::string& initiator,
                                                      bool is_third_party) {
  (void)initiator; (void)is_third_party;
  // Block non-http(s) unless explicitly allowed (e.g., calyra://, about:blank)
  if (url.rfind("https://", 0) == 0 || url.rfind("http://", 0) == 0) {
    if (tor_mode_) {
      return {Route::TOR, proxy_url_.empty() ? "socks5://127.0.0.1:19050" : proxy_url_, true};
    }
    if (!proxy_url_.empty()) {
      return {Route::PROXY, proxy_url_, true};
    }
    return {Route::DIRECT, "", false};
  }
  if (url.rfind("calyra://", 0) == 0 || url == "about:blank") {
    return {Route::DIRECT, "", false};
  }
  // Fail-closed for unknown schemes (e.g., file:// from web content)
  if (url.rfind("file://", 0) == 0) return {Route::BLOCK, "", false};
  return {Route::BLOCK, "", false};
}

} // namespace calyra
