package com.tycooncraft.event;

import com.tycooncraft.economy.Coins;
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
            {"타이쿤크래프트", "돈을 벌고, 그 돈과 재료로\n탈것·비행선·방어 장치처럼\n스스로 움직이고 효과를 내는\n장치를 만드는 모드예요.\n\n모든 제작은 제작대·화로·\n용광로·총기 제작대에서!\n(기계·전기 없음)"},
            {"0. 시작", "시작 도구\n 곡괭이·도끼·총기 제작대\n\n돈 벌기\n 몹 사냥·마을 상자: 동전\n 판매소에 물건 팔기\n 보스 전리품은 비싸요\n 암시장 의뢰·카지노\n\n/prices : 전체 시세"},
            {"1. 재료 만들기", "제작대 (전부 수동)\n철판·구리판·금판 ×2\n 주괴 2 세로\n철봉·강철봉 ×4\n 주괴 3 세로\n기어 ×2: 주괴 4 마름모\n구리선·금선 ×6\n 주괴 3 가로\n고무 ×4\n 슬라임볼+말린 켈프2"},
            {"2. 판매소", "빈손 우클릭: 판매소 창\n왼쪽 칸에 물건 → 판매 예정 금액 표시 → [판매]\n[인출]: 번 돈을 동전으로\n\n물건 들고 우클릭: 바로 판매\n/sell, /sell all"},
            {"3. 동전 & 환전기", "구리 ₩1 · 철 ₩10\n금 ₩100 · 에메랄드 ₩1,000\n다이아 ₩10,000\n\n환전기 우클릭: 동전을 큰 단위로 정리\n제작대: 큰 동전 1 → 작은 동전 10"},
            {"5. 리프트", "같은 리프트를 세로로 쌓으면 옆에 닿은 아이템이\n상승: 맨 위로\n하강: 맨 아래로\n이동해 끝 블록의 화살표 방향으로 나와요.\n\n입구는 화살표 반대쪽 면에!"},
            {"7. 강철·부품", "강철 주괴\n 용광로: 철 주괴\n 제작대: 철 주괴+석탄2\n강철판 ×2: 강철 주괴 2\n기계장치\n 철기어2+레드스톤2+철판\n회로\n 구리 주괴3+레드스톤2+철\n고급 회로\n 금4+회로2+레드스톤2+강철판\n엔진\n 강철판4+강철기어2+기계장치"},
            {"9. 장치 제작", "제작대에서 바로!\n포탑: 총열+강철판3\n +기계장치+회로2\nCIWS: 포탑+총열3\n +고급회로2+강철판2\n +기계장치\n리프트 ×2: 철판2+피스톤\n /호퍼(하강)\n부유체: 철판4+고무4\n +팬텀 막\n연료통 ×2: 철판4+석탄"},
            {"11. 시세", "같은 걸 많이 팔면 서버 전체 가격이 떨어져요. (최저 25%)\n\n시간이 지나면 회복돼요.\n\n여러 라인을 돌려서 골고루 파는 게 이득!"},
            {"12. 총기", "총기 제작대에서 조합\n 총열: 강철봉\n 기관부: 강철판2+기계장치\n  +철기어\n 개머리판: 판자3+가죽\n\n권총·기관단총·산탄총·소총·저격총\n웅크리고 우클릭: 재장전\n저격총: 꾹 눌러 조준"},
            {"12-2. 탄약", "총기 제작대\n경탄약 x16\n 구리판+화약+철봉\n중탄약 x8\n 구리판+화약2+강철봉\n산탄 x8\n 구리판+화약+철 조각4\n(주괴로 만들면 덜 나와요)"},
            {"15. 탈것", "제작대에서 제작\n자동차\n 유리판·철판2·엔진·바퀴2\n기관차\n 엔진+강철판·회로·기계장치\n +유리+바퀴3\n헬기\n 엔진+강철봉3+강철판2\n +유리2+기계장치\n전차: 엔진+총열+고급회로\n전투기: 엔진2+총열+고급회로"},
            {"15-2. 운전", "연료 들고 우클릭으로 주유\n 연료통(제작대)\n 석탄·석탄블록·용암 양동이\n\n자동차 W/S A/D, 스페이스 경적\n기관차 W 가속 S 브레이크\n 웅크리고 우클릭 화물칸\n헬기 로터가 다 돌면 이륙\n 스페이스↑ Ctrl↓ W/S A/D"},
            {"15-3. 정비", "강철판 들고 우클릭: 수리\n맨손으로 계속 치면 아이템으로 회수\n총에 맞아 부서지면 폭발!\n웅크리기: 내리기\n\n오른쪽 아래 계기판에\n속도·연료·차체 표시"},
            {"15-4. 헬파이어 바이크", "지옥불 초퍼 (2인승)\n 블레이즈 막대2·네더라이트 주괴\n +마그마 크림2·엔진·바퀴2\n\n자동차보다 빠르고 잘 꺾임\n불에 안 타고 용암 위를 달림\n들이받은 적은 불이 붙음\n타고 있으면 화염 저항\n스페이스: 배기 화염 포효\n (뒤쪽 적에게 불)"},
            {"12-3. 소음기", "총기 제작대: 강철판+강철봉\n +흰 양털\n\n제작대에 총+소음기 → 장착\n(탄·인챈트 그대로)\n소음기 단 총만 놓으면 → 분리\n\n소리 작음, 총구 화염·궤적 없음\n피해 -10%, 반동 감소"},
            {"12-4. 총 인챈트", "인챈트 테이블·모루로 총에!\n화력 I~V 피해+15%씩\n속사 I~III 연사+\n확장 탄창 I~III +25%씩\n빠른 장전 I~III\n관통탄 I~III 여러 명 관통\n정밀 사격 I~III 퍼짐·반동↓\n소이탄 불 붙임\n탄약 절약 I~III\n헤드헌터 I~II 헤드샷 강화"},
            {"12-5. 자폭 드론", "제작대: 철판4+회로2\n +기계장치\nMK2: 드론+고급회로+TNT2\n +강철판2+기계장치\n\n우클릭: 띄우고 드론 화면으로\nW 보는 쪽으로 비행 A/D 옆\n스페이스↑ Ctrl↓\n클릭: 폭파 / 빠르게 부딪혀도 폭발\n배터리 45초 · 거리 100블록"},
            {"16. 신무기", "VSS: 일체형 소음기 저격소총\n F: 단발(꾹 눌러 4배 조준)↔연사\n쌍권총: 빠른 연사, 작은 탄창\n (왼손 비워야 함, 방패 불가)\nRPG: 로켓 1발, 맞으면 폭발\n\n상위 소음기 (16블록, 피해 감소 없음)\n일반 소음기는 32블록까지 들림"},
            {"16-2. 방탄", "방탄조끼 I/II/III\n 몸통 총알 피해 -15/25/35%\n방탄모 I/II/III\n 헤드샷 추가 피해 -20/35/50%\nI: 철판+가죽\nII: I+강철판\nIII: II+강철판+다이아\n저거너트: III+네더라이트 조각\n\n낙하산: 다리 칸, 자동 펼침"},
            {"16-2b. 방어력 40", "방어력 최대 20 → 40\n 21~40은 금빛 갑옷 칸\n 20까지 피해 -80%(바닐라)\n 40이면 -90%\n\n세트 방어력\n I 15 · II 18 · III 20\n 저거너트 24\n보호: 레벨당 방어력 +1\n (보호 IV 풀세트 +16)\n\n총알: 방어력 60% 무시\n 총 감소는 최대 70%\n총알 보호 I~IV\n 레벨당 총알 방어력 +1.1\n가시 불가 · 반짝임 없음"},
            {"16-3. 병기", "자동 포탑: 탄약 넣으면\n 24블록 안 몹 자동 사격\n전차: 포탑은 마우스\n 좌클릭 주포(전차 포탄)\n전투기: W/S 출력, 마우스 조종\n Ctrl/Z: 에어브레이크(감속)\n 좌클릭 기관포 우클릭 미사일\n 비행 중 F: 배럴 롤\nMK2 드론: 더 빠르고 강력"},
            {"16-4. 적", "갱단원(권총) 용병(소총·방탄)\n무법자(산탄총) - 밤에 출몰\n변이 거미 여왕 - 드문 거대 거미\n 거미줄 뱉기(둔화) · 맹독 침 5발\n\n보스: 카르텔 두목\n 카르텔 신호탄으로 소환\n 소총 연사 + RPG + 부하 소환\n 보상: 다이아 동전, VSS/RPG"},
            {"16-5. 디스트로이어 변종", "정찰 디스트로이어\n 밤에 3~5마리 떼\n 좌우로 꺾으며 돌진·베기\n 정찰 코어(8%) ₩2,000\n\n플라잉 디스트로이어\n (월드 3일째~, 밤)\n 드론 6기·정찰기 2기 투하\n 동력원 3개(각 120)\n 2개 부수면 추락 → 대폭발!\n 비행 코어 ₩25,000\n※ 디스트로이어는 서로 빼고\n  모든 생물을 공격"},
            {"16-6. 코어 장비", "비행 코어 엔진\n 운송기 엔진 상위호환\n 엔진 6개 추진·부유체 40개\n 최고속도↑ 연료 40%↓\n\n드론 편대 격납고 (운송기 전용)\n 선체에 설치 → 드론 6기(최대 8)\n 선체 끝에서 30칸 안 적 사격\n 격추되면 드론 아이템으로 보충\n\n프로토-정찰 (정찰코어4)\n 빈손 우클릭: 원격 조종(300)\n 베기·덮치기·A/D×2 대시"},
            {"16-4b. 이계의 심판자", "날카로운 반자동 전설 권총\n신호기 네더라이트 신호기\n네더라이트 드래곤알 네더라이트\n네더라이트 권총 네더라이트\n\n중탄약 9발, 피해 15\n맞히면 표식 → 3초 안에\n 다시 맞히면 심판(번개 6)\n칼날 근접 피해 12\n내구도 500발, 다 닳으면 터짐\n 모루+네더라이트 주괴로 수리"},
            {"13-2. 보스 몹", "파괴수 골렘(중간 보스)\n 지상 밤, 드묾\n 개틀링·로켓·내려찍기\n\n화산 거수 베헤모스\n 네더, 매우 드묾\n 돌진(벽에 박으면 기절=약점)\n 내려찍기·화염 브레스·운석\n\n폭풍 와이번\n 산악 지대 밤, 드묾\n 번개 구체·급강하·낙뢰"},
            {"13-4. 강화된 위더", "심연의 위더 (체력 900)\n 해골 폭풍 · 영혼 폭발\n 암흑 운석(검은 고리 피하기)\n체력 절반: 각성\n 영혼 기사단 3기 소환\n 영혼 돌진 추가\n추가 보상: 네더라이트 조각,\n 다이아 동전"},
            {"13-5. 강화된 드래곤", "공허의 군주 (체력 800)\n 화염구 부채꼴 · 셜커 탄 군단\n 착지 중 충격파(거리 두기)\n체력 절반: 각성\n 공허 붕괴 - 발밑 보라 고리\n 2초 안에 벗어나기!\n추가 보상: 네더라이트 주괴 2,\n 겉날개, 다이아 동전 5"},
            {"13-3. 보스 전리품", "판매가 (기준)\n 용의 알 ₩150,000\n 네더의 별 ₩30,000\n 겉날개 ₩30,000\n 디스트로이어 코어 ₩10,000\n 용 머리 ₩10,000\n 위더 해골 ₩4,000\n 용의 숨결 ₩1,000\n 맹독 침 ₩500\n\n장물아비에겐 1.2배!"},
            {"16-5. 수류탄", "파편 수류탄: 철판+화약3\n섬광탄: 철판+화약+발광석가루2\n 보고 있으면 화면이 하얗게\n연막탄: 철판+화약+석탄+종이\n우클릭 던지기, 웅크리면 굴리기"},
            {"16-7. HDX 강화 폭약", "TNT보다 센 폭약\n위력 4.8 (TNT 4)\n 충전 크리퍼의 0.8배\n 파이는 구덩이 약 1.6배\n\n제작대 → 2개\n TNT2 + 화약4\n + 블레이즈 가루2 + 강철판\n\n불·레드스톤·불화살로 점화\n4초 뒤 폭발, 발사기 OK"},
            {"16-8. 상위 조종간", "운송기 조종기 상위호환\n(조종기 대신 놓고 우클릭)\n\n방향타 3배 빨리 감김·풀림\n선회 속도 1.8배\n느릴 때·큰 배도 잘 꺾임\n\n제작대\n 조종기 + 고급 회로\n + 강철 기어2 + 기계장치"},
            {"13. 탐험", "상인 마을\n 상인들이 동전으로 거래\n 총기상·장치상·약사 등\n경비병: 들어온 몹을 쏴 줌\n 마을에서 총을 쏘면 경고,\n 또 쏘면 1분간 공격당함!\n\n폐공장: 몹 조심!\n 고장난 기계 → 고철 부품\n몹 사냥·마을 상자: 동전"},
            {"18. 블랙마켓", "갱단이 떨어뜨리는\n암시장 출입증\n (갱단 3%, 두목 30%)\n\n마을 큰길의 맨홀에\n출입증 들고 우클릭\n→ 영구 등록 + 입장\n\n나갈 땐 안쪽 맨홀\n(들어온 곳으로 돌아감)\n안에선 건축 불가"},
            {"18-1. 암상인", "무기 밀수꾼\n 총·탄약 30% 싸게\n 총은 밀수품(가끔 걸림)\n희귀품 브로커\n 네더의 별·엘리트라 등\n 하루 수량 제한\n장물아비\n 오늘의 특가 4개\n 시세 무시 매입 70%\n 보스 전리품 1.2배"},
            {"18-2. 암시장 규칙", "안에서 싸우면\n상인들이 총으로 처리!\n\n상인을 죽이면\n[추적 V] 1시간\n 지울 수 없음\n 이동속도 -10%\n 재장전 -20%\n 갱단 출현 3배\n\n죽은 상인은 다음 날 부활"},
            {"18-3. 현상금·금고", "현상금 게시판\n 이름+금액(은행 잔고)\n 처치한 사람이 받음\n 익명 · 7일 뒤 반환\n 걸린 사람에겐\n 갱단 무리가 쫓아옴!\n\n개인 금고\n 웅크리고 우클릭: 칸 해금\n 칸당 ₩10,000 (최대 27)\n 죽어도 안 사라짐"},
            {"18-4. 평판", "낯선 자 → 단골(100)\n→ 동업자(300) → 간부(700)\n→ 대부(1500)\n\n얻기: 암상인 거래\n 의뢰 완료 · 카지노\n잃기: 소란 -30\n 상인 살해 -200\n 의뢰 실패 -15\n\n혜택: 암상인 할인(~15%)\n 의뢰 보상↑ 베팅 한도↑"},
            {"18-5. 의뢰 게시판", "매일 3건 · 한 번에 하나\n제한 2일\n\n배달: 밀수 화물을 지상\n 접선 지점에서 우클릭\n회수: 지상의 도난 화물\n 통에서 꺼내 가져오기\n 갱단이 지키고 있음!\n납품: 물건을 바로 넘김\n\n보상은 은행으로 입금"},
            {"18-6. 카지노", "골목 동쪽 문 안쪽\n\n슬롯머신\n 3개 일치 최대 ×100\n 해골 나오면 꽝\n룰렛\n 빨강/검정 ×2\n 초록(0) ×36\n\n베팅 한도 (평판별)\n ₩1만~₩50만\n당첨금은 현금 지급"},
            {"18-7. 불법 개조점", "총을 들고 개조대 우클릭\n\n과압탄 피해+20% (단골)\n확장 드럼 탄창+50%\n해머 연사+20% (동업자)\n반동 억제 -35%\n총열 탄퍼짐-30% (간부)\n일련번호 세탁 (대부)\n 경비병이 총성 무시\n\n개조는 영구 · 평판 할인"},
            {"18-8. 언더시티", "맨홀 아래 지하 도시\n블랙마켓은 그 한가운데\n골목 서쪽 문으로 나감\n\n골목 밖 = 갱단 영역\n 상인이 안 지켜줌\n 갱단이 사는 은신처 8곳\n (붉은 띠 건물)\n\n변종 갱단: 더 강함\n 집행자 · 돌격대 · 저격수"},
            {"18-9. 갱단 금고", "은신처의 잠긴 상자\n열쇠 1개 = 한 번 열기\n (열쇠는 사라짐)\n\n열쇠: 언더시티 갱단이\n 떨어뜨림\n 일반 20% · 변종 60%\n\n내용: 현금·탄약·강철\n 다이아·로켓·총·열쇠\n하루 뒤 다시 채워지고 잠김"},
            {"14. 명령어", "/guide /starter\n/sell /sell all\n/price /prices\n\n(관리자)\n/tycoon village\n/tycoon ruin\n/tycoon nearest"},
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

    /** 시작 도구: 곡괭이·도끼 + 제작대 (v0.30: 공장 키트 삭제) */
    public static List<ItemStack> kit() {
        List<ItemStack> k = new ArrayList<>();
        k.add(new ItemStack(ModItems.GUN_WORKBENCH.get()));
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
    public static final int GUIDE_VERSION = 47;

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
