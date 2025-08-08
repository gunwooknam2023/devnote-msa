package com.example.devnote.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {
    private Long id;
    private String name;
    private String picture;
    private String selfIntroduction;
    private Integer activityScore;
    private Integer favoriteVideoCount;
    private Integer favoriteNewsCount;
    private Integer favoriteChannelCount;
    private Integer commentCount;
}