package net.daanlokdrog.classicachievement;

import com.mojang.blaze3d.systems.RenderSystem;
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

import net.daanlokdrog.classicachievement.ClassicAchievementConfig;
import net.minecraft.Util;

import java.util.*;

//restore old achievement menu. pre-1.12 style.
//Since the logic of the old achievement system is completely different from the modern advancement system, adjustments have been made for the modern version.

public class LegacyAchievementScreen extends Screen implements ClientAdvancements.Listener {
    private static final ResourceLocation ACHIEVEMENT_BG = new ResourceLocation("minecraft", "textures/gui/achievement/achievement_background.png");
    
    private final ClientAdvancements clientAdvancements;
    private final Map<Advancement, AdvancementProgress> progressMap = new HashMap<>();
    private final Map<Advancement, int[]> posCache = new HashMap<>();
    private final List<Advancement> rootAdvancements = new ArrayList<>();
    
    private int currentPageIndex = 0;
    private Button pageSwitchButton;
    protected int imageWidth = 256;
    protected int imageHeight = 202;
    protected double xScrollP, yScrollP, xScrollTarget, yScrollTarget;
    private int minX, maxX, minY, maxY;
    protected float zoom = 1.0F;

    public LegacyAchievementScreen(ClientAdvancements clientAdvancements) {
        super(Component.literal("Achievements"));
        this.clientAdvancements = clientAdvancements;
    }

    @Override
    protected void init() {
        this.clientAdvancements.setListener(this);
        int left = (this.width - this.imageWidth) / 2;
        int top = (this.height - this.imageHeight) / 2;
        
        refreshRoots();

        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), (btn) -> this.onClose())
                .bounds(left + 141, top + 174, 100, 20).build());
        
        if (rootAdvancements.size() > 1) {
            this.pageSwitchButton = Button.builder(getCurrentPageName(), (btn) -> {
                currentPageIndex = (currentPageIndex + 1) % rootAdvancements.size();
                btn.setMessage(getCurrentPageName());
                calculateRadialLayout();
                centerOnRoot();
            }).bounds(left + 15, top + 174, 125, 20).build();
            this.addRenderableWidget(pageSwitchButton);
        }

        calculateRadialLayout();
        centerOnRoot();
    }

    private void refreshRoots() {
        rootAdvancements.clear();
        for (Advancement adv : clientAdvancements.getAdvancements().getAllAdvancements()) {
            if (adv.getDisplay() != null && adv.getParent() == null) {
                rootAdvancements.add(adv);
            }
        }
        if (currentPageIndex >= rootAdvancements.size()) currentPageIndex = 0;
    }

private Component getCurrentPageName() {
        if (rootAdvancements.isEmpty()) return Component.literal("Minecraft");
        
        Advancement root = rootAdvancements.get(currentPageIndex);
        ResourceLocation id = root.getId();

        if (id.getNamespace().equals("minecraft") && id.getPath().startsWith("story/")) {
            return Component.literal("Minecraft");
        }
        
        return root.getDisplay().getTitle();
    }

    private void calculateRadialLayout() {
        posCache.clear();
        if (rootAdvancements.isEmpty()) return;
        growBranch(rootAdvancements.get(currentPageIndex), 0, 0, 0, 0);

        int rMinX = Integer.MAX_VALUE, rMaxX = Integer.MIN_VALUE;
        int rMinY = Integer.MAX_VALUE, rMaxY = Integer.MIN_VALUE;
        for (int[] pos : posCache.values()) {
            rMinX = Math.min(rMinX, pos[0]); rMaxX = Math.max(rMaxX, pos[0]);
            rMinY = Math.min(rMinY, pos[1]); rMaxY = Math.max(rMaxY, pos[1]);
        }
        
        this.minX = rMinX - 112;
        this.maxX = rMaxX + 112;
        this.minY = rMinY - 250;
        this.maxY = rMaxY + 250;
    }

