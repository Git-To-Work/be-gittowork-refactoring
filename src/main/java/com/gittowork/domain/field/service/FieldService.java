package com.gittowork.domain.field.service;

import com.gittowork.domain.field.dto.response.FieldResponse;
import com.gittowork.domain.field.dto.response.GetInterestFieldsResponse;
import com.gittowork.domain.field.repository.FieldRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 관심 분야(Field) 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * <p>
 * - DB로부터 Field 엔티티를 조회하여 필요한 정보만 추출한 뒤,
 *   {@link FieldResponse} DTO로 변환하고 캐시에 저장합니다.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class FieldService {

    private final FieldRepository fieldRepository;

    /**
     * 사용자의 관심 분야 목록을 조회합니다.
     * <p>
     * 최초 호출 시에만 DB에서 전체 Field 엔티티를 조회하여 DTO로 변환한 뒤,
     * 이후에는 캐시된 결과를 반환합니다.
     * </p>
     *
     * @return 관심 분야 목록을 감싼 {@link GetInterestFieldsResponse} 객체
     */
    @Cacheable("interestFields")
    @Transactional(readOnly = true)
    public GetInterestFieldsResponse getInterestFields() {
        List<FieldResponse> responseList = fieldRepository.findAll().stream()
                .map(field -> FieldResponse.builder()
                        .fieldName(field.getFieldName())
                        .fieldLogoUrl(field.getFieldLogoUrl())
                        .build())
                .collect(Collectors.toList());

        return GetInterestFieldsResponse.builder()
                .fields(responseList)
                .build();
    }
}
