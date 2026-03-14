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
public class RecipeMeta {
    @JsonProperty("active_time")
    private String activeTime;

    @JsonProperty("passive_time")
    private String passiveTime;

    @JsonProperty("total_time")
    private String totalTime;

    @JsonProperty("overnight_required")
    private Boolean overnightRequired;

    private String yields;

    @JsonProperty("yield_count")
    private Integer yieldCount;

    @JsonProperty("serving_size_g")
    private Double servingSizeG;
}

