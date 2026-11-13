using Avalonia.Controls;
using Calyra.UI.Services;

namespace Calyra.UI;

public partial class SettingsWindow : Window
{
    public SettingsWindow(SettingsService s) { InitializeComponent(); }
    private void InitializeComponent() => Avalonia.Markup.Xaml.AvaloniaXamlLoader.Load(this);
}
