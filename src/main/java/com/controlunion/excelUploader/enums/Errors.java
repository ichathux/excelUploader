package com.controlunion.excelUploader.enums;

public enum Errors {

    INVALID_TABLE_HEADER("invalid table header"),
    MANDATORY_CELL("cell can't empty"),
    REQUIRED_DATE_VALUE("provided date format invalid"),
    REQUIRED_TEXT_VALUE("must be TEXT"),
    REQUIRED_NUMBER_VALUE("must be NUMBER"),
    INVALID_CROP_NAME("must be valid existing crop"),
    INVALID_FORMULA("contain invalid formula"),
    REQUIRED_CROP("You need add least one crop"),
    DUPLICATE_PLOT_VALUES("contains duplicate plot value"),
    CROP_NOT_VALID("crop not valid. (Duplicate or not existing.)"),
    EMPTY_ROW("contain empty row")
    ;

    private final String name;

    Errors(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
