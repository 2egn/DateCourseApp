package com.datecourse.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Reuses a visible browser before delegating to the OS default browser. */
public final class ExternalBrowser {
    private static final Set<String> EXECUTABLES = Set.of("chrome.exe", "msedge.exe", "firefox.exe", "brave.exe", "opera.exe");
    private static final String FIND_BROWSER = """
            $OutputEncoding = [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
            $browser = Get-Process -Name chrome,msedge,firefox,brave,opera -ErrorAction SilentlyContinue |
                Where-Object { $_.MainWindowHandle -ne 0 -and $_.Path } |
                Sort-Object StartTime -Descending | Select-Object -First 1
            if ($null -ne $browser) {
                [pscustomobject]@{path=$browser.Path;pid=$browser.Id;title=$browser.MainWindowTitle} | ConvertTo-Json -Compress
            }
            """;

    private ExternalBrowser() {}

    public static void open(URI address) throws IOException, InterruptedException {
        validate(address);
        if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows")) {
            Optional<BrowserTarget> existing = findExistingWindowsBrowser();
            if (existing.isPresent()) {
                try {
                    new ProcessBuilder(command(existing.get(), address))
                            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                            .redirectError(ProcessBuilder.Redirect.DISCARD).start();
                    return;
                } catch (IOException unavailable) { /* Browser may have closed; use the default handler. */ }
            }
        }
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
            throw new IOException("Browser is unavailable");
        Desktop.getDesktop().browse(address);
    }

    record BrowserTarget(Path executable, List<String> profileArguments, boolean privateMode) {
        BrowserTarget(Path executable, List<String> profileArguments) { this(executable, profileArguments, false); }
    }

    static Optional<BrowserTarget> findExistingWindowsBrowser() throws InterruptedException {
        try {
            Process query = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive",
                    "-WindowStyle", "Hidden", "-Command", FIND_BROWSER)
                    .redirectError(ProcessBuilder.Redirect.DISCARD).start();
            if (!query.waitFor(3, TimeUnit.SECONDS)) { query.destroyForcibly(); return Optional.empty(); }
            String output = new String(query.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
            if (query.exitValue() != 0 || output.isEmpty()) return Optional.empty();
            var data = new ObjectMapper().readTree(output);
            Path executable = Path.of(data.path("path").asText());
            if (!EXECUTABLES.contains(executable.getFileName().toString().toLowerCase(Locale.ROOT))
                    || !Files.isRegularFile(executable)) return Optional.empty();
            String[] arguments = ProcessHandle.of(data.path("pid").asLong())
                    .flatMap(p -> p.info().arguments()).orElse(new String[0]);
            return Optional.of(new BrowserTarget(executable, profileArguments(arguments),
                    isPrivateWindow(data.path("title").asText(), arguments)));
        } catch (IOException | RuntimeException unavailable) { return Optional.empty(); }
    }

    static List<String> profileArguments(String[] arguments) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < arguments.length; i++) {
            String arg = arguments[i];
            if (arg.startsWith("--profile-directory=") || arg.startsWith("--user-data-dir=")) result.add(arg);
            else if ((arg.equals("--profile-directory") || arg.equals("--user-data-dir")) && i + 1 < arguments.length) {
                result.add(arg); result.add(arguments[++i]);
            }
        }
        return List.copyOf(result);
    }

    static List<String> command(BrowserTarget browser, URI address) {
        validate(address);
        List<String> result = new ArrayList<>();
        result.add(browser.executable().toString());
        result.addAll(browser.profileArguments());
        String executable = browser.executable().getFileName().toString().toLowerCase(Locale.ROOT);
        if (browser.privateMode()) {
            result.add(executable.equals("firefox.exe") ? "--private-window"
                    : executable.equals("msedge.exe") ? "--inprivate"
                    : executable.equals("opera.exe") ? "--private" : "--incognito");
        } else if (executable.equals("firefox.exe")) result.add("--new-tab");
        // Chromium hands a bare URL to its existing instance as a new tab; do not pass --new-window.
        result.add(address.toASCIIString());
        return List.copyOf(result);
    }

    static boolean isPrivateWindow(String title, String[] arguments) {
        String lower = title.toLowerCase(Locale.ROOT);
        if (lower.contains("incognito") || lower.contains("inprivate") || lower.contains("private browsing")
                || lower.contains("시크릿") || lower.contains("사생활 보호") || lower.contains("비공개")) return true;
        return Arrays.stream(arguments).anyMatch(arg -> Set.of("--incognito", "--inprivate", "-inprivate",
                "--private", "--private-window", "-private-window").contains(arg.toLowerCase(Locale.ROOT)));
    }

    private static void validate(URI address) {
        if (address == null || address.getHost() == null || address.getUserInfo() != null
                || !("https".equalsIgnoreCase(address.getScheme()) || "http".equalsIgnoreCase(address.getScheme())))
            throw new IllegalArgumentException("A web address is required");
    }
}
