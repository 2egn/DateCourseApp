package kr.ac.university.localdb.ui;

import com.datecourse.controller.ConditionController;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.util.Duration;
import kr.ac.university.localdb.InputHistory;
import kr.ac.university.localdb.UserSession;
import kr.ac.university.localdb.db.MemberRepository;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * 로그인·회원가입 화면과 인증 후의 관리자 화면을 담당합니다.
 * 실행 흐름: main → show → 사용자 이벤트 → register/login → 관리자라면 showAdmin.
 * DB 작업은 run의 작업 스레드에서 실행하고, 결과에 따른 화면 변경은 Swing 이벤트 스레드에서 처리합니다.
 */
public class MembershipApp {

    // ===== 필드: 연결 대상·세션·상태 =====
    private volatile MemberRepository repository;
    private volatile ConditionController plannerController;
    private boolean closePending;
    private java.util.function.BooleanSupplier adminCloseCheck;
    private UserSession userSession;
    private Timer screenTransition;
    private JPanel authenticationContent;

    // ===== 필드: 메인 창 =====
    private final JFrame home = new JFrame("로그인 / 회원가입");
    private final Runnable exitApplication=()->{home.dispose();System.exit(0);};

    // ===== 필드: 화면 전환(로그인 카드 ↔ 회원가입 카드) =====
    // 한 창에서 로그인 카드와 회원가입 카드를 전환합니다.
    private final CardLayout pages = new CardLayout();
    private final JPanel pagePanel = new JPanel(pages);
    private final InputHistory history = new InputHistory(Path.of(".local","input-history.properties"));

    // ===== 필드: 로그인 입력 =====
    private final JTextField loginId = new LoginUI.TextField(24);
    private final JPasswordField loginPassword = new LoginUI.PasswordField(24);
    private final JCheckBox rememberId = new JCheckBox("아이디 저장");
    private final JButton signup = new JButton("회원가입"), login = new JButton("로그인");

    // ===== 필드: 회원가입 입력 =====
    private final JTextField email = new LoginUI.TextField(24), name = new LoginUI.TextField(24), phone = new LoginUI.TextField(24);
    private final JPasswordField password = new LoginUI.PasswordField(24), confirm = new LoginUI.PasswordField(24);
    private final JButton next = new JButton("회원가입 시작"), back = new JButton("로그인으로 돌아가기");
    private final JLabel guidance = new JLabel("버튼을 눌러 회원가입을 시작하세요.");
    // inputs와 steps의 순서는 이메일 → 비밀번호 → 확인 → 이름 → 전화번호로 대응합니다.
    private final JTextField[] inputs = {email,password,confirm,name,phone};
    private final RevealPanel[] steps = new RevealPanel[5];
    // -1은 시작 전, 0~4는 현재 입력 단계입니다. animating은 펼침 효과 중 중복 진행을 막습니다.
    private int step=-1;
    private boolean animating;

    // ===== 프로그램 시작 =====

