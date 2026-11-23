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

/**
 * 관리자용 회원 관리 화면: 검색 → 선택 → 수정/탈퇴/영구 삭제.
 * One selected member, one draft, one background operation at a time.
 */
public final class MemberAdminPanel extends JPanel {

    // ===== 회원 상태(화면에는 한글 라벨로 표시) =====
    enum Status {
        ACTIVE("정상"), INACTIVE("탈퇴");
        private final String label;

        Status(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    // ===== 필드: 데이터·표 =====
    private final MemberRepository.AdminSession session;
    private final MemberTableModel model = new MemberTableModel();
    private final JTable table = new JTable(model);
    private final BiPredicate<String, String> confirmation;   // 확인창(제목, 내용) → 예/아니오

    // ===== 필드: 입력·버튼·안내 문구 =====
    private final JTextField search = new LoginUI.TextField(24), person = new LoginUI.TextField(18), phone = new LoginUI.TextField(18);
    private final JComboBox<Status> memberStatus = new JComboBox<>(Status.values());
    private final JButton refresh = button("검색", false), all = button("전체 보기", false), save = button("변경 저장", true),
            withdraw = button("탈퇴 처리", false), delete = button("영구 삭제", false);
    private final JLabel selection = new JLabel("회원을 선택해 주세요."), count = new JLabel("회원 목록"), message = new JLabel(" ");

    // ===== 필드: 상태 =====
    private MemberRepository.Member selected;   // 현재 선택된 회원
    private boolean busy, filling;              // busy: 작업 중 / filling: 화면을 채우는 중(이벤트 무시)

    // ===== 생성자 =====

    /** 실제 사용용: 확인창은 기본 대화상자를 사용합니다. */
    public MemberAdminPanel(MemberRepository.AdminSession session) {
        this(session, null);
    }

    /** 화면 구성(제목·검색줄·표·편집 영역)과 이벤트 연결을 한 번에 처리합니다. 테스트에서는 confirmation을 바꿔 끼울 수 있습니다. */
    MemberAdminPanel(MemberRepository.AdminSession session, BiPredicate<String, String> confirmation) {
        super(new BorderLayout(12, 12));
        this.session = Objects.requireNonNull(session); // [변경] 간격 16 → 12
        this.confirmation = confirmation == null ? (title, text) -> AppDialogs.showConfirmDialog(this, text, title,
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION : confirmation;
        setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
        setBackground(AdminStyle.BG); // [변경] 여백 24,24,20,24 → 16,24,16,24 / 배경 흰색 → 베이지
        search.setName("memberSearch");
        person.setName("memberName");
        phone.setName("memberPhone");
        memberStatus.setName("memberStatus");
        table.setName("memberTable");
        save.setName("memberSave");
        withdraw.setName("memberWithdraw");
        delete.setName("memberDelete");
        refresh.setName("memberSearchButton");
        all.setName("memberAllButton");
        message.setName("memberMessage");
        search.setToolTipText("이메일 또는 이름으로 검색");
        delete.setForeground(new Color(0xB4233F));
        delete.setToolTipText("탈퇴 회원만 삭제할 수 있어요. 저장한 코스가 있으면 탈퇴 상태로 유지해 주세요.");
        for (JTextField field : List.of(search, person, phone))
            field.setPreferredSize(new Dimension(220, 40)); // [수정] 높이 36 → 40 (글자 잘림 방지)
        memberStatus.setPreferredSize(new Dimension(110, 40)); // [수정] 높이 36 → 40 (글자 잘림 방지)
        AdminStyle.combo(memberStatus); // [변경] 추가: 콤보박스 스타일
        configureTable();
        JPanel searchBar = new JPanel(new BorderLayout(8, 0));
        searchBar.setOpaque(false);
        searchBar.add(search, BorderLayout.CENTER);
        searchBar.add(new JLabel("이메일 · 이름"), BorderLayout.WEST);
        searchBar.add(row(refresh, all), BorderLayout.EAST); // [변경] FlowLayout 패널 3줄 → row() 한 줄
        JPanel heading = new JPanel(new BorderLayout(0, 12));
        heading.setOpaque(false);
        // [변경] 큰 제목(LoginUI.heading) → 작은 제목 + 설명 2줄
        var titleText = new JLabel("회원 관리");
        titleText.setFont(LoginUI.BODY.deriveFont(Font.BOLD, 22f));
        titleText.setForeground(AdminStyle.DARK);
        var subText = new JLabel("회원을 선택한 뒤 변경 내용을 확인하고 저장하세요.");
        subText.setFont(LoginUI.BODY.deriveFont(13f));
        subText.setForeground(LoginUI.MUTED);
        JPanel title = new JPanel(new GridLayout(0, 1, 0, 2));
        title.setOpaque(false);
        title.add(titleText);
        title.add(subText);
        heading.add(title, BorderLayout.NORTH);
        heading.add(searchBar, BorderLayout.CENTER);
        heading.add(count, BorderLayout.SOUTH);
        count.setForeground(LoginUI.MUTED);
        add(heading, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setColumnHeaderView(table.getTableHeader());
        AdminStyle.scroll(scroll);
        AdminStyle.table(table);
        add(scroll, BorderLayout.CENTER); // [변경] 회색 테두리 → AdminStyle 적용
        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        addField(fields, "이름", person, 0, 1.2);
        addField(fields, "전화번호", phone, 1, 1);
        addField(fields, "상태", memberStatus, 2, 0);
        JPanel actions = row(save, withdraw, delete); // [변경] FlowLayout 패널 → row()
        JPanel editor = new JPanel(new BorderLayout(0, 10));
        editor.setOpaque(false);
        selection.setFont(LoginUI.BODY.deriveFont(Font.BOLD, 15f));
        editor.add(selection, BorderLayout.NORTH);
        editor.add(fields, BorderLayout.CENTER);
        editor.add(actions, BorderLayout.SOUTH);
        JPanel bottom = new JPanel(new BorderLayout(0, 10));
        bottom.setOpaque(false);
        bottom.add(editor, BorderLayout.CENTER);
        bottom.add(message, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) selectionChanged();
        });
        DocumentListener edited = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                updateControls();
            }

            public void removeUpdate(DocumentEvent e) {
                updateControls();
            }

            public void changedUpdate(DocumentEvent e) {
                updateControls();
            }
        };
        person.getDocument().addDocumentListener(edited);
        phone.getDocument().addDocumentListener(edited);
        memberStatus.addActionListener(e -> updateControls());
        refresh.addActionListener(e -> search());
        search.addActionListener(e -> search());
        all.addActionListener(e -> {
            if (busy || !discardDraft()) return;
            search.setText("");
            load("전체 회원을 불러왔어요.");
        });
        save.addActionListener(e -> save());
        withdraw.addActionListener(e -> withdraw());
        delete.addActionListener(e -> delete());
        updateControls();
        load(" "); // [변경] "회원을 선택해 주세요." → " " (위쪽 문구와 중복 방지)
    }

