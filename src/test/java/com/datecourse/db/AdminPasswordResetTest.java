package com.datecourse.db;

import com.datecourse.auth.PasswordHash;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class AdminPasswordResetTest {
    @Test void changesOnlyTheExistingAdminAndNewPasswordAuthenticates()throws Exception {
        try(var pool=DatabasePool.create(new DbSettings("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL","sa",""))) {
            char[] old="old-test-password".toCharArray(),next="new-test-password".toCharArray();
            try {
                try(var connection=pool.getConnection();var statement=connection.createStatement()) {
                    statement.executeUpdate("CREATE TABLE app_admins(username VARCHAR(50) PRIMARY KEY,password_hash VARCHAR(255))");
                    statement.executeUpdate("CREATE TABLE untouched(sentinel_value INT)");statement.executeUpdate("INSERT INTO untouched VALUES(42)");
                }
                try(var connection=pool.getConnection();var insert=connection.prepareStatement("INSERT INTO app_admins VALUES(?,?)")) {
                    insert.setString(1,MemberRepository.ADMIN_USERNAME);insert.setString(2,PasswordHash.create(old));insert.executeUpdate();
                    insert.setString(1,"other-admin");insert.setString(2,PasswordHash.create(old));insert.executeUpdate();
                }
                AdminPasswordReset.reset(pool,next);
                var members=new MemberRepository(pool);
                assertNotNull(members.login(MemberRepository.ADMIN_USERNAME,next).adminSession());
                assertThrows(IllegalArgumentException.class,()->members.login(MemberRepository.ADMIN_USERNAME,old));
                try(var connection=pool.getConnection();var query=connection.createStatement()) {
                    try(var row=query.executeQuery("SELECT password_hash FROM app_admins WHERE username='other-admin'")) {
                        assertTrue(row.next());assertTrue(PasswordHash.verify(old,row.getString(1)));
                    }
                    try(var row=query.executeQuery("SELECT sentinel_value FROM untouched")){assertTrue(row.next());assertEquals(42,row.getInt(1));}
                }
                assertThrows(IllegalArgumentException.class,()->AdminPasswordReset.reset(pool,"short".toCharArray()));
            } finally {Arrays.fill(old,'\0');Arrays.fill(next,'\0');}
        }
    }
    @Test void missingAdminIsNotCreatedImplicitly()throws Exception {
        try(var pool=DatabasePool.create(new DbSettings("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL","sa",""))) {
            try(var c=pool.getConnection();var s=c.createStatement()){s.executeUpdate("CREATE TABLE app_admins(username VARCHAR(50) PRIMARY KEY,password_hash VARCHAR(255))");}
            char[] next="new-test-password".toCharArray();
            try {assertThrows(IllegalStateException.class,()->AdminPasswordReset.reset(pool,next));}
            finally {Arrays.fill(next,'\0');}
        }
    }
}
