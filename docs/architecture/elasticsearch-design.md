# Elasticsearch 상품 검색 설계 (ISSUE-32~35)

---

## ES1: 상품 검색 서비스 아키텍처

### 현재 문제

- JPA LIKE 쿼리: 풀 텍스트 검색 불가, 인덱스 미활용
- 한국어 형태소 분석 불가 (예: "나이키" → "나이키운동화" 미매칭)
- 관련도 점수(relevance score) 없음

### 목표 아키텍처

```
사용자 검색 요청
      │
      ▼
Search API (GET /products/search?q=나이키)
      │
      ▼
ProductSearchService
      │
      ├─ Elasticsearch 조회 (relevance score 기반)
      │
      └─ 결과 반환

(별도 동기화)
ProductService.createProduct()
      │
      ▼ Spring Event or Kafka
ProductSearchIndexer
      │
      ▼
Elasticsearch 인덱스 업데이트
```

---

## ES2: 인덱싱 전략

### 인덱스 매핑

```json
{
  "mappings": {
    "properties": {
      "id": {"type": "long"},
      "name": {
        "type": "text",
        "analyzer": "korean",
        "fields": {
          "keyword": {"type": "keyword"}
        }
      },
      "description": {
        "type": "text",
        "analyzer": "korean"
      },
      "price": {"type": "scaled_float", "scaling_factor": 100},
      "stockQuantity": {"type": "integer"},
      "status": {"type": "keyword"},
      "sellerId": {"type": "long"},
      "createdAt": {"type": "date"}
    }
  }
}
```

### 한국어 분석기

```json
{
  "settings": {
    "analysis": {
      "analyzer": {
        "korean": {
          "type": "custom",
          "tokenizer": "nori_tokenizer",
          "filter": ["nori_part_of_speech", "lowercase"]
        }
      }
    }
  }
}
```

### 동기화 전략

| 방식 | 장점 | 단점 |
|------|------|------|
| 실시간 동기 | 즉시 반영 | 결제·검색 DB 커플링 |
| 이벤트 기반 비동기 | 디커플링 | 약간의 지연 |
| CDC (Debezium) | 안정적 | 인프라 복잡도 ↑ |

→ **선택**: 이벤트 기반 비동기 (Spring Event → Kafka 연동)

---

## ES3: 검색 API 구현 계획

### 엔드포인트

```
GET /products/search?q={키워드}&minPrice={최소가}&maxPrice={최대가}&page={페이지}&size={사이즈}
```

### 쿼리 전략

```json
{
  "query": {
    "bool": {
      "must": [
        {
          "multi_match": {
            "query": "나이키 운동화",
            "fields": ["name^3", "description"],
            "type": "best_fields"
          }
        }
      ],
      "filter": [
        {"term": {"status": "ON_SALE"}},
        {"range": {"price": {"gte": 10000, "lte": 200000}}}
      ]
    }
  },
  "sort": [
    {"_score": {"order": "desc"}},
    {"createdAt": {"order": "desc"}}
  ]
}
```

---

## ES4: 검색 성능 튜닝

### 예상 병목 지점

1. **인덱스 새로고침**: 기본 1초 → 대량 색인 시 비활성화 후 재활성화
2. **페이징**: Deep pagination(from+size) 대신 search_after 사용
3. **필드 데이터 캐시**: Aggregation용 keyword 필드 분리
4. **레플리카**: 읽기 부하 분산 (replica shard로 검색 요청 분산)

### 성능 목표

| 지표 | 목표 |
|------|------|
| 검색 응답 시간 | p99 < 100ms |
| 인덱싱 지연 | < 2초 |
| 동시 검색 | 1000 QPS |
