package com.journey.web.controller.auth;

import com.journey.web.dto.auth.GoogleLoginDto;
import com.journey.web.dto.member.MemberJoinDto;
import com.journey.web.dto.member.MemberLoginDto;
import com.journey.web.dto.member.MemberNickNameDuplicateDto;
import com.journey.web.dto.member.PasswordResetDto;
import com.journey.web.dto.response.ResponseDto;
import com.journey.web.dto.token.TokenDto;
import com.journey.web.dto.token.TokenRequestDto;
import com.journey.web.exception.CustomException;
import com.journey.web.exception.CustomValidationException;
import com.journey.web.service.AuthService;
import com.journey.web.service.MemberService;
import com.journey.web.util.SecurityUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

import static com.journey.web.exception.ErrorCode.*;

@Slf4j
@Api(tags = {"회원가입, 로그인, 로그아웃, 토큰 재발행을 제공하는 Controller"})
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;
    private final AuthService authService;

    @ApiOperation(value = "회원가입", notes = "회원가입 성공 시 이메일을 반환해줍니다.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "회원가입 성공"),
            @ApiResponse(code = 400, message = "잘못된 접근"),
            @ApiResponse(code = 409, message = "이미 존재하는 이메일"),
            @ApiResponse(code = 500, message = "서버 에러")
    })
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody @Valid MemberJoinDto memberJoinDto, BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            Map<String, String> errorMap = new HashMap<>();

            for (FieldError error : bindingResult.getFieldErrors()) {
                errorMap.put(error.getField(), error.getDefaultMessage());
            }

            throw new CustomValidationException("유효성 검사 실패", errorMap);
        } else {
            if (!memberJoinDto.getPwd().equals(memberJoinDto.getPasswordConfirm())) {
                throw new CustomException(NOT_EQUAL_PASSWORD);
            }
            return new ResponseEntity<ResponseDto>(new ResponseDto(200, "회원가입 성공",
                    authService.signup(memberJoinDto)), HttpStatus.OK);
        }

    }

    @ApiOperation(value = "로그인", notes = "로그인 성공 시 accessToken과 refreshToken을 반환해줍니다.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "로그인 성공"),
            @ApiResponse(code = 400, message = "비밀번호 오류"),
            @ApiResponse(code = 404, message = "존재하지 않은 이메일입니다."),
            @ApiResponse(code = 500, message = "서버 에러")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody MemberLoginDto memberLoginDto) {
        TokenDto token = authService.login(memberLoginDto);
        return new ResponseEntity<ResponseDto>(new ResponseDto<>(200, "로그인 성공",
                token), HttpStatus.OK);
    }

    @PostMapping("/google/login")
    public ResponseEntity<?> googleLogin(@RequestBody GoogleLoginDto googleLoginDto) {
        TokenDto token = authService.googleLogin(googleLoginDto.getGoogleIdToken());
        return new ResponseEntity<ResponseDto>(new ResponseDto<>(200, "로그인 성공",
                token), HttpStatus.OK);
    }

    @ApiOperation(value = "토큰 재발행", notes = "accessToken 만료 시 재발행 용도입니다.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "토큰 재발행 성공"),
            @ApiResponse(code = 500, message = "서버 에러")
    })
    @PostMapping("/reissue")
    public ResponseEntity<?> reissue(@RequestBody TokenRequestDto tokenRequestDto) {
        return new ResponseEntity<ResponseDto>(new ResponseDto<>(200, "토큰 재발행",
                authService.reissue(tokenRequestDto)), HttpStatus.OK);
    }

    @ApiOperation(value = "로그아웃", notes = "로그아웃 시 accessToken과 refreshToken은 더이상 사용할 수 없습니다.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "로그아웃 성공"),
            @ApiResponse(code = 500, message = "서버 에러")
    })
    @GetMapping("/logout")
    public ResponseEntity logout(HttpServletRequest request) {
        log.info("로그아웃 성공!!");
        String accessToken = request.getHeader("Authorization").substring(7);
        Long memberId = SecurityUtil.getCurrentMemberId();
        memberService.logoutStatus(memberId);
        authService.logout(accessToken);
        return new ResponseEntity<>(new ResponseDto<Object>(200, "로그아웃 완료", null)
                , HttpStatus.OK);
    }

    @ApiOperation(value = "닉네임 중복 검사", notes = "닉네임 중복 시 에러 발생")
    @ApiResponses({
            @ApiResponse(code = 200, message = "닉네임 중복 X"),
            @ApiResponse(code = 409, message = "닉네임 중복")
    })
    @PostMapping("/nickname/check")
    public ResponseDto checkNickNameDuplication(@RequestBody @Valid MemberNickNameDuplicateDto memberNickNameDuplicateDto
            , BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            Map<String, String> errorMap = new HashMap<>();

            for (FieldError error : bindingResult.getFieldErrors()) {
                errorMap.put(error.getField(), error.getDefaultMessage());
            }

            throw new CustomValidationException("유효성 검사 실패", errorMap);
        }
        if (memberService.existsByNickName(memberNickNameDuplicateDto.getNickName()) == true) {
            throw new CustomException(ALREADY_USED_NICKNAME);
        } else {
            return new ResponseDto<String>(200, "success", "사용가능한 닉네임입니다.");
        }
    }

    @ApiOperation(value = "비밀번호 재설정", notes = "메일 본인 인증에 성공하면 비밀번호 재설정 가능")
    @PostMapping("/password/reset")
    public ResponseEntity<?> passwordReset(@RequestBody @Valid PasswordResetDto passwordResetDto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            Map<String, String> errorMap = new HashMap<>();

            for (FieldError error : bindingResult.getFieldErrors()) {
                errorMap.put(error.getField(), error.getDefaultMessage());
            }

            throw new CustomValidationException("유효성 검사 실패", errorMap);
        }
        if (!passwordResetDto.getPassword().equals(passwordResetDto.getPasswordConfirm())) {
            throw new CustomException(NOT_EQUAL_PASSWORD);
        }
        authService.passwordReset(passwordResetDto);
        return new ResponseEntity<>(new ResponseDto(200, "비밀번호 재설정", null)
                , HttpStatus.OK);
    }
}
