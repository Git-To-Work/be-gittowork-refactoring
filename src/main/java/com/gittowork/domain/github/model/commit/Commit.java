package com.gittowork.domain.github.model.commit;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Commit {

    private String commitSha;

    private String commitMessage;

    private LocalDateTime commitDate;

    private Map<String, String> author;

    private List<String> filesChanged;
}
