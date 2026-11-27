package com.datecourse.demo;

import com.datecourse.model.Place;
import com.datecourse.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Duration;
import netscape.javascript.JSObject;
import java.util.*;
import java.util.function.*;

/** Isolated, DB-free comparison. Business motion policy stays in Java; the page adapts SDK operations. */
public final class CameraMotionDemoApp extends Application {
    private static final double START_LAT=35.1578,START_LNG=129.06;
    private static final Duration SEQUENCE_STEP=Duration.millis(120);
    private static final ObjectMapper JSON=new ObjectMapper();
    private final CameraMotionPolicy policy=new CameraMotionPolicy();
    private final Slider radius=new Slider(200,2000,500);
    private final CheckBox reduced=new CheckBox("동작 줄이기");
    private final Label radiusLabel=new Label("500m");
    private final Label status=new Label("두 지도를 불러오는 중…");
    private final HBox controls=new HBox(8);
    private final ComboBox<Place> choices=new ComboBox<>();
    private Panel baseline,proposal;
    private Timeline sequence;
    private double latitude=START_LAT,longitude=START_LNG;
    private int nearIndex;
    private List<Place> samples;
    private record Command(double lat,double lng,int radius,String movement,int level,int duration) {}

    @Override public void start(Stage stage) throws Exception {
        var places=com.datecourse.demo.PublicPlaceImport.batch().places();
        var distance=new PlaceCalculationService();
        var sorted=places.stream().sorted(Comparator.comparingDouble(p->distance.calculateDistance(START_LAT,START_LNG,p.lat(),p.lng()))).toList();
        var selected=new ArrayList<>(sorted.stream().limit(5).toList());selected.add(sorted.getLast());samples=List.copyOf(selected);
        choices.getItems().setAll(samples);choices.setValue(samples.getFirst());choices.setPrefWidth(240);
        choices.setCellFactory(box->new ListCell<>(){@Override protected void updateItem(Place p,boolean empty){super.updateItem(p,empty);setText(empty||p==null?null:p.name());}});
        choices.setButtonCell(new ListCell<>(){@Override protected void updateItem(Place p,boolean empty){super.updateItem(p,empty);setText(empty||p==null?null:p.name());}});
        Button selectedButton=new Button("선택 장소로 이동"),near=new Button("가까운 곳 비교"),quick=new Button("빠른 3곳 선택"),reset=new Button("처음으로");
        selectedButton.getStyleClass().add("planner-primary");
        controls.getChildren().addAll(choices,selectedButton,near,quick,reset,reduced);controls.setDisable(true);
        selectedButton.setOnAction(e->{stopSequence();select(choices.getValue());});
        near.setOnAction(e->{stopSequence();select(samples.get(nearIndex++%5));});
        quick.setOnAction(e->runSequence());reset.setOnAction(e->reset());
        radius.setMajorTickUnit(100);radius.setMinorTickCount(0);radius.setSnapToTicks(true);radius.setBlockIncrement(100);radius.setPrefWidth(310);
        radius.setOnMousePressed(e->stopSequence());
        radius.valueProperty().addListener((o,a,b)->{radiusLabel.setText(radius()+"m");if(ready())apply(radius.isValueChanging()?CameraMotionPolicy.Intent.RANGE_DRAG:CameraMotionPolicy.Intent.RANGE_COMMIT);});
        radius.valueChangingProperty().addListener((o,a,b)->{if(!b&&ready())apply(CameraMotionPolicy.Intent.RANGE_COMMIT);});
        reduced.setOnAction(e->{stopSequence();if(ready())apply(CameraMotionPolicy.Intent.FIT);});
        var radiusRow=new HBox(10,new Label("검색 반경"),radius,radiusLabel,new Label("드래그 중과 놓은 뒤의 움직임을 비교해 보세요."));
        var title=new Label("지도 움직임 비교 시안");title.getStyleClass().add("condition-title");
        var description=new Label("지도에서 같은 출발지를 선택하거나 아래 버튼으로 이동을 비교해 보세요.");description.getStyleClass().add("planner-caption");
        VBox header=new VBox(10,title,description,controls,radiusRow);header.setPadding(new Insets(20));
        var uri=MapPageServer.sharedPage().resolve("camera-motion-demo.html");
        baseline=new Panel("기존 동작","출발지는 즉시 이동 · 반경 변경은 매번 화면을 다시 맞춰요.",false,uri.toString());
        proposal=new Panel("개선 시안","가까운 곳은 배율 유지 · 반경 조절 중에는 필요한 만큼만 맞춰요.",true,uri.toString());
        SplitPane comparison=new SplitPane(baseline.root,proposal.root);comparison.setDividerPositions(.5);
        status.getStyleClass().add("planner-caption");status.setPadding(new Insets(10,20,14,20));
        BorderPane root=new BorderPane(comparison,header,null,status,null);
        var scene=new Scene(root,1400,860);scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        stage.setTitle("DateCourse · 지도 움직임 테스트 시안");stage.setScene(scene);stage.setMinWidth(1050);stage.setMinHeight(650);stage.show();
    }
    private int radius(){return (int)Math.round(radius.getValue()/100)*100;}
    private boolean ready(){return baseline!=null&&proposal!=null&&baseline.ready&&proposal.ready;}
    private void select(Place place){if(place==null)return;choices.setValue(place);pick(place.lat(),place.lng());}
    private void pick(double lat,double lng){latitude=lat;longitude=lng;if(ready())apply(CameraMotionPolicy.Intent.FOCUS);}
    private void apply(CameraMotionPolicy.Intent intent){baseline.apply(intent);proposal.apply(intent);}
    private void onReady(){if(ready()){controls.setDisable(false);apply(CameraMotionPolicy.Intent.RESET);status.setText("지도 클릭으로 출발지 선택 · 드래그/휠 조작 시 자동 이동 대기를 취소해요.");}}
    private void reset(){stopSequence();latitude=START_LAT;longitude=START_LNG;radius.setValue(500);if(ready())apply(CameraMotionPolicy.Intent.RESET);}
    private void stopSequence(){if(sequence!=null){sequence.stop();sequence=null;}}
    private void runSequence(){
        stopSequence();sequence=new Timeline();
        for(int i=0;i<3;i++){Place target=samples.get(i==1?samples.size()-1:i);sequence.getKeyFrames().add(new KeyFrame(SEQUENCE_STEP.multiply(i),e->select(target)));}
        sequence.play();
    }
    @Override public void stop(){stopSequence();if(baseline!=null)baseline.view.getEngine().load(null);if(proposal!=null)proposal.view.getEngine().load(null);}
    private final class Panel {
        final WebView view=new WebView();final Label feedback=new Label("지도 준비 중…");final VBox root;final boolean enhanced;final Bridge bridge;
        boolean ready;
        Panel(String title,String explanation,boolean enhanced,String page){
            this.enhanced=enhanced;
            var heading=new Label(title);heading.getStyleClass().add("section");
            var hint=new Label(explanation);hint.getStyleClass().add("planner-caption");hint.setWrapText(true);
            feedback.getStyleClass().add("planner-caption");feedback.setWrapText(true);
            VBox copy=new VBox(5,heading,hint,feedback);copy.setPadding(new Insets(10,16,10,16));
            root=new VBox(copy,view);VBox.setVgrow(view,Priority.ALWAYS);root.setMinWidth(0);
            bridge=new Bridge(()->{ready=true;onReady();},(lat,lng)->{stopSequence();pick(lat,lng);},feedback::setText,CameraMotionDemoApp.this::stopSequence);
            view.getEngine().getLoadWorker().stateProperty().addListener((o,a,b)->{
                if(b==Worker.State.SUCCEEDED && page.equals(view.getEngine().getLocation())){((JSObject)view.getEngine().executeScript("window")).setMember("cameraDemoBridge",bridge);if(Boolean.TRUE.equals(view.getEngine().executeScript("!!window.cameraMotionDemo")))view.getEngine().executeScript("window.cameraMotionDemo.connect()");else feedback.setText("지도 초기화 오류: "+view.getEngine().executeScript("window.cameraDemoError || '스크립트를 불러오지 못했어요.'"));}
                else if(b==Worker.State.FAILED)feedback.setText("지도를 불러오지 못했어요. 앱을 다시 실행해 주세요.");
            });
            view.getEngine().load(page);
        }
        void apply(CameraMotionPolicy.Intent intent){
            if(!ready)return;
            try{
                String state=(String)view.getEngine().executeScript("window.cameraMotionDemo.measure("+latitude+","+longitude+","+radius()+","+reduced.isSelected()+")");
                var frame=JSON.readValue(state,CameraMotionPolicy.Frame.class);
                var transition=policy.decide(frame,intent);
                Command command=new Command(latitude,longitude,radius(),enhanced?transition.movement().name():(intent==CameraMotionPolicy.Intent.FOCUS?"BASELINE_FOCUS":"BASELINE"),transition.level(),transition.duration());
                view.getEngine().executeScript("window.cameraMotionDemo.apply("+JSON.writeValueAsString(command)+")");
            }catch(Exception error){feedback.setText("이동 요청을 적용하지 못했어요. 지도 준비 상태를 확인해 주세요.");}
        }
    }
    public static final class Bridge {
        private final Runnable ready,interaction;private final java.util.function.BiConsumer<Double,Double> picked;private final Consumer<String> status;
        Bridge(Runnable ready,java.util.function.BiConsumer<Double,Double> picked,Consumer<String> status,Runnable interaction){this.ready=ready;this.picked=picked;this.status=status;this.interaction=interaction;}
        public void onReady(){Platform.runLater(ready);}
        public void onPicked(double lat,double lng){if(Double.isFinite(lat)&&Double.isFinite(lng)&&Math.abs(lat)<=90&&Math.abs(lng)<=180)Platform.runLater(()->picked.accept(lat,lng));}
        public void onStatus(String value){Platform.runLater(()->status.accept(value));}
        public void onUserInteraction(){Platform.runLater(interaction);}
    }
}
