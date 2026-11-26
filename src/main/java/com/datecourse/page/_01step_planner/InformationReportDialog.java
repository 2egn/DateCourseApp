package com.datecourse.page._01step_planner;

import com.datecourse.model.*;
import com.datecourse.db.InformationReportRepository;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;
import java.util.Optional;

/** 장소 정보 정정 제안, 사유 및 출처 입력용 창 */
final class InformationReportDialog {
    record Submission(InformationReport.Field field,String value,String reason,String source) {}
    static Optional<Submission> show(Window owner,Place place) {
        return create(owner,place).showAndWait();
    }
    static Dialog<Submission> create(Window owner,Place place) {
        Dialog<Submission> dialog=new Dialog<>();
        dialog.setTitle("정보 정정 제보 · "+place.name());
        if(owner!=null)dialog.initOwner(owner);
        com.datecourse.page.common.controls.DialogTheme.apply(dialog);

        // 1. 중앙 정렬 헤더 영역 (큰 제목 + 장소명 부제목)
        Label headerTitle = new Label("정보 정정 제보");
        headerTitle.getStyleClass().add("dialog-main-title");
        Label headerSub = new Label(place.name());
        headerSub.getStyleClass().add("dialog-sub-title");
        VBox headerBox = new VBox(4, headerTitle, headerSub);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setStyle("-fx-padding: 0 0 12 0;");

        // 2. 입력 폼 요소들
        ComboBox<InformationReport.Field> field=new ComboBox<>();
        field.getItems().setAll(InformationReport.Field.values());
        field.getSelectionModel().selectFirst();
        field.setMaxWidth(Double.MAX_VALUE); // 드롭다운 가로폭을 칸에 꽉 차게 정렬

        Label current=new Label();
        current.setWrapText(true);
        current.getStyleClass().add("sub-text");

        TextField value=new TextField();
        value.setPromptText("가격은 원 단위 · 1인 기본 이용 기준");

        TextArea reason=new TextArea();
        reason.setPromptText("잘못된 내용과 확인한 근거를 적어 주세요.");
        reason.setPrefRowCount(3);
        reason.setWrapText(true);

        TextField source=new TextField();
        source.setPromptText("https:// 근거 링크 (선택)");

        Label guide=new Label("관리자 검토 후 반영돼요. 주소 수정은 지도 좌표를 자동 변경하지 않아요.");
        guide.setWrapText(true);
        guide.getStyleClass().add("sub-text");

        Label error=new Label();
        error.getStyleClass().add("error");
        error.setWrapText(true);

        field.setId("reportField");value.setId("reportValue");reason.setId("reportReason");
        source.setId("reportSource");error.setId("reportError");

        field.valueProperty().addListener((o,a,b)->{
            current.setText("현재 · "+InformationReportRepository.original(place,b));
            value.clear();
            value.setPromptText(b==InformationReport.Field.PRICE?"원 단위 · 1인 기본 이용 기준":"수정할 내용을 입력해 주세요.");
        });
        current.setText("현재 · "+place.priceText());

        VBox content=new VBox(10,
                headerBox,
                new Label("수정 대상"),field,current,
                new Label("정정 제목"),value,
                new Label("정정 이유"),reason,
                new Label("근거 링크"),source,
                guide,error
        );
        content.setPrefWidth(470);
        dialog.getDialogPane().setContent(content);

        // 3. 하단 버튼 배치 (왼쪽: 취소 / 오른쪽: 제보 보내기)
        ButtonType send=new ButtonType("제보 보내기",ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.CANCEL, send);
        var sendButton=dialog.getDialogPane().lookupButton(send);

        // 4. 입력값 검사 + 더블체크(재확인) 팝업
        sendButton.addEventFilter(javafx.event.ActionEvent.ACTION,e->{
            error.setText(""); // 이전 에러 메시지 초기화
            if(value.getText().isBlank()||value.getText().length()>1000||reason.getText().isBlank()||reason.getText().length()>1000||source.getText().length()>1000){
                error.setText("제안 값과 이유는 필수이며 각 항목은 최대 1,000자예요.");
                e.consume();return;
            }
            if(field.getValue()==InformationReport.Field.PRICE)try{
                int n=Integer.parseInt(value.getText().trim());
                if(n<0||n>10000000)throw new NumberFormatException();
            } catch(NumberFormatException ex){
                error.setText("가격은 0~10,000,000원의 정수로 입력해 주세요.");
                e.consume();return;
            }
            if(field.getValue()==InformationReport.Field.NAME&&value.getText().trim().length()>200){
                error.setText("장소명은 최대 200자예요.");
                e.consume();return;
            }
            if(!source.getText().isBlank())try {
                var uri=java.net.URI.create(source.getText().trim());
                if(!"https".equalsIgnoreCase(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null)throw new IllegalArgumentException();
            } catch(IllegalArgumentException ex){
                error.setText("근거 링크는 https 주소로 입력해 주세요.");
                e.consume();return;
            }

            // 모든 검사를 통과했을 때 더블체크 확인창 띄우기
            if(!confirmSend(dialog.getDialogPane().getScene().getWindow(), field.getValue().toString(), value.getText().trim())){
                e.consume(); // '아니요'를 누르면 전송을 취소하고 입력창 유지
            }
        });

        dialog.setResultConverter(b->b==send?new Submission(field.getValue(),value.getText(),reason.getText(),source.getText()):null);
        return dialog;
    }

    // 더블체크 확인 팝업창 (보내주신 예시의 오른쪽 카드 디자인 반영)
    private static boolean confirmSend(Window owner, String fieldName, String proposedValue) {
        Dialog<ButtonType> confirm = new Dialog<>();
        confirm.setTitle("제보 확인");
        if(owner != null) confirm.initOwner(owner);
        com.datecourse.page.common.controls.DialogTheme.apply(confirm);

        Label title = new Label("제보를 보내시겠습니까?");
        title.getStyleClass().add("confirm-title");

        Label desc = new Label("[" + fieldName + "] 항목을 '" + proposedValue + "'(으)로\n정정 요청을 보냅니다.");
        desc.setWrapText(true);
        desc.setAlignment(Pos.CENTER);
        desc.setStyle("-fx-text-alignment: center;");
        desc.getStyleClass().add("sub-text");

        VBox box = new VBox(10, title, desc);
        box.setAlignment(Pos.CENTER);
        box.setPrefWidth(340);
        box.setStyle("-fx-padding: 16 12 8 12;");
        confirm.getDialogPane().setContent(box);

        ButtonType noBtn = new ButtonType("아니요", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType yesBtn = new ButtonType("보내기", ButtonBar.ButtonData.OK_DONE);
        confirm.getDialogPane().getButtonTypes().setAll(noBtn, yesBtn);

        return confirm.showAndWait().orElse(noBtn) == yesBtn;
    }
}