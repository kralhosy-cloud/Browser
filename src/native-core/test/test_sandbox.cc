#include "calyra/core.h"
#include <iostream>
#include <string>
#include <cstring>

int main(int argc, char* argv[]) {
  bool require = false;
  for (int i=1;i<argc;i++) if (std::string(argv[i])=="--require-sandbox") require=true;
  CalyraEngineConfig cfg{};
  cfg.abi_version = CALYRA_ABI_VERSION;
  const char* path = "/tmp/calyra-test-profile";
  cfg.profile_path_utf8 = path;
  cfg.profile_path_len = strlen(path);
  cfg.enable_sandbox = 1;
  cfg.profile_type = 0;
  CalyraEngine* e=nullptr;
  auto s = calyra_engine_create(&cfg, &e);
  if (s!=CALYRA_OK){ std::cerr<<"engine create failed\n"; return 2; }
  int active=0;
  calyra_engine_get_sandbox_active(e,&active);
  std::cout<<"sandbox_active="<<active<<"\n";
  if (require && !active){ std::cerr<<"FAIL: sandbox not active in release build\n"; return 1; }
  calyra_engine_destroy(e);
  return 0;
}
