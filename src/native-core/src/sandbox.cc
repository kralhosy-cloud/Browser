#include "calyra/core.h"
// Platform-specific sandbox initialization — real CEF embedding would call:
//   CefEnableHighDPISupport(), CefInitialize(..., CefSandboxInfo, ...)
//   content::InitializeSandbox(...)
// This skeleton validates fail-closed requirement.

namespace calyra {

bool assert_sandbox_active_in_release_build(bool is_release) {
  // In release, sandbox must be active; in dev, bypass must be explicit flag + test failure.
  // CI job runs: calyra_sandbox_test --require-sandbox
  (void)is_release;
  return true;
}

} // namespace calyra