private void growBranch(Advancement adv, int x, int y, double parentAngle, int depth) {
        posCache.put(adv, new int[]{x, y});
        List<Advancement> children = new ArrayList<>();
        for (Advancement child : adv.getChildren()) {
            if (child.getDisplay() != null) {
                children.add(child);
            }
        }
        
        if (children.isEmpty()) return;

        Random random = new Random(adv.getId().toString().hashCode());
        double spread = Math.toRadians(depth == 0 ? 360 : 160);
        double startAngle = parentAngle - spread / 2.0;

        for (int i = 0; i < children.size(); i++) {
            double angle = startAngle + (i + 0.5) * (spread / children.size());
            double dist = 100 + random.nextInt(20);
            int nx = x + (int) (Math.cos(angle) * dist);
            int ny = y + (int) (Math.sin(angle) * dist);
            
            if (Math.abs(ny - y) < 32) {
                ny = y + (ny >= y ? 32 : -32);
            }
            
            growBranch(children.get(i), nx, ny, angle, depth + 1);
        }
    }

    private void centerOnRoot() {
        if (rootAdvancements.isEmpty()) return;
        int[] pos = posCache.get(rootAdvancements.get(currentPageIndex));
        if (pos != null) {
            this.xScrollTarget = pos[0] - 112;
            this.yScrollTarget = pos[1] - 77;
            clampScrollTarget();
            this.xScrollP = this.xScrollTarget;
            this.yScrollP = this.yScrollTarget;
        }
    }

    private void clampScrollTarget() {
        this.xScrollTarget = Mth.clamp(this.xScrollTarget, minX, maxX);
        this.yScrollTarget = Mth.clamp(this.yScrollTarget, minY, maxY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {

    	boolean olderUI = ClassicAchievementConfig.OLDER_UI.get();

    	if (olderUI) {
            this.zoom = 1.2F;
            return false;
        }
        
        float oldZoom = this.zoom;
        if (delta < 0) this.zoom += 0.25F;
        else if (delta > 0) this.zoom -= 0.25F;
        this.zoom = Mth.clamp(this.zoom, 1.0F, 2.0F);

        if (this.zoom != oldZoom) {
            float f = this.zoom * (float)this.imageWidth;
            float f1 = this.zoom * (float)this.imageHeight;
            float f3 = oldZoom * (float)this.imageWidth;
            float f4 = oldZoom * (float)this.imageHeight;
            this.xScrollP -= (double)((f - f3) * 0.5F);
            this.yScrollP -= (double)((f1 - f4) * 0.5F);
            this.xScrollTarget = this.xScrollP;
            this.yScrollTarget = this.yScrollP;
        }
        return true;
    }

@Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {

    	if (ClassicAchievementConfig.OLDER_UI.get()) {
            this.zoom = 1.2F;
        }
        
        this.xScrollP += (this.xScrollTarget - this.xScrollP) * 0.2D;
        this.yScrollP += (this.yScrollTarget - this.yScrollP) * 0.2D;
        
        this.renderBackground(guiGraphics);
        int left = (this.width - this.imageWidth) / 2;
        int top = (this.height - this.imageHeight) / 2;
        int viewX = left + 16, viewY = top + 17;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(viewX, viewY, 0.0F);
        guiGraphics.enableScissor(viewX, viewY, viewX + 224, viewY + 155);
        
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(1.0F / this.zoom, 1.0F / this.zoom, 1.0F);
        
        renderLegacyBackground(guiGraphics);

        Advancement hovered = null;
        if (!rootAdvancements.isEmpty()) {
            float zoomMX = (float)(mouseX - viewX) * this.zoom;
            float zoomMY = (float)(mouseY - viewY) * this.zoom;
            hovered = renderAdvancementTree(guiGraphics, (int)zoomMX, (int)zoomMY);
        }

        guiGraphics.pose().popPose();
        guiGraphics.disableScissor();
        guiGraphics.pose().popPose();
        
        RenderSystem.enableBlend();
        
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 200.0F);
        
        guiGraphics.blit(ACHIEVEMENT_BG, left, top, 0, 0, this.imageWidth, this.imageHeight);
        guiGraphics.drawString(this.font, Component.translatable("gui.achievements"), left + 15, top + 5, 4210752, false);
        
        guiGraphics.pose().popPose();
        
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 210.0F);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        guiGraphics.pose().popPose();

        if (hovered != null) renderLegacyTooltip(guiGraphics, hovered, mouseX, mouseY);
    }

