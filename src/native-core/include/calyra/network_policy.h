#pragma once
#include "core.h"
#include <string>

namespace calyra {

class FailClosedProxyPolicy : public NetworkPolicy {
public:
  explicit FailClosedProxyPolicy(const std::string& proxy_url, bool tor_mode);
  Decision decide(const std::string& url, const std::string& initiator, bool is_third_party) override;
private:
  std::string proxy_url_;
  bool tor_mode_;
};

} // namespace calyra
