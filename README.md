# DevNote: 개발자 콘텐츠 큐레이션 플랫폼

> **"지식의 분산화를 해결하고, 개발자에게 필요한 정보만 정제하여 제공한다."**  
> DevNote는 MSA 기반의 콘텐츠 큐레이션 플랫폼으로, 수많은 IT 뉴스 및 YouTube 개발 정보를 효율적으로 수집하고 사용자에게 개인화된 형태로 제공합니다.

---

## 아키텍처 설계 (Architecture)

단일 서비스의 한계를 극복하고, 서비스별 독립적인 확장성과 장애 격리를 위해 **Spring Cloud 기반의 MSA**를 채택했습니다.

```mermaid
graph TD
    subgraph "Client Layer"
        Next[Next.js Client]
    end

    subgraph "Infrastructure Layer"
        Nginx[Nginx Reverse Proxy]
        Gateway[Spring Cloud Gateway]
        Eureka[Eureka Server - Discovery]
    end

    subgraph "Microservices Layer"
        User[User Service - Auth/Profile]
        News[News & YouTube Service]
        Processor[Processor Service - Sync]
        Stats[Stats Service - Ranking]
    end

    subgraph "Data & Messaging Layer"
        MariaDB[(MariaDB)]
        Redis[(Redis - Token/Cache)]
        Kafka[[Apache Kafka]]
        ES[(Elasticsearch)]
    end

    %% Flow
    Next -->|HTTPS| Nginx
    Nginx --> Gateway
    Gateway --> User
    Gateway --> News
    Gateway --> Processor
    Gateway --> Stats

    User <--> MariaDB
    User <--> Redis
    Processor --> Kafka
    Stats <-- Kafka
    Stats <--> ES
```

---

## 주요 기술 스택 및 선정 이유

### **Backend Framework**
- **Spring Boot 3.x & Spring Cloud**: MSA 환경에서의 서비스 검색(Eureka), 라우팅(Gateway), 설정 중앙화 등을 위해 도입했습니다.
- **Spring Security & OAuth2**: 구글, 네이버, 카카오 등 다양한 소셜 로그인을 통합 관리하고, 보안이 강화된 **JWT + HttpOnly Cookie** 방식을 구현했습니다.

### **Communication & Persistence**
- **Apache Kafka**: 서비스 간 결합도를 낮추기 위해 도입했습니다. 통계 데이터 처리 및 랭킹 시스템 반영을 비동기로 처리하여 메인 로직의 응답 속도를 최적화했습니다.
- **Redis**: 분산 환경에서의 세션 관리 문제를 해결하고, JWT Refresh Token 저장 및 빈번하게 조회되는 랭킹 데이터 캐싱을 담당합니다.
- **MariaDB & Elasticsearch**: 정형 데이터의 무결성을 위해 MariaDB를, 대용량 통계 데이터의 빠른 집계와 검색을 위해 Elasticsearch를 병행 사용(CQRS 패턴 지향)합니다.

---

## 서비스 상세 역할

| 서비스 | 설명 |
| :--- | :--- |
| **Eureka Server** | 마이크로서비스들의 동적 주소 관리 및 서비스 디스커버리 수행 |
| **API Gateway** | 단일 진입점으로 라우팅, CORS 처리 및 공통 인증 필터링 |
| **User Service** | OAuth2 소셜 로그인, 유저 프로필 관리, 커뮤니티 게시판 기능을 제공하는 서비스 |
| **News-YouTube** | IT 뉴스 크롤링 메타데이터 관리 및 YouTube API 연동 콘텐츠 서빙 |
| **Processor** | 외부 데이터 정제 및 Kafka를 통한 도메인 이벤트 발행 전담 |
| **Stats** | 실시간 트래픽 집계, 콘텐츠 랭킹 알고리즘 처리 (Elasticsearch 기반) |

---
