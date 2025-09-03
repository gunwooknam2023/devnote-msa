package com.example.devnote.service;

import com.example.devnote.dto.AdminStatusDto;
import com.example.devnote.dto.NoticeDetailResponseDto;
import com.example.devnote.dto.NoticeListResponseDto;
import com.example.devnote.dto.NoticeRequestDto;
import com.example.devnote.entity.Notice;
import com.example.devnote.entity.User;
import com.example.devnote.repository.NoticeRepository;
import com.example.devnote.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Value("${app.admin-email}")
    private String adminEmail;

    /**
     * 관리자인지 확인하고, 관리자 User 객체를 반환하는 헬퍼 메서드
     * @return 관리자 User 엔티티
     * @throws ResponseStatusException 사용자가 관리자가 아닐 경우 403 Forbidden
     */
    private User getAdminUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !auth.getName().equals(adminEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "이 작업을 수행할 권한이 없습니다.");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "관리자 계정을 찾을 수 없습니다."));
    }

    /**
     * 에디터에서 사용할 이미지 단일 업로드
     */
    public String uploadImage(MultipartFile imageFile) {
        // 관리자만 이미지 업로드가 가능하도록 권한 확인
        getAdminUser();
        return fileStorageService.storeFile(imageFile, "inquiries");
    }

    /**
     * 공지사항 생성 (관리자 전용)
     */
    @Transactional
    public NoticeDetailResponseDto createNotice(NoticeRequestDto dto) {
        User admin = getAdminUser(); // 관리자 권한 확인

        Notice notice = Notice.builder()
                .user(admin)
                .category(dto.getCategory())
                .title(dto.getTitle())
                .content(dto.getContent())
                .build();

        Notice savedNotice = noticeRepository.save(notice);
        return toDetailDto(savedNotice);
    }

    /**
     * 공지사항 수정 (관리자 전용)
     */
    @Transactional
    public NoticeDetailResponseDto updateNotice(Long noticeId, NoticeRequestDto dto) {
        User admin = getAdminUser(); // 관리자 권한 확인

        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "공지사항을 찾을 수 없습니다."));

        notice.setCategory(dto.getCategory());
        notice.setTitle(dto.getTitle());
        notice.setContent(dto.getContent());

        Notice updatedNotice = noticeRepository.save(notice);
        return toDetailDto(updatedNotice);
    }

    /**
     * 공지사항 삭제 (관리자 전용)
     */
    @Transactional
    public void deleteNotice(Long noticeId) {
        getAdminUser(); // 관리자 권한 확인

        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "공지사항을 찾을 수 없습니다."));

        // 본문(HTML)에서 이미지 파일 URL을 파싱하여 서버에서 실제 파일을 삭제
        deleteImagesFromContent(notice.getContent());

        noticeRepository.delete(notice);
    }

    /**
     * 공지사항 상세 조회 (누구나 가능)
     */
    @Transactional(readOnly = true)
    public NoticeDetailResponseDto getNoticeDetail(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "공지사항을 찾을 수 없습니다."));
        return toDetailDto(notice);
    }

    /**
     * 공지사항 목록 조회 (누구나 가능)
     */
    @Transactional(readOnly = true)
    public Page<NoticeListResponseDto> getNoticeList(Pageable pageable) {
        Page<Notice> noticePage = noticeRepository.findAllByOrderByCreatedAtDesc(pageable);
        return noticePage.map(this::toListDto);
    }

    private NoticeDetailResponseDto toDetailDto(Notice notice) {
        return NoticeDetailResponseDto.builder()
                .id(notice.getId())
                .category(notice.getCategory())
                .title(notice.getTitle())
                .content(notice.getContent())
                .authorName(notice.getUser().getName())
                .createdAt(notice.getCreatedAt())
                .updatedAt(notice.getUpdatedAt())
                .build();
    }

    private NoticeListResponseDto toListDto(Notice notice) {
        return NoticeListResponseDto.builder()
                .id(notice.getId())
                .category(notice.getCategory())
                .title(notice.getTitle())
                .authorName(notice.getUser().getName())
                .createdAt(notice.getCreatedAt())
                .build();
    }

    /**
     * HTML 컨텐츠에서 img 태그의 src 속성을 추출하여 실제 파일을 삭제하는 헬퍼 메서드
     */
    private void deleteImagesFromContent(String content) {
        if (content == null || content.isBlank()) {
            return;
        }
        // img 태그를 찾는 정규식. src 속성의 값을 그룹으로 캡처
        Pattern pattern = Pattern.compile("<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>");
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            // 캡처된 src 값(URL)
            String imageUrl = matcher.group(1);
            fileStorageService.deleteFile(imageUrl);
        }
    }

    /**
     * 현재 로그인한 사용자가 관리자인지 확인하는 메서드
     * @return AdminStatusDto (isAdmin: true 또는 false)
     */
    public AdminStatusDto checkAdminStatus() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean isAdmin = false;

        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            if (auth.getName().equals(adminEmail)) {
                isAdmin = true;
            }
        }

        return new AdminStatusDto(isAdmin);
    }
}