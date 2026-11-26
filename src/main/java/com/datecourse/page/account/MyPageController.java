package com.datecourse.page.account;

import com.datecourse.page.common.BasePageController;
import com.datecourse.page.common.PageFeedback;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import com.datecourse.db.MemberRepository;
import com.datecourse.service.UserContext;

/**
 * - 로그인한 회원의 프로필과 저장 코스 이동을 담당하는 화면
 * - 공통 부모의 로딩과 알림을 사용, 회원 저장소는 로그인 앱에서 관리
 */
public final class MyPageController extends BasePageController {
    @FXML private Label avatarLabel, nameLabel, emailLabel, phoneLabel, statusLabel;
    private UserContext userContext;
    private long viewVersion;
    private Runnable savedCourses = () -> {}, back = () -> {};

    public void configure(UserContext context, Runnable savedCourses, Runnable back) {
        this.userContext = context;
        this.savedCourses = savedCourses;
        this.back = back;
        loadProfile();
    }

    @FXML private void onRefresh() {
        if (isLoading() || userContext == null) return;
        userContext.invalidateProfile();
        loadProfile();
    }

    public void clear() {
        viewVersion++;
        userContext = null;
        if (avatarLabel != null) avatarLabel.setText("?");
        nameLabel.setText("—"); emailLabel.setText("—"); phoneLabel.setText("—");
        statusLabel.setText("");
    }

    private void loadProfile() {
        UserContext context = userContext;
        long version = ++viewVersion;
        if (avatarLabel != null) avatarLabel.setText("-");
        nameLabel.setText("—"); emailLabel.setText("—"); phoneLabel.setText("—");
        statusLabel.setText("회원 정보를 불러오는 중…");
        showFuture("회원 정보를 불러오고 있어요", context.profile(), profile -> {
            if (context != userContext || context.isClosed() || version != viewVersion) return;
            String name = profile.name();
            nameLabel.setText(name);
            if (avatarLabel != null) {
                avatarLabel.setText(name != null && !name.isBlank() ? name.trim() + " 님" : "회원 님");
            }
            emailLabel.setText(profile.email());
            phoneLabel.setText(profile.phone() == null || profile.phone().isBlank() ? "미등록" : profile.phone());
            statusLabel.setText("");
        }, failure -> {
            if (context != userContext || context.isClosed() || version != viewVersion) return;
            statusLabel.setText("회원 정보를 불러오지 못했어요. 다시 시도해 주세요.");
            notifyUser(PageFeedback.Kind.ERROR, statusLabel.getText());
        });
    }

    @FXML private void onSavedCourses() { if (!isLoading()) savedCourses.run(); }
    @FXML private void onBack() { if (!isLoading()) back.run(); }
}