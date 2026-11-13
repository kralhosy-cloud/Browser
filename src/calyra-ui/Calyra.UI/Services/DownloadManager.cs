using System.IO;

namespace Calyra.UI.Services;

public class DownloadManager
{
    public string ProgressText { get; private set; } = "Beklemede";

    public string NormalizeFileName(string suggested)
    {
        // Prevent path traversal
        var name = Path.GetFileName(suggested);
        name = string.Join("_", name.Split(Path.GetInvalidFileNameChars()));
        if (string.IsNullOrWhiteSpace(name)) name = "download";
        return name;
    }

    public void OnStarted(string url, string profileRoute) {
        // Preserve route/profile
        ProgressText = $"{profileRoute}: indiriliyor — {url}";
    }
    // Never auto-execute; Tor profile warns about opening externally (IP leak via file content)
}
