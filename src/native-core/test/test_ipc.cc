#include "calyra/ipc.h"
#include <cassert>
#include <iostream>
int main(){
  std::string out;
  const char* body="hello";
  auto s = calyra::ipc::frame_message(calyra::ipc::MsgType::HELLO, 42, (const uint8_t*)body, 5, out);
  assert(s==CALYRA_OK);
  calyra::ipc::Header h;
  auto p = calyra::ipc::parse_header((const uint8_t*)out.data(), out.size(), &h);
  assert(p==CALYRA_OK);
  assert(h.request_id==42);
  assert(h.length==5);
  // overflow
  std::string big;
  big.assign(2*1024*1024,'x');
  s = calyra::ipc::frame_message(calyra::ipc::MsgType::HELLO,1,(const uint8_t*)big.data(), big.size(), out);
  assert(s==CALYRA_E_OVERFLOW);
  std::cout<<"ipc tests passed, socket="<<calyra::ipc::helper_socket_path()<<"\n";
  return 0;
}
