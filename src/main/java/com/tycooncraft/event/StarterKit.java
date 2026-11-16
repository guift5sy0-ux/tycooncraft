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
            {"타이쿤크래프트", "공장을 짓고, 가공품을 팔아 돈을 벌고, 그 돈으로 더 큰 공장을 만드는 모드예요.\n\n모든 기계는 동력·연료 없이 설치만 하면 작동해요!"},
            {"1. 가공하면 비싸요", "철 원석 1개 ₩5\n→ 분쇄: 가루 2 ₩7\n→ 제련: 주괴 2 ₩16\n→ 압착: 철판 2 ₩28\n→ 절삭: 기어 2 ₩44\n\n/prices : 전체 시세"},
            {"2. 판매소", "빈손 우클릭: 판매소 창\n왼쪽 칸에 물건 → 판매 예정 금액 표시 → [판매]\n[인출]: 번 돈을 동전으로\n\n물건 들고 우클릭: 바로 판매\n벨트로 넣으면 자동 판매\n/sell, /sell all"},
            {"3. 동전 & 환전기", "구리 ₩1 · 철 ₩10\n금 ₩100 · 에메랄드 ₩1,000\n다이아 ₩10,000\n\n환전기 우클릭: 동전을 큰 단위로 정리\n제작대: 큰 동전 1 → 작은 동전 10"},
            {"4. 컨베이어", "가고 싶은 방향을 바라보고 설치하면 화살표 방향으로 운반해요.\n\n아이템은 Q로 던져서 올려요.\n\n끝에 기계나 판매소를 두세요."},
            {"5. 리프트", "같은 리프트를 세로로 쌓으면 옆에 닿은 아이템이\n상승: 맨 위로\n하강: 맨 아래로\n이동해 끝 블록의 화살표 방향으로 나와요.\n\n입구는 화살표 반대쪽 면에!"},
            {"6. 기계 사용법", "우클릭: 기계 창 열기\n재료 칸에 넣으면 가공 → 결과 칸\n화살표에 마우스: 레시피\n\n자동화: 옆·위로 재료가 닿으면 흡수, 앞이 컨베이어·상자면 결과물 자동 배출\n안 쓰는 아이템은 통과!"},
            {"7. 1단계 기계", "분쇄기\n 원석 → 가루 2\n 석탄 → 석탄 가루 2\n 밀 → 밀가루 2\n제련기\n 가루 → 주괴\n 밀가루 → 빵\n압착기\n 주괴 → 판"},
            {"8. 2단계 기계", "(₩100 + 판)\n합금로\n 철 주괴+석탄 가루 2\n → 강철 주괴\n인발기\n 구리/금 → 선 3\n 철/강철 → 봉 2\n절삭기\n 판 → 기어\n 원목 → 판자 6"},
            {"9. 조립기", "(₩1,000 + 강철판)\n회로 ₩45\n 구리선3+철판+레드스톤\n기계 부품 ₩160\n 철기어2+강철봉+회로\n고급 회로 ₩230\n 회로2+금선2+강철판\n엔진 ₩700\n 강철판4+강철기어2+기계부품"},
            {"10. 제작대 레시피", "기계 없이도 OK\n(재료는 더 들어요)\n\n강철 주괴\n 철 주괴 + 석탄 2\n강철판\n 강철 주괴 2\n회로\n 구리 주괴 3 + 레드스톤 2 + 철 주괴"},
            {"11. 시세", "같은 걸 많이 팔면 서버 전체 가격이 떨어져요. (최저 25%)\n\n시간이 지나면 회복돼요.\n\n여러 라인을 돌려서 골고루 파는 게 이득!"},
            {"12. 총기", "부품 (기계로 생산)\n 총열: 절삭기(강철봉)\n 기관부·개머리판: 조립기\n\n제작대에서 조합\n 권총·기관단총·산탄총·소총·저격총\n\n웅크리고 우클릭: 재장전\n저격총: 꾹 눌러 조준"},
            {"12-2. 탄약", "조립기에서 대량 생산\n경탄약 x16\n 구리판+화약+철봉\n중탄약 x8\n 구리판+화약2+강철봉\n산탄 x8\n 구리판+화약+철 가루2\n\n화약: 분쇄기(부싯돌)"},
            {"15. 탈것", "조립기에서 제작 (엔진부터 넣기)\n\n자동차 ₩2,000\n 엔진+강철판6+강철기어4\n +회로2+유리4+검은염료4\n기관차 ₩4,000\n 엔진2+강철판12+강철기어8\n +기계장치2+회로2+유리4\n헬기 ₩5,500\n 엔진2+강철판10+고급회로3\n +기계장치3+강철봉8+유리6"},
            {"15-2. 운전", "연료 들고 우클릭으로 주유\n 연료통(조립기: 철판+석탄가루4→2)\n 석탄·석탄블록·용암 양동이\n\n자동차 W/S A/D, 스페이스 경적\n기관차 W 가속 S 브레이크\n 웅크리고 우클릭 화물칸\n헬기 로터가 다 돌면 이륙\n 스페이스↑ Ctrl↓ W/S A/D"},
            {"15-3. 정비", "강철판 들고 우클릭: 수리\n맨손으로 계속 치면 아이템으로 회수\n총에 맞아 부서지면 폭발!\n웅크리기: 내리기\n\n오른쪽 아래 계기판에\n속도·연료·차체 표시"},
            {"12-3. 소음기", "조립기: 강철판+강철봉+흰 양털\n\n제작대에 총+소음기 → 장착\n(탄·인챈트 그대로)\n소음기 단 총만 놓으면 → 분리\n\n소리 작음, 총구 화염·궤적 없음\n피해 -10%, 반동 감소"},
            {"12-4. 총 인챈트", "인챈트 테이블·모루로 총에!\n화력 I~V 피해+15%씩\n속사 I~III 연사+\n확장 탄창 I~III +25%씩\n빠른 장전 I~III\n관통탄 I~III 여러 명 관통\n정밀 사격 I~III 퍼짐·반동↓\n소이탄 불 붙임\n탄약 절약 I~III\n헤드헌터 I~II 헤드샷 강화"},
            {"12-5. 자폭 드론", "조립기: 기계장치+회로2+철판4\n +TNT+유리판+구리선2\n\n우클릭: 띄우고 드론 화면으로\nW 보는 쪽으로 비행 A/D 옆\n스페이스↑ Ctrl↓\n클릭: 폭파 / 빠르게 부딪혀도 폭발\nShift: 조종 끝 (맨손으로 쳐서 회수)\n배터리 45초 · 거리 100블록"},
            {"16. 신무기", "VSS: 일체형 소음기 저격소총\n F: 단발(꾹 눌러 4배 조준)↔연사\n쌍권총: 빠른 연사, 작은 탄창\nRPG: 로켓 1발, 맞으면 폭발\n\n상위 소음기 (16블록, 피해 감소 없음)\n일반 소음기는 32블록까지 들림"},
            {"16-2. 방탄", "방탄조끼 I/II/III\n 몸통 총알 피해 -15/25/35%\n방탄모 I/II/III\n 헤드샷 추가 피해 -20/35/50%\n\n낙하산: 다리 칸에 착용\n 떨어지면 자동으로 펼쳐짐\n W 로 앞으로 활공"},
            {"16-3. 병기", "자동 포탑: 탄약 넣으면\n 24블록 안 몹 자동 사격\n전차: 포탑은 마우스\n 좌클릭 주포(전차 포탄)\n전투기: W/S 출력, 마우스 조종\n 좌클릭 기관포 우클릭 미사일\n 비행 중 F: 배럴 롤\nMK2 드론: 더 빠르고 강력"},
            {"16-4. 적", "갱단원(권총) 용병(소총·방탄)\n무법자(산탄총) - 밤에 출몰\n변이 거미 여왕 - 드문 거대 거미\n 거미줄 뱉기(둔화) · 맹독 침 5발\n\n보스: 카르텔 두목\n 카르텔 신호탄으로 소환\n 소총 연사 + RPG + 부하 소환\n 보상: 다이아 동전, VSS/RPG"},
            {"16-4b. 이계의 심판자", "날카로운 반자동 전설 권총\n신호기 네더라이트 신호기\n네더라이트 드래곤알 네더라이트\n네더라이트 권총 네더라이트\n\n중탄약 9발, 피해 24, 관통 3\n맞히면 표식 → 5초 안에\n 다시 맞히면 심판(번개 14)\n칼날 근접 피해 12"},
            {"16-5. 수류탄", "유황 광석: 지하 -48~72\n 곡괭이(돌 이상)로 채굴 → 유황\n\n파편 수류탄: 철판+유황2+화약\n섬광탄: 철판+유황+발광석가루2\n 보고 있으면 화면이 하얗게\n연막탄: 철판+유황+석탄가루+종이\n우클릭 던지기, 웅크리면 굴리기"},
            {"16-6. 자동 굴착기", "드릴: 다이아+강철주괴2\n +강철톱니+강철판3\n바퀴: 말린켈프블록4+강철막대\n드릴+바퀴+엔진 = 자동 굴착기\n\n설치 후 연료 우클릭 → 굴착 시작\n3×3 터널, 다이아 광석까지\n단단할수록 느리고 연료 많이\n빈손 우클릭: 저장통\n웅크리고 우클릭: 켜기/끄기"},
            {"13. 탐험", "상인 마을\n 무적 상인이 동전으로 거래\n\n폐공장\n 몹 조심! 고장난 기계를 부수면 고철 부품\n\n몹 사냥: 확률로 동전\n마을 상자: 동전"},
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

    private static List<ItemStack> kit() {
        List<ItemStack> k = new ArrayList<>();
        k.add(new ItemStack(ModItems.SELL_STATION.get()));
        k.add(new ItemStack(ModItems.EXCHANGE.get()));
        k.add(new ItemStack(ModItems.CONVEYOR.get(), 64));
        k.add(new ItemStack(ModItems.LIFT_UP.get(), 8));
        k.add(new ItemStack(ModItems.LIFT_DOWN.get(), 8));
        k.add(new ItemStack(ModItems.machineItem(MachineType.CRUSHER)));
        k.add(new ItemStack(ModItems.machineItem(MachineType.SMELTER)));
        k.add(new ItemStack(ModItems.machineItem(MachineType.PRESS)));
        k.add(new ItemStack(Items.STONE_BRICKS, 64));
        k.add(new ItemStack(Items.STONE_BRICKS, 64));
        k.add(new ItemStack(Items.OAK_PLANKS, 64));
        k.add(new ItemStack(Items.GLASS, 32));
        k.add(new ItemStack(Items.OAK_LOG, 32));
        k.add(new ItemStack(Items.TORCH, 32));
        k.add(new ItemStack(Items.CRAFTING_TABLE));
        k.add(new ItemStack(Items.FURNACE, 2));
        k.add(new ItemStack(Items.CHEST, 4));
        k.add(new ItemStack(Items.IRON_PICKAXE));
        k.add(new ItemStack(Items.IRON_AXE));
        k.add(new ItemStack(Items.BREAD, 16));
        return k;
    }

    /** 플레이어별 저장 데이터 (사망해도 유지되는 영역) */
    private static CompoundTag persisted(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(Player.PERSISTED_NBT_TAG)) data.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        return data.getCompound(Player.PERSISTED_NBT_TAG);
    }

    public static boolean received(Player player) {
        return persisted(player).getBoolean(FLAG);
    }

    public static void give(Player player) {
        Coins.giveStack(player, guideBook());
        for (ItemStack stack : kit()) Coins.giveStack(player, stack);
        Coins.give(player, 100);
        persisted(player).putBoolean(FLAG, true);
        player.sendSystemMessage(Component.translatable("message.tycooncraft.welcome").withStyle(ChatFormatting.GOLD));
    }

    private StarterKit() {}
}
