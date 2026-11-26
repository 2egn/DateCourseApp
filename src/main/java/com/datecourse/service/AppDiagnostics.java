package com.datecourse.service;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * - 작업 종류, 실행 단계 및 결과를 한 줄로 기록하는 진단용 클래스
 * - 예외 메시지, 응답 본문, 경로 및 사용자 데이터는 로그에서 제외
 */
public final class AppDiagnostics {
    public enum Operation { PAGE_TASK, SCREEN_LOAD, ACCOUNT_TASK, COURSE_SAVE, PREVIEW_REBUILD, PREVIEW_READ, PREVIEW_STORAGE, MAP_LOAD, WALKING_ROUTE, TRAVEL_ROUTE, PLACE_LOAD }
    public enum Stage { START, MAP_PREPARE, MAP_CAPTURE, IMAGE_ENCODE, DATABASE_READ, DATABASE_WRITE, PROVIDER_REQUEST, RUN, COMPLETE }
    public enum Outcome { SUCCESS, FAILED, PARTIAL, CANCELLED }
    private static final System.Logger LOG=System.getLogger("datecourse.diagnostics");
    private AppDiagnostics() {}
    public static Trace begin(Operation operation){return new Trace(operation,null);}
    public static Trace begin(Operation operation,Class<?> component){return new Trace(operation,component);}
    public static final class HttpFailure extends java.io.IOException {
        private final int status;
        public HttpFailure(int status,String userMessage){super(userMessage);this.status=status;}
        public int status(){return status;}
    }
    public static void failure(Operation operation,Stage stage,Throwable error){begin(operation).report(stage,Outcome.FAILED,error);}
    public static Throwable root(Throwable error) {
        var seen=Collections.newSetFromMap(new IdentityHashMap<Throwable,Boolean>());
        while(error!=null && error.getCause()!=null && seen.add(error.getCause()))error=error.getCause();
        return error;
    }
    public static boolean cancelled(Throwable error){return root(error) instanceof java.util.concurrent.CancellationException
            || root(error) instanceof InterruptedException;}
    public static final class Trace {
        private final Operation operation;
        private final Class<?> component;
        private final String reference=UUID.randomUUID().toString().replace("-","").substring(0,12);
        private final long started=System.nanoTime();
        private Trace(Operation operation,Class<?> component){this.operation=Objects.requireNonNull(operation);this.component=component;}
        public void report(Stage stage,Outcome outcome,Throwable error) {
            LOG.log(outcome==Outcome.FAILED || outcome==Outcome.PARTIAL?System.Logger.Level.WARNING:System.Logger.Level.DEBUG,
                    format(operation,reference,stage,outcome,TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-started),error)
                            +(component==null?"":" component="+component.getSimpleName()));
        }
    }
    static String format(Operation operation,String reference,Stage stage,Outcome outcome,long elapsed,Throwable error) {
        Throwable root=root(error);
        String line="operation="+operation+" ref="+reference+" stage="+stage+" outcome="+outcome+" elapsed_ms="+elapsed;
        if(root!=null) {
            line+=" error="+root.getClass().getSimpleName();
            StackTraceElement location=null;
            for(var frame:root.getStackTrace())if(frame.getClassName().startsWith("com.datecourse.")){location=frame;break;}
            if(location!=null)line+=" location="+location.getClassName()+"."+location.getMethodName()+":"+location.getLineNumber();
        }
        if(root instanceof HttpFailure http)line+=" http_status="+http.status();
        SQLException sql=null;
        var seen=Collections.newSetFromMap(new IdentityHashMap<Throwable,Boolean>());
        for(Throwable item=error;item!=null && seen.add(item);item=item.getCause())if(item instanceof SQLException value){sql=value;break;}
        if(sql!=null) {
            String state=sql.getSQLState();
            if(state!=null && state.matches("[A-Z0-9]{5}"))line+=" sql_state="+state;
            line+=" sql_code="+sql.getErrorCode();
        }
        return line;
    }
}
