package kr.ac.university.localdb;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/** 실행 클래스: 회원가입 창과 비밀번호로 보호된 관리자 창. */
public class MembershipApp {
    private final MemberRepository repository = new MemberRepository();
    private final JFrame home = new JFrame("로그인 / 회원가입");
    private final JTextField email = new UiTheme.TextField(24), name = new UiTheme.TextField(24), phone = new UiTheme.TextField(24);
    private final JPasswordField password = new UiTheme.PasswordField(24), confirm = new UiTheme.PasswordField(24);
    private final JButton signup = new JButton("회원가입"), login = new JButton("로그인");
    private final JTextField loginId = new UiTheme.TextField(24);
    private final JPasswordField loginPassword = new UiTheme.PasswordField(24);
    private final CardLayout pages = new CardLayout();
    private final JPanel pagePanel = new JPanel(pages);
    private final InputHistory history = new InputHistory(Path.of(".local","input-history.properties"));
    private final JButton next = new JButton("회원가입 시작"), back = new JButton("로그인으로 돌아가기");
    private final JLabel guidance = new JLabel("버튼을 눌러 회원가입을 시작하세요.");
    private final JTextField[] inputs = {email,password,confirm,name,phone};
    private final RevealPanel[] steps = new RevealPanel[5];
    private int step=-1;
    private boolean animating;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> { UiTheme.install();new MembershipApp().show(); });
    }

    private void show() {
        home.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel content = new JPanel(new BorderLayout(15,15));
        content.setBorder(BorderFactory.createEmptyBorder(36,36,28,36));
        JPanel form = new JPanel(); form.setLayout(new BoxLayout(form,BoxLayout.Y_AXIS));
        String[] labels={"이메일 (회원 아이디)","비밀번호 (8자 이상)","비밀번호 확인","이름","전화번호"};
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
        signupPanel.add(scroll,BorderLayout.CENTER);
        UiTheme.button(next,true);UiTheme.button(back,false);
        JPanel signupButtons = new JPanel(new GridLayout(0,1,0,10)); signupButtons.add(next); signupButtons.add(back); signupPanel.add(signupButtons,BorderLayout.SOUTH);
        JButton noAccount=new JButton("아직 계정이 없으신가요?");
        JPanel loginPanel=UiTheme.loginPanel(loginId,loginPassword,login,noAccount);
        pagePanel.add(loginPanel,"login"); pagePanel.add(signupPanel,"signup");
        content.add(pagePanel,BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(8,12));
        content.add(bottom,BorderLayout.SOUTH); home.setContentPane(content);
        signup.setEnabled(false); next.setEnabled(false); login.setEnabled(false);
        next.addActionListener(e -> advance()); login.addActionListener(e -> login());
        noAccount.addActionListener(e -> { resetSignup(); pages.show(pagePanel,"signup"); });
        back.addActionListener(e -> { resetSignup(); pages.show(pagePanel,"login"); });
        history.attach(loginId,"identity"); history.attach(email,"identity");
        history.attach(name,"name"); history.attach(phone,"phone");
        loginPassword.addActionListener(e -> { if (login.isEnabled()) login(); });
        home.setSize(510,760); home.setMinimumSize(new Dimension(470,640)); home.setLocationRelativeTo(null); home.setVisible(true);
        run(home,new JButton(),() -> { repository.initialize(); return true; },ok -> {
            signup.setEnabled(true); next.setEnabled(true); login.setEnabled(true);
        });
    }

    private static void field(JPanel form,String label,JComponent component) {
        form.add(new JLabel(label)); form.add(component);
    }

    private void resetSignup() {
        step=-1; animating=false;
        for(RevealPanel panel:steps) panel.reset();
        for(JTextField input:inputs) input.setText("");
        next.setText("회원가입 시작"); next.setEnabled(signup.isEnabled());
        guidance.setText("버튼을 눌러 회원가입을 시작하세요.");
    }

    private void advance() {
        if(animating) return;
        try {
            if(step>=0) validateStep(step);
        } catch(IllegalArgumentException error) {
            AppDialogs.showMessageDialog(home,error.getMessage(),"입력을 확인해 주세요",JOptionPane.WARNING_MESSAGE); inputs[step].requestFocusInWindow(); return;
        }
        if(step==4) { register(); return; }
        step++; animating=true; next.setEnabled(false);
        String[] prompts={"1 / 5 · 사용할 이메일을 알려 주세요","2 / 5 · 안전한 비밀번호를 만들어 주세요","3 / 5 · 비밀번호를 한 번 더 입력해 주세요","4 / 5 · 어떻게 불러 드릴까요?","5 / 5 · 전화번호는 나중에 입력해도 돼요"};
        guidance.setText(prompts[step]);
        next.setText(step==4 ? "가입 완료" : "다음");
        steps[step].reveal(() -> {
            animating=false; next.setEnabled(true); inputs[step].requestFocusInWindow();
            steps[step].scrollRectToVisible(new Rectangle(0,0,steps[step].getWidth(),steps[step].getHeight()));
        });
    }

    private void validateStep(int index) {
        if(index==0) { MemberRepository.validate(email.getText().trim(),"이름",""); }
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

    private void register() {
        char[] secret = password.getPassword(), repeated = confirm.getPassword();
        if (!Arrays.equals(secret,repeated)) {
            Arrays.fill(secret,'\0'); Arrays.fill(repeated,'\0');
            AppDialogs.showMessageDialog(home,"비밀번호가 일치하지 않습니다."); return;
        }
        Arrays.fill(repeated,'\0');
        String mail=email.getText(), person=name.getText(), telephone=phone.getText();
        next.setEnabled(false); back.setEnabled(false);
        run(home,next,() -> {
            try { return repository.register(mail,secret,person,telephone); }
            finally { Arrays.fill(secret,'\0'); }
        }, id -> {
            if (askToSave(true)) {
                history.remember("identity",mail); history.remember("name",person); history.remember("phone",telephone);
            }
            email.setText(""); name.setText(""); phone.setText(""); password.setText(""); confirm.setText("");
            AppDialogs.showMessageDialog(home,"회원 정보가 저장되었습니다.");
            loginId.setText(mail.trim()); resetSignup(); pages.show(pagePanel,"login");
        }, () -> back.setEnabled(true));
    }

    private void login() {
        String username = loginId.getText(); char[] secret = loginPassword.getPassword();
        loginPassword.setText("");
        run(home,login,() -> {
            try { return repository.login(username,secret); }
            finally { Arrays.fill(secret,'\0'); }
        }, result -> {
            if (askToSave(false)) history.remember("identity",username);
            if (result.adminSession()!=null) showAdmin(result.adminSession());
            else AppDialogs.showMessageDialog(home,result.name()+"님, 로그인했습니다.");
        });
    }

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
        Runnable reload = () -> {
            String keyword = search.getText();
            run(window,refresh,() -> session.list(keyword),members -> {
                model.setRowCount(0);
                for (var member : members) model.addRow(new Object[]{member.id(),member.email(),member.name(),member.phone(),member.status(),member.created(),"INACTIVE".equals(member.status()) ? "×" : ""});
                person.setText(""); telephone.setText(""); count.setText("조회된 회원 " + members.size() + "명 · INACTIVE는 탈퇴/비활성 상태");
            });
        };
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
                int row=table.convertRowIndexToModel(viewRow);
                if(!"INACTIVE".equals(model.getValueAt(row,4))) return;
                long id=((Number)model.getValueAt(row,0)).longValue();
                if(AppDialogs.showConfirmDialog(window,model.getValueAt(row,1)+" 회원을 영구 삭제할까요?\n삭제한 회원 정보는 복구할 수 없습니다.",
                        "회원 삭제",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE)!=JOptionPane.YES_OPTION) return;
                run(window,refresh,() -> { session.deleteInactive(id); return true; },ok -> reload.run());
            }
        });
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

    private static <T> void run(Component parent,JButton button,Callable<T> action,Consumer<T> success) {
        run(parent,button,action,success,() -> {});
    }

    private boolean askToSave(boolean signupData) {
        String text="아이디를 저장하시겠습니까?";
        if(signupData) text+="\n동의하면 이름·전화번호도 입력 추천용으로 이 PC에 저장합니다.";
        return AppDialogs.showOptionDialog(home,text,"입력 정보 저장",JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,null,new String[]{"동의","거부"},"거부")==0;
    }

    private static <T> void run(Component parent,JButton button,Callable<T> action,Consumer<T> success,Runnable complete) {
        button.setEnabled(false);
        new SwingWorker<T,Void>() {
            protected T doInBackground() throws Exception { return action.call(); }
            protected void done() {
                button.setEnabled(true);
                try { success.accept(get()); }
                catch (Exception failure) {
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
