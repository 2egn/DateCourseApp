package com.datecourse.ui;

import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import java.util.*;
import java.util.function.IntSupplier;

/** A reusable filter surface: stable entry/chips, registered categories and bounded editing area. */
public final class ExpandableFilterPane extends VBox {
    public enum Tone {
        NEUTRAL, BLUE, AMBER, GREEN;
        String styleClass(){return "filter-tone-"+name().toLowerCase(Locale.ROOT);}
    }
    public record Section(String id,String title,String description,Node content,IntSupplier selectedCount,Tone tone) {
        public Section(String id,String title,String description,Node content,IntSupplier count){this(id,title,description,content,count,Tone.NEUTRAL);}
        public Section {Objects.requireNonNull(id);Objects.requireNonNull(content);Objects.requireNonNull(selectedCount);Objects.requireNonNull(tone);}
    }
    public record Selection(String id,String label,Runnable remove,String sectionId) {
        public Selection(String id,String label,Runnable remove){this(id,label,remove,null);}
        public Selection {Objects.requireNonNull(id);Objects.requireNonNull(label);Objects.requireNonNull(remove);}
    }
    private static final String SELECTED="selected";
    private final BooleanProperty expanded=new SimpleBooleanProperty(false);
    private final Button entry=new Button("필터 설정"),search=new Button("이 조건으로 검색");
    private final BooleanProperty searchAvailable=new SimpleBooleanProperty(false);
    private final FlowPane chips=new FlowPane(6,6),categories=new FlowPane(6,6),allSelected=new FlowPane(6,6);
    private final VBox editor=new VBox(10),content=new VBox(10);
    private final FormScrollPane scroll=new FormScrollPane();
    private final Label description=new Label();
    private final ToggleGroup tabs=new ToggleGroup();
    private final Map<String,ToggleButton> buttons=new LinkedHashMap<>();
    private final Hyperlink clear=new Hyperlink("선택 초기화");
    private List<Section> sections=List.of();
    private List<Selection> selections=List.of();
    private String current;
    public ExpandableFilterPane(){
        setSpacing(8);getStyleClass().add("filter-pane");setMinWidth(0);
        SVGPath icon=new SVGPath();icon.setContent("M 1 3 H 17 M 1 9 H 17 M 1 15 H 17 M 6 0 V 6 M 12 6 V 12 M 7 12 V 18");
        icon.getStyleClass().add("filter-entry-icon");entry.setGraphic(icon);entry.getStyleClass().add("filter-entry");
        entry.setOnAction(e->setExpanded(!isExpanded()));entry.setMinHeight(38);
        Label hint=new Label("종류 · 가격 · 원하는 특징");hint.getStyleClass().add("planner-caption");
        search.getStyleClass().addAll("planner-primary","filter-search");
        search.visibleProperty().bind(expanded.and(searchAvailable));search.managedProperty().bind(search.visibleProperty());
        hint.visibleProperty().bind(search.visibleProperty().not());hint.managedProperty().bind(hint.visibleProperty());
        FlowPane header=new FlowPane(10,6,entry,hint,search);header.setAlignment(Pos.CENTER_LEFT);
        description.setWrapText(true);description.getStyleClass().add("planner-caption");
        scroll.getStyleClass().add("filter-scroll");scroll.setMinWidth(0);
        scroll.setContent(content);scroll.setPrefHeight(232);scroll.setMinHeight(160);scroll.setMaxHeight(280);
        content.setPadding(new Insets(4,6,6,0));content.setMinWidth(0);
        editor.getChildren().addAll(categories,description,scroll);editor.getStyleClass().add("filter-editor");
        editor.visibleProperty().bind(expanded);editor.managedProperty().bind(expanded);
        chips.managedProperty().bind(chips.visibleProperty());
        getChildren().addAll(header,chips,editor);
        expanded.addListener((o,a,b)->{updateEntry();if(b&&current==null&&!sections.isEmpty())showSection(sections.getFirst().id());});
        addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED,e->{if(e.getCode()==KeyCode.ESCAPE&&isExpanded()){setExpanded(false);entry.requestFocus();e.consume();}});
    }
    public BooleanProperty expandedProperty(){return expanded;}
    public boolean isExpanded(){return expanded.get();}
    public void setExpanded(boolean value){expanded.set(value);}
    public String currentSection(){return current;}
    public List<Selection> selections(){return selections;}
    public void setOnClear(Runnable action){clear.setOnAction(e->action.run());}
    public void setOnSearch(Runnable action){
        searchAvailable.set(action!=null);
        search.setOnAction(e->{setExpanded(false);if(action!=null)action.run();});
    }
    public void setSections(List<Section> values){
        Set<String> ids=new HashSet<>();ids.add(SELECTED);
        for(Section section:values)if(!ids.add(section.id()))throw new IllegalArgumentException("필터 그룹 ID가 중복됐어요.");
        sections=List.copyOf(values);categories.getChildren().clear();buttons.clear();tabs.getToggles().clear();
        category(SELECTED,"선택한 필터",Tone.NEUTRAL);sections.forEach(s->category(s.id(),s.title(),s.tone()));
        clear.setMinHeight(28);categories.getChildren().add(clear);
        if(current==null||!buttons.containsKey(current))current=sections.isEmpty()?SELECTED:sections.getFirst().id();
        showSection(current);refresh();
    }
    private void category(String id,String title,Tone tone){
        ToggleButton button=new ToggleButton(title);button.setUserData(id);button.setToggleGroup(tabs);
        button.getStyleClass().addAll("filter-category",tone.styleClass());button.setOnAction(e->{button.setSelected(true);showSection(id);});
        buttons.put(id,button);categories.getChildren().add(button);
    }
    public void showSection(String id){
        if(!buttons.containsKey(id))throw new IllegalArgumentException("필터 그룹을 확인해 주세요.");
        current=id;buttons.get(id).setSelected(true);content.getChildren().clear();
        editor.getStyleClass().removeAll(Arrays.stream(Tone.values()).map(Tone::styleClass).toList());
        editor.getStyleClass().add(toneFor(id).styleClass());
        if(id.equals(SELECTED)){
            description.setText("선택한 조건을 하나씩 해제할 수 있어요. 변경 후 후보 검색을 눌러 반영해 주세요.");
            content.getChildren().add(allSelected);
        }else{
            Section section=sections.stream().filter(s->s.id().equals(id)).findFirst().orElseThrow();
            description.setText(section.description());content.getChildren().add(section.content());
        }
        scroll.setVvalue(0);
    }
    public void setSelections(List<Selection> values){
        Set<String> ids=new HashSet<>();for(Selection value:values)if(!ids.add(value.id()))throw new IllegalArgumentException("선택 ID가 중복됐어요.");
        selections=List.copyOf(values);refresh();
    }
    private void refresh(){
        updateEntry();chips.getChildren().clear();allSelected.getChildren().clear();
        selections.stream().limit(3).forEach(s->chips.getChildren().add(chip(s)));
        if(selections.size()>3){Button more=new Button("+"+(selections.size()-3)+"개 더 보기");more.getStyleClass().add("filter-chip");more.setOnAction(e->{showSection(SELECTED);setExpanded(true);});chips.getChildren().add(more);}
        selections.forEach(s->allSelected.getChildren().add(chip(s)));
        if(selections.isEmpty()){Label empty=new Label("선택한 필터가 없어요.");empty.getStyleClass().add("planner-caption");allSelected.getChildren().add(empty);}
        chips.setVisible(!selections.isEmpty());clear.setDisable(selections.isEmpty());
        if(buttons.containsKey(SELECTED))buttons.get(SELECTED).setText("선택한 필터 · "+selections.size());
        for(Section section:sections){int count=section.selectedCount().getAsInt();buttons.get(section.id()).setText(section.title()+(count==0?"":" · "+count));}
    }
    private Tone toneFor(String sectionId){
        return sections.stream().filter(s->s.id().equals(sectionId)).map(Section::tone).findFirst().orElse(Tone.NEUTRAL);
    }
    private Button chip(Selection selection){
        Button chip=new Button(selection.label()+" ×");chip.getStyleClass().addAll("filter-chip",toneFor(selection.sectionId()).styleClass());
        chip.setAccessibleText(selection.label()+" 필터 해제");chip.setWrapText(true);chip.setMaxWidth(260);
        chip.setTooltip(new Tooltip(selection.label()+" · 눌러서 해제"));chip.setOnAction(e->selection.remove().run());return chip;
    }
    private void updateEntry(){
        entry.setText("필터 설정"+(selections.isEmpty()?"":" · "+selections.size()+"개 선택")+(isExpanded()?" ▴":" ▾"));
        entry.setAccessibleText("필터 설정 "+(isExpanded()?"접기":"열기")+" · "+selections.size()+"개 선택");
    }
}
