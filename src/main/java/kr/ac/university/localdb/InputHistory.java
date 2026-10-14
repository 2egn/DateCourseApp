package kr.ac.university.localdb;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;
import java.awt.event.*;

/** 비밀번호를 받을 수 없는 제한된 입력 기록. 현재 프로젝트의 .local에 보관합니다. */
public final class InputHistory {
    private static final Set<String> ALLOWED = Set.of("identity", "name", "phone");
    private final Path file;
    private final Properties data = new Properties();
    public InputHistory(Path file) {
        this.file = file;
        if (Files.exists(file)) {
            try (InputStream in = Files.newInputStream(file)) { data.load(in); }
            catch (IOException ignored) { /* 기록이 없어도 로그인 기능은 사용 가능합니다. */ }
        }
        boolean trimmed = false;
        for (String key : new HashSet<>(data.stringPropertyNames())) {
            String[] parts = key.split("\\.");
            if (parts.length != 2 || !ALLOWED.contains(parts[0]) || !parts[1].matches("[0-2]")) {
                data.remove(key); trimmed = true;
            }
        }
        if (trimmed) save();
    }
    public java.util.List<String> values(String key) {
        check(key);
        java.util.List<String> values = new ArrayList<>();
        for (int i=0;i<3;i++) {
            String value = data.getProperty(key+"."+i);
            if (value!=null && !value.isBlank()) values.add(value);
        }
        return values;
    }
    public void remember(String key,String value) {
        check(key); value=value.trim(); if(value.isEmpty()) return;
        java.util.List<String> values = values(key); values.remove(value); values.add(0,value);
        for(int i=0;i<3;i++) {
            data.remove(key+"."+i);
            if(i<values.size()) data.setProperty(key+"."+i,values.get(i));
        }
        save();
    }
    private void save() {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            try (OutputStream out=Files.newOutputStream(file)) { data.store(out,"Non-password input history"); }
        } catch(IOException ignored) { /* 저장 실패 시 이번 실행의 기록만 유지합니다. */ }
    }
    private static void check(String key) {
        if(!ALLOWED.contains(key)) throw new IllegalArgumentException("입력 기록에 허용되지 않은 항목입니다.");
    }
    public void remove(String key,String value) {
        check(key);
        java.util.List<String> remaining=values(key);
        remaining.remove(value);
        for(int i=0;i<3;i++) {
            data.remove(key+"."+i);
            if(i<remaining.size()) data.setProperty(key+"."+i,remaining.get(i));
        }
        save();
    }

    private void fillPopup(JPopupMenu popup,JTextField field,String key) {
        popup.removeAll();
        for(String value:values(key)) {
            JPanel row=new JPanel(new java.awt.BorderLayout(4,0));
            row.setBackground(java.awt.Color.WHITE);
            JButton select=new JButton(value);
            select.setHorizontalAlignment(SwingConstants.LEFT);
            select.setBorder(BorderFactory.createEmptyBorder(8,12,8,8));
            select.setContentAreaFilled(false);select.setFocusPainted(false);
            select.addActionListener(e -> {field.setText(value);popup.setVisible(false);field.requestFocusInWindow();});
            JButton delete=new JButton("×");
            delete.setToolTipText("이 입력 기록 삭제");
            delete.setForeground(new java.awt.Color(0xB4233F));
            delete.setFont(delete.getFont().deriveFont(java.awt.Font.BOLD,18f));
            delete.setBorder(BorderFactory.createEmptyBorder(4,10,4,10));
            delete.setContentAreaFilled(false);delete.setFocusPainted(false);
            delete.addActionListener(e -> {
                remove(key,value);
                if(values(key).isEmpty()) popup.setVisible(false);
                else {fillPopup(popup,field,key);popup.pack();}
            });
            row.add(select,java.awt.BorderLayout.CENTER);row.add(delete,java.awt.BorderLayout.EAST);popup.add(row);
        }
    }
    public void attach(JTextField field,String key) {
        check(key);
        if(field instanceof JPasswordField) throw new IllegalArgumentException("비밀번호 기록은 금지됩니다.");
        field.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent event) {
                java.util.List<String> entries=values(key);
                if(entries.isEmpty()) return;
                JPopupMenu popup=new JPopupMenu();
                fillPopup(popup,field,key);
                popup.show(field,0,field.getHeight());
            }
        });
    }
}
