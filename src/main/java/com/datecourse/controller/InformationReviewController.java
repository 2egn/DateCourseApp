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
    @FXML private ComboBox<kr.ac.university.localdb.db.InformationReportRepository.ReportFilter> filterBox;
    @FXML private Button previousButton,nextButton,refreshButton;
    private final java.util.List<Long> pageCursors=new java.util.ArrayList<>(java.util.Collections.singletonList(null));
    private int pageIndex;
    private Long nextCursor;
    private MemberRepository.AdminSession session;
    private kr.ac.university.localdb.db.InformationReportRepository.ReviewContext reviewContext;
    private long detailVersion;
    private java.util.function.BiPredicate<String,String> confirmation=this::confirm;
    @FXML private Label valueHelp;
    @FXML private void initialize() {
        filterBox.getItems().setAll(kr.ac.university.localdb.db.InformationReportRepository.ReportFilter.values());
        filterBox.setValue(kr.ac.university.localdb.db.InformationReportRepository.ReportFilter.PENDING);
        filterBox.valueProperty().addListener((o,a,b)->reload());
        reports.setPlaceholder(new Label("접수된 제보가 없어요."));
        reports.setCellFactory(v->new ListCell<>() {
            @Override protected void updateItem(InformationReport r,boolean empty) {
                super.updateItem(r,empty);setText(empty||r==null?null:"#"+r.id()+" · "+r.placeName()+" · "+state(r.status())+"\n"+InformationReport.Field.valueOf(r.field()).label());
            }
        });
        reports.getSelectionModel().selectedItemProperty().addListener((o,a,r)->loadDetail(r));
        loadingProperty().addListener((o,a,b)->buttons());buttons();
    }
    public void configure(MemberRepository.AdminSession session) { this.session=java.util.Objects.requireNonNull(session);buttons();reload(); }
    public void clear() {session=null;detailVersion++;reviewContext=null;reports.getItems().clear();buttons();}
    private void loadDetail(InformationReport report) {
        long version=++detailVersion;reviewContext=null;correctedValue.clear();reviewNote.clear();
        details.setText(report==null?"제보를 선택해 주세요.":"최신 정정 정보를 확인하고 있어요…");buttons();
        if(report==null||session==null)return;
        var owner=session;
        runWork("최신 정정 정보 확인 중",()->owner.informationReportContext(report.id()),context->{
            if(owner!=session||version!=detailVersion)return;
            reviewContext=context;var current=context.report();
            String latest=context.correction()==null?"승인된 정정 기록 없음":context.correction().value();
            details.setText(current.placeName()+" · "+InformationReport.Field.valueOf(current.field()).label()
                    +"\n\n제보 당시 정보 · "+current.originalValue()+"\n현재 승인된 정정 · "+latest
                    +"\n제안한 정보 · "+current.proposedValue()+"\n\n제보 이유 · "+current.reason()
                    +"\n근거 · "+(current.sourceUrl().isBlank()?"미제공":current.sourceUrl())
                    +"\n접수 · "+current.createdAt()+"\n처리 상태 · "+state(current.status())
                    +(current.reviewNote().isBlank()?"":"\n검토 메모 · "+current.reviewNote()));
            details.setTooltip(new Tooltip("제보 #"+current.id()+" · 장소 식별자 "+current.placeId()));
            correctedValue.setText("PENDING".equals(current.status())?current.proposedValue():current.reviewedValue());
            valueHelp.setText(InformationReport.Field.valueOf(current.field())==InformationReport.Field.PRICE
                    ?"승인할 가격 · 1인 기준 원 단위 정수":"승인할 "+InformationReport.Field.valueOf(current.field()).label());
            buttons();
        },failure->{if(owner==session&&version==detailVersion){status.setText("최신 정보를 확인하지 못했어요. 새로고침해 주세요.");buttons();}});
    }
    private boolean confirm(String title,String message) {
        var dialog=new Alert(Alert.AlertType.CONFIRMATION,message,ButtonType.OK,ButtonType.CANCEL);
        if(reports.getScene()!=null)dialog.initOwner(reports.getScene().getWindow());
        dialog.setTitle(title);dialog.setHeaderText(title);com.datecourse.ui.DialogTheme.apply(dialog);
        return dialog.showAndWait().orElse(ButtonType.CANCEL)==ButtonType.OK;
    }
    @FXML private void reload() {loadPage(null,0,true);}
    @FXML private void previousPage() {if(pageIndex>0)loadPage(pageCursors.get(pageIndex-1),pageIndex-1,false);}
    @FXML private void nextPage() {if(nextCursor!=null)loadPage(nextCursor,pageIndex+1,false);}
    private void loadPage(Long beforeId,int index,boolean reset) {
        if(session==null||isLoading())return;
        var filter=filterBox.getValue();
        runWork("제보 조회 중",()->session.informationReports(filter,beforeId),page->{
            if(reset){pageCursors.clear();pageCursors.add(null);}
            if(index==pageCursors.size())pageCursors.add(beforeId);
            else pageCursors.set(index,beforeId);
            pageIndex=index;nextCursor=page.nextBeforeId();
            reports.getItems().setAll(page.items());
            if(!page.items().isEmpty())reports.getSelectionModel().selectFirst();
            status.setText(filter+" · "+page.items().size()+"건 · "+(pageIndex+1)+"페이지");buttons();
        },e->status.setText("제보 목록을 불러오지 못했어요. 다시 시도해 주세요."));
    }
    @FXML private void approve() { review(true); }
    @FXML private void reject() { review(false); }
    private void review(boolean approve) {
        var expected=reviewContext;if(expected==null||session==null||isLoading()||!"PENDING".equals(expected.report().status()))return;
        String value=correctedValue.getText(),note=reviewNote.getText();
        if(note.isBlank()){status.setText("검토 근거 또는 반려 이유를 입력해 주세요.");return;}
        String latest=expected.correction()==null?"승인된 정정 없음":expected.correction().value();
        String message=expected.report().placeName()+" · "+InformationReport.Field.valueOf(expected.report().field()).label()
                +(approve?"\n현재 승인된 정정: "+latest+"\n승인할 정보: "+value:"\n이 제보를 반려합니다.")
                +"\n검토 메모: "+note+"\n\n"+(approve?"위 내용으로 승인하고 수정할까요?":"반려할까요?");
        if(!confirmation.test(approve?"정보 정정 승인":"제보 반려",message))return;
        var owner=session;
        runWork("제보 검토 저장 중",()->{owner.reviewInformation(expected,approve,value,note);return true;},
            ok->{if(owner!=session)return;reload();status.setText(approve?"승인했어요. 이후 후보 업데이트부터 반영돼요. 저장한 코스는 유지돼요.":"반려했어요. 장소 정보는 변경되지 않았어요.");},
            failure->{if(owner!=session)return;reviewContext=null;buttons();status.setText(failure instanceof IllegalArgumentException?failure.getMessage():"처리하지 못했어요. 목록을 새로고침해 주세요.");});
    }
    private void buttons() {
        var r=reports.getSelectionModel().getSelectedItem();boolean blocked=session==null||isLoading()||r==null||reviewContext==null||!"PENDING".equals(reviewContext.report().status());
        approveButton.setDisable(blocked);rejectButton.setDisable(blocked);
        correctedValue.setDisable(blocked);reviewNote.setDisable(blocked);reports.setDisable(isLoading());
        previousButton.setDisable(session==null||isLoading()||pageIndex==0);
        nextButton.setDisable(session==null||isLoading()||nextCursor==null);
        refreshButton.setDisable(session==null||isLoading());filterBox.setDisable(session==null||isLoading());
    }
    private static String state(String s) {return switch(s){case "PENDING"->"검토 대기";case "APPROVED"->"승인";default->"반려";};}
}
