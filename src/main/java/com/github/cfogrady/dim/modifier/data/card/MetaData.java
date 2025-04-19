package com.github.cfogrady.dim.modifier.data.card;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class MetaData {
    private int id;
    private int revision;
    private int year;
    private int month;
    private int day;
    private int originalChecksum;
}
