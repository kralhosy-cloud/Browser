using System;

namespace Calyra.UI.Services;

public static class AddressBarLogic
{
    public static string NormalizeInput(string input, string searchTemplate)
    {
        input = input.Trim();
        if (string.IsNullOrEmpty(input)) return "about:blank";
        // IDN/punycode spoof protection: rely on CEF's secure display (chrome://net-internals)
        // Here we just detect URL vs search
        if (input.Contains(" ") || (!input.Contains(".") && !input.Contains("://")))
        {
            // search
            return searchTemplate.Replace("%s", Uri.EscapeDataString(input));
        }
        if (!input.Contains("://")) input = "https://" + input;
        if (Uri.TryCreate(input, UriKind.Absolute, out var uri))
        {
            if (uri.Scheme == "javascript") return "about:blank"; // require user gesture
            if (uri.Scheme == "file") return "about:blank"; // local file not opened from web
            return uri.ToString();
        }
        return searchTemplate.Replace("%s", Uri.EscapeDataString(input));
    }
}
