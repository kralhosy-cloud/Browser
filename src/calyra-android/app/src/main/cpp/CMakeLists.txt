cmake_minimum_required(VERSION 3.22)
project(calyra_policy_android)

add_library(calyra_policy SHARED
    ../../../../../c-policy/src/policy_codec.c
    policy_jni.cpp
)
target_include_directories(calyra_policy PRIVATE ../../../../../c-policy/include)
find_library(log-lib log)
target_link_libraries(calyra_policy ${log-lib})