    // ===== 외부에서 호출하는 메서드 =====

    /** 관리자 화면을 떠나도 되는지 확인합니다(작업 중이면 거절, 수정 중이면 버릴지 질문). */
    public boolean confirmLeave() {
        if (busy) {
            notice("진행 중인 작업이 끝난 뒤 다시 시도해 주세요.", false);
            return false;
        }
        return discardDraft();
    }

    // ===== 화면 구성 도우미 =====

    /** 공통 스타일의 버튼을 만들고 글자 길이에 맞게 너비를 정합니다. */
    private static JButton button(String text, boolean primary) {
        var button = new JButton(text);
        LoginUI.button(button, primary);
        button.setFont(LoginUI.BODY.deriveFont(primary ? Font.BOLD : Font.PLAIN, 13f)); // [변경] 글자 14 → 13
        button.setPreferredSize(new Dimension(button.getFontMetrics(button.getFont()).stringWidth(text) + 28, 34)); // [수정] 높이 32 → 34
        button.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        return button; // [수정] 안쪽 여백 위아래 4 → 6
    }

    /**
     * [추가] 버튼을 가로로 나란히 놓습니다. FlowLayout과 달리 가장자리에는 간격이 생기지 않습니다.
     */
    private static JPanel row(JButton... buttons) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        for (int i = 0; i < buttons.length; i++) {
            if (i > 0) panel.add(Box.createHorizontalStrut(8));
            panel.add(buttons[i]);
        }
        return panel;
    }

    /** 편집 영역에 "라벨 위, 입력칸 아래" 한 묶음을 지정한 열(column)에 추가합니다. */
    private static void addField(JPanel panel, String label, JComponent input, int column, double weight) {
        var c = new GridBagConstraints();
        c.gridx = column;
        c.gridy = 0;
        c.weightx = weight;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 6, column == 2 ? 0 : 12);
        var caption = new JLabel(label);
        caption.setForeground(LoginUI.MUTED);
        panel.add(caption, c);
        c.gridy = 1;
        c.insets = new Insets(0, 0, 0, column == 2 ? 0 : 12);
        panel.add(input, c);
    }

    /** 표의 모양(행 높이·열 너비·줄무늬·선택색)을 설정합니다. */
    private void configureTable() {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.setRowHeight(40); // [수정] 행 높이 38 → 40
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setBackground(Color.WHITE);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(0, 40));
        table.getTableHeader().setFont(LoginUI.BODY.deriveFont(Font.BOLD, 13f));
        int[] widths = {150, 280, 160, 85, 175};
        int[] minimums = {100, 180, 120, 70, 145};
        for (int i = 0; i < widths.length; i++) {
            var column = table.getColumnModel().getColumn(i);
            column.setPreferredWidth(widths[i]);
            column.setMinWidth(minimums[i]);
        }
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable grid, Object value, boolean chosen, boolean focus, int row, int column) {
                super.getTableCellRendererComponent(grid, value, chosen, focus, row, column);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                setFont(LoginUI.BODY.deriveFont(column == 0 ? Font.BOLD : Font.PLAIN, 14f));
                setBackground(chosen ? AdminStyle.BEIGE : row % 2 == 0 ? Color.WHITE : AdminStyle.STRIPE);
                setForeground(LoginUI.TEXT); // [변경] 선택색·줄무늬를 베이지 계열로
                setHorizontalAlignment(column == 3 ? SwingConstants.CENTER : SwingConstants.LEFT);
                if (column == 3) setForeground("정상".equals(value) ? new Color(0x28764A) : LoginUI.MUTED);
                setToolTipText(value == null ? null : value.toString());
                return this;
            }
        });
    }

    // ===== 선택·편집 상태 관리 =====

    /** 표에서 다른 회원을 고르면, 수정 중인 내용을 확인한 뒤 편집 영역을 새로 채웁니다. */
    private void selectionChanged() {
        if (filling || busy) return;
        int row = table.getSelectedRow();
        var next = row < 0 ? null : model.rows.get(table.convertRowIndexToModel(row));
        if (selected != null && next != null && selected.id() == next.id()) return;
        if (!discardDraft()) {
            restoreSelection();
            return;
        }
        selected = next;
        fillEditor();
    }

    /** 선택 변경을 취소했을 때 표의 선택을 원래 회원으로 되돌립니다. */
    private void restoreSelection() {
        filling = true;
        try {
            int row = selected == null ? -1 : model.indexOf(selected.id());
            if (row >= 0)
                table.setRowSelectionInterval(table.convertRowIndexToView(row), table.convertRowIndexToView(row));
            else table.clearSelection();
        } finally {
            filling = false;
        }
    }

    /** 선택된 회원의 정보를 편집 영역(이름·전화번호·상태)에 채웁니다. */
    private void fillEditor() {
        filling = true;
        try {
            person.setText(selected == null ? "" : selected.name());
            phone.setText(selected == null ? "" : Objects.toString(selected.phone(), ""));
            memberStatus.setSelectedItem(selected == null ? Status.ACTIVE : Status.valueOf(selected.status()));
            selection.setText(selected == null ? "회원을 선택해 주세요." : selected.name() + " · " + selected.email());
        } finally {
            filling = false;
        }
        updateControls();
    }

    /** 입력값이 선택된 회원의 원래 값과 다른지(저장하지 않은 변경이 있는지) 확인합니다. */
    private boolean dirty() {
        return selected != null && (!person.getText().trim().equals(selected.name())
                || !phone.getText().trim().equals(Objects.toString(selected.phone(), ""))
                || memberStatus.getSelectedItem() != Status.valueOf(selected.status()));
    }

    /** 저장하지 않은 변경이 없거나, 있어도 사용자가 버리겠다고 하면 true. */
    private boolean discardDraft() {
        return !dirty() || confirmation.test("변경 내용 확인", "저장하지 않은 회원 정보 변경을 버릴까요?");
    }

    // ===== 조회(검색·목록 표시) =====

    /** 검색 버튼/Enter: 작업 중이 아니고 변경을 버려도 될 때 목록을 다시 불러옵니다. */
    private void search() {
        if (!busy && discardDraft()) load("검색 결과를 확인해 주세요.");
    }

    /** 검색어로 회원 목록을 백그라운드에서 조회한 뒤 표에 보여줍니다. */
    private void load(String done) {
        String keyword = search.getText();
        task("회원 목록을 불러오고 있어요…", () -> session.list(keyword), rows -> showRows(rows, done));
    }

    /** 목록을 표에 표시합니다(선택 유지 없음). */
    private void showRows(List<MemberRepository.Member> rows, String done) {
        showRows(rows, null, done);
    }

    /** 목록을 표에 표시하고, keep으로 받은 id의 회원이 있으면 다시 선택해 줍니다. */
    private void showRows(List<MemberRepository.Member> rows, Long keep, String done) {
        filling = true;
        try {
            model.rows = List.copyOf(rows);
            model.fireTableDataChanged();
            table.clearSelection();
            selected = null;
            if (keep != null) {
                int index = model.indexOf(keep);
                if (index >= 0) {
                    selected = model.rows.get(index);
                    int view = table.convertRowIndexToView(index);
                    table.setRowSelectionInterval(view, view);
                }
            }
            count.setText("조회된 회원 " + rows.size() + "명");
            fillEditor();
        } finally {
            filling = false;
        }
        notice(done, false);
    }

    // ===== 변경 작업(저장·탈퇴·삭제) =====

    /** 변경 저장: 입력 검사 → 변경 내용 확인창 → 수정 후 목록 새로고침. */
    private void save() {
        if (busy || selected == null || !dirty()) return;
        String name = person.getText().trim(), telephone = phone.getText().trim();
        Status state = (Status) memberStatus.getSelectedItem();
        if (state == null) {
            notice("상태를 선택해 주세요.", true);
            return;
        }
        if (name.isEmpty() || name.length() > 100 || telephone.length() > 30) {
            notice("이름은 1~100자, 전화번호는 30자 이내로 입력해 주세요.", true);
            return;
        }
        String changes = "대상: " + selected.name() + " (" + selected.email() + ")\n";
        if (!name.equals(selected.name())) changes += "이름: " + selected.name() + " → " + name + "\n";
        if (!telephone.equals(Objects.toString(selected.phone(), "")))
            changes += "전화번호: " + Objects.toString(selected.phone(), "없음") + " → " + telephone + "\n";
        if (!state.name().equals(selected.status()))
            changes += "상태: " + Status.valueOf(selected.status()) + " → " + state + "\n";
        if (!confirmation.test("회원 정보 수정", changes + "위 내용으로 변경할까요?")) return;
        var expected = selected;
        change(() -> {
            session.update(expected, name, telephone, state.name());
            return null;
        }, expected.id(), "회원 정보를 변경했어요.");
    }

    /** 탈퇴 처리: 정상 회원만 가능하며, 정보와 코스는 남기고 로그인만 제한합니다. */
    private void withdraw() {
        if (busy || selected == null || !"ACTIVE".equals(selected.status())) return;
        if (dirty()) {
            notice("작성한 변경 내용을 먼저 저장하거나 회원을 다시 선택해 주세요.", true);
            return;
        }
        var expected = selected;
        if (!confirmation.test("회원 탈퇴 처리", expected.name() + " (" + expected.email() + ") 회원을 탈퇴 처리할까요?\n회원 정보와 코스는 남고, 로그인은 제한돼요."))
            return;
        change(() -> {
            session.changeStatus(expected, "INACTIVE");
            return null;
        }, expected.id(), "탈퇴 처리했어요. 회원 정보와 코스는 유지돼요.");
    }

    /** 영구 삭제: 탈퇴 회원만 가능. 삭제 가능 여부 확인 → 확인창 → 삭제 후 목록 새로고침. */
    private void delete() {
        if (busy || selected == null || !"INACTIVE".equals(selected.status())) return;
        if (dirty()) {
            notice("작성한 변경 내용을 먼저 저장하거나 회원을 다시 선택해 주세요.", true);
            return;
        }
        var expected = selected;
        task("삭제 가능 여부를 확인하고 있어요…", () -> session.deletionRestriction(expected), reason -> {
            if (!reason.isEmpty()) {
                notice(reason, true);
                return;
            }
            // Start the write only after this read has released the busy state.
            SwingUtilities.invokeLater(() -> {
                if (!confirmation.test("회원 영구 삭제", expected.name() + " (" + expected.email() + ") 회원을 영구 삭제할까요?\n삭제한 회원 정보는 복구할 수 없어요.")) {
                    notice("삭제를 취소했어요.", false);
                    return;
                }
                change(() -> {
                    session.deleteInactive(expected);
                    return null;
                }, null, "회원을 영구 삭제했어요.");
            });
        });
    }

    /** DB를 바꾸는 작업(write)을 실행한 뒤 같은 검색어로 목록을 다시 불러와 보여줍니다. */
    private void change(Callable<Void> write, Long keep, String done) {
        String keyword = search.getText();
        task("변경 내용을 처리하고 있어요…", () -> {
            write.call();
            try {
                return session.list(keyword);
            } catch (java.sql.SQLException failure) {
                AppDiagnostics.failure(AppDiagnostics.Operation.ACCOUNT_TASK, AppDiagnostics.Stage.DATABASE_READ, failure);
                return null;
            }
        }, rows -> showRows(rows == null ? List.of() : rows, keep, done + (rows == null ? " 목록을 불러오지 못했어요. 다시 검색해 주세요." : "")));
    }

    // ===== 공통: 백그라운드 실행·안내·버튼 상태 =====

    /** 공통 비동기 처리: 작업 중 표시 → 백그라운드 실행 → 성공/오류 안내 → 버튼 상태 복구. */
    private <T> void task(String pending, Callable<T> work, Consumer<T> success) {
        if (busy) return;
        busy = true;
        updateControls();
        notice(pending, false);
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return work.call();
            }

            @Override
            protected void done() {
                try {
                    success.accept(get());
                } catch (Exception failure) {
                    AppDiagnostics.failure(AppDiagnostics.Operation.ACCOUNT_TASK, AppDiagnostics.Stage.RUN, failure);
                    Throwable cause = AppDiagnostics.root(failure);
                    notice(cause instanceof IllegalArgumentException ? cause.getMessage() : "처리하지 못했어요. 잠시 후 다시 시도해 주세요.", true);
                } finally {
                    busy = false;
                    updateControls();
                }
            }
        }.execute();
    }

    /** 아래쪽 안내 문구를 표시합니다(error가 true면 빨간색). */
    private void notice(String text, boolean error) {
        message.setText(text);
        message.setForeground(error ? new Color(0xB4233F) : LoginUI.MUTED);
    }

    /** 작업 중/선택/수정 여부에 따라 입력칸과 버튼의 활성화 상태를 갱신합니다. */
    private void updateControls() {
        boolean editable = selected != null && !busy;
        for (JComponent control : List.of(search, refresh, all, table, table.getTableHeader()))
            control.setEnabled(!busy);
        person.setEnabled(editable);
        phone.setEnabled(editable);
        memberStatus.setEnabled(editable);
        save.setEnabled(editable && dirty());
        withdraw.setEnabled(editable && !dirty() && "ACTIVE".equals(selected.status()));
        delete.setEnabled(editable && !dirty() && "INACTIVE".equals(selected.status()));
    }

    // ===== 내부 클래스: 표 데이터 모델 =====

    /** 회원 목록(rows)을 표의 행·열로 보여주는 모델. */
    private static final class MemberTableModel extends AbstractTableModel {
        private final String[] columns = {"이름", "이메일", "전화번호", "상태", "가입일"};
        private List<MemberRepository.Member> rows = List.of();

        /** 회원 id로 목록에서의 위치를 찾습니다(없으면 -1). */
        int indexOf(long id) {
            for (int i = 0; i < rows.size(); i++) if (rows.get(i).id() == id) return i;
            return -1;
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return String.class;
        }

        /** 칸에 표시할 값: 가입일은 "yyyy-MM-dd HH:mm" 형태로 잘라 보여줍니다. */
        @Override
        public Object getValueAt(int row, int column) {
            var member = rows.get(row);
            return switch (column) {
                case 0 -> member.name();
                case 1 -> member.email();
                case 2 -> Objects.toString(member.phone(), "");
                case 3 -> Status.valueOf(member.status()).toString();
                default ->
                        member.created() == null ? "" : member.created().replace('T', ' ').substring(0, Math.min(16, member.created().length()));
            };
        }
    }
}