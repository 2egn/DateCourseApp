package com.datecourse.service;

import org.junit.jupiter.api.Test;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ExternalBrowserTest {
    @Test void privateWindowsUseTheBrowsersPrivateMode() {
        for (var entry : java.util.Map.of("chrome.exe", "--incognito", "brave.exe", "--incognito",
                "msedge.exe", "--inprivate", "firefox.exe", "--private-window", "opera.exe", "--private").entrySet()) {
            var target = new ExternalBrowser.BrowserTarget(Path.of(entry.getKey()), List.of("--profile-directory=Default"), true);
            assertEquals(List.of(entry.getKey(), "--profile-directory=Default", entry.getValue(), "https://map.kakao.com/"),
                    ExternalBrowser.command(target, URI.create("https://map.kakao.com/")));
        }
    }

    @Test void detectsPrivateTitlesAndLaunchFlagsWithoutCopyingOtherArguments() {
        assertTrue(ExternalBrowser.isPrivateWindow("지도 - 시크릿", new String[0]));
        assertTrue(ExternalBrowser.isPrivateWindow("Map - InPrivate", new String[0]));
        assertTrue(ExternalBrowser.isPrivateWindow("Map", new String[]{"--incognito"}));
        assertFalse(ExternalBrowser.isPrivateWindow("Map - Google Chrome", new String[0]));
    }

    @Test void chromiumReusesExistingProfileAndKeepsUrlOutOfShellCode() {
        var target = new ExternalBrowser.BrowserTarget(Path.of("C:/Program Files/Google/Chrome/Application/chrome.exe"),
                List.of("--profile-directory=Profile 1"));
        URI uri = URI.create("https://map.kakao.com/link/search/example?a=1&b=2");
        var command = ExternalBrowser.command(target, uri);
        assertEquals(List.of(target.executable().toString(), "--profile-directory=Profile 1", uri.toString()), command);
        assertFalse(command.contains("--new-window"));
    }

    @Test void firefoxExplicitlyRequestsNewTab() {
        var target = new ExternalBrowser.BrowserTarget(Path.of("C:/Firefox/firefox.exe"), List.of());
        assertEquals(List.of(target.executable().toString(), "--new-tab", "https://place.map.kakao.com/123"),
                ExternalBrowser.command(target, URI.create("https://place.map.kakao.com/123")));
    }

    @Test void onlyBrowserProfileSwitchesAreCarriedOver() {
        assertEquals(List.of("--user-data-dir", "C:/Browser Profiles/Work", "--profile-directory=Profile 2"),
                ExternalBrowser.profileArguments(new String[]{"--new-window", "--user-data-dir", "C:/Browser Profiles/Work",
                        "--profile-directory=Profile 2", "https://example.com/private", "--incognito"}));
    }

    @Test void rejectsNonWebAddressesBeforeStartingAProcess() {
        var target = new ExternalBrowser.BrowserTarget(Path.of("chrome.exe"), List.of());
        for (String value : List.of("file:///C:/test", "javascript:alert(1)", "https://user@example.com/"))
            assertThrows(IllegalArgumentException.class, () -> ExternalBrowser.command(target, URI.create(value)));
    }
}
