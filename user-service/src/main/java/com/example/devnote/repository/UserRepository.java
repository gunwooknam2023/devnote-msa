package com.example.devnote.repository;

import com.example.devnote.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByProviderAndProviderId(String provider, String providerId);
    Optional<User> findByEmail(String email);

    /** 활동점수 증가 */
    @Modifying
    @Query("UPDATE User u SET u.activityScore = u.activityScore + 1 WHERE u.id = :userId")
    void incrementActivityScore(@Param("userId") Long userId);

    /** 활동점수 감소 */
    @Modifying
    @Query("UPDATE User u SET u.activityScore = u.activityScore - 1 WHERE u.id = :userId")
    void decrementActivityScore(@Param("userId") Long userId);

    /**
     * 활동 점수가 가장 높은 사용자 상위 10명을 조회
     */
    List<User> findTop10ByOrderByActivityScoreDesc();
}
