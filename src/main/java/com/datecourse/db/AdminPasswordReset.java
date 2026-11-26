package com.datecourse.db;

import com.datecourse.service.AppDiagnostics;
import com.datecourse.auth.PasswordHash;
import com.datecourse.page.common.swing.LoginUI;
import javax.sql.DataSource;
import javax.swing.*;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/** 
- 관리자 패스워드 초기화용 유틸리티용 클래스
- 메인 흐름에는 포함되지 않고, 따로 실행 필요
*/
public final class AdminPasswordReset {
    private AdminPasswordReset() {}

    static void reset(DataSource dataSource,char[] password) throws Exception {
        if(password==null || password.length<8 || password.length>128)
            throw new IllegalArgumentException("비밀번호는 8~128자로 입력해 주세요.");
        String hash=PasswordHash.create(password);
        try(var connection=dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try(var query=connection.prepareStatement("SELECT username FROM app_admins WHERE username=? FOR UPDATE")) {
                    query.setString(1,MemberRepository.ADMIN_USERNAME);
                    try(var row=query.executeQuery()) {
                        if(!row.next())throw new IllegalStateException("관리자 계정이 없어요. 앱 초기 설정을 확인해 주세요.");
                    }
                }
                try(var update=connection.prepareStatement("UPDATE app_admins SET password_hash=? WHERE username=?")) {
                    update.setString(1,hash);update.setString(2,MemberRepository.ADMIN_USERNAME);
                    if(update.executeUpdate()!=1)throw new IllegalStateException("관리자 비밀번호를 변경하지 못했어요.");
                }
                try(var query=connection.prepareStatement("SELECT password_hash FROM app_admins WHERE username=?")) {
                    query.setString(1,MemberRepository.ADMIN_USERNAME);
                    try(var row=query.executeQuery()) {
                        if(!row.next() || !PasswordHash.verify(password,row.getString(1)))
                            throw new IllegalStateException("관리자 인증 정보 확인에 실패했어요.");
                    }
                }
                connection.commit();
            } catch(Exception failure) {
                try{connection.rollback();}catch(Exception rollback){failure.addSuppressed(rollback);}
                throw failure;
            }
        }
    }

    public static void main(String[] args) {
        char[] password=null;
        try {
            var entered=new AtomicReference<char[]>();
            SwingUtilities.invokeAndWait(()->{
                LoginUI.install();entered.set(askPassword());});
            password=entered.get();
            if(password==null){System.out.println("관리자 비밀번호 재설정을 취소했습니다. DB는 변경하지 않았습니다.");return;}
            System.out.println("관리자 비밀번호를 변경하고 있습니다. 비밀번호는 출력하거나 파일에 저장하지 않습니다.");
            try(var pool=DatabasePool.create(DbSettings.load())) {
                reset(pool,password);
                var login=new MemberRepository(pool).login(MemberRepository.ADMIN_USERNAME,password);
                if(login.adminSession()==null)throw new IllegalStateException("관리자 로그인 확인에 실패했어요.");
            }
            Arrays.fill(password,'\0');
            System.out.println("관리자 비밀번호 재설정 완료. 새 비밀번호로 관리자 로그인 검증 성공.");
            SwingUtilities.invokeAndWait(()->JOptionPane.showMessageDialog(null,
                    "관리자 비밀번호를 변경했어요.\n아이디 "+MemberRepository.ADMIN_USERNAME+"와 새 비밀번호로 로그인하세요.",
                    "변경 완료",JOptionPane.INFORMATION_MESSAGE));
        } catch(Exception failure) {
            AppDiagnostics.failure(AppDiagnostics.Operation.ACCOUNT_TASK,AppDiagnostics.Stage.DATABASE_WRITE,failure);
            System.err.println("재설정을 완료하지 못했습니다. 안전한 진단 로그와 DB 연결을 확인하세요.");
            try{SwingUtilities.invokeAndWait(()->JOptionPane.showMessageDialog(null,
                    "비밀번호 변경을 완료하지 못했어요. 연결 상태를 확인한 뒤 다시 시도해 주세요.","변경 실패",JOptionPane.ERROR_MESSAGE));}
            catch(Exception ignored) { /* Console status remains available if the desktop is closing. */ }
        } finally {if(password!=null)Arrays.fill(password,'\0');}
    }

    private static char[] askPassword() {
        for(;;) {
            var first=new JPasswordField(24);var second=new JPasswordField(24);
            var panel=new JPanel(new GridLayout(0,1,0,8));
            panel.add(new JLabel("관리자 "+MemberRepository.ADMIN_USERNAME+" · 새 비밀번호를 입력하세요."));
            panel.add(new JLabel("8~128자 · 비밀번호는 채팅이나 파일에 남지 않아요."));
            panel.add(new JLabel("새 비밀번호"));panel.add(first);
            panel.add(new JLabel("새 비밀번호 확인"));panel.add(second);
            var option=new JOptionPane(panel,JOptionPane.PLAIN_MESSAGE,JOptionPane.OK_CANCEL_OPTION);
            var dialog=option.createDialog("관리자 비밀번호 재설정");
            SwingUtilities.invokeLater(first::requestFocusInWindow);
            dialog.setVisible(true);dialog.dispose();
            Object choice=option.getValue();
            char[] value=first.getPassword(),confirmation=second.getPassword();first.setText("");second.setText("");
            boolean matches=Arrays.equals(value,confirmation);Arrays.fill(confirmation,'\0');
            if(!(choice instanceof Integer code) || code!=JOptionPane.OK_OPTION){Arrays.fill(value,'\0');return null;}
            if(value.length>=8 && value.length<=128 && matches)return value;
            Arrays.fill(value,'\0');
            JOptionPane.showMessageDialog(null,matches?"비밀번호는 8~128자로 입력해 주세요.":"두 비밀번호가 일치하지 않아요.",
                    "입력을 확인해 주세요",JOptionPane.WARNING_MESSAGE);
        }
    }
}
