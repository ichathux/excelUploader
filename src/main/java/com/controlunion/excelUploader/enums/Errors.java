package com.controlunion.excelUploader.enums;

public enum Errors {

    INVALID_TABLE_HEADER("invalid table header"),
    MANDATORY_CELL("cell can't empty"),
    REQUIRED_DATE_VALUE("provided date format invalid"),
    REQUIRED_TEXT_VALUE("must be TEXT"),
    REQUIRED_NUMBER_VALUE("must be NUMBER"),
    INVALID_CROP_NAME("must be valid existing crop"),
    REQUIRED_CROP("You need add least one crop"),
    DUPPLICATE_PLOT_VALUES("contains duplicate plot value")
    ;

    Errors(String name) {

    }
}
