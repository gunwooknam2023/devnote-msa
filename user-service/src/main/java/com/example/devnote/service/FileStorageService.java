package com.example.devnote.service;

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

@Service
public class FileStorageService {

    private final Path fileStorageLocation; // 파일이 저장될 실제 경로
    private final long maxFileSize;         // 허용되는 파일의 최대 크기

    // 허용할 이미지 파일의 MIME 타입 목록
    private static final List<String> ALLOWED_IMAGE_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/png", "image/gif");

    /**
     * 생성자
     * @param uploadDir 파일 저장 디렉터리 경로
     * @param maxFileSizeStr 파일 최대 크기 (문자열, 예: "10MB")
     */
    public FileStorageService(@Value("${file.upload-dir}") String uploadDir,
                              @Value("${spring.servlet.multipart.max-file-size}") String maxFileSizeStr) {

        // 1. 파일 저장 위치(Path 객체)를 설정
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        // 2. '10MB'와 같은 문자열을 long 타입의 바이트로 변환
        this.maxFileSize = parseSize(maxFileSizeStr);

        try {
            // 3. 서버 시작 시 파일 저장 디렉터리가 없으면 자동으로 생성
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("파일을 업로드할 디렉터리를 생성할 수 없습니다.", ex);
        }
    }

    /**
     * 파일을 유효성 검사 후 저장하고, 서버에서 접근 가능한 URL 경로를 반환
     * @param file 저장할 MultipartFile
     * @return 저장된 파일의 접근 URL (예: /images/inquiries/uuid-filename.jpg)
     */
    public String storeFile(MultipartFile file) {
        // 1. 파일 유효성 검사를 먼저 수행
        validateFile(file);

        // 2. 원본 파일 이름에서 확장자를 추출하고, 고유한 파일명을 생성
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

        // 3. 파일을 실제로 디스크에 저장
        try {
            Path targetLocation = this.fileStorageLocation.resolve(storedFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new RuntimeException(storedFileName + " 파일을 저장할 수 없습니다. 다시 시도해주세요.", ex);
        }

        // 4. 외부에서 접근 가능한 최종 URL 경로를 반환
        return "/images/inquiries/" + storedFileName;
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

    /**
     * 서버에 저장된 파일 삭제
     * @param fileUrl 삭제할 파일의 URL (예: /images/inquiries/uuid-name.jpg)
     */
    public void deleteFile(String fileUrl) {
        try {
            String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.deleteIfExists(targetLocation);
        } catch (Exception e) {
            System.err.println("파일 삭제에 실패했습니다: " + fileUrl);
            e.printStackTrace();
        }
    }
}