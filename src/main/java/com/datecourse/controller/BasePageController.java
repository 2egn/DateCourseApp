package com.datecourse.controller;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.concurrent.Task;
import javafx.scene.control.Label;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/** Common UI state only. DB pools, sessions and maps retain their original owners. */
public abstract class BasePageController {
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
