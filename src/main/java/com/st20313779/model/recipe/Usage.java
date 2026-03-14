package com.st20313779.model.recipe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Usage {
    @JsonProperty("monthly_remaining")
    private Integer monthlyRemaining;

    @JsonProperty("monthly_limit")
    private Integer monthlyLimit;

    @JsonProperty("daily_remaining")
    private Integer dailyRemaining;

    @JsonProperty("daily_limit")
    private Integer dailyLimit;
}

