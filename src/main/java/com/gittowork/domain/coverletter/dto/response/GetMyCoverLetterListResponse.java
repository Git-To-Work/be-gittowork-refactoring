package com.gittowork.domain.coverletter.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetMyCoverLetterListResponse {
    private List<FileInfo> files;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FileInfo {
        private int fileId;
        private String fileName;
        private String title;
        private String fileUrl;
        private String createDttm;
    }
}
