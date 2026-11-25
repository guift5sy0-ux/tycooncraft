package com.tycooncraft.blackmarket;

import com.tycooncraft.economy.BankData;
import com.tycooncraft.economy.Coins;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 개인 금고: 우클릭 = 열기, 웅크리고 우클릭 = 칸 1개 해금 (10,000원, 지갑 → 은행 순으로 냄).
 * 잠긴 칸은 회색 유리판으로 표시되고 물건을 넣을 수 없음.
 */
public class VaultBlock extends Block {
    public static int purchases;

    public VaultBlock(Properties p) {
        super(p);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        if (sp.isShiftKeyDown()) buySlot(sp);
        else open(sp);
        return InteractionResult.SUCCESS;
    }

    public static Component buySlot(ServerPlayer sp) {
        VaultData d = VaultData.get(sp.server);
        VaultData.Box b = d.box(sp.getUUID());
        Component msg;
        if (b.unlocked >= VaultData.SIZE) msg = Component.translatable("message.tycooncraft.vault.full");
        else if (pay(sp, VaultData.SLOT_PRICE)) {
            b.unlocked++;
            d.setDirty();
            purchases++;
            msg = Component.translatable("message.tycooncraft.vault.bought", b.unlocked, VaultData.SIZE).withStyle(ChatFormatting.GREEN);
        } else msg = Component.translatable("message.tycooncraft.vault.no_money", Coins.format(VaultData.SLOT_PRICE)).withStyle(ChatFormatting.RED);
        sp.displayClientMessage(msg, true);
        return msg;
    }

    /** 지갑 현금 먼저, 모자라면 은행 잔고에서 */
    static boolean pay(ServerPlayer sp, long amount) {
        var acc = BankData.get(sp.server).account(sp.getUUID());
        long wallet = Coins.wallet(sp);
        if (wallet + acc.balance < amount) return false;
        long taken = Coins.takeAll(sp);
        if (taken >= amount) {
            Coins.give(sp, taken - amount);
        } else {
            acc.balance -= amount - taken;
            BankData.get(sp.server).setDirty();
        }
        return true;
    }

    public static void open(ServerPlayer sp) {
        VaultData d = VaultData.get(sp.server);
        VaultData.Box b = d.box(sp.getUUID());
        Container c = new Container() {
            boolean locked(int i) {
                return i >= b.unlocked;
            }

            @Override public int getContainerSize() { return VaultData.SIZE; }
            @Override public boolean isEmpty() { return b.items.stream().allMatch(ItemStack::isEmpty); }

            @Override
            public ItemStack getItem(int i) {
                if (!locked(i)) return b.items.get(i);
                ItemStack g = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
                g.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.translatable("gui.tycooncraft.vault.locked", Coins.format(VaultData.SLOT_PRICE)));
                return g;
            }

            @Override
            public ItemStack removeItem(int i, int n) {
                if (locked(i)) return ItemStack.EMPTY;
                ItemStack r = net.minecraft.world.ContainerHelper.removeItem(b.items, i, n);
                setChanged();
                return r;
            }

            @Override
            public ItemStack removeItemNoUpdate(int i) {
                if (locked(i)) return ItemStack.EMPTY;
                ItemStack r = b.items.get(i);
                b.items.set(i, ItemStack.EMPTY);
                return r;
            }

            @Override
            public void setItem(int i, ItemStack s) {
                if (locked(i)) return;
                b.items.set(i, s);
                setChanged();
            }

            @Override public boolean canPlaceItem(int i, ItemStack s) { return !locked(i); }
            @Override public boolean canTakeItem(Container target, int i, ItemStack s) { return !locked(i); }
            @Override public void setChanged() { d.setDirty(); }
            @Override public boolean stillValid(Player p) { return p.isAlive(); }
            @Override public void clearContent() {}
        };
        sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x3, id, inv, c, 3) {
            @Override
            public void clicked(int slot, int button, net.minecraft.world.inventory.ClickType type, Player p) {
                if (slot >= 0 && slot < VaultData.SIZE && slot >= b.unlocked) return;   // 잠긴 칸은 아예 못 건드림
                super.clicked(slot, button, type, p);
            }
        }, Component.translatable("gui.tycooncraft.vault.title", b.unlocked, VaultData.SIZE)));
    }
}
