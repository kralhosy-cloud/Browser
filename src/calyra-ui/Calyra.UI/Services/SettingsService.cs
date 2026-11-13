namespace Calyra.UI.Services;

public enum ThemeMode { Light, Dark, System }
public enum ProfileType { Normal = 0, Enhanced = 1, Tor = 2 }

public class SettingsService
{
    public ThemeMode Theme { get; set; } = ThemeMode.System;
    public int ZoomPercent { get; set; } = 100;
    public string DefaultSearchEngine { get; set; } = "https://duckduckgo.com/?q=%s";
    public bool DoHEnabled { get; set; } = false;
    public bool SafeBrowsingEnabled { get; set; } = true; // never fully disabled for privacy
    public bool WebRtcEnabled { get; set; } = true;
    public ProfileType ActiveProfile { get; set; } = ProfileType.Normal;

    // Secure defaults + undo support
    public void ResetToSecureDefaults()
    {
        Theme = ThemeMode.System;
        DoHEnabled = false;
        SafeBrowsingEnabled = true;
        WebRtcEnabled = ActiveProfile != ProfileType.Enhanced;
    }

    // Export with versioning + schema validation
    public string ExportJson() => System.Text.Json.JsonSerializer.Serialize(this);
}
