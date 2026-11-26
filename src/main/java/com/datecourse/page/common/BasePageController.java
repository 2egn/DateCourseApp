package com.datecourse.page.common;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
// DB·HTTP 대기 작업을 화면 스레드 밖에서 처리하기 위한 JavaFX 작업 클래스
import javafx.concurrent.Task;
import javafx.scene.control.Label;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * - JavaFX 화면의 공통 로딩, 알림 및 배경 작업 처리용 부모 클래스
 * - DB 연결 풀, 로그인 세션 및 지도는 각 소유 클래스에서 관리
 */
public abstract class BasePageController {
    // 공유된 비동기 조회 결과를 현재 화면의 로딩·성공·실패 처리에 연결
    protected final <T> void showFuture(String label, java.util.concurrent.CompletableFuture<T> future,
                                      Consumer<T> success, Consumer<Throwable> failure) {
        if (future.isDone()) {
            try { success.accept(future.join()); }
            catch (java.util.concurrent.CompletionException | java.util.concurrent.CancellationException problem) {
                diagnose(problem);failure.accept(problem.getCause()==null?problem:problem.getCause());
            }
            return;
        }
        Runnable finish = beginLoading(label);
        future.whenComplete((value, problem) -> javafx.application.Platform.runLater(() -> {
            finish.run();
            if (problem == null) success.accept(value);
            else {diagnose(problem);failure.accept(problem);}
        }));
    }
    private final ReadOnlyBooleanWrapper loading = new ReadOnlyBooleanWrapper();
    private int pending;
    private PageFeedback feedback;

    public final ReadOnlyBooleanProperty loadingProperty() { return loading.getReadOnlyProperty(); }
    public final boolean isLoading() { return loading.get(); }
    public final void useFeedback(PageFeedback feedback) { this.feedback = feedback; }
    protected final void updateLoading(String text) {if(feedback!=null)feedback.updateLoading(text);}
    protected final void notifyUser(PageFeedback.Kind kind, String text) {
        if (feedback != null) feedback.show(kind, text);
    }
    protected final void watchNotice(Label label, PageFeedback.Kind kind) {
        label.textProperty().addListener((o, old, text) -> {
            if (text != null && !text.isBlank() && !text.endsWith("…")) notifyUser(kind, text);
        });
    }
    // 중첩된 로딩 수를 관리하고 한 번만 종료되는 콜백 반환
    protected final Runnable beginLoading(String text) {
        pending++;
        loading.set(true);
        Runnable endUi = feedback == null ? () -> {} : feedback.beginLoading(text);
        boolean[] ended = {false};
        return () -> {
            if (ended[0]) return;
            ended[0] = true;
            endUi.run();
            loading.set(--pending > 0);
        };
    }
    private void diagnose(Throwable failure) {
        var diagnostic=com.datecourse.service.AppDiagnostics.begin(com.datecourse.service.AppDiagnostics.Operation.PAGE_TASK,getClass());
        diagnostic.report(com.datecourse.service.AppDiagnostics.Stage.RUN,
                com.datecourse.service.AppDiagnostics.cancelled(failure)?com.datecourse.service.AppDiagnostics.Outcome.CANCELLED
                        :com.datecourse.service.AppDiagnostics.Outcome.FAILED,failure);
    }
    // Task로 배경 작업 실행 → 화면 스레드에서 결과와 로딩 상태 처리
    protected final <T> void runWork(String label, Callable<T> work, Consumer<T> success, Consumer<Throwable> failure) {
        Runnable finish = beginLoading(label);
        Task<T> task = new Task<>() { @Override protected T call() throws Exception { return work.call(); } };
        task.setOnSucceeded(event -> { finish.run(); success.accept(task.getValue()); });
        task.setOnFailed(event -> {
            diagnose(task.getException());
            finish.run(); failure.accept(task.getException());
        });
        task.setOnCancelled(event -> finish.run());
        Thread thread = new Thread(task, "page-background-work");
        thread.setDaemon(true);
        thread.start();
    }
}
