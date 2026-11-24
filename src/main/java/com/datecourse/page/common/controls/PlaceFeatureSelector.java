package com.datecourse.page.common.controls;

import com.datecourse.model.*;
import javafx.collections.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.*;

/** Evidence-backed multiple selection. Missing facts never become available amenities. */
public final class PlaceFeatureSelector extends VBox {
    private final ObservableSet<PlaceTag> selected=FXCollections.observableSet(new LinkedHashSet<>());
    private final Map<PlaceTag,CheckBox> checks=new EnumMap<>(PlaceTag.class);
    private Set<PlaceTag> available=Set.of();
    public PlaceFeatureSelector(){
        setSpacing(6);
        selected.addListener((SetChangeListener<PlaceTag>)change->checks.forEach((tag,check)->check.setSelected(selected.contains(tag))));
    }
    public ObservableSet<PlaceTag> selectedTags(){return selected;}
    public Set<PlaceTag> availableTags(){return available;}
    public void configure(List<Place> places,Set<PlaceTag> selection){
        configure(places,selection,Set.of());
    }
    /** 화면에서 허용하지 않는 특징은 선택지와 이전 선택에서 함께 제외한다. */
    public void configure(List<Place> places,Set<PlaceTag> selection,Set<PlaceTag> excludedTags){
        checks.clear();getChildren().clear();
        var counts=new EnumMap<PlaceTag,Integer>(PlaceTag.class);
        places.forEach(p->p.details().availableTags().stream().filter(tag->!excludedTags.contains(tag)).forEach(tag->counts.merge(tag,1,Integer::sum)));
        available=Set.copyOf(counts.keySet());selected.clear();
        selection.stream().filter(tag->!excludedTags.contains(tag)).forEach(selected::add);
        group("가격·혜택",List.of(PlaceTag.GOOD_PRICE,PlaceTag.MODEL_RESTAURANT,PlaceTag.FREE_ADMISSION),counts);
        group("교통·접근성",List.of(PlaceTag.NEAR_SUBWAY,PlaceTag.NEAR_BUS,PlaceTag.PARKING,PlaceTag.WHEELCHAIR_ACCESSIBLE_ENTRANCE),counts);
        group("이용 편의",List.of(PlaceTag.RESERVABLE,PlaceTag.GOOD_FOR_GROUPS,PlaceTag.TAKEOUT,PlaceTag.VEGETARIAN_FOOD,
                PlaceTag.OUTDOOR_SEATING,PlaceTag.ALLOWS_DOGS,PlaceTag.GOOD_FOR_CHILDREN),counts);
        var hint=new Label(counts.isEmpty()?"이 활동에는 아직 확인된 특징 자료가 없어요.":"특징은 여러 개 선택할 수 있고 모두 충족하는 장소만 찾아요. 건수는 현재 활동의 전체 목록 기준이에요.");
        hint.setWrapText(true);hint.getStyleClass().add("planner-caption");getChildren().add(hint);
    }
    private void group(String title,List<PlaceTag> tags,Map<PlaceTag,Integer> counts){
        FlowPane options=new FlowPane(12,8);
        for(PlaceTag tag:tags)if(counts.containsKey(tag)||selected.contains(tag)){
            CheckBox check=new CheckBox(tag.label()+" · "+counts.getOrDefault(tag,0)+"곳");
            check.setAccessibleText(tag.label());check.setSelected(selected.contains(tag));
            check.setTooltip(new Tooltip("해당 특징이 확인된 장소만 포함해요. 미확인은 제외돼요."));
            check.selectedProperty().addListener((o,a,b)->{if(b)selected.add(tag);else selected.remove(tag);});
            checks.put(tag,check);options.getChildren().add(check);
        }
        if(!options.getChildren().isEmpty()){
            Label heading=new Label(title);heading.getStyleClass().add("planner-caption");getChildren().addAll(heading,options);
        }
    }
}
