package com.controlunion.excelUploader.enums;

public enum Errors {

    INVALID_TABLE_HEADER("invalid table header"),
    INVALID_HEADER("invalid header"),
    INVALID_LOCATION("invalid value"),
    MUST_BE("must be"),
    MANDATORY_CELL("cell can't empty"),
    REQUIRED_DATE_VALUE("provided date format invalid"),
    REQUIRED_TEXT_VALUE("must be TEXT"),
    REQUIRED_NUMBER_VALUE("must be NUMBER"),
    INVALID_FORMULA("contain invalid formula"),
    REQUIRED_CROP("You need add least one crop"),
    DUPLICATE_PLOT_VALUES("contains duplicate plot value"),
    CROP_NOT_VALID("crop not valid. (Duplicate or not existing.)"),
    EMPTY_ROW("contain empty row"),
    PROJECT_NAME_MISMATCH("Project name not matched"),
    PROJECT_CODE_MISMATCH("project id not match")
    ;

    private final String name;

    Errors(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