    /** 프로그램 진입점: 이벤트 스레드에서 창을 만들고, 종료 시 DB 연결을 닫도록 등록합니다. */
    public static void main(String[] args) {
        // Swing 구성 요소의 생성과 화면 조작은 이벤트 디스패치 스레드(EDT)에서 시작합니다.
        SwingUtilities.invokeLater(() -> {
            LoginUI.install();
            MembershipApp app = new MembershipApp();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                MemberRepository repository = app.repository;
                if (repository != null) repository.close();
            }, "membership-db-shutdown"));
            app.show();
        });
    }

    /** 입력 단계 구성 → 화면 전환/버튼 이벤트 연결 → 창 표시 → DB 초기화 순서로 준비합니다. */
    private void show() {
        installCloseHandling();
        JPanel content = new JPanel(new BorderLayout(15,15));
        content.setBorder(BorderFactory.createEmptyBorder(36,36,28,36));
        JPanel form = new SignupForm();
        String[] labels={"이메일 (회원 아이디)","비밀번호 (8자 이상)","비밀번호 확인","이름","전화번호"};
        // 각 입력란을 펼침 패널에 연결하고, 현재 단계에서 Enter를 누르면 다음으로 진행합니다.
        for(int i=0;i<steps.length;i++) {
            steps[i]=new RevealPanel(labels[i],inputs[i]); form.add(steps[i]);
            final int index=i;
            inputs[i].addActionListener(e -> { if(step==index && next.isEnabled()) advance(); });
        }
        JPanel signupPanel = new JPanel(new BorderLayout(12,12));
        JPanel signupHeading=new JPanel(new BorderLayout(0,20));
        signupHeading.add(LoginUI.heading("처음이라면, 반가워요","필요한 정보만 하나씩 입력해 주세요"),BorderLayout.NORTH);
        guidance.setFont(LoginUI.BODY.deriveFont(13f));guidance.setForeground(LoginUI.MUTED);
        signupHeading.add(guidance,BorderLayout.SOUTH);signupPanel.add(signupHeading,BorderLayout.NORTH);
        JScrollPane scroll=new JScrollPane(form); scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        signupPanel.add(scroll,BorderLayout.CENTER);
        LoginUI.button(next,true);
        LoginUI.button(back,false);
        JPanel signupButtons = new JPanel(new GridLayout(0,1,0,10)); signupButtons.add(next); signupButtons.add(back); signupPanel.add(signupButtons,BorderLayout.SOUTH);
        String savedId = history.savedIdentity();
        loginId.setText(savedId);
        rememberId.setSelected(!savedId.isEmpty());
        rememberId.addActionListener(e -> {
            if (!rememberId.isSelected()) history.saveIdentity("");
        });
        JPanel loginPanel= LoginUI.loginPanel(loginId,loginPassword,rememberId,login,signup);
        StartupPanel startup = new StartupPanel(() -> {
            if (repository == null) repository = new MemberRepository();
            repository.initialize();
            return null;
        }, () -> {
            pages.show(pagePanel, "login");
            if (loginId.getText().isBlank()) loginId.requestFocusInWindow();
            else loginPassword.requestFocusInWindow();
        }, () -> home.dispatchEvent(new java.awt.event.WindowEvent(home, java.awt.event.WindowEvent.WINDOW_CLOSING)));
        pagePanel.add(startup, "startup");
        pagePanel.add(loginPanel,"login"); pagePanel.add(signupPanel,"signup");
        content.add(pagePanel,BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(8,12));
        content.add(bottom,BorderLayout.SOUTH); home.setContentPane(content);
        authenticationContent = content;
        pages.show(pagePanel,"startup");
        next.addActionListener(e -> advance()); login.addActionListener(e -> login());
        signup.addActionListener(e -> { resetSignup(); pages.show(pagePanel,"signup"); advance(); });
        back.addActionListener(e -> { resetSignup(); pages.show(pagePanel,"login"); });
        loginPassword.addActionListener(e -> { if (login.isEnabled()) login(); });
        Rectangle usable = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        home.setMinimumSize(new Dimension(Math.min(470,usable.width),Math.min(640,usable.height)));
        home.setSize(Math.min(580,usable.width),Math.min(900,usable.height));
        home.setLocationRelativeTo(null); home.setVisible(true);
        startup.start();
    }

    // ===== 창 닫기 처리 =====

    /** 창의 X 버튼을 눌러도 바로 닫히지 않고 requestWindowClose가 먼저 실행되도록 연결합니다. */
    void installCloseHandling() {
        home.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        home.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent event) { requestWindowClose(); }
        });
    }

    /** 닫기 요청 처리: 관리자 화면 확인 → 플래너 화면(JavaFX) 확인 → 통과하면 종료합니다. */
    private void requestWindowClose() {
        if(closePending)return;
        if(adminCloseCheck!=null && !adminCloseCheck.getAsBoolean())return;
        ConditionController controller=plannerController;
        if(controller==null){exitApplication.run();return;}
        closePending=true;
        Platform.runLater(()->{
            boolean decision;
            try {decision=controller.confirmWindowClose();}
            catch(RuntimeException failure){decision=false;}
            final boolean allowed=decision;
            SwingUtilities.invokeLater(()->{
                closePending=false;
                if(allowed && plannerController==controller)exitApplication.run();
            });
        });
    }

    // ===== 회원가입 =====

    /** 가입 화면 진입/복귀 시 입력값과 펼침 상태를 모두 시작 전 상태로 되돌립니다. */
    private void resetSignup() {
        step=-1; animating=false;
        for(RevealPanel panel:steps) panel.reset();
        for(JTextField input:inputs) input.setText("");
        next.setText("회원가입 시작"); next.setEnabled(signup.isEnabled());
        guidance.setText("버튼을 눌러 회원가입을 시작하세요.");
    }

    /** 현재 단계 검증 → 다음 입력란 펼치기. 마지막 단계라면 실제 가입을 요청합니다. */
    private void advance() {
        if(animating) return;
        try {
            if(step>=0) validateStep(step);
        } catch(IllegalArgumentException error) {
            AppDialogs.showMessageDialog(home,error.getMessage(),"입력을 확인해 주세요",JOptionPane.WARNING_MESSAGE); inputs[step].requestFocusInWindow(); return;
        }
        // 전화번호 단계까지 검증이 끝나면 새 입력란 대신 저장 작업으로 넘어갑니다.
        if(step==4) { register(); return; }
        step++; animating=true; next.setEnabled(false);
        String[] prompts={"1 / 5 · 사용할 이메일을 알려 주세요","2 / 5 · 안전한 비밀번호를 만들어 주세요","3 / 5 · 비밀번호를 한 번 더 입력해 주세요","4 / 5 · 어떻게 불러 드릴까요?","5 / 5 · 전화번호는 나중에 입력해도 돼요"};
        guidance.setText(prompts[step]);
        next.setText(step==4 ? "가입 완료" : "다음");
        // 펼침이 끝난 시점에 버튼을 다시 켜고 새 입력란으로 포커스와 스크롤을 이동합니다.
        steps[step].reveal(() -> {
            animating=false; next.setEnabled(true); inputs[step].requestFocusInWindow();
            steps[step].scrollRectToVisible(new Rectangle(0,0,steps[step].getWidth(),steps[step].getHeight()));
        });
    }

    /** 다음 단계로 넘어가기 전에 현재까지 입력한 값을 검사합니다. 오류는 advance에서 안내합니다. */
    private void validateStep(int index) {
        // 이메일 단계에서는 아직 이름을 받지 않았으므로 공통 검증에 임시 이름을 전달합니다.
        if(index==0) { MemberRepository.validate(email.getText().trim(),"이름",""); }
        // 검증용으로 복사한 비밀번호 배열은 성공/실패와 관계없이 사용 직후 덮어씁니다.
        if(index==1) {
            char[] value=password.getPassword();
            try { if(value.length<8 || value.length>128) throw new IllegalArgumentException("비밀번호는 8자 이상입니다."); }
            finally { Arrays.fill(value,'\0'); }
        }
        if(index==2) {
            char[] a=password.getPassword(), b=confirm.getPassword();
            try { if(!Arrays.equals(a,b)) throw new IllegalArgumentException("비밀번호 확인이 일치하지 않습니다."); }
            finally { Arrays.fill(a,'\0'); Arrays.fill(b,'\0'); }
        }
        if(index>=3) {
            MemberRepository.validate(email.getText().trim(),name.getText().trim(),phone.getText().trim());
        }
    }

    /** 가입 성공 후 로그인 화면으로 돌아갑니다. */
    private void register() {
        char[] secret = password.getPassword(), repeated = confirm.getPassword();
        if (!Arrays.equals(secret,repeated)) {
            Arrays.fill(secret,'\0'); Arrays.fill(repeated,'\0');
            AppDialogs.showMessageDialog(home,"비밀번호가 일치하지 않습니다."); return;
        }
        Arrays.fill(repeated,'\0');
        // 화면 값은 EDT에서 읽어 두고, 작업 스레드에는 확보한 값만 전달합니다.
        String mail=email.getText(), person=name.getText(), telephone=phone.getText();
        // 저장 중에는 중복 가입과 화면 복귀를 막고, 완료 콜백에서 복귀 버튼을 다시 켭니다.
        next.setEnabled(false); back.setEnabled(false);
        run(home,next,() -> {
            try { return repository.register(mail,secret,person,telephone); }
            finally { Arrays.fill(secret,'\0'); }
        }, id -> {
            email.setText(""); name.setText(""); phone.setText(""); password.setText(""); confirm.setText("");
            AppDialogs.showMessageDialog(home,"회원 정보가 저장되었습니다.");
            loginId.setText(mail.trim()); resetSignup(); pages.show(pagePanel,"login");
        }, () -> back.setEnabled(true));
    }

    // ===== 로그인 / 로그아웃 =====

    /** 인증 결과의 관리자 세션 유무로 관리자 화면과 일반 회원 안내를 구분합니다. */
    private void login() {
        String username = loginId.getText(); char[] secret = loginPassword.getPassword();
        // 화면의 비밀번호는 즉시 지우고, 인증에 사용한 배열도 작업 종료 시 덮어씁니다.
        loginPassword.setText("");
        run(home,login,() -> {
            try { return repository.login(username,secret); }
            finally { Arrays.fill(secret,'\0'); }
        }, result -> {
            userSession = new UserSession(result.memberId(), result.name());

            history.saveIdentity(rememberId.isSelected() ? username : "");
            if (result.adminSession()!=null) showAdmin(result.adminSession()); //[폼 이동] -> 어드민 페이지
            else showConditionScreen(); //[폼 이동] ->
        });
    }

    /** 로그아웃: 세션과 전환 효과를 정리하고 로그인 화면(원래 창 크기)으로 되돌립니다. */
    private void logout(JFXPanel panel) {
        plannerController = null;
        userSession = null;
        if (screenTransition != null) screenTransition.stop();
        Platform.runLater(() -> panel.setScene(null));
        loginPassword.setText("");
        if (!rememberId.isSelected()) loginId.setText("");
        resetSignup();
        pages.show(pagePanel, "login");
        home.setContentPane(authenticationContent);
        home.setTitle("로그인 / 회원가입");
        home.setMinimumSize(new Dimension(0,0));
        Rectangle usable = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        home.setSize(Math.min(580, usable.width), Math.min(900, usable.height));
        home.setLocationRelativeTo(null);
        home.revalidate(); home.repaint(); home.toFront();
        SwingUtilities.invokeLater(() -> {
            if (loginId.getText().isBlank()) loginId.requestFocusInWindow();
            else loginPassword.requestFocusInWindow();
        });
    }

    // ===== 일반 회원: 코스 만들기(JavaFX) 화면 =====

    /** 로딩창을 띄운 뒤 코스 만들기 화면 불러오기를 시작합니다. */
    private void showConditionScreen(){
        PlannerLoadingDialog loading = new PlannerLoadingDialog(home);
        loading.open();
        SwingUtilities.invokeLater(() -> loadConditionScreen(loading));
    }

    /** condition-view.fxml을 불러와 컨트롤러를 설정하고, 준비가 끝나면 창에 붙여 보여줍니다. */
    private void loadConditionScreen(PlannerLoadingDialog loading) {
        final JFXPanel panel;
        try { panel = new JFXPanel(); }
        catch (RuntimeException failure) { showConditionFailure(loading, failure); return; }

        Platform.runLater(() ->{
            try{
                Platform.setImplicitExit(false);
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/condition-view.fxml"));

                Parent root = loader.load();

                ConditionController controller = loader.getController();
                plannerController = controller;
                controller.setUserSession(userSession);
                controller.setSavedCourseRepository(repository.savedCourses());
                controller.setAccountActions(repository, () -> SwingUtilities.invokeLater(() -> logout(panel)));
                controller.setPlaceRepository(repository.places());

                root.setOpacity(0);
                root.setTranslateY(10);
                panel.setScene(new Scene(root));

                controller.prepareStartup(() -> SwingUtilities.invokeLater(()->{
                    home.setContentPane(panel);
                    home.setTitle("DateCourse · 코스 만들기");
                    loading.close();

                    Rectangle usable = GraphicsEnvironment
                            .getLocalGraphicsEnvironment()
                            .getMaximumWindowBounds();

                    home.revalidate();
                    home.repaint();
                    bringPlannerToFront(panel);
                    int width = Math.min(1440, usable.width);
                    int height = Math.min(960, usable.height);
                    animatePlannerWindow(new Rectangle(usable.x + (usable.width - width) / 2,
                            usable.y + (usable.height - height) / 2, width, height));
                    Platform.runLater(() -> {
                        FadeTransition fade = new FadeTransition(Duration.millis(260), root);
                        fade.setFromValue(0); fade.setToValue(1);
                        TranslateTransition slide = new TranslateTransition(Duration.millis(260), root);
                        slide.setFromY(10); slide.setToY(0);
                        ParallelTransition entrance = new ParallelTransition(fade, slide);
                        entrance.setInterpolator(Interpolator.EASE_OUT);
                        entrance.play();
                        root.requestFocus();
                    });
                }));


            }catch(IOException | RuntimeException failure){
                SwingUtilities.invokeLater(() -> showConditionFailure(loading, failure));
            }
        });

    }

    /** 화면 로딩에 실패했을 때 로딩창을 닫고 기록을 남긴 뒤 안내 메시지를 보여줍니다. */
    private void showConditionFailure(PlannerLoadingDialog loading, Exception failure) {
        loading.close();
        com.datecourse.service.AppDiagnostics.failure(com.datecourse.service.AppDiagnostics.Operation.SCREEN_LOAD,
                com.datecourse.service.AppDiagnostics.Stage.START,failure);
        AppDialogs.showMessageDialog(home, "화면을 열지 못했어요. 잠시 후 다시 로그인해 주세요.",
                "화면 전환 실패", JOptionPane.ERROR_MESSAGE);
    }

    /** 창을 최소화 상태에서 풀고 맨 앞으로 가져와 플래너 화면에 포커스를 줍니다. */
    private void bringPlannerToFront(JFXPanel panel) {
        home.setExtendedState(home.getExtendedState() & ~Frame.ICONIFIED);
        home.toFront();
        home.requestFocus();
        panel.requestFocusInWindow();
    }

    /** Keep the authenticated planner in the same window while its bounds settle into place. */
    private void animatePlannerWindow(Rectangle target) {
        if (screenTransition != null) screenTransition.stop();
        Rectangle start = home.getBounds();
        long started = System.nanoTime();
        screenTransition = new Timer(16, event -> {
            if (!home.isDisplayable()) { ((Timer) event.getSource()).stop(); return; }
            double progress = Math.min(1, (System.nanoTime() - started) / 240_000_000.0);
            double eased = 1 - Math.pow(1 - progress, 3);
            home.setBounds(
                    (int) Math.round(start.x + (target.x - start.x) * eased),
                    (int) Math.round(start.y + (target.y - start.y) * eased),
                    (int) Math.round(start.width + (target.width - start.width) * eased),
                    (int) Math.round(start.height + (target.height - start.height) * eased));
            if (progress >= 1) {
                ((Timer) event.getSource()).stop();
                if (home.getContentPane() instanceof JFXPanel panel) bringPlannerToFront(panel);
            }
        });
        screenTransition.start();
    }

    // ===== 관리자 화면 =====

    /** 인증된 관리자 세션으로 회원 검색·수정·비활성화·영구 삭제 기능을 연결합니다. */
    private void showAdmin(MemberRepository.AdminSession session) {
        var members=new MemberAdminPanel(session);
        var reviewBusy=new java.util.concurrent.atomic.AtomicBoolean();
        var reviewController=new java.util.concurrent.atomic.AtomicReference<com.datecourse.controller.InformationReviewController>();
        var tabs=new JTabbedPane();AdminStyle.tabs(tabs);tabs.setBorder(BorderFactory.createEmptyBorder(0,20,0,0));tabs.addTab("회원 관리",members);
        var reportPanel=new JFXPanel();tabs.addTab("장소 정보 제보",reportPanel);
        var logoutButton=new JButton("로그아웃");
        LoginUI.button(logoutButton,false);
        logoutButton.setPreferredSize(new Dimension(120,40));
        var top=new JPanel(new BorderLayout());top.setBorder(BorderFactory.createEmptyBorder(12,24,12,24));
        var title=new JLabel("DateCourse · 관리자");title.setFont(LoginUI.BODY.deriveFont(Font.BOLD,18f));top.add(title,BorderLayout.WEST);top.add(logoutButton,BorderLayout.EAST);
        adminCloseCheck=()->{
            if(reviewBusy.get()){AppDialogs.showMessageDialog(home,"제보 처리 작업이 끝난 뒤 다시 시도해 주세요.");return false;}
            return members.confirmLeave();
        };
        logoutButton.addActionListener(event->{
            if(adminCloseCheck!=null&&!adminCloseCheck.getAsBoolean())return;
            adminCloseCheck=null;
            Platform.runLater(()->{if(reviewController.get()!=null)reviewController.get().clear();});
            logout(reportPanel);
        });
        Platform.runLater(()->{
            try {
                var loader=new FXMLLoader(getClass().getResource("/information-review-view.fxml"));Parent page=loader.load();
                var controller=loader.<com.datecourse.controller.InformationReviewController>getController();reviewController.set(controller);
                controller.loadingProperty().addListener((o,a,value)->reviewBusy.set(value));
                controller.configure(session);reportPanel.setScene(new Scene(page));
            } catch(IOException failure) {
                com.datecourse.service.AppDiagnostics.failure(com.datecourse.service.AppDiagnostics.Operation.SCREEN_LOAD,
                        com.datecourse.service.AppDiagnostics.Stage.START,failure);
                reportPanel.setScene(new Scene(new javafx.scene.control.Label("제보 화면을 열지 못했어요. 다시 로그인해 주세요.")));
            }
        });
        var content=new JPanel(new BorderLayout());content.add(top,BorderLayout.NORTH);content.add(tabs,BorderLayout.CENTER);
        home.setContentPane(content);home.setTitle("DateCourse · 관리자");
        Rectangle usable=GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        home.setMinimumSize(new Dimension(Math.min(900,usable.width),Math.min(680,usable.height)));
        home.setSize(Math.min(1200,usable.width),Math.min(840,usable.height));home.setLocationRelativeTo(null);home.revalidate();home.repaint();
    }

    // ===== 공통: 백그라운드 작업 실행 =====

    /** 성공 처리만 필요할 때 쓰는 간단한 버전(마무리 작업 없음). */
    private static <T> void run(Component parent,JButton button,Callable<T> action,Consumer<T> success) {
        run(parent,button,action,success,() -> {});
    }

    /**
     * 공통 비동기 처리: 버튼 잠금 → 백그라운드 작업 → EDT에서 성공/오류 안내 → 공통 마무리.
     * action은 DB 작업, success는 성공한 결과의 화면 반영, complete는 성공/실패 공통 정리입니다.
     */
    private static <T> void run(Component parent,JButton button,Callable<T> action,Consumer<T> success,Runnable complete) {
        button.setEnabled(false);
        new SwingWorker<T,Void>() {
            // 시간이 걸리는 DB 작업을 EDT 밖에서 실행해 화면이 멈추지 않게 합니다.
            protected T doInBackground() throws Exception { return action.call(); }
            // done은 EDT에서 실행됩니다. get()으로 결과를 받거나 작업 중 발생한 예외를 확인합니다.
            protected void done() {
                button.setEnabled(true);
                try { success.accept(get()); }
                catch (Exception failure) {
                    // 작업 예외의 원인을 찾아 MySQL 중복 키 오류(1062)는 이해하기 쉬운 안내로 바꿉니다.
                    com.datecourse.service.AppDiagnostics.begin(com.datecourse.service.AppDiagnostics.Operation.ACCOUNT_TASK,MembershipApp.class)
                            .report(com.datecourse.service.AppDiagnostics.Stage.RUN,com.datecourse.service.AppDiagnostics.Outcome.FAILED,failure);
                    Throwable cause=com.datecourse.service.AppDiagnostics.root(failure);
                    String text=cause instanceof SQLException sql && sql.getErrorCode()==1062
                            ? "이미 사용 중인 이메일 또는 관리자 계정입니다."
                            : cause instanceof IllegalArgumentException ? cause.getMessage()
                            : "요청을 처리하지 못했어요. 잠시 후 다시 시도해 주세요.";
                    AppDialogs.showMessageDialog(parent,text,"처리 실패",JOptionPane.ERROR_MESSAGE);
                }
                finally { complete.run(); }
            }
        }.execute();
    }
}