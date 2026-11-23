package com.datecourse.page.saved;

import com.datecourse.page.common.map.CourseMapView;

import com.datecourse.model.RoutePlan;
import com.datecourse.service.*;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import javafx.stage.*;
import java.util.concurrent.*;

/** Rebuilds one optional preview using the existing map adapter; no walking-route lookup. */
final class CoursePreviewDialog {
    static CompletableFuture<byte[]> generate(Window owner,RoutePlan plan) {
        var result=new CompletableFuture<byte[]>();
        var stage=new Stage();stage.initOwner(owner);stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("코스 미리보기 만들기");
        var view=new WebView();
        var status=new Label("지도를 준비하고 있어요…");
        var cancel=new Button("취소");cancel.setOnAction(event->stage.close());
        var indicator=new ProgressIndicator();indicator.setMaxSize(36,36);
        var card=new VBox(12,indicator,status,cancel);card.setAlignment(javafx.geometry.Pos.CENTER);
        card.setMaxSize(320,150);card.getStyleClass().add("loading-card");
        var root=new StackPane(view,card);
        root.getStylesheets().add(CoursePreviewDialog.class.getResource("/style.css").toExternalForm());
        stage.setScene(new Scene(root,CoursePreviewImage.MAP_WIDTH,CoursePreviewImage.MAP_HEIGHT));
        stage.getScene().setOnKeyPressed(event->{if(event.getCode()==javafx.scene.input.KeyCode.ESCAPE)stage.close();});
        var map=new CourseMapView(view,(lat,lng)->{},message->{});
        stage.setOnHidden(event->{if(!result.isDone())result.cancel(false);map.dispose();});
        result.whenComplete((image,failure)->Platform.runLater(()->{if(stage.isShowing())stage.close();}));
        stage.show();
        map.present(plan,null,plan.radius(),false,false);map.load();
        map.capturePreview(plan,phase->{
            if(phase==CourseMapView.CaptureStage.CAPTURING)status.setText("미리보기를 만들고 있어요…");
        }).whenComplete((image,failure)->{
            if(result.isDone())return;
            if(failure!=null){result.completeExceptionally(failure);return;}
            status.setText("이미지를 준비하고 있어요…");
            CompletableFuture.supplyAsync(()->{
                try{return new CoursePreviewImage().renderWithMap(plan,image);}
                catch(Exception problem){throw new CompletionException(problem);}
            }).whenComplete((png,problem)->{
                if(problem==null)result.complete(png);else result.completeExceptionally(problem);
            });
        });
        return result;
    }
}
