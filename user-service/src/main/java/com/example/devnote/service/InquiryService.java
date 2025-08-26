package com.example.devnote.service;

import com.example.devnote.dto.InquiryDetailResponseDto;
import com.example.devnote.dto.InquiryListResponseDto;
import com.example.devnote.dto.InquiryRequestDto;
import com.example.devnote.entity.Inquiry;
import com.example.devnote.entity.InquiryImage;
import com.example.devnote.entity.User;
import com.example.devnote.repository.InquiryRepository;
import com.example.devnote.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin-email}")
    private String adminEmail;

    /**
     * 문의사항 생성 (회원/비회원 공용)
     */
    @Transactional
    public InquiryDetailResponseDto createInquiry(InquiryRequestDto dto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = null;

        // 1. 인증 정보 확인하여 회원/비회원 정보 설정
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            user = userRepository.findByEmail(auth.getName())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다."));
        }

        // 2. Inquiry 엔티티 생성
        Inquiry inquiry = Inquiry.builder()
                .userId(user != null ? user.getId() : null)
                .username(user != null ? user.getName() : dto.getUsername())
                .passwordHash(user == null ? passwordEncoder.encode(dto.getPassword()) : null)
                .title(dto.getTitle())
                .content(dto.getContent())
                .isPublic(dto.isPublic())
                .build();

        // 비회원 정보 유효성 검사
        if(user == null && (dto.getUsername() == null || dto.getPassword() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "비회원 문의는 사용자명과 비밀번호가 필수입니다.");
        }

        // 3. 첨부된 이미지 URL이 있다면 InquiryImage 엔티티로 변환하여 추가
        if (dto.getImageUrls() != null && !dto.getImageUrls().isEmpty()) {
            dto.getImageUrls().forEach(imageUrl -> {
                InquiryImage image = InquiryImage.builder().imageUrl(imageUrl).build();
                inquiry.addImage(image);
            });
        }

        Inquiry savedInquiry = inquiryRepository.save(inquiry);
        return toDetailDto(savedInquiry, user);
    }

    /**
     * 문의사항 상세 조회
     */
    @Transactional(readOnly = true)
    public InquiryDetailResponseDto getInquiryDetail(Long inquiryId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "문의사항을 찾을 수 없습니다."));

        if (!inquiry.isPublic()) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "비공개 글에 접근하려면 로그인이 필요합니다.");
            }
            User currentUser = userRepository.findByEmail(auth.getName())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자 정보를 찾을 수 없습니다."));

            boolean isAdmin = auth.getName().equals(adminEmail);
            boolean isAuthor = (inquiry.getUserId() != null) && inquiry.getUserId().equals(currentUser.getId());

            if (!isAdmin && !isAuthor) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "비공개 글에 접근할 권한이 없습니다.");
            }
        }

        // 문의사항 작성자(회원)의 최신 정보를 DB에서 조회
        User author = (inquiry.getUserId() != null)
                ? userRepository.findById(inquiry.getUserId()).orElse(null)
                : null;

        return toDetailDto(inquiry, author);
    }

    /**
     * 문의사항 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<InquiryListResponseDto> getInquiryList(Pageable pageable) {
        Page<Inquiry> inquiryPage = inquiryRepository.findAllByOrderByCreatedAtDesc(pageable);

        // 1. 현재 페이지의 문의사항들에서 회원 ID 목록을 추출
        List<Long> userIds = inquiryPage.getContent().stream()
                .map(Inquiry::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        // 2. 추출된 ID 목록으로 User 정보를 한 번에 조회하여 Map으로 생성
        Map<Long, User> userMap = (userIds.isEmpty())
                ? Collections.emptyMap()
                : userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        // 3. 각 문의사항을 DTO로 변환할 때, 위에서 만든 Map을 사용하여 User 정보를 매핑
        return inquiryPage.map(inquiry -> toListDto(inquiry, userMap.get(inquiry.getUserId())));
    }

    /**
     * 문의사항 삭제
     */
    @Transactional
    public void deleteInquiry(Long inquiryId, String password) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "문의사항을 찾을 수 없습니다."));

        if (inquiry.getUserId() != null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
            }
            User user = userRepository.findByEmail(auth.getName())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다."));
            if (!Objects.equals(user.getId(), inquiry.getUserId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인이 작성한 글만 삭제할 수 있습니다.");
            }
        } else {
            if (password == null || !passwordEncoder.matches(password, inquiry.getPasswordHash())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다.");
            }
        }

        inquiryRepository.delete(inquiry);
    }

    /**
     * Inquiry 엔티티를 InquiryDetailResponseDto로 변환
     */
    private InquiryDetailResponseDto toDetailDto(Inquiry inquiry, User author) {
        return InquiryDetailResponseDto.builder()
                .id(inquiry.getId())
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .userId(author != null ? author.getId() : null)
                .username(inquiry.getUsername())
                .userPicture(author != null ? author.getPicture() : null)
                .answered(inquiry.isAnswered())
                .isPublic(inquiry.isPublic())
                .imageUrls(inquiry.getImages().stream().map(InquiryImage::getImageUrl).collect(Collectors.toList()))
                .createdAt(inquiry.getCreatedAt())
                .updatedAt(inquiry.getUpdatedAt())
                .build();
    }

    /**
     * Inquiry 엔티티를 InquiryListResponseDto로 변환
     */
    private InquiryListResponseDto toListDto(Inquiry inquiry, User author) {
        String displayTitle = inquiry.isPublic() ? inquiry.getTitle() : "비공개 게시글입니다.";
        return InquiryListResponseDto.builder()
                .id(inquiry.getId())
                .title(displayTitle)
                .userId(author != null ? author.getId() : null)
                .username(inquiry.getUsername())
                .userPicture(author != null ? author.getPicture() : null)
                .answered(inquiry.isAnswered())
                .isPublic(inquiry.isPublic())
                .createdAt(inquiry.getCreatedAt())
                .build();
    }
}