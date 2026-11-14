package com.datecourse.controller;

import com.datecourse.model.InformationReport;
import kr.ac.university.localdb.db.MemberRepository;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/** The real administrator login supplies the otherwise unconstructable capability. */
public final class InformationReviewController extends BasePageController {
    @FXML private ListView<InformationReport> reports;
    @FXML private Label details,status;
    @FXML private TextField correctedValue;
    @FXML private TextArea reviewNote;
    @FXML private Button approveButton,rejectButton;
    private MemberRepository.AdminSession session;
    @FXML private void initialize() {
        reports.setPlaceholder(new Label("접수된 제보가 없어요."));
        reports.setCellFactory(v->new ListCell<>() {
            @Override protected void updateItem(InformationReport r,boolean empty) {
                super.updateItem(r,empty);setText(empty||r==null?null:"#"+r.id()+" · "+r.placeName()+" · "+state(r.status())+"\n"+InformationReport.Field.valueOf(r.field()).label());
            }
        });
        reports.getSelectionModel().selectedItemProperty().addListener((o,a,r)->{
            details.setText(r==null?"제보를 선택해 주세요.":r.placeName()+"\n장소 ID · "+r.placeId()+"\n현재 · "+r.originalValue()
                +"\n제안 · "+r.proposedValue()+"\n이유 · "+r.reason()+"\n근거 · "+r.sourceUrl()+"\n접수 · "+r.createdAt()+"\n승인 값 · "+r.reviewedValue()+"\n검토 메모 · "+r.reviewNote());
            correctedValue.setText(r==null?"":r.proposedValue());reviewNote.clear();buttons();
        });
        loadingProperty().addListener((o,a,b)->buttons());buttons();
    }
    public void configure(MemberRepository.AdminSession session) { this.session=java.util.Objects.requireNonNull(session);buttons();reload(); }
    @FXML private void reload() {
        if(session==null||isLoading())return;
        runWork("제보 조회 중",session::informationReports,rows->{reports.getItems().setAll(rows);status.setText(rows.size()+"건 · 최근 300건");},
            e->status.setText("제보 목록을 불러오지 못했어요. 다시 시도해 주세요."));
    }
    @FXML private void approve() { review(true); }
    @FXML private void reject() { review(false); }
    private void review(boolean approve) {
        InformationReport r=reports.getSelectionModel().getSelectedItem();if(r==null||session==null||isLoading())return;
        String value=correctedValue.getText(),note=reviewNote.getText();
        if(note.isBlank()){status.setText("검토 근거 또는 반려 이유를 입력해 주세요.");return;}
        runWork("제보 검토 저장 중",()->{session.reviewInformation(r.id(),approve,value,note);return true;},
            ok->{reload();status.setText(approve?"승인했어요. 이후 후보 업데이트부터 반영돼요. 저장한 코스는 유지돼요.":"반려했어요. 장소 정보는 변경되지 않았어요.");},
            e->status.setText(e instanceof IllegalArgumentException?e.getMessage():"처리하지 못했어요. 목록을 새로고침해 주세요."));
    }
    private void buttons() {
        var r=reports.getSelectionModel().getSelectedItem();boolean blocked=session==null||isLoading()||r==null||!"PENDING".equals(r.status());
        approveButton.setDisable(blocked);rejectButton.setDisable(blocked);
    }
    private static String state(String s) {return switch(s){case "PENDING"->"검토 대기";case "APPROVED"->"승인";default->"반려";};}
}
