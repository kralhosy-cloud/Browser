cmake_minimum_required(VERSION 3.20)
project(calyra-policy C)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_EXTENSIONS OFF)

add_library(calyra_policy STATIC
    src/policy_codec.c
)
target_include_directories(calyra_policy PUBLIC include)
target_compile_options(calyra_policy PRIVATE -Wall -Wextra -Werror -fstack-protector-strong -D_FORTIFY_SOURCE=2 -Wformat -Wformat-security)
target_link_options(calyra_policy PRIVATE -Wl,-z,RELRO -Wl,-z,now)

enable_testing()
add_executable(test_policy test/test_policy.c)
target_link_libraries(test_policy PRIVATE calyra_policy)
add_test(NAME test_policy COMMAND test_policy)

add_executable(fuzz_policy test/fuzz_policy.c)
target_link_libraries(fuzz_policy PRIVATE calyra_policy)
