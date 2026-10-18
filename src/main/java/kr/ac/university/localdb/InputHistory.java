package kr.ac.university.localdb;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/** 로그인 아이디 하나만 이 PC에 보관합니다. 비밀번호와 가입 상세 정보는 저장하지 않습니다. */
public final class InputHistory {
    private final Path file;
    private final Properties data = new Properties();

    public InputHistory(Path file) {
        this.file = file;
        if (Files.exists(file)) {
            try (InputStream in = Files.newInputStream(file)) { data.load(in); }
            catch (IOException ignored) { /* 기록이 없어도 로그인 기능은 사용 가능합니다. */ }
        }
        // 기존 버전의 이름·전화번호와 여러 아이디 기록을 정리합니다.
        String identity = data.getProperty("identity.0", "").trim();
        boolean obsolete = data.stringPropertyNames().stream().anyMatch(key -> !key.equals("identity.0"));
        if (obsolete) {
            data.clear();
            if (!identity.isEmpty()) data.setProperty("identity.0", identity);
            save();
        }
    }

    public String savedIdentity() {
        return data.getProperty("identity.0", "");
    }

    /** 빈 값은 아이디 저장 해제를 뜻합니다. */
    public void saveIdentity(String value) {
        data.clear();
        if (!value.isBlank()) data.setProperty("identity.0", value.trim());
        save();
    }

    private void save() {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            try (OutputStream out = Files.newOutputStream(file)) {
                data.store(out, "Saved login identity only");
            }
        } catch (IOException ignored) { /* 저장 실패 시 이번 실행의 기록만 유지합니다. */ }
    }
}