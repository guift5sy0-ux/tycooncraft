# TycoonCraft

친구들과 공장을 짓고, 가공품을 팔아 돈을 벌고, 그 돈으로 더 큰 공장을 만드는 경제·공장 모드.
**Minecraft 1.21.1 · NeoForge 21.1** — 다른 모드 없이 단독으로 동작합니다.

## 받는 법
저장소의 **Actions** 탭 → 가장 최근 성공한 "Build" 실행 → 아래쪽 **Artifacts** 의 `tycooncraft-jar` 를 받아 압축을 풀고,
`.jar` 파일을 `mods` 폴더에 넣으세요. (`v0.6.0` 같은 태그를 올리면 **Releases** 에도 올라갑니다.)

## 주요 기능
- **동전 & 환전기**: 구리 ₩1 · 철 ₩10 · 금 ₩100 · 에메랄드 ₩1,000 · 다이아 ₩10,000
- **판매소 & 변동 시세**: 많이 팔면 가격이 떨어지고 시간이 지나면 회복
- **컨베이어 · 상승/하강 리프트** (동력 필요 없음)
- **기계 7종**: 분쇄기 · 제련기 · 합금로 · 압착기 · 인발기 · 절삭기 · 조립기
- **스타터 키트 & 가이드 책** (`/starter`, `/guide`)
- **상인 마을 · 폐공장** 자연 생성, 마을 상자·몹 처치 시 동전
- 명령어: `/sell`, `/sell all`, `/price`, `/prices`, 관리자용 `/tycoon factory|village|ruin|nearest`

## 개발
`./gradlew build` → `build/libs/tycooncraft-<버전>.jar`
코드를 `main` 에 올리면 GitHub Actions 가 자동으로 빌드합니다.
