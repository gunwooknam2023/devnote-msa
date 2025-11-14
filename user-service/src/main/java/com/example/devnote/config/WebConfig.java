package com.example.devnote.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.profile-upload-dir}")
    private String profileUploadDir;

    @Value("${file.posts-upload-dir}")
    private String postsUploadDir;

    /**
     * 정적 리소스 핸들러를 추가하여 URL과 실제 파일 경로를 매핑
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 클라이언트가 /images/inquiries/some-file.jpg 라는 URL로 요청하면,
        // 서버의 실제 물리 경로인 file:./uploads/inquiries/some-file.jpg 파일을 찾아서 제공
        registry.addResourceHandler("/images/inquiries/**")
                .addResourceLocations("file:" + uploadDir + "/");

        // /images/profile/ 로 시작하는 URL 요청이 오면
        // file:./uploads/profile/ 경로에서 파일을 찾아 반환
        registry.addResourceHandler("/images/profile/**")
                .addResourceLocations("file:" + profileUploadDir + "/");

        // /images/posts/ 로 시작하는 URL 요청이 오면
        // file:./uploads/posts/ 경로에서 파일을 찾아 반환
        registry.addResourceHandler("/images/posts/**")
                .addResourceLocations("file:" + postsUploadDir + "/");
    }
}