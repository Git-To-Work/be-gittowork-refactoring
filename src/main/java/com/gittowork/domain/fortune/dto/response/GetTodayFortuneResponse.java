package com.gittowork.domain.fortune.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetTodayFortuneResponse {

    @JsonProperty("fortune")
    private Fortune fortune;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Fortune{

        @JsonProperty("overall")
        private String overall;

        @JsonProperty("wealth")
        private String wealth;

        @JsonProperty("love")
        private String love;

        @JsonProperty("study")
        private String study;

        @JsonProperty("date")
        private String date;
    }
}
