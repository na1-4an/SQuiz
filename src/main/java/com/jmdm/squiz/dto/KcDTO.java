package com.jmdm.squiz.dto;

import lombok.Data;

@Data
public class KcDTO {
    private int kcId;
    private int correct; // 맞으면 1, 틀리면 0
}
