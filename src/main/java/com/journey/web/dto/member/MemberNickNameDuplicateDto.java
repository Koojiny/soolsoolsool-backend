package com.journey.web.dto.member;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class MemberNickNameDuplicateDto {

    @NotBlank
    @Size(min = 3, max = 20, message = "닉네임은 3~20자리수여야 합니다.")
    private String nickName;
}
