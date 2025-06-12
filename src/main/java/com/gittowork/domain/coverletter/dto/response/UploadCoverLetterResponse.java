package com.gittowork.domain.coverletter.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadCoverLetterResponse {
    private String message;
    private int coverLetterId;
}
