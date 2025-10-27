package com.example.devnote.controller;

import com.example.devnote.dto.*;
import com.example.devnote.entity.enums.BoardType;
import com.example.devnote.entity.enums.PostSortType;
import com.example.devnote.entity.enums.SearchType;
import com.example.devnote.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /**
     * 게시글 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponseDto<PostDetailResponseDto>> createPost(@Valid @RequestBody PostRequestDto requestDto) {
        PostDetailResponseDto responseDto = postService.createPost(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponseDto<>("게시글이 성공적으로 등록되었습니다.", HttpStatus.CREATED.value(), responseDto));
    }

    /**
     * 게시글 목록 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponseDto<Page<PostListResponseDto>>> getPosts(
            @RequestParam(required = false) BoardType boardType,
            @RequestParam(required = false, defaultValue = "LATEST") PostSortType sortType,
            @RequestParam(required = false) SearchType searchType,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<PostListResponseDto> responsePage = postService.getPosts(boardType, sortType, searchType, keyword, pageable); // ✅ searchType 전달
        return ResponseEntity.ok(new ApiResponseDto<>("게시글 목록을 조회했습니다.", HttpStatus.OK.value(), responsePage));
    }

    /**
     * 게시글 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<PostDetailResponseDto>> getPost(@PathVariable Long id) {
        PostDetailResponseDto responseDto = postService.getPostById(id);
        return ResponseEntity.ok(new ApiResponseDto<>("게시글 상세 정보를 조회했습니다.", HttpStatus.OK.value(), responseDto));
    }

    /**
     * 게시글 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDto<PostDetailResponseDto>> updatePost(@PathVariable Long id, @Valid @RequestBody PostRequestDto requestDto) {
        PostDetailResponseDto responseDto = postService.updatePost(id, requestDto);
        return ResponseEntity.ok(new ApiResponseDto<>("게시글이 성공적으로 수정되었습니다.", HttpStatus.OK.value(), responseDto));
    }

    /**
     * 게시글 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDto<Void>> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.ok(new ApiResponseDto<>("게시글이 성공적으로 삭제되었습니다.", HttpStatus.OK.value(), null));
    }

    /**
     * Q&A 답변 채택
     */
    @PostMapping("/{id}/adopt")
    public ResponseEntity<ApiResponseDto<PostDetailResponseDto>> adoptAnswer(@PathVariable("id") Long postId, @Valid @RequestBody AdoptionRequestDto requestDto) {
        PostDetailResponseDto responseDto = postService.adoptAnswer(postId, requestDto.getCommentId());
        return ResponseEntity.ok(new ApiResponseDto<>("답변이 채택되었습니다.", HttpStatus.OK.value(), responseDto));
    }

    /**
     * 에디터 이미지 업로드
     */
    @PostMapping("/images")
    public ResponseEntity<ApiResponseDto<Map<String, String>>> uploadImage(@RequestParam("image") MultipartFile imageFile) {
        String imageUrl = postService.uploadImage(imageFile);
        return ResponseEntity.ok(new ApiResponseDto<>("이미지가 성공적으로 업로드되었습니다.", HttpStatus.OK.value(), Map.of("imageUrl", imageUrl)));
    }
}
