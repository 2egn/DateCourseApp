package com.datecourse.page.common.map;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.scene.Node;
import javafx.scene.web.WebView;
import java.util.ArrayList;
import java.util.List;

/**
 * - WebView가 실제로 표시된 크기를 기준으로 지도 배치를 맞추는 클래스
 * - 숨겨진 상태의 기본 크기로 배치하지 않도록 실제 레이아웃까지 대기
 */
final class MapViewportLifecycle implements AutoCloseable {
    private final WebView view;
    private final Runnable displayed;
    private final List<Observable> observed=new ArrayList<>();
    private final InvalidationListener changed=ignored->refresh();
    private boolean queued,closed;
    MapViewportLifecycle(WebView view,Runnable displayed){this.view=view;this.displayed=displayed;refresh();}
    void refresh(){
        if(closed||queued)return;queued=true;
        Platform.runLater(()->{
            queued=false;if(closed)return;
            observed.forEach(value->value.removeListener(changed));observed.clear();
            observe(view.widthProperty());observe(view.heightProperty());observe(view.sceneProperty());
            boolean visible=true;
            for(Node node=view;node!=null;node=node.getParent()){
                observe(node.parentProperty());observe(node.visibleProperty());visible&=node.isVisible();
            }
            var scene=view.getScene();
            if(scene!=null){observe(scene.windowProperty());var window=scene.getWindow();
                if(window!=null){observe(window.showingProperty());
                    if(visible&&window.isShowing()&&view.getWidth()>0&&view.getHeight()>0)displayed.run();
                }
            }
        });
    }
    private void observe(Observable value){value.addListener(changed);observed.add(value);}
    @Override public void close(){closed=true;observed.forEach(value->value.removeListener(changed));observed.clear();}
}
