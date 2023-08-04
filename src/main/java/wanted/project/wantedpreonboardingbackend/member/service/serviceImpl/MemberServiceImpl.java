package wanted.project.wantedpreonboardingbackend.member.service.serviceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wanted.project.wantedpreonboardingbackend.member.dto.request.LoginRequestDto;
import wanted.project.wantedpreonboardingbackend.member.dto.request.LogoutRequestDto;
import wanted.project.wantedpreonboardingbackend.member.dto.request.SignUpRequestDto;
import wanted.project.wantedpreonboardingbackend.member.dto.response.Response;
import wanted.project.wantedpreonboardingbackend.member.entity.Authority;
import wanted.project.wantedpreonboardingbackend.member.entity.Member;
import wanted.project.wantedpreonboardingbackend.member.exception.MemberException;
import wanted.project.wantedpreonboardingbackend.member.repository.MemberRepository;
import wanted.project.wantedpreonboardingbackend.member.service.MemberService;
import wanted.project.wantedpreonboardingbackend.security.dto.TokenResponseDto;
import wanted.project.wantedpreonboardingbackend.security.jwt.JwtTokenProvider;
import wanted.project.wantedpreonboardingbackend.security.util.SecurityUtil;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberServiceImpl implements MemberService {
    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final Response response;
    private final RedisTemplate redisTemplate;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;


    /**
     * 회원가입
     * @param signUp
     * @return
     */
    @Override
    @Transactional
    public ResponseEntity<?> signup(SignUpRequestDto signUp) {

        if (isDuplicatedEmail(signUp.getEmail())) {
            throw new MemberException("이미 존재하는 회원입니다.");
        }
        if (!isValidEmail(signUp.getEmail())) {
            throw new MemberException("유효하지 않은 이메일 형식입니다.");
        }
        if (!isValidPassword(signUp.getPassword())) {
            throw new MemberException("비밀번호는 숫자와 영문자를 포함한 8자 이상이어야 합니다.");
        }
        Member member = Member.builder()
                        .email(signUp.getEmail())
                        .password(passwordEncoder.encode(signUp.getPassword()))
                        .roles(Collections.singletonList(Authority.ROLE_USER.name()))
                        .build();
        memberRepository.save(member);


        return response.success("회원가입 성공했습니다.");
    }

    /**
     * 로그인
     * @param login
     * @return
     */
    @Override
    @Transactional
    public ResponseEntity<?> login(LoginRequestDto login) {
        Member member = memberRepository.findByEmail(login.getEmail())
                .orElseThrow(MemberException::new);
        if (!isMatchesPassword(login.getPassword(), member.getPassword())) {
            throw new MemberException("비밀번호가 일치하지 않습니다.");
        }
        // 인증 성공 시 토큰 생성 및 반환
        Authentication authentication = new UsernamePasswordAuthenticationToken(member.getEmail(), member.getPassword(), member.getAuthorities());
        TokenResponseDto tokenResponse = jwtTokenProvider.generateToken(authentication);

        redisTemplate.opsForValue()
                .set("RT:" + authentication.getName(), tokenResponse.getRefreshToken(), tokenResponse.getRefreshTokenExpirationTime(), TimeUnit.MILLISECONDS);


        // 로그인 성공 응답에 토큰 정보 추가하여 반환
        return response.success(tokenResponse, "로그인에 성공했습니다.", HttpStatus.OK);
    }

    /**
     * 로그아웃
     * @param logout
     * @return
     */
    @Override
    public ResponseEntity<?> logout(LogoutRequestDto logout) {
        // 1. Access Token 검증
        if (!jwtTokenProvider.validateToken(logout.getAccessToken())) {
            return response.fail("잘못된 요청입니다.", HttpStatus.BAD_REQUEST);
        }

        // 2. Access Token 에서 User email 을 가져옴
        Authentication authentication = jwtTokenProvider.getAuthentication(logout.getAccessToken());

        // 3. Redis 에서 해당 User email 로 저장된 Refresh Token 이 있는지 여부를 확인 후 있을 경우 삭제
        if (redisTemplate.opsForValue().get("RT:" + authentication.getName()) != null) {
            // Refresh Token 삭제
            redisTemplate.delete("RT:" + authentication.getName());
        }

        // 4. 해당 Access Token 유효시간 가지고 와서 BlackList 로 저장
        Long expiration = jwtTokenProvider.getExpiration(logout.getAccessToken());
        redisTemplate.opsForValue()
                .set(logout.getAccessToken(), "logout", expiration, TimeUnit.MILLISECONDS);

        return response.success("로그아웃 되었습니다.");
    }

//    @Override
//    public LogoutResponseDto logout(LogoutRequestDto logout) {
//        System.out.println(logout.getAccessToken());
//        LogoutResponseDto responseDto = new LogoutResponseDto();
//
//        // 1. Access Token 검증
//        if (!jwtTokenProvider.validateToken(logout.getAccessToken())) {
//            responseDto.setMessage("잘못된 요청입니다.");
//            return responseDto;
//        }
//        // 2. Access Token 에서 email 을 가져옴
//        Authentication authentication = jwtTokenProvider.getAuthentication(logout.getAccessToken());
//
//        // 3. Redis 에서 해당 User email 로 저장된 Refresh Token 이 있는지 여부를 확인 후 있을 경우 삭제
//        if (redisTemplate.opsForValue().get("RT:" + authentication.getName()) != null) {
//            // Refresh Token 삭제
//            redisTemplate.delete("RT:" + authentication.getName());
//        }
//
//        // 4. 해당 Access Token 유효시간 가지고 와서 BlackList 로 저장
//        Long expiration = jwtTokenProvider.getExpiration(logout.getAccessToken());
//        redisTemplate.opsForValue()
//                .set(logout.getAccessToken(), "logout", expiration, TimeUnit.MILLISECONDS);
//
//        responseDto.setMessage("로그아웃 되었습니다.");
//        return responseDto;
//    }


    @Override
    public ResponseEntity<?> authority() {
        // SecurityContext에 담겨 있는 authentication userEamil 정보
        String memberEmail = SecurityUtil.getCurrentUserEmail();

        Member member = memberRepository.findByEmail(memberEmail)
                .orElseThrow(() -> new UsernameNotFoundException("No authentication information."));

        // add ROLE_ADMIN
        member.getRoles().add(Authority.ROLE_ADMIN.name());
        memberRepository.save(member);

        return response.success();
    }

    // 이메일 유효성 검사
    private boolean isValidEmail(String email) {
        String regex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";
        return email.matches(regex);
    }

    // 비밀번호 유효성 검사
    private boolean isValidPassword(String password) {
        String regex = "^(?=.*[0-9])(?=.*[a-zA-Z]).{8,}$";
        return password.matches(regex);
    }

    // 이메일 중복 검사
    private boolean isDuplicatedEmail(String email) {
        return memberRepository.findByEmail(email).isPresent();
    }

    // 비밀번호 일치 검사
    private boolean isMatchesPassword(String password, String encryptedPassword) {
        // 비밀번호를 복호화하여 일치 여부 확인
        return passwordEncoder.matches(password, encryptedPassword);
    }


}
