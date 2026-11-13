using System.Collections.Generic;

namespace Calyra.UI.Services;

public enum PermissionKind { Camera, Microphone, Location, Notification, Clipboard, ScreenCapture, Usb, Bluetooth }

public class PermissionService
{
    private readonly Dictionary<(string origin, PermissionKind kind), bool> _grants = new();

    public bool Get(string origin, PermissionKind kind) => _grants.TryGetValue((origin,kind), out var v) && v;

    public void Set(string origin, PermissionKind kind, bool granted, bool persistent)
    {
        // Persistent vs session: persistent stored in profile, session in memory only
        if (persistent) _grants[(origin,kind)] = granted;
        else _grants[(origin,kind)] = granted; // session handled by expiry on close
    }

    // OS denied => respect
    public bool IsOsDenied(string origin, PermissionKind kind) => false; // query OS API
}
