package net.daanlokdrog.classicachievement;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import java.util.*;

public class LegacyAchievementScreen extends Screen implements ClientAdvancements.Listener {
    private static final ResourceLocation ACHIEVEMENT_BG = new ResourceLocation("minecraft", "textures/gui/achievement/achievement_background.png");
    private final ClientAdvancements clientAdvancements;
    private final Map<Advancement, AdvancementProgress> progressMap = new HashMap<>();
    private final Map<Advancement, int[]> posCache = new HashMap<>();
    private final List<Advancement> rootAdvancements = new ArrayList<>();
    private int currentPageIndex = 0;
    private final int imageWidth = 256, imageHeight = 202;
    private double xScrollP, yScrollP, xScrollTarget, yScrollTarget;
    private int minX, maxX, minY, maxY;
    protected float zoom = 1.0F;

    public LegacyAchievementScreen(ClientAdvancements clientAdvancements) {
        super(Component.literal("Achievements"));
        this.clientAdvancements = clientAdvancements;
    }

    @Override
    protected void init() {
        this.clientAdvancements.setListener(this);
        int left = (this.width - imageWidth) / 2, top = (this.height - imageHeight) / 2;
        refreshRoots();
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(left + 141, top + 174, 100, 20).build());
        if (rootAdvancements.size() > 1) {
            this.addRenderableWidget(Button.builder(getCurrentPageName(), b -> {
                currentPageIndex = (currentPageIndex + 1) % rootAdvancements.size();
                b.setMessage(getCurrentPageName());
                updateLayout();
            }).bounds(left + 15, top + 174, 125, 20).build());
        }
        updateLayout();
    }

    private void updateLayout() {
        calculateRadialLayout();
        centerOnRoot();
    }

    private void refreshRoots() {
        rootAdvancements.clear();
        for (Advancement a : clientAdvancements.getAdvancements().getAllAdvancements()) {
            if (a.getDisplay() != null && a.getParent() == null) rootAdvancements.add(a);
        }
        if (currentPageIndex >= rootAdvancements.size()) currentPageIndex = 0;
    }

    private Component getCurrentPageName() {
        if (rootAdvancements.isEmpty()) return Component.literal("Minecraft");
        Advancement r = rootAdvancements.get(currentPageIndex);
        return (r.getId().getNamespace().equals("minecraft") && r.getId().getPath().startsWith("story/")) ? Component.literal("Minecraft") : r.getDisplay().getTitle();
    }

    private void calculateRadialLayout() {
        posCache.clear();
        if (rootAdvancements.isEmpty()) return;
        growBranch(rootAdvancements.get(currentPageIndex), 0, 0, 0, 0);
        int rMinX = Integer.MAX_VALUE, rMaxX = Integer.MIN_VALUE, rMinY = Integer.MAX_VALUE, rMaxY = Integer.MIN_VALUE;
        for (int[] p : posCache.values()) {
            rMinX = Math.min(rMinX, p[0]); rMaxX = Math.max(rMaxX, p[0]);
            rMinY = Math.min(rMinY, p[1]); rMaxY = Math.max(rMaxY, p[1]);
        }
        minX = rMinX - 112; maxX = rMaxX + 112; minY = rMinY - 250; maxY = rMaxY + 250;
    }

    private void growBranch(Advancement adv, int x, int y, double pAngle, int depth) {
        posCache.put(adv, new int[]{x, y});
        Random rnd = new Random(adv.getId().toString().hashCode());
        double spread = Math.toRadians(depth == 0 ? 360 : 160), start = pAngle - spread / 2.0;
        
        List<Advancement> children = new ArrayList<>();
        for (Advancement child : adv.getChildren()) {
            if (child.getDisplay() != null) children.add(child);
        }

        for (int i = 0; i < children.size(); i++) {
            double angle = start + (i + 0.5) * (spread / children.size());
            int nx = x + (int)(Math.cos(angle) * (100 + rnd.nextInt(20)));
            int ny = y + (int)(Math.sin(angle) * (100 + rnd.nextInt(20)));
            if (Math.abs(ny - y) < 32) ny = y + (ny >= y ? 32 : -32);
            growBranch(children.get(i), nx, ny, angle, depth + 1);
        }
    }

    private void centerOnRoot() {
        if (rootAdvancements.isEmpty()) return;
        int[] p = posCache.get(rootAdvancements.get(currentPageIndex));
        if (p != null) {
            xScrollTarget = xScrollP = p[0] - 112;
            yScrollTarget = yScrollP = p[1] - 77;
            clampScrollTarget();
        }
    }

    private void clampScrollTarget() {
        xScrollTarget = Mth.clamp(xScrollTarget, minX, maxX);
        yScrollTarget = Mth.clamp(yScrollTarget, minY, maxY);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (ClassicAchievementConfig.OLDER_UI.get()) { this.zoom = 1.2F; return false; }
        float old = zoom;
        zoom = Mth.clamp(zoom + (delta < 0 ? 0.25F : -0.25F), 1.0F, 2.0F);
        if (zoom != old) {
            xScrollP -= (zoom * imageWidth - old * imageWidth) * 0.5;
            yScrollP -= (zoom * imageHeight - old * imageHeight) * 0.5;
            xScrollTarget = xScrollP; yScrollTarget = yScrollP;
        }
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        if (ClassicAchievementConfig.OLDER_UI.get()) zoom = 1.2F;
        xScrollP += (xScrollTarget - xScrollP) * 0.2;
        yScrollP += (yScrollTarget - yScrollP) * 0.2;
        renderBackground(g);
        int l = (width - imageWidth) / 2, t = (height - imageHeight) / 2, vx = l + 16, vy = t + 17;
        g.pose().pushPose();
        g.pose().translate(vx, vy, 0);
        g.enableScissor(vx, vy, vx + 224, vy + 155);
        g.pose().pushPose();
        g.pose().scale(1 / zoom, 1 / zoom, 1);
        renderLegacyBackground(g);
        Advancement h = rootAdvancements.isEmpty() ? null : renderAdvancementTree(g, (int)((mx - vx) * zoom), (int)((my - vy) * zoom));
        g.pose().popPose();
        g.disableScissor();
        g.pose().popPose();
        RenderSystem.enableBlend();
        g.pose().pushPose();
        g.pose().translate(0, 0, 200);
        g.blit(ACHIEVEMENT_BG, l, t, 0, 0, imageWidth, imageHeight);
        g.drawString(font, Component.translatable("gui.achievements"), l + 15, t + 5, 4210752, false);
        g.pose().popPose();
        g.pose().pushPose();
        g.pose().translate(0, 0, 210);
        super.render(g, mx, my, pt);
        g.pose().popPose();
        if (h != null) renderLegacyTooltip(g, h, mx, my);
    }

    private void renderLegacyBackground(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        int sx = (int)Math.floor(xScrollP) + 288, sy = (int)Math.floor(yScrollP) + 288;
        int k1 = sx >> 4, l1 = sy >> 4, i2 = Math.floorMod(sx, 16), j2 = Math.floorMod(sy, 16);
        Random rnd = new Random();
        for (int y = 0; y * 16.0F - j2 < 155.0F * zoom; y++) {
            float f = Mth.clamp(0.6F - (l1 + y) / 25.0F * 0.3F, 0.3F, 0.6F);
            for (int x = 0; x * 16.0F - i2 < 224.0F * zoom; x++) {
                rnd.setSeed(mc.getUser().getUuid().hashCode() + k1 + x + (l1 + y) * 16L);
                int cy = l1 + y, j4 = rnd.nextInt(1 + Math.max(0, cy)) + cy / 2;
                Block b = (j4 <= 37 && cy != 35) ? (j4 == 22 ? (rnd.nextBoolean() ? Blocks.DIAMOND_ORE : Blocks.REDSTONE_ORE) : (j4 == 10 ? Blocks.IRON_ORE : (j4 == 8 ? Blocks.COAL_ORE : (j4 > 4 ? Blocks.STONE : (j4 > 0 ? Blocks.DIRT : (j4 >= -2 && rnd.nextInt(4) <= j4 + 2 ? Blocks.DIRT : Blocks.SAND)))))) : Blocks.BEDROCK;
                TextureAtlasSprite s = mc.getBlockRenderer().getBlockModel(b.defaultBlockState()).getParticleIcon();
                RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
                g.setColor(f, f, f, 1);
                g.blit(x * 16 - i2, y * 16 - j2, 0, 16, 16, s);
            }
        }
        g.setColor(1, 1, 1, 1);
    }

    private Advancement renderAdvancementTree(GuiGraphics g, int rmx, int rmy) {
        Advancement hov = null;
        boolean old = ClassicAchievementConfig.OLDER_UI.get(), bright = !old || (Util.getMillis() / 400L % 2 == 0);
        for (Advancement a : posCache.keySet()) {
            if (a.getParent() == null || !posCache.containsKey(a.getParent()) || (getRequirementCount(a) > 4 && !isUnlocked(a))) continue;
            int[] p = posCache.get(a), pp = posCache.get(a.getParent());
            boolean done = isUnlocked(a), can = canUnlock(a);
            int c = done ? 0xFFA0A0A0 : (can ? 0xFF00FF00 : 0xFF000000);
            g.setColor((c >> 16 & 255) / 255f, (c >> 8 & 255) / 255f, (c & 255) / 255f, (old && can && !done && !bright) ? 0.5F : 1.0F);
            int tx = p[0] - (int)xScrollP + 13, ty = p[1] - (int)yScrollP + 13, cx = pp[0] - (int)xScrollP + 13, cy = pp[1] - (int)yScrollP + 13;
            g.hLine(cx, tx, cy, -1); g.vLine(tx, cy, ty, -1);
            if (!old) {
                if (ty != cy) g.blit(ACHIEVEMENT_BG, tx - 5, ty > cy ? ty - 18 : ty + 11, 96, ty > cy ? 234 : 241, 11, 7);
                else if (cx != tx) g.blit(ACHIEVEMENT_BG, tx > cx ? tx - 18 : tx + 11, ty - 5, tx > cx ? 114 : 107, 234, 7, 11);
            }
        }
        g.setColor(1, 1, 1, 1);
        for (Advancement a : posCache.keySet()) {
            int[] p = posCache.get(a);
            int x = p[0] - (int)xScrollP, y = p[1] - (int)yScrollP, d = getRequirementCount(a);
            if (x < -26 || y < -26 || x > 224 * zoom || y > 155 * zoom || (d > 4 && !isUnlocked(a))) continue;
            boolean done = isUnlocked(a), can = canUnlock(a);
            float br = done ? 1.0F : (can ? (old ? (bright ? 0.776F : 0.466F) : 0.4F) : (d == 3 ? 0.2F : 0.1F));
            g.setColor(br, br, br, 1);
            g.blit(ACHIEVEMENT_BG, x, y, a.getDisplay().getFrame().getName().equals("challenge") ? 26 : 0, 202, 26, 26);
            g.setColor(1, 1, 1, 1);
            g.renderFakeItem(a.getDisplay().getIcon(), x + 5, y + 5);
            if (rmx >= x && rmx <= x + 26 && rmy >= y && rmy <= y + 26 && (done || can || d <= 3)) hov = a;
        }
        return hov;
    }

    private void renderLegacyTooltip(GuiGraphics g, Advancement a, int mx, int my) {
        DisplayInfo d = a.getDisplay();
        boolean done = isUnlocked(a), can = canUnlock(a), chal = d.getFrame().getName().equals("challenge");
        int dist = getRequirementCount(a), color = (done || can) ? (chal ? 0xFFFFFF80 : 0xFFFFFFFF) : (chal ? 0xFF808040 : 0xFF808080);
        String title = (dist == 3 && !done && !can) ? Component.translatable("achievement.unknown").getString() : d.getTitle().getString();
        List<net.minecraft.util.FormattedCharSequence> lines = (done || can) ? font.split(d.getDescription(), 120) : Collections.emptyList();
        String pTitle = (a.getParent() != null && a.getParent().getDisplay() != null) ? a.getParent().getDisplay().getTitle().getString() : null;
        boolean req = !done && !can && pTitle != null && dist <= 3;
        int h = 12 + lines.size() * 9 + (done ? 12 : (req ? 22 : 0)), w = Math.max(font.width(title), 120);
        g.pose().pushPose(); g.pose().translate(0, 0, 400);
        g.fillGradient(mx + 9, my - 7, mx + w + 15, my + h + 3, 0xC0101010, 0xC0101010);
        g.drawString(font, title, mx + 12, my, color, true);
        int cy = my + 12;
        for (var l : lines) { g.drawString(font, l, mx + 12, cy, -6250336, false); cy += 9; }
        if (done) g.drawString(font, Component.translatable("achievement.taken"), mx + 12, cy + 4, -7302913, false);
        else if (req) { g.drawString(font, Component.translatable("achievement.requires.text"), mx + 12, cy + 4, -9416624, false); g.drawString(font, "‘" + pTitle + "’", mx + 12, cy + 13, -9416624, false); }
        g.pose().popPose();
    }

    private int getRequirementCount(Advancement a) {
        int c = 0;
        for (Advancement cur = a; cur != null && !isUnlocked(cur); cur = cur.getParent()) c++;
        return c;
    }

    private boolean isUnlocked(Advancement a) { AdvancementProgress p = progressMap.get(a); return p != null && p.isDone(); }
    private boolean canUnlock(Advancement a) { return a.getParent() == null || isUnlocked(a.getParent()); }
    @Override public boolean mouseDragged(double mx, double my, int b, double dx, double dy) { if (b == 0) { xScrollTarget -= dx * zoom; yScrollTarget -= dy * zoom; clampScrollTarget(); return true; } return false; }
    @Override public void removed() { clientAdvancements.setListener(null); }
    @Override public boolean isPauseScreen() { return true; }
    @Override public void onUpdateAdvancementProgress(Advancement a, AdvancementProgress p) { progressMap.put(a, p); }
    @Override public void onSelectedTabChanged(Advancement a) {}
    @Override public void onAddAdvancementRoot(Advancement a) { refreshRoots(); updateLayout(); }
    @Override public void onRemoveAdvancementRoot(Advancement a) { refreshRoots(); updateLayout(); }
    @Override public void onAddAdvancementTask(Advancement a) {}
    @Override public void onRemoveAdvancementTask(Advancement a) {}
    @Override public void onAdvancementsCleared() { progressMap.clear(); refreshRoots(); updateLayout(); }
}
