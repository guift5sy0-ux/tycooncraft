package com.tycooncraft.event;

import com.tycooncraft.economy.Coins;
import com.tycooncraft.machine.MachineType;
import com.tycooncraft.registry.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.ArrayList;
import java.util.List;

/** 첫 접속 스타터 키트 & 가이드 책 */
public final class StarterKit {
    private static final String FLAG = "tycooncraft_starter";

    private static final String[][] GUIDE = {
            {"타이쿤크래프트", "공장을 짓고, 가공품을 팔아 돈을 벌고, 그 돈으로 더 큰 공장을 만드는 모드예요.\n\n전선으로 전기를 이으면\n기계가 1.5배 빨라져요!\n→ 17장 산업 혁명"},
            {"0. 공장 키트", "공장 키트를 들고 우클릭하면 바라보는 쪽에 기본 공장이 세워져요.\n\n웅크리고 우클릭: 자리 미리 보기\n\n보일러·증기기관·전선 포함\n(보일러에 석탄 넣기!)\n원석을 첫 벨트에 던지면\n분쇄→제련→압착→판매 자동"},
            {"1. 가공하면 비싸요", "철 원석 1개 ₩13\n→ 분쇄: 가루 2 ₩22\n→ 제련: 주괴 2 ₩40\n→ 압착: 철판 2 ₩72\n→ 절삭: 기어 2 ₩126\n\n/prices : 전체 시세"},
            {"2. 판매소", "빈손 우클릭: 판매소 창\n왼쪽 칸에 물건 → 판매 예정 금액 표시 → [판매]\n[인출]: 번 돈을 동전으로\n\n물건 들고 우클릭: 바로 판매\n벨트로 넣으면 자동 판매\n/sell, /sell all"},
            {"3. 동전 & 환전기", "구리 ₩1 · 철 ₩10\n금 ₩100 · 에메랄드 ₩1,000\n다이아 ₩10,000\n\n환전기 우클릭: 동전을 큰 단위로 정리\n제작대: 큰 동전 1 → 작은 동전 10"},
            {"4. 컨베이어", "가고 싶은 방향을 바라보고 설치하면 화살표 방향으로 운반해요.\n\n아이템은 Q로 던져서 올려요.\n\n끝에 기계나 판매소를 두세요."},
            {"5. 리프트", "같은 리프트를 세로로 쌓으면 옆에 닿은 아이템이\n상승: 맨 위로\n하강: 맨 아래로\n이동해 끝 블록의 화살표 방향으로 나와요.\n\n입구는 화살표 반대쪽 면에!"},
            {"6. 기계 사용법", "우클릭: 기계 창 열기\n재료 칸에 넣으면 가공 → 결과 칸\n화살표에 마우스: 레시피\n\n자동화: 옆·위로 재료가 닿으면 흡수, 앞이 컨베이어·상자면 결과물 자동 배출\n안 쓰는 아이템은 통과!"},
            {"7. 1단계 기계", "분쇄기\n 원석 → 가루 2\n 석탄 → 석탄 가루 2\n 밀 → 밀가루 2\n제련기\n 가루 → 주괴\n 밀가루 → 빵\n압착기\n 주괴 → 판"},
            {"8. 2단계 기계", "(₩1,000 + 판)\n합금로\n 철 주괴+석탄 가루 2\n → 강철 주괴\n인발기\n 구리/금 → 선 3\n 철/강철 → 봉 2\n절삭기\n 판 → 기어\n 원목 → 판자 6"},
            {"9. 조립기", "(₩10,000 + 강철판)\n회로 ₩75\n 구리선3+철판+레드스톤\n기계 부품 ₩378\n 철기어2+강철봉+회로\n고급 회로 ₩432\n 회로2+금선2+강철판\n엔진 ₩937\n 강철판4+강철기어2+기계부품"},
            {"10. 제작대 레시피", "기계 없이도 OK\n(재료는 더 들어요)\n\n강철 주괴\n 철 주괴 + 석탄 2\n강철판\n 강철 주괴 2\n회로\n 구리 주괴 3 + 레드스톤 2 + 철 주괴"},
            {"11. 시세", "같은 걸 많이 팔면 서버 전체 가격이 떨어져요. (최저 25%)\n\n시간이 지나면 회복돼요.\n\n여러 라인을 돌려서 골고루 파는 게 이득!"},
            {"12. 총기", "부품 (기계로 생산)\n 총열: 절삭기(강철봉)\n 기관부·개머리판: 조립기\n\n제작대에서 조합\n 권총·기관단총·산탄총·소총·저격총\n\n웅크리고 우클릭: 재장전\n저격총: 꾹 눌러 조준"},
            {"12-2. 탄약", "조립기에서 대량 생산\n경탄약 x16\n 구리판+화약+철봉\n중탄약 x8\n 구리판+화약2+강철봉\n산탄 x8\n 구리판+화약+철 가루2\n\n화약: 분쇄기(부싯돌)"},
            {"15. 탈것", "조립기에서 제작 (엔진부터 넣기)\n\n자동차 ₩2,990\n 엔진+강철판6+강철기어4\n +회로2+유리4+검은염료4\n기관차 ₩6,890\n 엔진2+강철판12+강철기어8\n +기계장치2+회로2+유리4\n헬기 ₩8,230\n 엔진2+강철판10+고급회로3\n +기계장치3+강철봉8+유리6"},
            {"15-2. 운전", "연료 들고 우클릭으로 주유\n 연료통(조립기: 철판+석탄가루4→2)\n 석탄·석탄블록·용암 양동이\n\n자동차 W/S A/D, 스페이스 경적\n기관차 W 가속 S 브레이크\n 웅크리고 우클릭 화물칸\n헬기 로터가 다 돌면 이륙\n 스페이스↑ Ctrl↓ W/S A/D"},
            {"15-3. 정비", "강철판 들고 우클릭: 수리\n맨손으로 계속 치면 아이템으로 회수\n총에 맞아 부서지면 폭발!\n웅크리기: 내리기\n\n오른쪽 아래 계기판에\n속도·연료·차체 표시"},
            {"12-3. 소음기", "조립기: 강철판+강철봉+흰 양털\n\n제작대에 총+소음기 → 장착\n(탄·인챈트 그대로)\n소음기 단 총만 놓으면 → 분리\n\n소리 작음, 총구 화염·궤적 없음\n피해 -10%, 반동 감소"},
            {"12-4. 총 인챈트", "인챈트 테이블·모루로 총에!\n화력 I~V 피해+15%씩\n속사 I~III 연사+\n확장 탄창 I~III +25%씩\n빠른 장전 I~III\n관통탄 I~III 여러 명 관통\n정밀 사격 I~III 퍼짐·반동↓\n소이탄 불 붙임\n탄약 절약 I~III\n헤드헌터 I~II 헤드샷 강화"},
            {"12-5. 자폭 드론", "조립기: 기계장치+회로2+철판4\n +TNT+유리판+구리선2\n\n우클릭: 띄우고 드론 화면으로\nW 보는 쪽으로 비행 A/D 옆\n스페이스↑ Ctrl↓\n클릭: 폭파 / 빠르게 부딪혀도 폭발\n한 번 띄우면 취소 불가 (무조건 폭발)\n배터리 45초 · 거리 100블록"},
            {"16. 신무기", "VSS: 일체형 소음기 저격소총\n F: 단발(꾹 눌러 4배 조준)↔연사\n쌍권총: 빠른 연사, 작은 탄창\nRPG: 로켓 1발, 맞으면 폭발\n\n상위 소음기 (16블록, 피해 감소 없음)\n일반 소음기는 32블록까지 들림"},
            {"16-2. 방탄", "방탄조끼 I/II/III\n 몸통 총알 피해 -15/25/35%\n방탄모 I/II/III\n 헤드샷 추가 피해 -20/35/50%\n\n낙하산: 다리 칸에 착용\n 떨어지면 자동으로 펼쳐짐\n W 로 앞으로 활공"},
            {"16-3. 병기", "자동 포탑: 탄약 넣으면\n 24블록 안 몹 자동 사격\n전차: 포탑은 마우스\n 좌클릭 주포(전차 포탄)\n전투기: W/S 출력, 마우스 조종\n Ctrl/Z: 에어브레이크(감속)\n 좌클릭 기관포 우클릭 미사일\n 비행 중 F: 배럴 롤\nMK2 드론: 더 빠르고 강력"},
            {"16-4. 적", "갱단원(권총) 용병(소총·방탄)\n무법자(산탄총) - 밤에 출몰\n변이 거미 여왕 - 드문 거대 거미\n 거미줄 뱉기(둔화) · 맹독 침 5발\n\n보스: 카르텔 두목\n 카르텔 신호탄으로 소환\n 소총 연사 + RPG + 부하 소환\n 보상: 다이아 동전, VSS/RPG"},
            {"16-5. 디스트로이어 변종", "정찰 디스트로이어\n 밤에 3~5마리 떼\n 좌우로 꺾으며 돌진·베기\n 정찰 코어(8%) ₩2,000\n\n공중 디스트로이어\n (월드 3일째~, 밤)\n 미니 드론 10기·정찰기 투하\n 동력원 3개: 맞은 쪽이 깎임\n 2개 부수면 추락 → 대폭발!\n 비행 코어 ₩25,000"},
            {"16-6. 코어 장비", "비행 코어 엔진\n 운송기 엔진 상위호환\n 엔진 6개 추진·부유체 40개\n 최고속도↑ 연료 40%↓\n\n드론 편대 발사기\n 우클릭: 드론 6기 발진\n 1분간 각자 적 사격\n\n프로토-정찰 (정찰코어4)\n 좌클릭 베기·우클릭 덮치기\n A/D 두 번: 옆 대시"},
            {"16-4b. 이계의 심판자", "날카로운 반자동 전설 권총\n신호기 네더라이트 신호기\n네더라이트 드래곤알 네더라이트\n네더라이트 권총 네더라이트\n\n중탄약 9발, 피해 15\n맞히면 표식 → 3초 안에\n 다시 맞히면 심판(번개 6)\n칼날 근접 피해 12\n내구도 500발, 다 닳으면 터짐\n 모루+네더라이트 주괴로 수리"},
            {"13-2. 보스 몹", "파괴수 골렘(중간 보스)\n 지상 밤, 드묾\n 개틀링·로켓·내려찍기\n\n화산 거수 베헤모스\n 네더, 매우 드묾\n 돌진(벽에 박으면 기절=약점)\n 내려찍기·화염 브레스·운석\n\n폭풍 와이번\n 산악 지대 밤, 드묾\n 번개 구체·급강하·낙뢰"},
            {"13-4. 강화된 위더", "심연의 위더 (체력 900)\n 해골 폭풍 · 영혼 폭발\n 암흑 운석(검은 고리 피하기)\n체력 절반: 각성\n 영혼 기사단 3기 소환\n 영혼 돌진 추가\n추가 보상: 네더라이트 조각,\n 다이아 동전"},
            {"13-5. 강화된 드래곤", "공허의 군주 (체력 800)\n 화염구 부채꼴 · 셜커 탄 군단\n 착지 중 충격파(거리 두기)\n체력 절반: 각성\n 공허 붕괴 - 발밑 보라 고리\n 2초 안에 벗어나기!\n추가 보상: 네더라이트 주괴 2,\n 겉날개, 다이아 동전 5"},
            {"13-3. 보스 전리품", "판매가 (기준)\n 용의 알 ₩150,000\n 네더의 별 ₩30,000\n 겉날개 ₩30,000\n 디스트로이어 코어 ₩10,000\n 용 머리 ₩10,000\n 위더 해골 ₩4,000\n 용의 숨결 ₩1,000\n 맹독 침 ₩500\n\n장물아비에겐 1.2배!"},
            {"16-5. 수류탄", "유황 광석: 지하 -48~72\n 곡괭이(돌 이상)로 채굴 → 유황\n\n파편 수류탄: 철판+유황2+화약\n섬광탄: 철판+유황+발광석가루2\n 보고 있으면 화면이 하얗게\n연막탄: 철판+유황+석탄가루+종이\n우클릭 던지기, 웅크리면 굴리기"},
            {"16-6. 자동 굴착기", "드릴: 다이아+강철주괴2\n +강철톱니+강철판3\n바퀴: 말린켈프블록4+강철막대\n드릴+바퀴+엔진 = 자동 굴착기\n\n설치 후 연료 우클릭 → 굴착 시작\n3×3 터널, 다이아 광석까지\n단단할수록 느리고 연료 많이\n빈손 우클릭: 저장통\n웅크리고 우클릭: 켜기/끄기"},
            {"13. 탐험", "상인 마을 (큰 산업 마을)\n 상인 12명이 동전으로 거래\n 총기상·기계공학자·약사 등\n 공산품 매입상: 시세 무관\n경비병: 들어온 몹을 쏴 줌\n 마을에서 총을 쏘면 경고,\n 또 쏘면 1분간 공격당함!\n\n폐공장: 몹 조심!\n 고장난 기계 → 고철 부품\n몹 사냥·마을 상자: 동전"},
            {"17. 산업 혁명", "기계는 동력 없이도 돌아가요\n(기본 속도)\n\n전기를 이으면 1.5배!\n 증기기관·태양광 → 전선\n\n전기 전용: 유도로·아크로\n자동화: 로봇 팔·분류기·창고\n\n제작법: 제작대 레시피 북\n(초록 책)에서 검색"},
            {"17-1. 증기 발전", "① 보일러: 석탄 넣기\n 물: 옆에 물 원천\n   or 물 양동이\n② 증기기관: 보일러 옆에\n → 전기 32 ⚡\n③ 전선: 기계까지 이어 깔기\n (자동으로 연결됨)\n\n석탄 1개 = 160초\n빈손 우클릭: 상태 보기"},
            {"17-2. 전기 계산", "1.5배 속도에 드는 전기\n 분쇄·제련·인발 12\n 압착·절삭 18\n 합금·조립·화학 24\n 전해 36\n\n증기기관 1개\n = 분쇄기 2~3대\n모자라면 1~1.5배 사이\n\n기계 창: 전기 % 표시"},
            {"17-3. 전기", "전선: 이어 깔면 자동 연결\n 거리 제한·손실 없음\n기계를 전선에 붙이기\n → 1.5배 빠름\n\n전기 블록 빈손 우클릭:\n 생산·사용·저장량\n\n옛 회전축·분배기·발전기는\n 전선처럼 그대로 작동"},
            {"17-4. 전기 블록", "태양광 (초반 OK)\n 낮 8 ⚡ 비 3 밤 0\n 유리3+구리 주괴2\n +레드스톤+철판3\n축전지\n 남는 전기 20만 저장\n 모자랄 때 내보냄\n\n전선\n 고무6+구리선3 → 12"},
            {"17-5. 전기 전용 기계", "전선에 붙여야만 작동\n\n유도로 (32 ⚡)\n 원광석 → 주괴 2개!\n 규사 가루 → 유리 2\n전기 아크로 (48 ⚡)\n 철 주괴2+흑연 → 강철 3\n 규사 가루2+코크스 → 규소 2\n\n재료비가 줄어 이윤↑"},
            {"17-6. 로봇 팔", "바라보는 쪽에 내려놓고\n등 뒤에서 집어 옴\n(설치할 때 내려놓을 쪽을 보기)\n\n1.2초마다 12개씩\n상자·창고·기계·벨트 OK\n기계: 결과물만 꺼내고\n 맞는 재료만 넣음\n전선 필수 (4 ⚡)"},
            {"17-7. 필터·분류기", "필터 (로봇 팔·분류기)\n 아이템 들고 우클릭 = 설정\n 웅크리고 빈손 = 해제\n\n분류기: 벨트 사이에 설치\n 필터 아이템 → 왼쪽\n 나머지 → 앞\n 여러 개 이으면 여러 갈래\n\n창고: 한 블록 54칸"},
            {"17-8. 자동 공장 예시", "창고(원철)\n↓ 로봇 팔 (필터: 원철)\n유도로\n↓ 로봇 팔\n컨베이어\n↓\n분류기 (필터: 철 주괴)\n ├왼쪽 → 창고\n └앞 → 판매소\n\n+ 증기기관에서 전선으로\n 팔·유도로에 전기 연결"},
            {"18. 블랙마켓", "갱단이 떨어뜨리는\n암시장 출입증\n (갱단 3%, 두목 30%)\n\n마을 큰길의 맨홀에\n출입증 들고 우클릭\n→ 영구 등록 + 입장\n\n나갈 땐 안쪽 맨홀\n(들어온 곳으로 돌아감)\n안에선 건축 불가"},
            {"18-1. 암상인", "무기 밀수꾼\n 총·탄약 30% 싸게\n 총은 밀수품(가끔 걸림)\n희귀품 브로커\n 네더의 별·엘리트라 등\n 하루 수량 제한\n장물아비\n 오늘의 특가 4개\n 시세 무시 매입 70%\n 보스 전리품 1.2배"},
            {"18-2. 암시장 규칙", "안에서 싸우면\n상인들이 총으로 처리!\n\n상인을 죽이면\n[추적 V] 1시간\n 지울 수 없음\n 이동속도 -10%\n 재장전 -20%\n 갱단 출현 3배\n\n죽은 상인은 다음 날 부활"},
            {"18-3. 현상금·금고", "현상금 게시판\n 이름+금액(은행 잔고)\n 처치한 사람이 받음\n 익명 · 7일 뒤 반환\n 걸린 사람에겐\n 갱단 무리가 쫓아옴!\n\n개인 금고\n 웅크리고 우클릭: 칸 해금\n 칸당 ₩10,000 (최대 27)\n 죽어도 안 사라짐"},
            {"14. 명령어", "/guide /starter\n/sell /sell all\n/price /prices\n\n(관리자)\n/tycoon factory\n/tycoon village\n/tycoon ruin\n/tycoon nearest"},
    };