private void renderLegacyBackground(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        int scrollX = (int) Math.floor(xScrollP);
        int scrollY = (int) Math.floor(yScrollP);
        
        int totalScrollX = scrollX + 288;
        int totalScrollY = scrollY + 288;

        int k1 = totalScrollX >> 4;
        int l1 = totalScrollY >> 4;
        int i2 = Math.floorMod(totalScrollX, 16);
        int j2 = Math.floorMod(totalScrollY, 16);
        
        Random random = new Random();

        for (int l3 = 0; (float)l3 * 16.0F - (float)j2 < 155.0F * zoom; ++l3) {
            float f2 = Mth.clamp(0.6F - (float) (l1 + l3) / 25.0F * 0.3F, 0.3F, 0.6F);
            for (int i4 = 0; (float)i4 * 16.0F - (float)i2 < 224.0F * zoom; ++i4) {
                random.setSeed((long) (mc.getUser().getUuid().hashCode() + k1 + i4 + (l1 + l3) * 16));
                int curY = l1 + l3;
                int j4 = random.nextInt(1 + Math.max(0, curY)) + curY / 2;
                Block block;
                if (j4 <= 37 && curY != 35) {
                    if (j4 == 22) block = random.nextInt(2) == 0 ? Blocks.DIAMOND_ORE : Blocks.REDSTONE_ORE;
                    else if (j4 == 10) block = Blocks.IRON_ORE;
                    else if (j4 == 8) block = Blocks.COAL_ORE;
                    else if (j4 > 4) block = Blocks.STONE;
                    else if (j4 > 0) block = Blocks.DIRT;
                    else if (j4 >= -2 && random.nextInt(4) <= (j4 + 2)) block = Blocks.DIRT;
                    else block = Blocks.SAND;
                } else {
                    block = Blocks.BEDROCK;
                }

                TextureAtlasSprite sprite = mc.getBlockRenderer().getBlockModel(block.defaultBlockState()).getParticleIcon();
                RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
                guiGraphics.setColor(f2, f2, f2, 1.0F);
                guiGraphics.blit(i4 * 16 - i2, l3 * 16 - j2, 0, 16, 16, sprite);
            }
        }
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

private Advancement renderAdvancementTree(GuiGraphics guiGraphics, int relativeMX, int relativeMY) {
        Advancement hovered = null;
        boolean olderUI = ClassicAchievementConfig.OLDER_UI.get();
        boolean isBright = !olderUI || (Util.getMillis() / 400L % 2L == 0L);

        for (Advancement adv : posCache.keySet()) {
            if (adv.getParent() == null || !posCache.containsKey(adv.getParent())) continue;
            int[] pos = posCache.get(adv);
            int[] pPos = posCache.get(adv.getParent());
            
            int dist = getRequirementCount(adv);
            if (dist > 4 && !isUnlocked(adv)) continue;

            int x = pos[0] - (int)xScrollP;
            int y = pos[1] - (int)yScrollP;
            int px = pPos[0] - (int)xScrollP;
            int py = pPos[1] - (int)yScrollP;

            boolean unlocked = isUnlocked(adv);
            boolean canUnlock = canUnlock(adv);

            int color;
            float alpha = 1.0F;

            if (unlocked) {
                color = 0xFFA0A0A0;
            } else if (canUnlock) {
                color = 0xFF00FF00;
                if (olderUI && !isBright) {
                    alpha = 0.5F;
                }
            } else {
                color = 0xFF000000;
            }

            int centerX = px + 13;
            int centerY = py + 13;
            int targetX = x + 13;
            int targetY = y + 13;

            guiGraphics.setColor((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, alpha);
            guiGraphics.hLine(centerX, targetX, centerY, -1);
            guiGraphics.vLine(targetX, centerY, targetY, -1);
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

            if (!olderUI) {
                guiGraphics.setColor((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, 1.0F);
                if (targetY != centerY) {
                    if (targetY > centerY) guiGraphics.blit(ACHIEVEMENT_BG, targetX - 5, targetY - 18, 96, 234, 11, 7);
                    else guiGraphics.blit(ACHIEVEMENT_BG, targetX - 5, targetY + 11, 96, 241, 11, 7);
                } else if (centerX != targetX) {
                    if (targetX > centerX) guiGraphics.blit(ACHIEVEMENT_BG, targetX - 18, targetY - 5, 114, 234, 7, 11);
                    else guiGraphics.blit(ACHIEVEMENT_BG, targetX + 11, targetY - 5, 107, 234, 7, 11);
                }
                guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
        }

        for (Advancement adv : posCache.keySet()) {
            int[] pos = posCache.get(adv);
            int x = pos[0] - (int)xScrollP, y = pos[1] - (int)yScrollP;
            if (x < -26 || y < -26 || x > 224 * zoom || y > 155 * zoom) continue;

            int dist = getRequirementCount(adv);
            if (dist > 4 && !isUnlocked(adv)) continue;

            boolean isDone = isUnlocked(adv);
            boolean canUnlock = canUnlock(adv);

            if (isDone) {
                guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            } else if (canUnlock) {
                if (olderUI) {
                    float br = isBright ? 0.776F : 0.466F;
                    guiGraphics.setColor(br, br, br, 1.0F);
                } else {
                    guiGraphics.setColor(0.4F, 0.4F, 0.4F, 1.0F);
                }
            } else {
                float brightness = (dist == 3 ? 0.2F : 0.1F);
                guiGraphics.setColor(brightness, brightness, brightness, 1.0F);
            }

            int u = adv.getDisplay().getFrame().getName().equals("challenge") ? 26 : 0;
            guiGraphics.blit(ACHIEVEMENT_BG, x, y, u, 202, 26, 26);
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            guiGraphics.renderFakeItem(adv.getDisplay().getIcon(), x + 5, y + 5);

            if (relativeMX >= x && relativeMX <= x + 26 && relativeMY >= y && relativeMY <= y + 26) {
                if (isDone || canUnlock || dist <= 3) {
                    hovered = adv;
                }
            }
        }
        return hovered;
    }

private void renderLegacyTooltip(GuiGraphics guiGraphics, Advancement adv, int mX, int mY) {
        DisplayInfo d = adv.getDisplay();
        boolean isDone = isUnlocked(adv);
        boolean canUnlock = canUnlock(adv);
        boolean isChallenge = d.getFrame().getName().equals("challenge");
        int dist = getRequirementCount(adv);
        
        boolean showUnknown = dist == 3 && !isDone && !canUnlock;
        
        int titleColor;
        if (isDone || canUnlock) {
            titleColor = isChallenge ? 0xFFFFFF80 : 0xFFFFFFFF;
        } else {
            titleColor = isChallenge ? 0xFF808040 : 0xFF808080;
        }

        String titleText = showUnknown ? Component.translatable("achievement.unknown").getString() : d.getTitle().getString();
        
        List<net.minecraft.util.FormattedCharSequence> descLines;
        if (isDone || canUnlock) {
            descLines = this.font.split(d.getDescription(), 120);
        } else {
            descLines = Collections.emptyList();
        }

        String parentTitle = (adv.getParent() != null && adv.getParent().getDisplay() != null) ? adv.getParent().getDisplay().getTitle().getString() : null;
        boolean showRequires = !isDone && !canUnlock && parentTitle != null && dist <= 3;

        int totalHeight = 12 + descLines.size() * 9;
        if (isDone) totalHeight += 12; else if (showRequires) totalHeight += 22;

        int boxWidth = Math.max(this.font.width(titleText), 120);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 400);
        guiGraphics.fillGradient(mX + 9, mY - 7, mX + boxWidth + 15, mY + totalHeight + 3, 0xC0101010, 0xC0101010);
        guiGraphics.drawString(this.font, titleText, mX + 12, mY, titleColor, true);
        
        int curY = mY + 12;
        for (var line : descLines) {
            guiGraphics.drawString(this.font, line, mX + 12, curY, -6250336, false);
            curY += 9;
        }
        
        if (isDone) {
            guiGraphics.drawString(this.font, Component.translatable("achievement.taken"), mX + 12, curY + 4, -7302913, false);
        } else if (showRequires) {
            guiGraphics.drawString(this.font, Component.translatable("achievement.requires.text"), mX + 12, curY + 4, -9416624, false);
            guiGraphics.drawString(this.font, "‘" + parentTitle + "’", mX + 12, curY + 13, -9416624, false);
        }
        guiGraphics.pose().popPose();
    }

    private int getRequirementCount(Advancement adv) {
        int count = 0;
        Advancement current = adv;
        while (current != null && !isUnlocked(current)) {
            count++;
            current = current.getParent();
        }
        return count;
    }

    private boolean isUnlocked(Advancement adv) { 
        AdvancementProgress p = progressMap.get(adv); 
        return p != null && p.isDone(); 
    }
    
    private boolean canUnlock(Advancement adv) { 
        return adv.getParent() == null || isUnlocked(adv.getParent()); 
    }

    @Override public boolean mouseDragged(double mX, double mY, int b, double dX, double dY) {
        if (b == 0) { this.xScrollTarget -= dX * zoom; this.yScrollTarget -= dY * zoom; clampScrollTarget(); return true; }
        return false;
    }

    @Override public void removed() { this.clientAdvancements.setListener(null); }
    @Override public boolean isPauseScreen() { return true; }
    @Override public void onUpdateAdvancementProgress(Advancement a, AdvancementProgress p) { this.progressMap.put(a, p); }
    @Override public void onSelectedTabChanged(Advancement a) {}
    @Override public void onAddAdvancementRoot(Advancement a) { refreshRoots(); calculateRadialLayout(); }
    @Override public void onRemoveAdvancementRoot(Advancement a) { refreshRoots(); calculateRadialLayout(); }
    @Override public void onAddAdvancementTask(Advancement a) {}
    @Override public void onRemoveAdvancementTask(Advancement a) {}
    @Override public void onAdvancementsCleared() { this.progressMap.clear(); refreshRoots(); calculateRadialLayout(); }
}