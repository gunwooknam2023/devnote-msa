package com.example.devnote.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 서버에 파일을 저장하고 관리하는 역할을 전담하는 범용 서비스
 */
@Service
@Slf4j
public class FileStorageService {

    // 업로드 파일이 저장될 최상위 기본 경로를 설정
    private final Path fileStorageRootLocation;
    private final long maxFileSize;

    // 허용할 이미지 파일의 MIME 타입 목록
    private static final List<String> ALLOWED_IMAGE_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/png", "image/gif");

    /**
     * 생성자
     * @param maxFileSizeStr 파일 최대 크기 (문자열, 예: "10MB")
     */
    public FileStorageService(@Value("${spring.servlet.multipart.max-file-size}") String maxFileSizeStr) {
        this.fileStorageRootLocation = Paths.get("./uploads").toAbsolutePath().normalize();
        this.maxFileSize = parseSize(maxFileSizeStr);

        try {
            // 서버 시작 시 기본 업로드 폴더가 없으면 생성
            Files.createDirectories(this.fileStorageRootLocation);
        } catch (Exception ex) {
            throw new RuntimeException("파일을 업로드할 디렉터리를 생성할 수 없습니다.", ex);
        }
    }

    /**
     * 파일을 지정된 하위 디렉터리에 저장하고, 서버에서 접근 가능한 URL 경로를 반환
     * @param file 저장할 MultipartFile
     * @param subDirectory 저장할 하위 폴더 (예: "inquiries", "profile")
     * @return 저장된 파일의 접근 URL (예: /images/profile/uuid-filename.jpg)
     */
    public String storeFile(MultipartFile file, String subDirectory) {
        // 1. 파일 유효성 검사
        validateFile(file);

        // 2. 하위 폴더를 포함한 전체 저장 경로를 설정. (예: ./uploads/profile)
        Path storageDirectory = this.fileStorageRootLocation.resolve(subDirectory).normalize();
        try {
            // 하위 폴더가 없으면 생성
            Files.createDirectories(storageDirectory);
        } catch (Exception ex) {
            throw new RuntimeException("하위 디렉터리를 생성할 수 없습니다.", ex);
        }

        // 3. 파일 이름에서 확장자를 추출하고, 중복되지 않는 고유한 파일명을 생성
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension;
        try {
            if (originalFileName.contains("..")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 파일 경로가 포함되어 있습니다: " + originalFileName);
            }
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일 확장자를 확인할 수 없습니다.");
        }
        String storedFileName = UUID.randomUUID().toString() + fileExtension;

        // 4. 파일을 실제로 디스크에 저장
        try {
            Path targetLocation = storageDirectory.resolve(storedFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new RuntimeException(storedFileName + " 파일을 저장할 수 없습니다. 다시 시도해주세요.", ex);
        }

        // 5. 외부에서 접근 가능한 최종 URL 경로를 반환 (예: /images/profile/uuid-filename.jpg)
        return "/images/" + subDirectory + "/" + storedFileName;
    }

    /**
     * 서버에 저장된 파일을 URL 경로를 기반으로 삭제합니다.
     * @param fileUrl 삭제할 파일의 URL (예: /images/profile/uuid-name.jpg)
     */
    public void deleteFile(String fileUrl) {
        // 기본 이미지 경로는 삭제하지 않도록 예외 처리
        if (fileUrl == null || fileUrl.isBlank() || !fileUrl.startsWith("/images/")) {
            return;
        }
        try {
            // URL 경로에서 실제 파일 시스템 경로를 계산
            // 예: /images/profile/some-file.jpg -> ./uploads/profile/some-file.jpg
            String relativePath = fileUrl.substring("/images/".length());
            Path targetLocation = this.fileStorageRootLocation.resolve(relativePath).toAbsolutePath().normalize();
            Files.deleteIfExists(targetLocation);
        } catch (Exception e) {
            log.error("파일 삭제에 실패했습니다: {}", fileUrl, e);
        }
    }

    /**
     * 파일이 유효한지 (빈 파일, 크기, 타입) 검사하는 헬퍼 메서드
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "업로드할 파일이 비어있습니다.");
        }
        if (file.getSize() > this.maxFileSize) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일 크기가 너무 큽니다. (최대 " + (maxFileSize / 1024 / 1024) + "MB)");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_CONTENT_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "허용되지 않는 파일 형식입니다. (jpeg, png, gif만 가능)");
        }
    }

    /**
     * '10MB'와 같은 문자열 크기를 long 타입의 바이트로 변환하는 헬퍼 메서드
     */
    private long parseSize(String size) {
        size = size.toUpperCase();
        if (size.endsWith("MB")) {
            return Long.parseLong(size.substring(0, size.length() - 2)) * 1024 * 1024;
        }
        if (size.endsWith("KB")) {
            return Long.parseLong(size.substring(0, size.length() - 2)) * 1024;
        }
        return Long.parseLong(size);
    }
}