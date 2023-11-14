package com.controlunion.excelUploader.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.stereotype.Service;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class FarmChangesDto implements Serializable {

    private String last;
    private String current;
    private String change;

    public FarmChangesDto( String chnage, Number last, Number current) {
        this.last = String.valueOf(last);
        this.current = String.valueOf(current);
        this.change = chnage;
    }



    public FarmChangesDto(String chng, String last, String current) {
        this.last = last;
        this.current = current;
        this.change = chng;
    }
}
