package org.daanlokdrog.classicachievement.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.advancements.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.daanlokdrog.classicachievement.ClassicAchievementConfig;

import java.util.*;

public class LegacyAchievementScreen extends Screen implements ClientAdvancements.Listener {
    private static final ResourceLocation ACHIEVEMENT_BG = ResourceLocation.withDefaultNamespace("textures/gui/achievement/achievement_background.png");
    private final ClientAdvancements clientAdvancements;
    private final Map<AdvancementHolder, AdvancementProgress> progressMap = new HashMap<>();
    private final Map<AdvancementHolder, int[]> posCache = new LinkedHashMap<>();
    private final List<AdvancementNode> rootNodes = new ArrayList<>();
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
        refreshRoots();
        int left = (this.width - imageWidth) / 2, top = (this.height - imageHeight) / 2;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(left + 141, top + 174, 100, 20).build());
        if (rootNodes.size() > 1 && !ClassicAchievementConfig.isHidePage()) {
            this.addRenderableWidget(Button.builder(getCurrentPageName(), b -> {
                currentPageIndex = (currentPageIndex + 1) % rootNodes.size();
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
        rootNodes.clear();
        AdvancementTree tree = clientAdvancements.getTree();
        for (AdvancementNode node : tree.roots()) {
            if (node.advancement().display().isPresent()) rootNodes.add(node);
        }
        for (int i = 0; i < rootNodes.size(); i++) {
            AdvancementHolder r = rootNodes.get(i).holder();
            if ("minecraft".equals(r.id().getNamespace()) && r.id().getPath().contains("story/")) {
                currentPageIndex = i;
                break;
            }
        }
        if (currentPageIndex >= rootNodes.size()) currentPageIndex = 0;
    }

    private Component getCurrentPageName() {
        if (rootNodes.isEmpty()) return Component.literal("Minecraft");
        AdvancementNode node = rootNodes.get(currentPageIndex);
        return node.advancement().display().map(DisplayInfo::getTitle).orElse(Component.literal("Unknown"));
    }

    private void calculateRadialLayout() {
        posCache.clear();
        if (rootNodes.isEmpty()) return;
        growBranch(rootNodes.get(currentPageIndex), 0, 0, 0.0, 0);
        int rMinX = Integer.MAX_VALUE, rMaxX = Integer.MIN_VALUE, rMinY = Integer.MAX_VALUE, rMaxY = Integer.MIN_VALUE;
        for (int[] p : posCache.values()) {
            rMinX = Math.min(rMinX, p[0]);
            rMaxX = Math.max(rMaxX, p[0]);
            rMinY = Math.min(rMinY, p[1]);
            rMaxY = Math.max(rMaxY, p[1]);
        }
        minX = rMinX - 112;
        maxX = rMaxX + 112;
        minY = rMinY - 250;
        maxY = rMaxY + 250;
    }

    private void growBranch(AdvancementNode node, int x, int y, double pAngle, int depth) {
        posCache.put(node.holder(), new int[]{x, y});
        Random rnd = new Random(node.holder().id().toString().hashCode());
        double spread = depth == 0 ? 6.283185307179586 : 2.792526803190927;
        double start = pAngle - spread / 2.0;
        List<AdvancementNode> children = new ArrayList<>();
        for (AdvancementNode child : node.children()) if (child.advancement().display().isPresent()) children.add(child);
        for (int i = 0; i < children.size(); i++) {
            double angle = start + (i + 0.5) * (spread / children.size());
            int nx = 0, ny = 0;
            float scale = 1.0f;
            for (int t = 0; t < 12; t++) {
                int dist = (int) ((100 + rnd.nextInt(30)) * scale);
                nx = x + (int) (Math.cos(angle) * dist);
                ny = y + (int) (Math.sin(angle) * dist);
                if (Math.abs(ny - y) < 35) ny = y + (ny >= y ? 35 : -35);
                if (isPosClear(nx, ny)) break;
                scale += 0.2f;
                angle += (rnd.nextBoolean() ? 0.15 : -0.15);
            }
            growBranch(children.get(i), nx, ny, angle, depth + 1);
        }
    }

    private boolean isPosClear(int nx, int ny) {
        for (int[] p : posCache.values()) {
            long dx = nx - p[0], dy = ny - p[1];
            if (dx * dx + dy * dy < 2025L) return false;
        }
        return true;
    }

    private void centerOnRoot() {
        if (rootNodes.isEmpty()) return;
        int[] p = posCache.get(rootNodes.get(currentPageIndex).holder());
        if (p != null) {
            xScrollTarget = xScrollP = p[0] - 112;
            yScrollTarget = yScrollP = p[1] - 77;
            clampScrollTarget();
        }
    }

    private void clampScrollTarget() {
        xScrollTarget = Mth.clamp(xScrollTarget, (double) minX, (double) maxX);
        yScrollTarget = Mth.clamp(yScrollTarget, (double) minY, (double) maxY);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (ClassicAchievementConfig.isOlderUI()) {
            this.zoom = 1.2F;
            return false;
        }
        float old = zoom;
        zoom = Mth.clamp(zoom + (scrollY < 0 ? 0.25F : -0.25F), 1.0F, 2.0F);
        if (zoom != old) {
            xScrollP -= (zoom * imageWidth - old * imageWidth) * 0.5;
            yScrollP -= (zoom * imageHeight - old * imageHeight) * 0.5;
            xScrollTarget = xScrollP;
            yScrollTarget = yScrollP;
        }
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        if (ClassicAchievementConfig.isOlderUI()) zoom = 1.2F;
        xScrollP += (xScrollTarget - xScrollP) * 0.2;
        yScrollP += (yScrollTarget - yScrollP) * 0.2;

        g.fill(0, 0, this.width, this.height, 0x44000000);

        int l = (width - imageWidth) / 2;
        int t = (height - imageHeight) / 2;
        int vx = l + 16;
        int vy = t + 17;

        g.pose().pushPose();
        g.enableScissor(vx, vy, vx + 224, vy + 155);
        g.pose().pushPose();
        g.pose().translate(vx, vy, 0);
        g.pose().scale(1 / zoom, 1 / zoom, 1);

        renderLegacyBackground(g);

        RenderSystem.disableDepthTest();
        AdvancementHolder h = rootNodes.isEmpty() ? null : renderAdvancementTree(g, (int) ((mx - vx) * zoom), (int) ((my - vy) * zoom));
        RenderSystem.enableDepthTest();

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
        g.pose().translate(0, 0, 300);
        for (var r : this.children()) {
            if (r instanceof net.minecraft.client.gui.components.Renderable renderable) {
                renderable.render(g, mx, my, pt);
            }
        }
        g.pose().popPose();

        if (h != null) {
            renderLegacyTooltip(g, h, mx, my);
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, this.width, this.height, 0x44000000);
    }

    private void renderLegacyBackground(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);

        int sx = Mth.floor(xScrollP) + 288;
        int sy = Mth.floor(yScrollP) + 288;

        int k1 = sx >> 4;
        int l1 = sy >> 4;
        int i2 = sx & 15;
        int j2 = sy & 15;

        Random rnd = new Random();

        for (int y = -1; y * 16.0F - j2 < 155.0F * zoom + 16.0F; y++) {
            float f = Mth.clamp(0.6F - (l1 + y) / 25.0F * 0.3F, 0.3F, 0.6F);
            for (int x = -1; x * 16.0F - i2 < 224.0F * zoom + 16.0F; x++) {
                rnd.setSeed(mc.getUser().getProfileId().hashCode() + k1 + x + (l1 + y) * 16L);
                int cy = l1 + y;
                int j4 = rnd.nextInt(1 + Math.max(0, cy)) + cy / 2;

                Block b = (j4 <= 37 && cy != 35) ? (j4 == 22 ? (rnd.nextBoolean() ? Blocks.DIAMOND_ORE : Blocks.REDSTONE_ORE) : (j4 == 10 ? Blocks.IRON_ORE : (j4 == 8 ? Blocks.COAL_ORE : (j4 > 4 ? Blocks.STONE : (j4 > 0 ? Blocks.DIRT : (j4 >= -2 && rnd.nextInt(4) <= j4 + 2 ? Blocks.DIRT : Blocks.SAND)))))) : Blocks.BEDROCK;

                TextureAtlasSprite s = mc.getBlockRenderer().getBlockModel(b.defaultBlockState()).getParticleIcon();
                g.setColor(f, f, f, 1.0F);
                g.blit(x * 16 - i2, y * 16 - j2, 0, 16, 16, s);
            }
        }
        g.setColor(1, 1, 1, 1);
    }

    private AdvancementHolder renderAdvancementTree(GuiGraphics g, int rmx, int rmy) {
        AdvancementHolder hov = null;
        boolean old = ClassicAchievementConfig.isOlderUI(), bright = !old || (Util.getMillis() / 400L % 2 == 0);
        AdvancementTree tree = clientAdvancements.getTree();
        for (Map.Entry<AdvancementHolder, int[]> e : posCache.entrySet()) {
            AdvancementHolder a = e.getKey();
            AdvancementNode node = tree.get(a);
            if (node == null || node.parent() == null || !posCache.containsKey(node.parent().holder()))
                continue;
            int[] pos = e.getValue(), ppos = posCache.get(node.parent().holder());
            boolean done = isUnlocked(a), can = canUnlock(a);
            int c = done ? 0xFFA0A0A0 : (can ? 0xFF00FF00 : 0xFF000000);
            g.setColor((c >> 16 & 255) / 255f, (c >> 8 & 255) / 255f, (c & 255) / 255f, (old && can && !done && !bright) ? 0.5F : 1.0F);
            float tx = pos[0] - (float) xScrollP + 13, ty = pos[1] - (float) yScrollP + 13, cx = ppos[0] - (float) xScrollP + 13, cy = ppos[1] - (float) yScrollP + 13;
            g.hLine((int)cx, (int)tx, (int)cy, -1);
            g.vLine((int)tx, (int)cy, (int)ty, -1);
            if (!old) {
                if ((int)ty != (int)cy)
                    g.blit(ACHIEVEMENT_BG, (int)tx - 5, (int)ty > (int)cy ? (int)ty - 18 : (int)ty + 11, 96, (int)ty > (int)cy ? 234 : 241, 11, 7);
                else if ((int)cx != (int)tx)
                    g.blit(ACHIEVEMENT_BG, (int)tx > (int)cx ? (int)tx - 18 : (int)tx + 11, (int)ty - 5, (int)tx > (int)cx ? 114 : 107, 234, 7, 11);
            }
        }
        g.setColor(1, 1, 1, 1);
        for (Map.Entry<AdvancementHolder, int[]> e : posCache.entrySet()) {
            AdvancementHolder a = e.getKey();
            int[] p = e.getValue();
            float x = p[0] - (float) xScrollP, y = p[1] - (float) yScrollP;
            int d = getRequirementCount(a);
            if (x < -26 || y < -26 || x > 224 * zoom || y > 155 * zoom) continue;
            DisplayInfo di = a.value().display().get();
            boolean done = isUnlocked(a), can = canUnlock(a);
            float br = done ? 1.0F : (can ? (old ? (bright ? 0.776F : 0.466F) : 0.4F) : (d == 3 ? 0.2F : 0.1F));
            g.setColor(br, br, br, 1.0F);
            g.blit(ACHIEVEMENT_BG, (int)x, (int)y, di.getType() == AdvancementType.CHALLENGE ? 26 : 0, 202, 26, 26);
            g.setColor(1, 1, 1, 1);
            g.renderFakeItem(di.getIcon(), (int)x + 5, (int)y + 5);
            if (rmx >= x && rmx <= x + 26 && rmy >= y && rmy <= y + 26 && (done || can || d <= 3)) hov = a;
        }
        return hov;
    }
    private void renderLegacyTooltip(GuiGraphics g, AdvancementHolder a, int mx, int my) {
        DisplayInfo d = a.value().display().get();
        boolean done = isUnlocked(a), can = canUnlock(a), chal = d.getType() == AdvancementType.CHALLENGE;
        int dist = getRequirementCount(a), color = (done || can) ? (chal ? 0xFFFFFF80 : 0xFFFFFFFF) : (chal ? 0xFF808040 : 0xFF808080);
        String title = (dist == 3 && !done && !can) ? Component.translatable("achievement.unknown").getString() : d.getTitle().getString();
        List<net.minecraft.util.FormattedCharSequence> lines = (done || can) ? font.split(d.getDescription(), 120) : Collections.emptyList();
        AdvancementNode node = clientAdvancements.getTree().get(a);
        String pt = (node != null && node.parent() != null) ? node.parent().advancement().display().map(di -> di.getTitle().getString()).orElse(null) : null;
        boolean req = !done && !can && pt != null && dist <= 3;
        int h = 12 + lines.size() * 9 + (done ? 12 : (req ? 22 : 0)), w = Math.max(font.width(title), 120);
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.fillGradient(mx + 9, my - 7, mx + w + 15, my + h + 3, 0xC0101010, 0xC0101010);
        g.drawString(font, title, mx + 12, my, color, true);
        int cy = my + 12;
        for (var l : lines) {
            g.drawString(font, l, mx + 12, cy, -6250336, false);
            cy += 9;
        }
        if (done) g.drawString(font, Component.translatable("achievement.taken"), mx + 12, cy + 4, -7302913, false);
        else if (req) {
            g.drawString(font, Component.translatable("achievement.requires.text"), mx + 12, cy + 4, -9416624, false);
            g.drawString(font, "‘" + pt + "’", mx + 12, cy + 13, -9416624, false);
        }
        g.pose().popPose();
    }

    private int getRequirementCount(AdvancementHolder a) {
        int c = 0;
        AdvancementNode node = clientAdvancements.getTree().get(a);
        for (AdvancementNode cur = node; cur != null && !isUnlocked(cur.holder()); cur = cur.parent()) c++;
        return c;
    }

    private boolean isUnlocked(AdvancementHolder a) {
        AdvancementProgress p = progressMap.get(a);
        return p != null && p.isDone();
    }

    private boolean canUnlock(AdvancementHolder a) {
        AdvancementNode n = clientAdvancements.getTree().get(a);
        return n == null || n.parent() == null || isUnlocked(n.parent().holder());
    }

    @Override
    public boolean mouseDragged(double mx, double my, int b, double dx, double dy) {
        if (b == 0) {
            xScrollTarget -= dx * zoom;
            yScrollTarget -= dy * zoom;
            clampScrollTarget();
            return true;
        }
        return false;
    }

    @Override
    public void removed() {
        clientAdvancements.setListener(null);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public void onUpdateAdvancementProgress(AdvancementNode n, AdvancementProgress p) {
        progressMap.put(n.holder(), p);
    }

    @Override
    public void onSelectedTabChanged(AdvancementHolder a) {
    }

    @Override
    public void onAddAdvancementRoot(AdvancementNode n) {
        refreshRoots();
        updateLayout();
    }

    @Override
    public void onRemoveAdvancementRoot(AdvancementNode n) {
        refreshRoots();
        updateLayout();
    }

    @Override
    public void onAddAdvancementTask(AdvancementNode n) {
    }

    @Override
    public void onRemoveAdvancementTask(AdvancementNode n) {
    }

    @Override
    public void onAdvancementsCleared() {
        progressMap.clear();
        refreshRoots();
        updateLayout();
    }
}
