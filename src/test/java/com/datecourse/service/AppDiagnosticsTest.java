package com.datecourse.service;

import org.junit.jupiter.api.Test;
import java.sql.SQLException;
import java.io.IOException;
import java.util.concurrent.CompletionException;
import static org.junit.jupiter.api.Assertions.*;

class AppDiagnosticsTest {
    @Test void diagnosticLineIncludesUsefulCodesWithoutExceptionMessagesOrSensitiveData() {
        var sql=new SQLException("jdbc:mysql://private-db password=secret token=private-key email=person@example.test","28000",1045,
                new IOException("https://private-api/?key=private-key"));
        var text=AppDiagnostics.format(AppDiagnostics.Operation.COURSE_SAVE,"ref-test",AppDiagnostics.Stage.DATABASE_WRITE,
                AppDiagnostics.Outcome.FAILED,123,new CompletionException(sql));
        assertTrue(text.contains("operation=COURSE_SAVE"));assertTrue(text.contains("stage=DATABASE_WRITE"));
        assertTrue(text.contains("elapsed_ms=123"));assertTrue(text.contains("sql_state=28000"));assertTrue(text.contains("sql_code=1045"));
        for(String privateValue:new String[]{"private-db","secret","private-key","person@example.test","https://"})assertFalse(text.contains(privateValue));
        assertFalse(text.contains("\n"));
    }
    @Test void httpStatusIsAvailableToDiagnosticsAndCancellationIsRecognizedThroughWrappers() {
        var failure=new AppDiagnostics.HttpFailure(429,"잠시 후 다시 시도해 주세요.");
        var text=AppDiagnostics.format(AppDiagnostics.Operation.WALKING_ROUTE,"ref-test",AppDiagnostics.Stage.PROVIDER_REQUEST,
                AppDiagnostics.Outcome.FAILED,20,failure);
        assertTrue(text.contains("http_status=429"));assertFalse(text.contains(failure.getMessage()));
        assertTrue(AppDiagnostics.cancelled(new CompletionException(new java.util.concurrent.CancellationException())));
    }
}
