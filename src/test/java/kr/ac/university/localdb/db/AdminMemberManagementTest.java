package kr.ac.university.localdb.db;

import com.zaxxer.hikari.HikariDataSource;
import kr.ac.university.localdb.PasswordHash;
import kr.ac.university.localdb.ui.MemberAdminPanel;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.*;
import javax.swing.*;
import java.awt.*;
import java.nio.file.*;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.BiPredicate;
import static org.junit.jupiter.api.Assertions.*;

class AdminMemberManagementTest {
    HikariDataSource pool;
    MemberRepository repository;
    MemberRepository.AdminSession admin;
    @BeforeEach void setup() throws Exception {
        pool=DatabasePool.create(new DbSettings("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL","sa",""));
        repository=new MemberRepository(pool);
        char[] fixture="test-only-password".toCharArray();
        try(var c=pool.getConnection()) {
            for(String file:new String[]{"02_create_tables.sql","07_saved_courses.sql"})
                try(var reader=Files.newBufferedReader(Path.of("data/sql",file))){RunScript.execute(c,reader);}
            try(var s=c.createStatement()) {
                s.execute("CREATE TABLE app_admins(username VARCHAR(50) PRIMARY KEY,password_hash VARCHAR(255))");
                s.execute("INSERT INTO members(member_id,email,password_hash,name,phone,status) VALUES"
                        +"(7,'alice@example.test','unused','김하늘','010-1234-5678','ACTIVE'),"
                        +"(8,'bob@example.test','unused','이바다',NULL,'INACTIVE'),"
                        +"(9,'carol@example.test','unused','박노을','010-9876-5432','ACTIVE')");
            }
            try(var s=c.prepareStatement("INSERT INTO app_admins VALUES (?,?)")) {
                s.setString(1,MemberRepository.ADMIN_USERNAME);s.setString(2,PasswordHash.create(fixture));s.executeUpdate();
            }
        }
        admin=repository.login(MemberRepository.ADMIN_USERNAME,fixture).adminSession();
    }
    @AfterEach void close(){repository.close();}
    MemberRepository.Member member(long id)throws Exception{return admin.list("").stream().filter(m->m.id()==id).findFirst().orElseThrow();}

