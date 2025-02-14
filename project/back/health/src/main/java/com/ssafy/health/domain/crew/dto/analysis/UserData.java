package com.ssafy.health.domain.crew.dto.analysis;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserData {
    @JsonProperty("user_id")
    private Long userId;
    private ScoreData score;
    @JsonProperty("favorite_sports")
    private List<Long> favoriteSports;
    @JsonProperty("crew_list")
    private List<Long> crewList;
}