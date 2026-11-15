package kr.ac.university.localdb.ui;

import com.datecourse.service.AppDiagnostics;
import kr.ac.university.localdb.db.MemberRepository;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.*;

/** One selected member, one draft, one background operation at a time. */
public final class MemberAdminPanel extends JPanel {
    enum Status {
        ACTIVE("정상"), INACTIVE("탈퇴");
        private final String label;
        Status(String label){this.label=label;}
        @Override public String toString(){return label;}
    }
    private final MemberRepository.AdminSession session;
    private final MemberTableModel model=new MemberTableModel();
    private final JTable table=new JTable(model);
    private final JTextField search=new UiTheme.TextField(24),person=new UiTheme.TextField(18),phone=new UiTheme.TextField(18);
    private final JComboBox<Status> memberStatus=new JComboBox<>(Status.values());
    private final JButton refresh=button("검색",false),all=button("전체 보기",false),save=button("변경 저장",true),
            withdraw=button("탈퇴 처리",false),delete=button("영구 삭제",false);
    private final JLabel selection=new JLabel("회원을 선택해 주세요."),count=new JLabel("회원 목록"),message=new JLabel(" ");
    private final BiPredicate<String,String> confirmation;
    private MemberRepository.Member selected;
    private boolean busy,filling;

    public MemberAdminPanel(MemberRepository.AdminSession session){this(session,null);}
    MemberAdminPanel(MemberRepository.AdminSession session,BiPredicate<String,String> confirmation) {
        super(new BorderLayout(16,16));this.session=Objects.requireNonNull(session);
        this.confirmation=confirmation==null?(title,text)->AppDialogs.showConfirmDialog(this,text,title,
                JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE)==JOptionPane.YES_OPTION:confirmation;
        setBorder(BorderFactory.createEmptyBorder(24,24,20,24));setBackground(Color.WHITE);
        search.setName("memberSearch");person.setName("memberName");phone.setName("memberPhone");memberStatus.setName("memberStatus");
        table.setName("memberTable");save.setName("memberSave");withdraw.setName("memberWithdraw");delete.setName("memberDelete");
        refresh.setName("memberSearchButton");all.setName("memberAllButton");message.setName("memberMessage");
        search.setToolTipText("이메일 또는 이름으로 검색");delete.setForeground(new Color(0xB4233F));
        delete.setToolTipText("탈퇴 회원만 삭제할 수 있어요. 저장한 코스가 있으면 탈퇴 상태로 유지해 주세요.");
        for(JTextField field:List.of(search,person,phone))field.setPreferredSize(new Dimension(220,40));
        memberStatus.setPreferredSize(new Dimension(110,40));
        configureTable();
        JPanel searchBar=new JPanel(new BorderLayout(8,0));searchBar.setOpaque(false);searchBar.add(search,BorderLayout.CENTER);searchBar.add(new JLabel("이메일 · 이름"),BorderLayout.WEST);
        JPanel searchActions=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));searchActions.setOpaque(false);
        searchActions.add(refresh);searchActions.add(all);searchBar.add(searchActions,BorderLayout.EAST);
        JPanel heading=new JPanel(new BorderLayout(0,12));heading.setOpaque(false);
        var title=UiTheme.heading("회원 관리","회원을 선택한 뒤 변경 내용을 확인하고 저장하세요.");title.setOpaque(false);
        heading.add(title,BorderLayout.NORTH);
        heading.add(searchBar,BorderLayout.CENTER);heading.add(count,BorderLayout.SOUTH);count.setForeground(UiTheme.MUTED);
        add(heading,BorderLayout.NORTH);
        JScrollPane scroll=new JScrollPane(table);scroll.setColumnHeaderView(table.getTableHeader());scroll.setBorder(BorderFactory.createLineBorder(new Color(0xE5E8EB)));add(scroll,BorderLayout.CENTER);
        JPanel fields=new JPanel(new GridBagLayout());fields.setOpaque(false);
        addField(fields,"이름",person,0,1);addField(fields,"전화번호",phone,1,1);addField(fields,"상태",memberStatus,2,0);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));actions.setOpaque(false);actions.add(save);actions.add(withdraw);actions.add(delete);
        JPanel editor=new JPanel(new BorderLayout(0,10));editor.setOpaque(false);selection.setFont(UiTheme.BODY.deriveFont(Font.BOLD,15f));
        editor.add(selection,BorderLayout.NORTH);editor.add(fields,BorderLayout.CENTER);editor.add(actions,BorderLayout.SOUTH);
        JPanel bottom=new JPanel(new BorderLayout(0,10));bottom.setOpaque(false);bottom.add(editor,BorderLayout.CENTER);bottom.add(message,BorderLayout.SOUTH);add(bottom,BorderLayout.SOUTH);
        table.getSelectionModel().addListSelectionListener(event->{if(!event.getValueIsAdjusting())selectionChanged();});
        DocumentListener edited=new DocumentListener(){public void insertUpdate(DocumentEvent e){updateControls();}public void removeUpdate(DocumentEvent e){updateControls();}public void changedUpdate(DocumentEvent e){updateControls();}};
        person.getDocument().addDocumentListener(edited);phone.getDocument().addDocumentListener(edited);memberStatus.addActionListener(e->updateControls());
        refresh.addActionListener(e->search());search.addActionListener(e->search());
        all.addActionListener(e->{if(busy||!discardDraft())return;search.setText("");load(null,"전체 회원을 불러왔어요.");});
        save.addActionListener(e->save());withdraw.addActionListener(e->withdraw());delete.addActionListener(e->delete());
        updateControls();load(null,"회원을 선택해 주세요.");
    }
    private static JButton button(String text,boolean primary) {
        var button=new JButton(text);UiTheme.button(button,primary);button.setFont(UiTheme.BODY.deriveFont(primary?Font.BOLD:Font.PLAIN,14f));
        button.setPreferredSize(new Dimension(button.getFontMetrics(button.getFont()).stringWidth(text)+32,40));
        button.setBorder(BorderFactory.createEmptyBorder(8,16,8,16));return button;
    }
    private static void addField(JPanel panel,String label,JComponent input,int column,double weight) {
        var c=new GridBagConstraints();c.gridx=column;c.gridy=0;c.weightx=weight;c.fill=GridBagConstraints.HORIZONTAL;c.anchor=GridBagConstraints.WEST;
        c.insets=new Insets(0,0,6,column==2?0:12);var caption=new JLabel(label);caption.setForeground(UiTheme.MUTED);panel.add(caption,c);
        c.gridy=1;c.insets=new Insets(0,0,0,column==2?0:12);panel.add(input,c);
    }
    private void configureTable() {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);table.setAutoCreateRowSorter(true);table.setFillsViewportHeight(true);
        table.setRowHeight(44);table.setShowGrid(false);table.setIntercellSpacing(new Dimension(0,0));table.setBackground(Color.WHITE);
        table.getTableHeader().setReorderingAllowed(false);table.getTableHeader().setPreferredSize(new Dimension(0,40));
        table.getTableHeader().setFont(UiTheme.BODY.deriveFont(Font.BOLD,13f));
        int[] widths={150,280,160,85,175};int[] minimums={100,180,120,70,145};
        for(int i=0;i<widths.length;i++){var column=table.getColumnModel().getColumn(i);column.setPreferredWidth(widths[i]);column.setMinWidth(minimums[i]);}
        table.setDefaultRenderer(Object.class,new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable grid,Object value,boolean chosen,boolean focus,int row,int column) {
                super.getTableCellRendererComponent(grid,value,chosen,focus,row,column);
                setBorder(BorderFactory.createEmptyBorder(0,12,0,12));setFont(UiTheme.BODY.deriveFont(column==0?Font.BOLD:Font.PLAIN,14f));
                setBackground(chosen?UiTheme.SOFT:row%2==0?Color.WHITE:new Color(0xF7F8FA));setForeground(UiTheme.TEXT);
                setHorizontalAlignment(column==3?SwingConstants.CENTER:SwingConstants.LEFT);
                if(column==3)setForeground("정상".equals(value)?new Color(0x28764A):UiTheme.MUTED);
                setToolTipText(value==null?null:value.toString());return this;
            }
        });
    }
    private void selectionChanged() {
        if(filling||busy)return;
        int row=table.getSelectedRow();var next=row<0?null:model.rows.get(table.convertRowIndexToModel(row));
        if(selected!=null && next!=null && selected.id()==next.id())return;
        if(!discardDraft()){restoreSelection();return;}
        selected=next;fillEditor();
    }
    private void restoreSelection() {
        filling=true;
        try {int row=selected==null?-1:model.indexOf(selected.id());if(row>=0)table.setRowSelectionInterval(table.convertRowIndexToView(row),table.convertRowIndexToView(row));else table.clearSelection();}
        finally{filling=false;}
    }
    private void fillEditor() {
        filling=true;
        try {
            person.setText(selected==null?"":selected.name());phone.setText(selected==null?"":Objects.toString(selected.phone(),""));
            memberStatus.setSelectedItem(selected==null?Status.ACTIVE:Status.valueOf(selected.status()));
            selection.setText(selected==null?"회원을 선택해 주세요.":selected.name()+" · "+selected.email());
        } finally{filling=false;}
        updateControls();
    }
    private boolean dirty() {
        return selected!=null && (!person.getText().trim().equals(selected.name())
                || !phone.getText().trim().equals(Objects.toString(selected.phone(),""))
                || memberStatus.getSelectedItem()!=Status.valueOf(selected.status()));
    }
    private boolean discardDraft() {return !dirty() || confirmation.test("변경 내용 확인","저장하지 않은 회원 정보 변경을 버릴까요?");}
    public boolean confirmLeave() {
        if(busy){notice("진행 중인 작업이 끝난 뒤 다시 시도해 주세요.",false);return false;}
        return discardDraft();
    }
    private void search(){if(!busy && discardDraft())load(null,"검색 결과를 확인해 주세요.");}
    private void load(Long keep,String done) {
        String keyword=search.getText();task("회원 목록을 불러오고 있어요…",()->session.list(keyword),rows->showRows(rows,keep,done));
    }
    private void showRows(List<MemberRepository.Member> rows,Long keep,String done) {
        filling=true;
        try {
            model.rows=List.copyOf(rows);model.fireTableDataChanged();table.clearSelection();selected=null;
            if(keep!=null){int index=model.indexOf(keep);if(index>=0){selected=model.rows.get(index);int view=table.convertRowIndexToView(index);table.setRowSelectionInterval(view,view);}}
            count.setText("조회된 회원 "+rows.size()+"명");fillEditor();
        } finally{filling=false;}
        notice(done,false);
    }
    private void save() {
        if(busy||selected==null||!dirty())return;
        String name=person.getText().trim(),telephone=phone.getText().trim();Status state=(Status)memberStatus.getSelectedItem();
        if(name.isEmpty()||name.length()>100||telephone.length()>30){notice("이름은 1~100자, 전화번호는 30자 이내로 입력해 주세요.",true);return;}
        String changes="대상: "+selected.name()+" ("+selected.email()+")\n";
        if(!name.equals(selected.name()))changes+="이름: "+selected.name()+" → "+name+"\n";
        if(!telephone.equals(Objects.toString(selected.phone(),"")))changes+="전화번호: "+Objects.toString(selected.phone(),"없음")+" → "+telephone+"\n";
        if(!state.name().equals(selected.status()))changes+="상태: "+Status.valueOf(selected.status())+" → "+state+"\n";
        if(!confirmation.test("회원 정보 수정",changes+"위 내용으로 변경할까요?"))return;
        var expected=selected;
        change(()->{session.update(expected,name,telephone,state.name());return null;},expected.id(),"회원 정보를 변경했어요.");
    }
    private void withdraw() {
        if(busy||selected==null||!"ACTIVE".equals(selected.status()))return;
        if(dirty()){notice("작성한 변경 내용을 먼저 저장하거나 회원을 다시 선택해 주세요.",true);return;}
        var expected=selected;
        if(!confirmation.test("회원 탈퇴 처리",expected.name()+" ("+expected.email()+") 회원을 탈퇴 처리할까요?\n회원 정보와 코스는 남고, 로그인은 제한돼요."))return;
        change(()->{session.changeStatus(expected,"INACTIVE");return null;},expected.id(),"탈퇴 처리했어요. 회원 정보와 코스는 유지돼요.");
    }
    private void delete() {
        if(busy||selected==null||!"INACTIVE".equals(selected.status()))return;
        if(dirty()){notice("작성한 변경 내용을 먼저 저장하거나 회원을 다시 선택해 주세요.",true);return;}
        var expected=selected;
        task("삭제 가능 여부를 확인하고 있어요…",()->session.deletionRestriction(expected),reason->{
            if(!reason.isEmpty()){notice(reason,true);return;}
            // Start the write only after this read has released the busy state.
            SwingUtilities.invokeLater(()->{
                if(!confirmation.test("회원 영구 삭제",expected.name()+" ("+expected.email()+") 회원을 영구 삭제할까요?\n삭제한 회원 정보는 복구할 수 없어요.")){notice("삭제를 취소했어요.",false);return;}
                change(()->{session.deleteInactive(expected);return null;},null,"회원을 영구 삭제했어요.");
            });
        });
    }
    private void change(Callable<Void> write,Long keep,String done) {
        String keyword=search.getText();
        task("변경 내용을 처리하고 있어요…",()->{
            write.call();
            try{return session.list(keyword);}
            catch(java.sql.SQLException failure){
                AppDiagnostics.failure(AppDiagnostics.Operation.ACCOUNT_TASK,AppDiagnostics.Stage.DATABASE_READ,failure);
                return null;
            }
        },rows->showRows(rows==null?List.of():rows,keep,done+(rows==null?" 목록을 불러오지 못했어요. 다시 검색해 주세요.":"")));
    }
    private <T> void task(String pending,Callable<T> work,Consumer<T> success) {
        if(busy)return;busy=true;updateControls();notice(pending,false);
        new SwingWorker<T,Void>() {
            @Override protected T doInBackground()throws Exception{return work.call();}
            @Override protected void done() {
                try{success.accept(get());}
                catch(Exception failure){AppDiagnostics.failure(AppDiagnostics.Operation.ACCOUNT_TASK,AppDiagnostics.Stage.RUN,failure);
                    Throwable cause=AppDiagnostics.root(failure);notice(cause instanceof IllegalArgumentException?cause.getMessage():"처리하지 못했어요. 잠시 후 다시 시도해 주세요.",true);}
                finally{busy=false;updateControls();}
            }
        }.execute();
    }
    private void notice(String text,boolean error){message.setText(text);message.setForeground(error?new Color(0xB4233F):UiTheme.MUTED);}
    private void updateControls() {
        boolean editable=selected!=null&&!busy;
        for(JComponent control:List.of(search,refresh,all,table,table.getTableHeader()))control.setEnabled(!busy);
        person.setEnabled(editable);phone.setEnabled(editable);memberStatus.setEnabled(editable);
        save.setEnabled(editable&&dirty());withdraw.setEnabled(editable&&!dirty()&&"ACTIVE".equals(selected.status()));
        delete.setEnabled(editable&&!dirty()&&"INACTIVE".equals(selected.status()));
    }
    private static final class MemberTableModel extends AbstractTableModel {
        private final String[] columns={"이름","이메일","전화번호","상태","가입일"};
        private List<MemberRepository.Member> rows=List.of();
        int indexOf(long id){for(int i=0;i<rows.size();i++)if(rows.get(i).id()==id)return i;return -1;}
        @Override public int getRowCount(){return rows.size();}
        @Override public int getColumnCount(){return columns.length;}
        @Override public String getColumnName(int column){return columns[column];}
        @Override public Class<?> getColumnClass(int column){return String.class;}
        @Override public Object getValueAt(int row,int column) {
            var member=rows.get(row);
            return switch(column){case 0->member.name();case 1->member.email();case 2->Objects.toString(member.phone(),"");case 3->Status.valueOf(member.status()).toString();default->member.created()==null?"":member.created().replace('T',' ').substring(0,Math.min(16,member.created().length()));};
        }
    }
}