    @Test void staleChangesCannotOverwriteNewerDataAndWithdrawalOnlyChangesStatus()throws Exception {
        var old=member(7);
        admin.update(old,"변경된 이름","새 전화번호","ACTIVE");
        assertThrows(IllegalArgumentException.class,()->admin.changeStatus(old,"INACTIVE"));
        assertThrows(IllegalArgumentException.class,()->admin.update(old,"이전 이름","이전 번호","ACTIVE"));
        var latest=member(7);admin.changeStatus(latest,"INACTIVE");
        assertEquals("변경된 이름",member(7).name());assertEquals("새 전화번호",member(7).phone());
        assertEquals("INACTIVE",member(7).status());
        assertThrows(IllegalArgumentException.class,()->admin.deleteInactive(latest));
    }
    @Test void deletionRetainsSavedCoursesAndRequiresInactiveLatestMember()throws Exception {
        assertFalse(admin.deletionRestriction(member(7)).isEmpty());
        assertThrows(IllegalArgumentException.class,()->admin.deleteInactive(member(7)));
        try(var c=pool.getConnection();var s=c.createStatement()) {
            s.executeUpdate("INSERT INTO saved_courses(member_id,start_lat,start_lng,people,radius_m) VALUES(8,35,129,2,1000)");
        }
        assertTrue(admin.deletionRestriction(member(8)).contains("코스"));
        assertThrows(IllegalArgumentException.class,()->admin.deleteInactive(member(8)));
        assertEquals(3,admin.list("").size());
        admin.changeStatus(member(9),"INACTIVE");admin.deleteInactive(member(9));
        assertEquals(2,admin.list("").size());
    }
    @Test void tableLabelsConfirmationsDraftSelectionAndDeleteWorkThroughActualButtons()throws Exception {
        AtomicBoolean allow=new AtomicBoolean(false);AtomicInteger confirms=new AtomicInteger();AtomicReference<String> prompt=new AtomicReference<>();
        MemberAdminPanel panel=edt(()->{
            var constructor=MemberAdminPanel.class.getDeclaredConstructor(MemberRepository.AdminSession.class,BiPredicate.class);
            constructor.setAccessible(true);
            return constructor.newInstance(admin,(BiPredicate<String,String>)(title,message)->{confirms.incrementAndGet();prompt.set(message);return allow.get();});
        });
        await(()->find(panel,"memberTable",JTable.class).isEnabled()&&find(panel,"memberTable",JTable.class).getRowCount()==3);
        edt(()->{
            var table=find(panel,"memberTable",JTable.class);
            assertEquals("이메일",table.getColumnName(1));
            for(int row=0;row<3;row++)assertTrue(java.util.Set.of("정상","탈퇴").contains(table.getValueAt(row,3)));
            assertFalse(find(panel,"memberSave",JButton.class).isEnabled());
            select(table,"김하늘");find(panel,"memberName",JTextField.class).setText("김하늘 수정");
            find(panel,"memberSave",JButton.class).doClick();return null;
        });
        assertEquals("김하늘",member(7).name());assertTrue(prompt.get().contains("김하늘 → 김하늘 수정"));
        edt(()->{
            select(find(panel,"memberTable",JTable.class),"이바다");
            assertEquals("김하늘 수정",find(panel,"memberName",JTextField.class).getText());
            assertEquals("김하늘",find(panel,"memberTable",JTable.class).getValueAt(find(panel,"memberTable",JTable.class).getSelectedRow(),0));
            allow.set(true);find(panel,"memberSave",JButton.class).doClick();
            assertFalse(find(panel,"memberTable",JTable.class).isEnabled());assertFalse(panel.confirmLeave());return null;
        });
        await(()->find(panel,"memberTable",JTable.class).isEnabled()&&!find(panel,"memberSave",JButton.class).isEnabled());
        assertEquals("김하늘 수정",member(7).name());
        edt(()->{allow.set(false);find(panel,"memberWithdraw",JButton.class).doClick();return null;});
        assertEquals("ACTIVE",member(7).status());
        edt(()->{allow.set(true);find(panel,"memberWithdraw",JButton.class).doClick();return null;});
        await(()->find(panel,"memberTable",JTable.class).isEnabled()&&find(panel,"memberDelete",JButton.class).isEnabled());
        assertEquals("INACTIVE",member(7).status());
        int before=confirms.get();
        edt(()->{allow.set(false);find(panel,"memberDelete",JButton.class).doClick();return null;});
        await(()->confirms.get()>before&&find(panel,"memberTable",JTable.class).isEnabled());
        assertEquals(3,admin.list("").size());
        edt(()->{allow.set(true);find(panel,"memberDelete",JButton.class).doClick();return null;});
        await(()->find(panel,"memberTable",JTable.class).isEnabled()&&find(panel,"memberTable",JTable.class).getRowCount()==2);
        assertEquals(2,admin.list("").size());
        edt(()->{
            select(find(panel,"memberTable",JTable.class),"박노을");
            panel.setSize(1150,700);layout(panel);
            var image=new java.awt.image.BufferedImage(1150,700,java.awt.image.BufferedImage.TYPE_INT_RGB);
            var graphics=image.createGraphics();panel.printAll(graphics);graphics.dispose();
            var file=Path.of("build/ui-preview/admin-members.png");Files.createDirectories(file.getParent());
            javax.imageio.ImageIO.write(image,"png",file.toFile());return null;
        });
    }
    static void select(JTable table,String name){for(int row=0;row<table.getRowCount();row++)if(name.equals(table.getValueAt(row,0))){table.setRowSelectionInterval(row,row);return;}fail("Member missing");}
    static void layout(Container parent){parent.doLayout();for(Component child:parent.getComponents())if(child instanceof Container c)layout(c);}
    static <T extends Component>T find(Container parent,String name,Class<T> type){
        for(Component child:parent.getComponents()){
            if(name.equals(child.getName()))return type.cast(child);
            if(child instanceof Container c){T found=find(c,name,type);if(found!=null)return found;}
        }return null;
    }
    static <T>T edt(Callable<T> work)throws Exception{var task=new FutureTask<T>(work);SwingUtilities.invokeLater(task);return task.get(10,TimeUnit.SECONDS);}
    static void await(Callable<Boolean> condition)throws Exception {
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
        while(System.nanoTime()<end){if(edt(condition))return;Thread.sleep(20);}fail("UI operation timed out");
    }
}
