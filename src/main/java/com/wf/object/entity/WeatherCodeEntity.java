package com.wf.object.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class WeatherCodeEntity implements Serializable {

    private String city;

    private LocalDateTime beginTime;

    private LocalDateTime endTime;

    private List<WeatherDataEntity> weatherDataEntityList;
}