    public static ItemStack guideBook() {
        List<Filterable<Component>> pages = new ArrayList<>();
        for (String[] page : GUIDE) {
            MutableComponent text = Component.literal(page[0] + "\n\n")
                    .withStyle(Style.EMPTY.withBold(true).withColor(ChatFormatting.DARK_GREEN));
            text.append(Component.literal(page[1]).withStyle(Style.EMPTY.withBold(false).withColor(ChatFormatting.BLACK)));
            pages.add(Filterable.passThrough(text));
        }
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT,
                new WrittenBookContent(Filterable.passThrough("타이쿤크래프트 가이드"), "TycoonCraft", 0, pages, true));
        return book;
    }

    /** v0.18.7: 곡괭이·도끼 + 공장 키트 (우클릭하면 기본 공장이 세워짐) */
    public static List<ItemStack> kit() {
        List<ItemStack> k = new ArrayList<>();
        k.add(new ItemStack(ModItems.FACTORY_KIT.get()));
        k.add(new ItemStack(Items.IRON_PICKAXE));
        k.add(new ItemStack(Items.IRON_AXE));
        return k;
    }

    /** 플레이어별 저장 데이터 (사망해도 유지되는 영역) */
    private static CompoundTag persisted(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(Player.PERSISTED_NBT_TAG)) data.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        return data.getCompound(Player.PERSISTED_NBT_TAG);
    }

    /** v0.23.1: 가이드 내용이 바뀌면 올림 → 기존 플레이어도 접속 시 새 책을 받음 */
    public static final int GUIDE_VERSION = 31;

    public static void updateGuide(Player player) {
        CompoundTag p = persisted(player);
        if (p.getInt("tycooncraft_guide_ver") >= GUIDE_VERSION) return;
        p.putInt("tycooncraft_guide_ver", GUIDE_VERSION);
        if (!p.getBoolean(FLAG)) return;   // 처음 온 사람은 스타터 키트에서 새 책을 받음
        Coins.giveStack(player, guideBook());
        player.sendSystemMessage(Component.translatable("message.tycooncraft.guide_updated").withStyle(ChatFormatting.AQUA));
    }

    public static boolean received(Player player) {
        return persisted(player).getBoolean(FLAG);
    }

    public static void give(Player player) {
        Coins.giveStack(player, guideBook());
        for (ItemStack stack : kit()) Coins.giveStack(player, stack);
        persisted(player).putBoolean(FLAG, true);
        persisted(player).putInt("tycooncraft_guide_ver", GUIDE_VERSION);
        player.sendSystemMessage(Component.translatable("message.tycooncraft.welcome").withStyle(ChatFormatting.GOLD));
    }

    private StarterKit() {}
}
