package com.datecourse.controller;

import com.datecourse.model.InformationReport;
import kr.ac.university.localdb.db.MemberRepository;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/** The real administrator login supplies the otherwise unconstructable capability. */
public final class InformationReviewController extends BasePageController {

    // ===== FXML 컴포넌트 (information-review-view.fxml의 위→아래 순서) =====

    // [상단] 상태 필터 / 새로고침
    @FXML private ComboBox<kr.ac.university.localdb.db.InformationReportRepository.ReportFilter> filterBox;
    @FXML private Button refreshButton;

    // [중앙 왼쪽] 제보 목록
    @FXML private ListView<InformationReport> reports;

    // [중앙 오른쪽] 제보 상세 / 입력 / 승인·반려
    @FXML private Label details;
    @FXML private Label valueHelp;
    @FXML private TextField correctedValue;
    @FXML private TextArea reviewNote;
    @FXML private Button approveButton,rejectButton;

    // [하단] 상태 메시지 / 페이지 이동
    @FXML private Label status;
    @FXML private Button previousButton,nextButton;

    // ===== 내부 상태 =====

    // 페이지별 시작 커서 목록 (첫 페이지는 null)
    private final java.util.List<Long> pageCursors=new java.util.ArrayList<>(java.util.Collections.singletonList(null));
    private int pageIndex;       // 현재 페이지 번호 (0부터)
    private Long nextCursor;     // 다음 페이지 커서 (없으면 null)
    private MemberRepository.AdminSession session;  // 관리자 로그인 세션
    private kr.ac.university.localdb.db.InformationReportRepository.ReviewContext reviewContext;  // 선택한 제보의 최신 검토 정보
    private long detailVersion;  // 상세 조회 요청 번호 (오래된 응답 무시용)
    private final java.util.function.BiPredicate<String,String> confirmation=this::confirm;  // 확인 창 (제목, 내용)

    // ===== 초기화 / 외부에서 호출하는 메서드 =====

    /** 화면 초기 설정: 필터, 목록 표시 방식, 선택 이벤트 연결 */
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

    /** 관리자 로그인 후 세션을 받아 목록을 불러옴 */
    public void configure(MemberRepository.AdminSession session) { this.session=java.util.Objects.requireNonNull(session);buttons();reload(); }

    /** 로그아웃 시 세션과 화면 데이터를 비움 */
    public void clear() {session=null;detailVersion++;reviewContext=null;reports.getItems().clear();buttons();}

    // ===== 버튼 이벤트 (FXML onAction) =====

    /** 새로고침 / 필터 변경 → 첫 페이지부터 다시 조회 */
    @FXML private void reload() {loadPage(null,0,true);}

    /** 승인 버튼 */
    @FXML private void approve() { review(true); }

    /** 반려 버튼 */
    @FXML private void reject() { review(false); }

    /** 이전 페이지 버튼 */
    @FXML private void previousPage() {if(pageIndex>0)loadPage(pageCursors.get(pageIndex-1),pageIndex-1,false);}

    /** 다음 페이지 버튼 */
    @FXML private void nextPage() {if(nextCursor!=null)loadPage(nextCursor,pageIndex+1,false);}

    // ===== 목록 조회 =====

    /** 제보 목록 한 페이지를 불러와 화면에 표시 */
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

    // ===== 상세 조회 =====

    /** 선택한 제보의 최신 정정 정보를 불러와 상세 영역에 표시 */
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

    // ===== 검토 (승인 / 반려) =====

    /** 승인 또는 반려 처리: 입력 확인 → 확인 창 → 저장 */
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

    /** 확인/취소 대화상자 (OK를 누르면 true) */
    private boolean confirm(String title,String message) {
        var dialog=new Alert(Alert.AlertType.CONFIRMATION,message,ButtonType.OK,ButtonType.CANCEL);
        if(reports.getScene()!=null)dialog.initOwner(reports.getScene().getWindow());
        dialog.setTitle(title);dialog.setHeaderText(title);com.datecourse.ui.DialogTheme.apply(dialog);
        return dialog.showAndWait().orElse(ButtonType.CANCEL)==ButtonType.OK;
    }

    // ===== 화면 상태 / 보조 메서드 =====

    /** 로그인·로딩·선택 상태에 따라 버튼과 입력창을 활성화/비활성화 */
    private void buttons() {
        var r=reports.getSelectionModel().getSelectedItem();boolean blocked=session==null||isLoading()||r==null||reviewContext==null||!"PENDING".equals(reviewContext.report().status());
        approveButton.setDisable(blocked);rejectButton.setDisable(blocked);
        correctedValue.setDisable(blocked);reviewNote.setDisable(blocked);reports.setDisable(isLoading());
        previousButton.setDisable(session==null||isLoading()||pageIndex==0);
        nextButton.setDisable(session==null||isLoading()||nextCursor==null);
        refreshButton.setDisable(session==null||isLoading());filterBox.setDisable(session==null||isLoading());
    }

    /** 상태 코드를 화면에 보여줄 한글로 변환 */
    private static String state(String s) {return switch(s){case "PENDING"->"검토 대기";case "APPROVED"->"승인";default->"반려";};}
}