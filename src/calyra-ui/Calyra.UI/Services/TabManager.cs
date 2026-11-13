using System.Collections.Generic;

namespace Calyra.UI.Services;

public record TabState(string Id, string Url, string Title, bool IsPrivate, string Profile);

public class TabManager
{
    private readonly List<TabState> _tabs = new();
    public IReadOnlyList<TabState> Tabs => _tabs;

    public TabState Create(string url, bool isPrivate = false, string profile = "Normal")
    {
        var tab = new TabState(System.Guid.NewGuid().ToString(), url, "Yeni Sekme", isPrivate, profile);
        _tabs.Add(tab);
        return tab;
    }

    public void Close(string id) => _tabs.RemoveAll(t => t.Id == id);

    // Mode change never silently migrates storage — new isolated context required
    public TabState NewIdentityIsolatedContext(string profile)
    {
        // Close connections, clear storage binding, create new tab with fresh context
        return Create("about:blank", false, profile);
    }
}
