package kr.ac.university.localdb.ui;

import kr.ac.university.localdb.InputHistory;
import kr.ac.university.localdb.db.MemberRepository;
import kr.ac.university.localdb.UserSession;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
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
    private volatile MemberRepository repository;
    private UserSession userSession;
    private final JFrame home = new JFrame("로그인 / 회원가입");
    private final JTextField email = new UiTheme.TextField(24), name = new UiTheme.TextField(24), phone = new UiTheme.TextField(24);
    private final JPasswordField password = new UiTheme.PasswordField(24), confirm = new UiTheme.PasswordField(24);
    private final JButton signup = new JButton("회원가입"), login = new JButton("로그인");
    private final JTextField loginId = new UiTheme.TextField(24);
    private final JPasswordField loginPassword = new UiTheme.PasswordField(24);
    private final JCheckBox rememberId = new JCheckBox("아이디 저장");
    // 한 창에서 로그인 카드와 회원가입 카드를 전환합니다.
    private final CardLayout pages = new CardLayout();
    private final JPanel pagePanel = new JPanel(pages);
    private final InputHistory history = new InputHistory(Path.of(".local","input-history.properties"));
    private final JButton next = new JButton("회원가입 시작"), back = new JButton("로그인으로 돌아가기");
    private final JLabel guidance = new JLabel("버튼을 눌러 회원가입을 시작하세요.");
    // inputs와 steps의 순서는 이메일 → 비밀번호 → 확인 → 이름 → 전화번호로 대응합니다.
    private final JTextField[] inputs = {email,password,confirm,name,phone};
    private final RevealPanel[] steps = new RevealPanel[5];
    // -1은 시작 전, 0~4는 현재 입력 단계입니다. animating은 펼침 효과 중 중복 진행을 막습니다.
    private int step=-1;
    private boolean animating;

    public static void main(String[] args) {
        // Swing 구성 요소의 생성과 화면 조작은 이벤트 디스패치 스레드(EDT)에서 시작합니다.
        SwingUtilities.invokeLater(() -> {
            UiTheme.install();
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
        home.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
        signupHeading.add(UiTheme.heading("처음이라면, 반가워요","필요한 정보만 하나씩 입력해 주세요"),BorderLayout.NORTH);
        guidance.setFont(UiTheme.BODY.deriveFont(13f));guidance.setForeground(UiTheme.MUTED);
        signupHeading.add(guidance,BorderLayout.SOUTH);signupPanel.add(signupHeading,BorderLayout.NORTH);
        JScrollPane scroll=new JScrollPane(form); scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        scroll.getViewport().setBackground(Color.WHITE);
        signupPanel.add(scroll,BorderLayout.CENTER);
        UiTheme.button(next,true);UiTheme.button(back,false);
        JPanel signupButtons = new JPanel(new GridLayout(0,1,0,10)); signupButtons.add(next); signupButtons.add(back); signupPanel.add(signupButtons,BorderLayout.SOUTH);
        String savedId = history.savedIdentity();
        loginId.setText(savedId);
        rememberId.setSelected(!savedId.isEmpty());
        rememberId.addActionListener(e -> {
            if (!rememberId.isSelected()) history.saveIdentity("");
        });
        JPanel loginPanel=UiTheme.loginPanel(loginId,loginPassword,rememberId,login,signup);
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

    /** 관리자 편집 영역에 라벨과 입력 요소를 한 쌍으로 배치합니다. */
    private static void field(JPanel form,String label,JComponent component) {
        form.add(new JLabel(label)); form.add(component);
    }

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
            if (result.adminSession()!=null) showAdmin(result.adminSession());
            else AppDialogs.showMessageDialog(home,result.name()+"님, 로그인했습니다.");
        });
    }

    /** 인증된 관리자 세션으로 회원 검색·수정·비활성화·영구 삭제 기능을 연결합니다. */
    private void showAdmin(MemberRepository.AdminSession session) {
        JFrame window = new JFrame("관리자 · 회원 관리");
        window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        DefaultTableModel model = new DefaultTableModel(new String[]{"번호","이메일","이름","전화번호","상태","가입일","삭제"},0) {
            @Override public boolean isCellEditable(int r,int c) { return false; }
        };
        JTable table = new JTable(model); table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true); table.setRowHeight(44);
        table.setShowGrid(false);table.setIntercellSpacing(new Dimension(0,0));table.setSelectionBackground(UiTheme.SOFT);table.setSelectionForeground(UiTheme.TEXT);
        table.getTableHeader().setBackground(UiTheme.INPUT);table.getTableHeader().setForeground(UiTheme.MUTED);
        table.getTableHeader().setPreferredSize(new Dimension(0,44));
        JTextField search = new UiTheme.TextField(20); JButton refresh = new JButton("검색 / 새로고침");
        JTextField person = new UiTheme.TextField(15), telephone = new UiTheme.TextField(15);
        JComboBox<String> status = new JComboBox<>(new String[]{"ACTIVE","INACTIVE"});
        JButton save = new JButton("선택 회원 수정"), deactivate = new JButton("선택 회원 탈퇴 처리");
        UiTheme.button(refresh,false);UiTheme.button(save,true);UiTheme.button(deactivate,false);
        JLabel count = new JLabel("회원 목록");
        // 최초 표시, 검색, 변경 성공 후에 같은 조회 흐름으로 목록과 회원 수를 갱신합니다.
        Runnable reload = () -> {
            String keyword = search.getText();
            run(window,refresh,() -> session.list(keyword),members -> {
                model.setRowCount(0);
                for (var member : members) model.addRow(new Object[]{member.id(),member.email(),member.name(),member.phone(),member.status(),member.created(),"INACTIVE".equals(member.status()) ? "×" : ""});
                person.setText(""); telephone.setText(""); count.setText("조회된 회원 " + members.size() + "명 · INACTIVE는 탈퇴/비활성 상태");
            });
        };
        // 마지막 열의 ×는 비활성 회원에만 표시하며, 실제 삭제는 아래 클릭 이벤트에서 처리합니다.
        table.getColumnModel().getColumn(6).setMaxWidth(48);
        table.getColumnModel().getColumn(6).setCellRenderer((grid,value,selected,focused,row,column) -> {
            JLabel cross=new JLabel(String.valueOf(value),SwingConstants.CENTER);
            cross.setFont(UiTheme.BODY.deriveFont(Font.BOLD,22f)); cross.setForeground(new Color(0xB4233F));
            cross.setOpaque(true);cross.setBackground(selected ? UiTheme.SOFT : Color.WHITE);
            cross.setToolTipText("×".equals(value) ? "비활성 회원 영구 삭제" : "활성 회원은 삭제할 수 없습니다.");
            return cross;
        });
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent event) {
                int viewRow=table.rowAtPoint(event.getPoint()), viewCol=table.columnAtPoint(event.getPoint());
                if(viewRow<0 || viewCol<0 || table.convertColumnIndexToModel(viewCol)!=6) return;
                // 정렬된 화면의 행 번호를 원본 모델의 행 번호로 변환해야 정확한 회원을 찾습니다.
                int row=table.convertRowIndexToModel(viewRow);
                if(!"INACTIVE".equals(model.getValueAt(row,4))) return;
                long id=((Number)model.getValueAt(row,0)).longValue();
                if(AppDialogs.showConfirmDialog(window,model.getValueAt(row,1)+" 회원을 영구 삭제할까요?\n삭제한 회원 정보는 복구할 수 없습니다.",
                        "회원 삭제",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE)!=JOptionPane.YES_OPTION) return;
                run(window,refresh,() -> { session.deleteInactive(id); return true; },ok -> reload.run());
            }
        });
        // 선택한 회원 정보를 하단 편집란에 채우고, 저장 시 해당 회원 ID로 수정합니다.
        table.getSelectionModel().addListSelectionListener(e -> {
            if (table.getSelectedRow()<0) return;
            int row=table.convertRowIndexToModel(table.getSelectedRow());
            person.setText(String.valueOf(model.getValueAt(row,2)));
            Object value=model.getValueAt(row,3); telephone.setText(value==null ? "" : value.toString());
            status.setSelectedItem(model.getValueAt(row,4));
        });
        refresh.addActionListener(e -> reload.run()); search.addActionListener(e -> reload.run());
        save.addActionListener(e -> {
            if (table.getSelectedRow()<0) { AppDialogs.showMessageDialog(window,"수정할 회원을 선택하세요."); return; }
            int row=table.convertRowIndexToModel(table.getSelectedRow());
            long id=((Number)model.getValueAt(row,0)).longValue();
            String n=person.getText(), p=telephone.getText(), state=(String)status.getSelectedItem();
            run(window,save,() -> { session.update(id,n,p,state); return true; },ok -> reload.run());
        });
        // 탈퇴 처리는 정보를 남긴 채 상태만 INACTIVE로 바꾸며, 영구 삭제와 구분됩니다.
        deactivate.addActionListener(e -> {
            if (table.getSelectedRow()<0) { AppDialogs.showMessageDialog(window,"탈퇴 처리할 회원을 선택하세요."); return; }
            int row=table.convertRowIndexToModel(table.getSelectedRow());
            if (AppDialogs.showConfirmDialog(window,model.getValueAt(row,1)+" 회원을 비활성화할까요?",
                    "탈퇴 처리",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION) return;
            long id=((Number)model.getValueAt(row,0)).longValue();
            String n=String.valueOf(model.getValueAt(row,2)); Object v=model.getValueAt(row,3);
            run(window,deactivate,() -> { session.update(id,n,v==null?"":v.toString(),"INACTIVE"); return true; },ok -> reload.run());
        });
        JPanel top = new JPanel(); top.add(new JLabel("이메일 / 이름")); top.add(search); top.add(refresh);
        JPanel editor = new JPanel(new GridLayout(0,2,8,8));
        field(editor,"이름",person); field(editor,"전화번호",telephone); field(editor,"상태",status);
        editor.add(save); editor.add(deactivate);
        JPanel bottom = new JPanel(new BorderLayout(8,8)); bottom.add(editor,BorderLayout.CENTER); bottom.add(count,BorderLayout.SOUTH);
        JPanel content = new JPanel(new BorderLayout(20,20)); content.setBorder(BorderFactory.createEmptyBorder(28,28,28,28));
        JPanel header=new JPanel(new BorderLayout(0,24));header.add(UiTheme.heading("회원 관리","가입한 회원의 정보를 한눈에 확인하세요"),BorderLayout.NORTH);header.add(top,BorderLayout.SOUTH);
        JScrollPane list=new JScrollPane(table);list.setBorder(BorderFactory.createEmptyBorder());
        content.add(header,BorderLayout.NORTH); content.add(list,BorderLayout.CENTER); content.add(bottom,BorderLayout.SOUTH);
        window.setContentPane(content); window.setSize(1080,780); window.setLocationRelativeTo(home); window.setVisible(true); reload.run();
    }

    /** 별도 마무리 처리가 없는 작업은 빈 완료 콜백을 사용합니다. */
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
                    Throwable cause=failure; while (cause.getCause()!=null) cause=cause.getCause();
                    String text=cause instanceof SQLException sql && sql.getErrorCode()==1062
                            ? "이미 사용 중인 이메일 또는 관리자 계정입니다." : cause.getMessage();
                    AppDialogs.showMessageDialog(parent,text,"처리 실패",JOptionPane.ERROR_MESSAGE);
                }
                finally { complete.run(); }
            }
        }.execute();
    }
}
