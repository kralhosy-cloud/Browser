using Avalonia.Controls;
using Avalonia.Interactivity;
using Avalonia.Input;
using Calyra.UI.Services;

namespace Calyra.UI;

public partial class MainWindow : Window
{
    private readonly TabManager _tabs = new();
    private readonly SettingsService _settings = new();
    private readonly PermissionService _perms = new();
    private readonly DownloadManager _downloads = new();

    public MainWindow()
    {
        InitializeComponent();
        // Accessibility: focus order, visible focus via Fluent theme
        AddressBox.Focus();
    }

    private void OnNavigate(object? s, RoutedEventArgs e) => NavigateFromBox();
    private void OnAddressKeyDown(object? s, KeyEventArgs e) { if (e.Key == Key.Enter) NavigateFromBox(); }

    private void NavigateFromBox()
    {
        var text = AddressBox.Text ?? "";
        // Address bar correctly separates URL vs search
        var url = AddressBarLogic.NormalizeInput(text, _settings.DefaultSearchEngine);
        // Engine call via NativeInterop (real CEF) — check sandbox active
        // NativeCore.calyra_engine_navigate(...);
        RouteStatus.Text = $"Gidiliyor: {url}";
        // Keep chrome/content boundary visible — browserHost will load via native_core
    }

    private void OnBack(object? s, RoutedEventArgs e) { /* CefBrowser::GoBack */ }
    private void OnForward(object? s, RoutedEventArgs e) {}
    private void OnReload(object? s, RoutedEventArgs e) {}
    private void OnMenuClick(object? s, RoutedEventArgs e) { new SettingsWindow(_settings).ShowDialog(this); }
    private void OnSettings(object? s, RoutedEventArgs e) { new SettingsWindow(_settings).ShowDialog(this); }
    private void OnExitFullscreen(object? s, RoutedEventArgs e) { FullscreenOriginBar.IsVisible = false; }
}
