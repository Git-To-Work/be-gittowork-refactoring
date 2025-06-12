package com.gittowork.domain.coverletter.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetCoverLetterAnalysisResponse {
    private int coverLetterId;
    private String aiAnalysisResult;
    private CoverLetterAnalysisStat stat;
    private String analysisDttm;
    private String fileUrl;
}
