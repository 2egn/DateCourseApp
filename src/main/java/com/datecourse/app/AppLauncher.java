package com.datecourse.app;

import kr.ac.university.localdb.ui.MembershipApp;

/** Application entry point. Screen setup and database initialization remain in MembershipApp. */
public final class AppLauncher {
    private AppLauncher() {}

    public static void main(String[] args) {
        MembershipApp.main(args);
    }
}