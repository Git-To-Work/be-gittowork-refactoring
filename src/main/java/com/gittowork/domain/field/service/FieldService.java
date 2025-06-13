package com.gittowork.domain.field.service;

import com.gittowork.domain.field.dto.response.FieldResponse;
import com.gittowork.domain.field.entity.Field;
import com.gittowork.domain.field.repository.FieldRepository;
import com.gittowork.domain.field.dto.response.GetInterestFieldsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FieldService {

    private final FieldRepository fieldRepository;

    /**
     * 1. 메서드 설명: 모든 관심 분야(Fields) 목록을 조회하여 GetInterestFieldsResponse 객체로 반환하는 API.
     * 2. 로직:
     *    - FieldsRepository의 findAll()을 통해 DB에서 모든 Fields 엔티티를 조회한다.
     *    - 조회한 결과를 GetInterestFieldsResponse 빌더를 사용해 Response 객체로 변환하여 반환한다.
     * 3. param: 없음.
     * 4. return: 모든 관심 분야 목록을 포함하는 GetInterestFieldsResponse 객체.
     */
    @Transactional(readOnly = true)
    public GetInterestFieldsResponse getInterestFields() {
        List<Field> interestFields = fieldRepository.findAll();

        List<FieldResponse> responseList = interestFields.stream()
                .map(field ->
                        FieldResponse.builder()
                                .fieldName(field.getFieldName())
                                .fieldLogoUrl(field.getFieldLogoUrl())
                                .build()
                )
                .collect(Collectors.toList());

        return GetInterestFieldsResponse.builder()
                .fields(responseList)
                .build();
    }
}
