package com.datecourse;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.beans.property.SimpleStringProperty;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class AdminController implements Initializable {

    // 통계
    @FXML private Label memberCountLabel;
    @FXML private Label reviewCountLabel;
    @FXML private Label avgRatingLabel;

    // 리뷰 관리
    @FXML private TextField reviewSearchField;
    @FXML private TableView<Review> reviewTable;
    @FXML private TableColumn<Review, String> reviewAuthorCol;
    @FXML private TableColumn<Review, String> reviewCourseCol;
    @FXML private TableColumn<Review, String> reviewRatingCol;
    @FXML private TableColumn<Review, String> reviewContentCol;
    @FXML private TableColumn<Review, String> reviewDateCol;
    @FXML private Label reviewMessageLabel;

    // 회원 관리
    @FXML private TableView<Member> memberTable;
    @FXML private TableColumn<Member, String> memberNameCol;
    @FXML private TableColumn<Member, String> memberRoleCol;
    @FXML private TableColumn<Member, String> memberJoinedCol;
    @FXML private TableColumn<Member, String> memberStatusCol;
    @FXML private Label memberMessageLabel;

    private final ObservableList<Review> reviews = FXCollections.observableArrayList();
    private final ObservableList<Member> members = FXCollections.observableArrayList();
    private FilteredList<Review> filteredReviews;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // TODO: 관리자 권한 확인 (로그인 담당과 방식 합의). 관리자가 아니면 이 화면을 열지 않기

        setupReviewTable();
        setupMemberTable();
        loadData();
    }

    private void setupReviewTable() {
        reviewAuthorCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().author()));
        reviewCourseCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().course().title()));
        reviewRatingCol.setCellValueFactory(c -> new SimpleStringProperty("★" + c.getValue().rating()));
        reviewContentCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().content()));
        reviewDateCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().createdAt().toString()));

        reviewTable.setPlaceholder(new Label("리뷰가 없어요."));

        // 검색어 입력 시 자동 필터링
        filteredReviews = new FilteredList<>(reviews, r -> true);
        reviewSearchField.textProperty().addListener((obs, oldV, newV) -> {
            String keyword = newV.trim();
            filteredReviews.setPredicate(r -> keyword.isEmpty()
                    || r.author().contains(keyword)
                    || r.content().contains(keyword));
        });
        reviewTable.setItems(filteredReviews);
    }

    private void setupMemberTable() {
        memberNameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().name()));
        memberRoleCol.setCellValueFactory(c -> new SimpleStringProperty(
                "ADMIN".equals(c.getValue().role()) ? "관리자" : "일반"));
        memberJoinedCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().joinedAt().toString()));
        memberStatusCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().suspended() ? "정지" : "정상"));

        memberTable.setPlaceholder(new Label("회원이 없어요."));
        memberTable.setItems(members);
    }

    // 나중에 백엔드 호출로 바뀌는 자리
    private void loadData() {
        reviews.setAll(FakeReviewData.getReviews());
        members.setAll(FakeMemberData.getMembers());
        updateStats();
    }

    private void updateStats() {
        memberCountLabel.setText(String.valueOf(members.size()));
        reviewCountLabel.setText(String.valueOf(reviews.size()));

        double avg = reviews.stream().mapToInt(Review::rating).average().orElse(0);
        avgRatingLabel.setText(reviews.isEmpty() ? "-" : String.format("%.1f", avg));
    }

    @FXML
    private void onReviewSearchReset() {
        reviewSearchField.clear();
    }

    @FXML
    private void onDeleteReviewClick() {
        Review selected = reviewTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            reviewMessageLabel.setText("삭제할 리뷰를 선택해주세요.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                selected.author() + "님의 리뷰를 삭제할까요?", ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText("리뷰 삭제");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        // TODO: 백엔드에 삭제 요청 (selected.reviewId())
        reviews.remove(selected);
        updateStats();
        reviewMessageLabel.setText("리뷰를 삭제했어요.");
    }

    @FXML
    private void onToggleSuspendClick() {
        Member selected = memberTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            memberMessageLabel.setText("회원을 선택해주세요.");
            return;
        }
        if ("ADMIN".equals(selected.role())) {
            memberMessageLabel.setText("관리자는 정지할 수 없어요.");
            return;
        }

        // TODO: 백엔드에 정지/해제 요청 (selected.memberId())
        Member updated = new Member(selected.memberId(), selected.name(), selected.role(),
                selected.joinedAt(), !selected.suspended());
        int index = members.indexOf(selected);
        members.set(index, updated);
        memberTable.getSelectionModel().select(index);
        memberMessageLabel.setText(updated.name() + "님을 "
                + (updated.suspended() ? "정지" : "정상 상태로 변경") + "했어요.");
    }
}