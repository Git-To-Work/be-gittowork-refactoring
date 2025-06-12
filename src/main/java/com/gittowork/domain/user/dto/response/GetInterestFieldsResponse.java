package com.gittowork.domain.user.dto.response;

import com.gittowork.domain.field.entity.Field;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetInterestFieldsResponse {

    private List<Field> fields;

}
