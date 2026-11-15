package com.tycooncraft.economy;

import com.tycooncraft.blockentity.SellStationBlockEntity;
import com.tycooncraft.registry.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 판매소 창: 판매 칸 3x3 + [판매] [인출] 버튼 + 잔액/판매 예정 금액 표시.
 * 창을 닫으면 판매 칸에 남은 물건은 돌려준다.
 */
public class SellMenu extends AbstractContainerMenu {
    public static final int BUTTON_SELL = 0, BUTTON_WITHDRAW = 1;
    private static final int SELL_SLOTS = 9;

    private final SellStationBlockEntity station;
    private final Container sellSlots = new SimpleContainer(SELL_SLOTS);
    private final ContainerData data;
    private final ContainerLevelAccess access;

    /** 클라이언트 */
    public SellMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, (SellStationBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()), new SimpleContainerData(8));
    }

    /** 서버 */
    public SellMenu(int id, Inventory inventory, SellStationBlockEntity station) {
        this(id, inventory, station, null);
    }

    private SellMenu(int id, Inventory inventory, SellStationBlockEntity station, ContainerData clientData) {
        super(ModMenus.SELL.get(), id);
        this.station = station;
        this.access = ContainerLevelAccess.create(inventory.player.level(), station.getBlockPos());
        this.data = clientData != null ? clientData : new ContainerData() {
            // 정수 동기화는 16비트 단위라 금액(long)을 4조각씩 나눠 보냄: 0~3 잔액, 4~7 판매 예정(×10)
            @Override
            public int get(int index) {
                long v = index < 4 ? station.balance() : Math.round(pendingValue() * 10);
                return (int) ((v >> (16 * (index % 4))) & 0xFFFF);
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 8;
            }
        };

        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                addSlot(new Slot(sellSlots, c + r * 3, 12 + c * 18, 18 + r * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return MarketData.canSell(stack);
                    }
                });
            }
        }
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 142));

        addDataSlots(data);
    }

    /** 서버: 판매 칸 물건의 현재 시세 합계 */
    private double pendingValue() {
        if (!(station.getLevel() instanceof ServerLevel level)) return 0;
        MarketData market = MarketData.get(level.getServer());
        double total = 0;
        for (int i = 0; i < SELL_SLOTS; i++) {
            ItemStack s = sellSlots.getItem(i);
            if (s.isEmpty()) continue;
            total += Coins.isCoin(s) ? Coins.valueOf(s) : market.currentPrice(s.getItem()) * s.getCount();
        }
        return total;
    }

    private long combine(int start) {
        long v = 0;
        for (int k = 0; k < 4; k++) v |= ((long) (data.get(start + k) & 0xFFFF)) << (16 * k);
        return v;
    }

    /** 클라이언트 표시용 */
    public long balance() {
        return combine(0);
    }

    public double pending() {
        return combine(4) / 10.0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_SELL) {
            station.sellContainer(sellSlots, player);
            return true;
        }
        if (id == BUTTON_WITHDRAW) {
            station.withdraw(player);
            return true;
        }
        return false;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) clearContainer(player, sellSlots);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < SELL_SLOTS) {
            if (!moveItemStackTo(stack, SELL_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!MarketData.canSell(stack) || !moveItemStackTo(stack, 0, SELL_SLOTS, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, station.getBlockState().getBlock());
    }
}
