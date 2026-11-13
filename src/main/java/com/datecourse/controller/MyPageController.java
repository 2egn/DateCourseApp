package com.datecourse.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import kr.ac.university.localdb.db.MemberRepository;

/** Reuses the base loading and notices; account storage stays with the login application. */
public final class MyPageController extends BasePageController {
    @FXML private Label nameLabel, emailLabel, phoneLabel, statusLabel;
    private java.util.concurrent.Callable<MemberRepository.Profile> loadProfile;
    private Runnable savedCourses = () -> {}, back = () -> {};

    public void configure(java.util.concurrent.Callable<MemberRepository.Profile> loadProfile, Runnable savedCourses, Runnable back) {
        this.loadProfile = loadProfile;
        this.savedCourses = savedCourses;
        this.back = back;
        onRefresh();
    }

    @FXML private void onRefresh() {
        if (isLoading() || loadProfile == null) return;
        nameLabel.setText("—"); emailLabel.setText("—"); phoneLabel.setText("—");
        statusLabel.setText("회원 정보를 불러오는 중…");
        runWork("회원 정보를 불러오고 있어요", loadProfile, profile -> {
            nameLabel.setText(profile.name());
            emailLabel.setText(profile.email());
            phoneLabel.setText(profile.phone() == null || profile.phone().isBlank() ? "미등록" : profile.phone());
            statusLabel.setText("");
        }, failure -> {
            statusLabel.setText("회원 정보를 불러오지 못했어요. 다시 시도해 주세요.");
            notifyUser(PageFeedback.Kind.ERROR, statusLabel.getText());
        });
    }

    @FXML private void onSavedCourses() { if (!isLoading()) savedCourses.run(); }
    @FXML private void onBack() { if (!isLoading()) back.run(); }
}
