package com.datecourse.controller;

import com.datecourse.service.TmapSettings;
import com.datecourse.model.WalkingSearchOption;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import java.io.IOException;

final class TmapSettingsDialog {
    @FunctionalInterface interface OptionSaver { void save(int option) throws IOException; }
    static boolean show(Window owner) {
        int selected;
        try { selected = TmapSettings.load().searchOption(); }
        catch (IOException | IllegalArgumentException failure) { selected = 0; }
        return create(owner, selected).showAndWait().filter(ButtonType.APPLY::equals).isPresent();
    }

    static Dialog<ButtonType> create(Window owner, int selected) {
        return create(owner, selected, TmapSettings::saveSearchOption);
    }

    static Dialog<ButtonType> create(Window owner, int selected, OptionSaver saver) {
        Dialog<ButtonType> dialog = new Dialog<>();
        if (owner != null) dialog.initOwner(owner);
        dialog.setTitle("도보 경로 탐색 방식");
        dialog.setHeaderText("어떤 길로 걸을까요?");
        dialog.getDialogPane().getStylesheets().add(TmapSettingsDialog.class.getResource("/style.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("route-options-dialog");
        ToggleGroup choices = new ToggleGroup();
        VBox content = new VBox(10);
        for (WalkingSearchOption option : WalkingSearchOption.values()) {
            Label title = new Label(option.title());
            title.getStyleClass().add("section");
            Label description = new Label(option.description());
            description.getStyleClass().add("planner-caption");
            description.setWrapText(true);
            RadioButton radio = new RadioButton();
            radio.setId("route-option-" + option.code());
            radio.setAccessibleText(option.title() + ". " + option.description());
            VBox caption = new VBox(4, title, description);
            caption.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
            radio.setGraphic(caption);
            radio.setToggleGroup(choices);
            radio.setUserData(option);
            radio.setMaxWidth(Double.MAX_VALUE);
            radio.setMinHeight(76);
            radio.setPrefHeight(76);
            radio.setMaxHeight(76);
            radio.getStyleClass().add("route-option-card");
            radio.setSelected(option.code() == selected);
            content.getChildren().add(radio);
        }
        if (choices.getSelectedToggle() == null) choices.selectToggle(choices.getToggles().get(0));
        Label note = new Label("선택한 방식으로 도보 경로를 다시 찾아드려요.");
        note.setWrapText(true);
        note.getStyleClass().add("planner-caption");
        Label error = new Label();
        error.setWrapText(true);
        error.getStyleClass().add("error");
        content.getChildren().addAll(note, error);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(440);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.APPLY, ButtonType.CANCEL);
        ((Button)dialog.getDialogPane().lookupButton(ButtonType.APPLY)).setText("이 방식으로 적용");
        ((Button)dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("취소");
        dialog.getDialogPane().lookupButton(ButtonType.APPLY).addEventFilter(ActionEvent.ACTION, event -> {
            try { saver.save(((WalkingSearchOption)choices.getSelectedToggle().getUserData()).code()); }
            catch (IOException | IllegalArgumentException failure) {
                error.setText("탐색 방식을 저장하지 못했어요. 다시 시도해 주세요.");
                event.consume();
            }
        });
        return dialog;
    }
}
