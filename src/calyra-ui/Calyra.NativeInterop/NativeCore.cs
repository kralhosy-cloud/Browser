using System;
using System.Runtime.InteropServices;

namespace Calyra.NativeInterop;

// Strict ABI contract — must match src/native-core/include/calyra/core.h
// No arbitrary native pointer marshaling; opaque handles only.

internal enum CalyraStatus : uint
{
    Ok = 0,
    InvalidArg = 1,
    Overflow = 2,
    NotFound = 3,
    Denied = 4,
    Timeout = 5,
    Internal = 6,
}

[StructLayout(LayoutKind.Sequential)]
internal struct CalyraEngineConfig
{
    public uint AbiVersion; // must be 1
    public IntPtr ProfilePathUtf8; // UTF-8 bytes, not NUL-dependent, length in next field
    public UIntPtr ProfilePathLen;
    public uint EnableSandbox;
    public uint ProfileType; // 0 Normal, 1 Enhanced, 2 Tor
    public IntPtr ProxyUrlUtf8;
    public UIntPtr ProxyUrlLen;
}

internal static class NativeCore
{
    private const string LibName = "calyra_core"; // libcalyra_core.so / calyra_core.dll

    [DllImport(LibName, CallingConvention = CallingConvention.Cdecl)]
    internal static extern CalyraStatus calyra_engine_create(ref CalyraEngineConfig cfg, out IntPtr outEngine);

    [DllImport(LibName, CallingConvention = CallingConvention.Cdecl)]
    internal static extern CalyraStatus calyra_engine_destroy(IntPtr engine);

    [DllImport(LibName, CallingConvention = CallingConvention.Cdecl)]
    internal static extern CalyraStatus calyra_engine_navigate(IntPtr engine, byte[] urlUtf8, UIntPtr len);

    [DllImport(LibName, CallingConvention = CallingConvention.Cdecl)]
    internal static extern CalyraStatus calyra_engine_get_sandbox_active(IntPtr engine, out int active);

    [DllImport(LibName, CallingConvention = CallingConvention.Cdecl)]
    internal static extern void calyra_free(IntPtr p);
}

// SafeHandle wrappers — no raw pointer leaks to UI layer
internal sealed class EngineHandle : SafeHandle
{
    public EngineHandle() : base(IntPtr.Zero, true) {}
    public override bool IsInvalid => handle == IntPtr.Zero;
    protected override bool ReleaseHandle()
    {
        NativeCore.calyra_engine_destroy(handle);
        return true;
    }
}
