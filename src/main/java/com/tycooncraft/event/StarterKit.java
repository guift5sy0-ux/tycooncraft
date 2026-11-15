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
            {"2. 판매소", "들고 우클릭: 판매\n빈손 우클릭: 번 돈 인출\n웅크리고 빈손 우클릭: 잔액 확인\n\n/sell : 손에 든 것 판매\n/sell all : 전부 판매"},
            {"3. 동전 & 환전기", "구리 ₩1 · 철 ₩10\n금 ₩100 · 에메랄드 ₩1,000\n다이아 ₩10,000\n\n환전기 우클릭: 동전을 큰 단위로 정리\n제작대: 큰 동전 1 → 작은 동전 10"},
            {"4. 컨베이어", "가고 싶은 방향을 바라보고 설치하면 화살표 방향으로 운반해요.\n\n아이템은 Q로 던져서 올려요.\n\n끝에 기계나 판매소를 두세요."},
            {"5. 리프트", "같은 리프트를 세로로 쌓으면 옆에 닿은 아이템이\n상승: 맨 위로\n하강: 맨 아래로\n이동해 끝 블록의 화살표 방향으로 나와요.\n\n입구는 화살표 반대쪽 면에!"},
            {"6. 기계 사용법", "옆이나 위로 재료가 닿으면 가공해서 화살표 방향으로 내보내요.\n\n들고 우클릭: 직접 넣기\n빈손 우클릭: 레시피\n\n안 쓰는 아이템은 그대로 통과!"},
            {"7. 1단계 기계", "분쇄기\n 원석 → 가루 2\n 석탄 → 석탄 가루 2\n 밀 → 밀가루 2\n제련기\n 가루 → 주괴\n 밀가루 → 빵\n압착기\n 주괴 → 판"},
            {"8. 2단계 기계", "(₩100 + 판)\n합금로\n 철 주괴+석탄 가루 2\n → 강철 주괴\n인발기\n 구리/금 → 선 3\n 철/강철 → 봉 2\n절삭기\n 판 → 기어\n 원목 → 판자 6"},
            {"9. 조립기", "(₩1,000 + 강철판)\n회로 ₩45\n 구리선3+철판+레드스톤\n기계 부품 ₩160\n 철기어2+강철봉+회로\n고급 회로 ₩230\n 회로2+금선2+강철판\n엔진 ₩700\n 강철판4+강철기어2+기계부품"},
            {"10. 제작대 레시피", "기계 없이도 OK\n(재료는 더 들어요)\n\n강철 주괴\n 철 주괴 + 석탄 2\n강철판\n 강철 주괴 2\n회로\n 구리 주괴 3 + 레드스톤 2 + 철 주괴"},
            {"11. 시세", "같은 걸 많이 팔면 서버 전체 가격이 떨어져요. (최저 25%)\n\n시간이 지나면 회복돼요.\n\n여러 라인을 돌려서 골고루 파는 게 이득!"},
            {"12. 탐험", "상인 마을\n 무적 상인이 동전으로 거래\n\n폐공장\n 몹 조심! 고장난 기계를 부수면 고철 부품\n\n몹 사냥: 확률로 동전\n마을 상자: 동전"},
            {"13. 명령어", "/guide /starter\n/sell /sell all\n/price /prices\n\n(관리자)\n/tycoon factory\n/tycoon village\n/tycoon ruin\n/tycoon nearest"},
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
