package com.gittowork.domain.interaction.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInteractionResult {

    private int companyId;
    private String companyName;
    private String logo;
    private String fieldName;            // 연결된 Field의 이름
    private List<String> techStacks;     // 연결된 기업 공고에 있는 tech_stack 이름 목록
    private boolean hasActiveJobNotice;  // 채용 중인 공고가 있는지 여부
    private boolean scrapped;            // 현재 사용자가 스크랩한 상태 여부

}
