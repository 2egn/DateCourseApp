package com.datecourse;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.UUID;

public class CourseEditController implements Initializable {

    // 왼쪽: 추천 코스
    @FXML private ListView<Course> candidateList;
    @FXML private ListView<CoursePlace> candidatePlaceList;
    @FXML private Button importButton;
    @FXML private Button addPlaceButton;

    // 오른쪽: 내 코스
    @FXML private TextField titleField;
    @FXML private Label summaryLabel;
    @FXML private Label budgetLabel;
    @FXML private ListView<CoursePlace> placeList;
    @FXML private Button upButton;
    @FXML private Button downButton;
    @FXML private Button removeButton;
    @FXML private Button clearButton;

    // 아래
    @FXML private Button saveButton;
    @FXML private Button shareButton;
    @FXML private Label messageLabel;

    private static final int MAX_TITLE_LENGTH = 20;

    private final ObservableList<CoursePlace> myPlaces = FXCollections.observableArrayList();
    private int budget;          // 2번에서 입력한 예산 (0이면 예산 정보 없음)
    private Course lastSaved;    // 마지막 저장 결과 (나중에 백엔드 응답으로 교체)

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 추천 코스 목록 한 칸 표시
        candidateList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Course c, boolean empty) {
                super.updateItem(c, empty);
                if (empty || c == null) {
                    setText(null);
                } else {
                    setText("추천 코스 " + (getIndex() + 1) + "\n"
                            + c.coursePlaces().size() + "곳 · " + won(c.totalPrice()));
                }
            }
        });
        candidateList.getSelectionModel().selectedItemProperty()
                .addListener((obs, o, n) -> showCandidatePlaces(n));

        candidatePlaceList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(CoursePlace p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? null : placeText(p));
            }
        });
        candidatePlaceList.getSelectionModel().selectedItemProperty()
                .addListener((obs, o, n) -> updateButtons());
        candidatePlaceList.setPlaceholder(new Label("추천 코스를 선택해주세요."));
        candidatePlaceList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                onAddPlaceClick();   // "내 코스에 추가" 버튼과 같은 동작
            }
        });

        // 내 코스 목록 한 칸 표시 (번호 포함)
        placeList.setItems(myPlaces);
        placeList.setPlaceholder(new Label("아직 담은 가게가 없어요.\n왼쪽에서 가게를 담거나 코스를 통째로 가져오세요."));
        placeList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(CoursePlace p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? null : (getIndex() + 1) + ". " + placeText(p));
            }
        });
        placeList.getSelectionModel().selectedIndexProperty()
                .addListener((obs, o, n) -> updateButtons());

        setCourses(null, 0);   // 코스를 넘겨받기 전의 빈 상태
    }

    // ★ 3번 화면이 추천 코스 3개와 예산을 넘겨줄 때 부르는 입구
    public void setCourses(List<Course> courses, int budget) {
        this.budget = budget;
        myPlaces.clear();
        titleField.clear();
        messageLabel.setText("");

        if (courses == null || courses.isEmpty()) {
            candidateList.getItems().clear();
            messageLabel.setText("추천된 코스가 없어요.");
        } else {
            candidateList.getItems().setAll(courses);
            candidateList.getSelectionModel().selectFirst();
        }
        refreshAll();
    }

    private void showCandidatePlaces(Course course) {
        if (course == null) {
            candidatePlaceList.getItems().clear();
        } else {
            candidatePlaceList.getItems().setAll(course.coursePlaces());
        }
        updateButtons();
    }

    private static String placeText(CoursePlace p) {
        return p.name() + " (" + p.category() + ") - " + won(p.price()) + "\n    " + p.address();
    }

    private static String won(int amount) {
        return String.format("%,d원", amount);
    }

    private int total() {
        return myPlaces.stream().mapToInt(CoursePlace::price).sum();
    }

    // 금액·예산 표시와 버튼 상태를 한 번에 갱신
    private void refreshAll() {
        placeList.refresh();
        int total = total();
        summaryLabel.setText("총 " + won(total) + " · " + myPlaces.size() + "곳");

        if (budget <= 0) {
            budgetLabel.setText("");
        } else if (total > budget) {
            budgetLabel.setText("예산 " + won(budget) + " · " + won(total - budget) + " 초과했어요");
            budgetLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        } else {
            budgetLabel.setText("예산 " + won(budget) + " · " + won(budget - total) + " 남음");
            budgetLabel.setStyle("-fx-text-fill: #2a7a2a;");
        }
        updateButtons();
    }

    private void updateButtons() {
        importButton.setDisable(candidateList.getSelectionModel().getSelectedItem() == null);
        addPlaceButton.setDisable(candidatePlaceList.getSelectionModel().getSelectedItem() == null);

        int idx = placeList.getSelectionModel().getSelectedIndex();
        boolean hasSelection = idx >= 0;
        upButton.setDisable(!hasSelection || idx == 0);
        downButton.setDisable(!hasSelection || idx == myPlaces.size() - 1);
        removeButton.setDisable(!hasSelection);

        boolean empty = myPlaces.isEmpty();
        clearButton.setDisable(empty);
        saveButton.setDisable(empty);
        shareButton.setDisable(empty);
    }

    // ---------- 왼쪽: 추천 코스에서 가져오기 ----------

    @FXML
    private void onImportClick() {
        Course selected = candidateList.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        myPlaces.setAll(selected.coursePlaces());
        placeList.getSelectionModel().clearSelection();
        messageLabel.setText("추천 코스 " + (candidateList.getSelectionModel().getSelectedIndex() + 1)
                + "번을 가져왔어요. 이제 순서를 바꾸거나 다른 코스의 가게를 담아보세요.");
        refreshAll();
    }

    @FXML
    private void onAddPlaceClick() {
        CoursePlace place = candidatePlaceList.getSelectionModel().getSelectedItem();
        if (place == null) return;

        boolean already = myPlaces.stream()
                .anyMatch(p -> Objects.equals(p.placeId(), place.placeId()));
        if (already) {
            messageLabel.setText("'" + place.name() + "'은(는) 이미 내 코스에 있어요.");
            return;
        }
        myPlaces.add(place);
        placeList.getSelectionModel().select(myPlaces.size() - 1);
        messageLabel.setText("'" + place.name() + "'을(를) 담았어요.");
        refreshAll();
    }

    // ---------- 오른쪽: 내 코스 편집 ----------

    @FXML
    private void onMoveUpClick() {
        int idx = placeList.getSelectionModel().getSelectedIndex();
        if (idx <= 0) return;
        Collections.swap(myPlaces, idx, idx - 1);
        placeList.getSelectionModel().select(idx - 1);
        refreshAll();
    }

    @FXML
    private void onMoveDownClick() {
        int idx = placeList.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= myPlaces.size() - 1) return;
        Collections.swap(myPlaces, idx, idx + 1);
        placeList.getSelectionModel().select(idx + 1);
        refreshAll();
    }

    @FXML
    private void onRemoveClick() {
        int idx = placeList.getSelectionModel().getSelectedIndex();
        if (idx < 0) return;
        String name = myPlaces.get(idx).name();
        myPlaces.remove(idx);
        messageLabel.setText("'" + name + "'을(를) 코스에서 뺐어요.");
        refreshAll();
    }

    @FXML
    private void onClearClick() {
        myPlaces.clear();
        messageLabel.setText("내 코스를 비웠어요.");
        refreshAll();
    }

    // ---------- 저장 / 공유 ----------

    private String resolveTitle() {
        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            title = "내 코스 " + LocalDate.now().format(DateTimeFormatter.ofPattern("MM/dd"));
        }
        return title;
    }

    private Course buildCourse(String title) {
        // 내가 조합한 코스는 항상 새 코스이므로 새 ID (실제 ID는 백엔드가 줄 수도 있음)
        return new Course(UUID.randomUUID().toString(), title, new ArrayList<>(myPlaces), total());
    }

    @FXML
    private void onSaveClick() {
        if (myPlaces.isEmpty()) {
            messageLabel.setText("가게가 하나 이상 있어야 저장할 수 있어요.");
            return;
        }
        String title = resolveTitle();
        if (title.length() > MAX_TITLE_LENGTH) {
            messageLabel.setText("코스 이름은 " + MAX_TITLE_LENGTH + "자 이하로 입력해주세요.");
            return;
        }

        // TODO: 백엔드에 저장 요청 (title, 가게 순서의 placeId 목록)
        lastSaved = buildCourse(title);
        titleField.setText(title);

        String msg = "'" + title + "' 코스를 저장했어요. (지금은 화면에서만 저장돼요)";
        if (budget > 0 && lastSaved.totalPrice() > budget) {
            msg += "\n예산을 " + won(lastSaved.totalPrice() - budget) + " 초과한 코스예요.";
        }
        messageLabel.setText(msg);
        System.out.println("저장: " + lastSaved);
    }

    @FXML
    private void onShareClick() {
        if (myPlaces.isEmpty()) return;

        Course course = buildCourse(resolveTitle());
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(course.title()).append("]\n");
        sb.append("총 ").append(won(course.totalPrice())).append(" · ")
                .append(course.coursePlaces().size()).append("곳\n\n");

        int order = 1;
        for (CoursePlace p : course.coursePlaces()) {
            sb.append(order++).append(". ").append(p.name())
                    .append(" (").append(p.category()).append(") - ").append(won(p.price())).append("\n")
                    .append("   ").append(p.address()).append("\n");
        }

        ClipboardContent content = new ClipboardContent();
        content.putString(sb.toString());
        Clipboard.getSystemClipboard().setContent(content);

        // TODO: 링크 공유/커뮤니티 게시는 백엔드가 정해지면 추가
        messageLabel.setText("코스 내용을 복사했어요. 메신저에 붙여넣기(Ctrl+V)해서 공유해보세요!");
    }
}