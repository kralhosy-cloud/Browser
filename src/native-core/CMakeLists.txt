cmake_minimum_required(VERSION 3.20)
project(calyra-native-core CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(Threads REQUIRED)

add_library(calyra_core SHARED
    src/core.cc
    src/network_policy.cc
    src/ipc.cc
    src/sandbox.cc
)
target_include_directories(calyra_core PUBLIC include PRIVATE ../c-policy/include)
target_link_libraries(calyra_core PRIVATE Threads::Threads)

# Hardening
if(UNIX)
  target_compile_options(calyra_core PRIVATE -Wall -Wextra -Werror -fstack-protector-strong -D_FORTIFY_SOURCE=2 -fPIE -Wformat -Wformat-security)
  target_link_options(calyra_core PRIVATE -Wl,-z,RELRO -Wl,-z,now -pie)
endif()

# CEF integration — real build would add:
# add_subdirectory(${CEF_ROOT} cef)
# target_include_directories(calyra_core PRIVATE ${CEF_ROOT})
# target_link_libraries(calyra_core PRIVATE libcef_dll_wrapper)

add_executable(calyra_sandbox_test test/test_sandbox.cc)
target_link_libraries(calyra_sandbox_test PRIVATE calyra_core)

enable_testing()
add_test(NAME sandbox_active COMMAND calyra_sandbox_test --require-sandbox)

add_executable(test_ipc test/test_ipc.cc)
target_link_libraries(test_ipc PRIVATE calyra_core)
add_test(NAME test_ipc COMMAND test_ipc)
