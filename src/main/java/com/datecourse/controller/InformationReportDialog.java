package com.datecourse.controller;

import com.datecourse.model.*;
import kr.ac.university.localdb.db.InformationReportRepository;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;
import java.util.Optional;

final class InformationReportDialog {
    record Submission(InformationReport.Field field,String value,String reason,String source) {}
    static Optional<Submission> show(Window owner,Place place) {
        return create(owner,place).showAndWait();
    }
    static Dialog<Submission> create(Window owner,Place place) {
        Dialog<Submission> dialog=new Dialog<>();dialog.setTitle("정보 정정 제보 · "+place.name());
        if(owner!=null)dialog.initOwner(owner);
        com.datecourse.ui.DialogTheme.apply(dialog);
        ComboBox<InformationReport.Field> field=new ComboBox<>();field.getItems().setAll(InformationReport.Field.values());field.getSelectionModel().selectFirst();
        Label current=new Label();current.setWrapText(true);
        TextField value=new TextField();value.setPromptText("가격은 원 단위 · 1인 기본 이용 기준");
        TextArea reason=new TextArea();reason.setPromptText("잘못된 내용과 확인한 근거를 적어 주세요.");reason.setPrefRowCount(3);reason.setWrapText(true);
        TextField source=new TextField();source.setPromptText("https:// 근거 링크 (선택)");
        Label error=new Label();error.getStyleClass().add("error");error.setWrapText(true);
        field.setId("reportField");value.setId("reportValue");reason.setId("reportReason");
        source.setId("reportSource");error.setId("reportError");
        field.valueProperty().addListener((o,a,b)->{current.setText("현재 · "+InformationReportRepository.original(place,b));value.clear();
            value.setPromptText(b==InformationReport.Field.PRICE?"원 단위 · 1인 기본 이용 기준":"수정할 내용을 입력해 주세요.");});
        current.setText("현재 · "+place.priceText());
        VBox content=new VBox(8,new Label("수정 대상"),field,current,new Label("제안 값"),value,
            new Label("정정 이유"),reason,new Label("근거 링크"),source,
            new Label("관리자 검토 후 반영돼요. 주소 수정은 지도 좌표를 자동 변경하지 않아요."),error);
        content.setPrefWidth(470);dialog.getDialogPane().setContent(content);
        ButtonType send=new ButtonType("제보 보내기",ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(send,ButtonType.CANCEL);
        var sendButton=dialog.getDialogPane().lookupButton(send);
        sendButton.addEventFilter(javafx.event.ActionEvent.ACTION,e->{
            if(value.getText().isBlank()||value.getText().length()>1000||reason.getText().isBlank()||reason.getText().length()>1000||source.getText().length()>1000){error.setText("제안 값과 이유는 필수이며 각 항목은 최대 1,000자예요.");e.consume();return;}
            if(field.getValue()==InformationReport.Field.PRICE)try{int n=Integer.parseInt(value.getText().trim());if(n<0||n>10000000)throw new NumberFormatException();}
            catch(NumberFormatException ex){error.setText("가격은 0~10,000,000원의 정수로 입력해 주세요.");e.consume();}
            if(field.getValue()==InformationReport.Field.NAME&&value.getText().trim().length()>200){error.setText("장소명은 최대 200자예요.");e.consume();}
            if(!source.getText().isBlank())try {
                var uri=java.net.URI.create(source.getText().trim());
                if(!"https".equalsIgnoreCase(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null)throw new IllegalArgumentException();
            } catch(IllegalArgumentException ex){error.setText("근거 링크는 https 주소로 입력해 주세요.");e.consume();}
        });
        dialog.setResultConverter(b->b==send?new Submission(field.getValue(),value.getText(),reason.getText(),source.getText()):null);
        return dialog;
    }
}
