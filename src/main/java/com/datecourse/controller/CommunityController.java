package com.datecourse.controller;

import com.datecourse.model.Course;
import com.datecourse.model.CoursePlace;
import com.datecourse.demo.FakeCourseData;
import com.datecourse.demo.FakeReviewData;
import com.datecourse.model.Review;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;

public class CommunityController implements Initializable {

    @FXML private ListView<Review> reviewList;
    @FXML private ComboBox<String> sortBox;
    @FXML private ComboBox<Course> courseBox;
    @FXML private ComboBox<Integer> ratingBox;
    @FXML private TextArea contentArea;
    @FXML private Button submitButton;
    @FXML private Label countLabel;
    @FXML private Label messageLabel;
    @FXML private Label detailTitle;
    @FXML private Label detailSummary;
    @FXML private ListView<String> detailPlaces;

    private static final String SORT_NEWEST = "최신순";
    private static final String SORT_HIGH = "별점 높은순";
    private static final String SORT_LOW = "별점 낮은순";

    // TODO: 로그인한 사용자 이름으로 교체 (1번 화면 담당과 방식 합의)
    private static final String CURRENT_USER = "나";

    private final List<Review> reviews = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        sortBox.getItems().addAll(SORT_NEWEST, SORT_HIGH, SORT_LOW);
        sortBox.getSelectionModel().selectFirst();
        sortBox.setOnAction(e -> refreshList(null));

        ratingBox.getItems().addAll(5, 4, 3, 2, 1);

        setupReviewCell();
        setupCourseBox();

        // 리뷰를 클릭하면 오른쪽에 코스 상세 표시
        reviewList.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldV, newV) -> showDetail(newV));

        // 리뷰가 하나도 없을 때 목록 자리에 보여줄 안내
        Label placeholder = new Label("아직 작성된 리뷰가 없어요.\n첫 번째 리뷰를 남겨보세요!");
        placeholder.setStyle("-fx-text-fill: #888; -fx-text-alignment: center;");
        reviewList.setPlaceholder(placeholder);
        detailPlaces.setPlaceholder(new Label("가게 정보가 없어요."));

        loadReviews();
        loadMyCourses();
    }

    // 리뷰 한 칸을 어떻게 그릴지 지정
    private void setupReviewCell() {
        reviewList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Review r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) {
                    setGraphic(null);
                    return;
                }
                Label head = new Label(r.author() + "   " + stars(r.rating()));
                head.setStyle("-fx-font-weight: bold;");
                Label course = new Label("📍 " + r.course().title() + "  ·  "
                        + r.course().coursePlaces().size() + "곳  ·  " + r.createdAt());
                course.setStyle("-fx-text-fill: #777; -fx-font-size: 11px;");
                Label content = new Label(r.content());
                content.setWrapText(true);

                VBox box = new VBox(4, head, course, content);
                box.setPadding(new Insets(6));
                box.prefWidthProperty().bind(lv.widthProperty().subtract(40));
                setGraphic(box);
            }
        });
    }

    // 코스 선택 상자에 코스 "이름"이 보이게 지정
    private void setupCourseBox() {
        courseBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Course c, boolean empty) {
                super.updateItem(c, empty);
                setText(empty || c == null ? null : courseLabel(c));
            }
        });
        courseBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Course c, boolean empty) {
                super.updateItem(c, empty);
                setText(empty || c == null ? "내가 다녀온 코스 선택" : courseLabel(c));
            }
        });
    }

    private static String courseLabel(Course c) {
        return c.title() + " (" + c.coursePlaces().size() + "곳, " + c.totalPrice() + "원)";
    }

    // 나중에 백엔드 호출로 바뀌는 자리: 리뷰 목록 조회
    private void loadReviews() {
        reviews.clear();
        reviews.addAll(FakeReviewData.getReviews());
        refreshList(null);
    }

    // 나중에 백엔드 호출로 바뀌는 자리: 내가 저장/다녀온 코스 조회
    private void loadMyCourses() {
        List<Course> myCourses = FakeCourseData.getSavedCourses();
        courseBox.getItems().setAll(myCourses);

        // 코스가 하나도 없으면 작성 불가
        boolean noCourse = myCourses.isEmpty();
        courseBox.setDisable(noCourse);
        ratingBox.setDisable(noCourse);
        contentArea.setDisable(noCourse);
        submitButton.setDisable(noCourse);
        if (noCourse) {
            messageLabel.setText("리뷰를 쓰려면 먼저 코스를 추천받고 저장해주세요.");
        }
    }

    // toSelect가 있으면 정렬 후 그 리뷰를 다시 선택
    private void refreshList(Review toSelect) {
        List<Review> sorted = new ArrayList<>(reviews);
        String sort = sortBox.getValue();
        if (SORT_HIGH.equals(sort)) {
            sorted.sort(Comparator.comparingInt(Review::rating).reversed());
        } else if (SORT_LOW.equals(sort)) {
            sorted.sort(Comparator.comparingInt(Review::rating));
        } else {
            sorted.sort(Comparator.comparing(Review::createdAt).reversed());
        }
        reviewList.getItems().setAll(sorted);
        countLabel.setText("(" + reviews.size() + "개)");

        if (toSelect != null) {
            reviewList.getSelectionModel().select(toSelect);
        } else {
            showDetail(null);
        }
    }

    // 오른쪽 코스 상세 패널
    private void showDetail(Review review) {
        detailPlaces.getItems().clear();

        if (review == null) {
            detailTitle.setText(reviews.isEmpty()
                    ? "리뷰가 등록되면 여기서 코스를 볼 수 있어요."
                    : "리뷰를 클릭하면 코스를 볼 수 있어요.");
            detailSummary.setText("");
            return;
        }

        Course course = review.course();
        detailTitle.setText(course.title());
        detailSummary.setText("총 " + course.totalPrice() + "원 · "
                + course.coursePlaces().size() + "곳");

        int order = 1;
        for (CoursePlace p : course.coursePlaces()) {
            detailPlaces.getItems().add(order++ + ". " + p.name()
                    + " (" + p.category() + ") - " + p.price() + "원\n    " + p.address());
        }
    }

    private static String stars(int rating) {
        return "★".repeat(rating) + "☆".repeat(5 - rating);
    }

    @FXML
    private void onSubmitClick() {
        Course course = courseBox.getValue();
        Integer rating = ratingBox.getValue();
        String content = contentArea.getText().trim();

        if (course == null) {
            messageLabel.setText("리뷰를 남길 코스를 선택해주세요.");
            return;
        }
        if (rating == null) {
            messageLabel.setText("별점을 선택해주세요.");
            return;
        }
        if (content.length() < 5) {
            messageLabel.setText("내용을 5자 이상 입력해주세요.");
            return;
        }

        // TODO: 백엔드에 리뷰 등록 요청 (course.courseId(), rating, content),
        //       응답으로 받은 reviewId 사용
        Review review = new Review(UUID.randomUUID().toString(),
                CURRENT_USER, course, rating, content, LocalDate.now());
        reviews.add(review);
        refreshList(review);   // 방금 쓴 리뷰를 선택해서 상세도 같이 보여줌

        courseBox.setValue(null);
        ratingBox.setValue(null);
        contentArea.clear();
        messageLabel.setText("");
    }
}