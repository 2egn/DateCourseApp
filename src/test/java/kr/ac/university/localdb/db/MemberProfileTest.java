package kr.ac.university.localdb.db;

import org.junit.jupiter.api.Test;
import org.h2.tools.RunScript;
import kr.ac.university.localdb.UserSession;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class MemberProfileTest {
    @Test void readsOnlyRequestedActiveMemberWithoutPasswordFields() throws Exception {
        var pool=DatabasePool.create(new DbSettings("jdbc:h2:mem:"+java.util.UUID.randomUUID()+";MODE=MySQL","sa",""));
        try(var repository=new MemberRepository(pool)) {
            try(var connection=pool.getConnection();var reader=Files.newBufferedReader(Path.of("data/sql/02_create_tables.sql"))) {
                RunScript.execute(connection,reader);
                connection.createStatement().execute("INSERT INTO members(member_id,email,password_hash,name,phone,status) VALUES"
                        +"(7,'me@test','unused','내 이름',NULL,'ACTIVE'),(8,'other@test','unused','다른 회원','010','ACTIVE'),"
                        +"(9,'inactive@test','unused','비활성',NULL,'INACTIVE')");
            }
            var profile=repository.profile(new UserSession(7,"이전 이름"));
            assertEquals("me@test",profile.email());assertEquals("내 이름",profile.name());assertNull(profile.phone());
            assertThrows(IllegalArgumentException.class,()->repository.profile(null));
            assertThrows(java.sql.SQLException.class,()->repository.profile(new UserSession(9,"비활성")));
            assertThrows(java.sql.SQLException.class,()->repository.profile(new UserSession(999,"없음")));
        }
    }
}
