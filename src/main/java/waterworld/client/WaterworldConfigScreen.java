package waterworld.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import waterworld.WaterworldConfig;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class WaterworldConfigScreen extends Screen {
	private static final int ROW_HEIGHT = 24;
	private static final int LIST_TOP = 32;
	private static final int FOOTER_HEIGHT = 36;
	private static final int TOOLTIP_WIDTH = 250;
	private static final List<String> ACTIVATION_MODES = List.of("auto", "always", "never");

	private final Screen parent;
	private final Map<AbstractWidget, Component> tooltips = new IdentityHashMap<>();
	private final List<Runnable> appliers = new ArrayList<>();
	private WaterworldConfig config;
	private Path configDir;
	private ConfigOptionsList optionList;

	public WaterworldConfigScreen(Screen parent) {
		super(Component.translatable("config.project-waterworld.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		configDir = FabricLoader.getInstance().getConfigDir();
		config = WaterworldConfig.load(configDir);
		WaterworldConfig.INSTANCE = config;
		tooltips.clear();
		appliers.clear();

		int listHeight = this.height - LIST_TOP - FOOTER_HEIGHT;
		optionList = new ConfigOptionsList(this.minecraft, this.width - 40, listHeight, LIST_TOP, ROW_HEIGHT);
		optionList.setX(20);

		addSectionKey("config.project-waterworld.section.activation");
		addActivationModeOption();

		addSectionKey("config.project-waterworld.section.player_spawn");
		addStringOption("Spawn Ocean Biome", config.spawnOceanBiome, "warm_ocean",
				"Force spawn in a specific ocean biome. Leave empty to disable. Examples: warm_ocean, lukewarm_ocean, deep_ocean",
				v -> config.spawnOceanBiome = v);
		addBoolOption("Spawn Island", config.spawnIsland,
				"Generate a small island at world spawn.",
				v -> config.spawnIsland = v);
		addBoolOption("Spawn Gear", config.spawnGear,
				"Give players a bamboo chest raft with starter items on first spawn.",
				v -> config.spawnGear = v);

		addSectionKey("config.project-waterworld.section.guardians");
		addBoolOption("Wild Guardian Spawns", config.wildGuardianSpawns,
				"Guardians can spawn in open ocean water outside monuments.",
				v -> config.wildGuardianSpawns = v);
		addIntOption("Guardian Spawn Weight", config.guardianSpawnWeight,
				"Spawn weight for wild guardians. Squid is ~5, keep very low.",
				v -> config.guardianSpawnWeight = v);
		addDoubleOption("Guardian Spawn Chance", config.guardianSpawnChance,
				"Chance a guardian spawn attempt succeeds (0.0-1.0, lower = rarer).",
				v -> config.guardianSpawnChance = v);
		addIntOption("Guardian Min Days", config.guardianMinDays,
				"Days before wild guardians start spawning.",
				v -> config.guardianMinDays = v);
		addIntOption("Guardian Full Strength Days", config.guardianFullStrengthDays,
				"Day guardian spawn chance reaches its full configured value.",
				v -> config.guardianFullStrengthDays = v);
		addBoolOption("Drowned Ride Guardians", config.drownedRideGuardians,
				"Drowned riders appear on wild guardians with a configurable trident chance.",
				v -> config.drownedRideGuardians = v);
		addDoubleOption("Drowned Rider Chance", config.drownedRiderChance,
				"Chance a wild guardian spawns with a drowned rider (0.0-1.0).",
				v -> config.drownedRiderChance = v);
		addIntOption("Drowned Rider Min Days", config.drownedRiderMinDays,
				"Days before drowned riders appear on guardians.",
				v -> config.drownedRiderMinDays = v);
		addIntOption("Rider Full Strength Days", config.drownedRiderFullStrengthDays,
				"Day drowned rider chance reaches its full configured value.",
				v -> config.drownedRiderFullStrengthDays = v);
		addDoubleOption("Trident Rider Chance", config.tridentRiderChance,
				"Chance a drowned rider carries a trident (0.0-1.0).",
				v -> config.tridentRiderChance = v);
		addIntOption("Trident Drowned Min Days", config.tridentDrownedMinDays,
				"Days before drowned can spawn with tridents. 0 = immediate.",
				v -> config.tridentDrownedMinDays = v);
		addBoolOption("Drowned Can Go On Land", config.drownedCanGoOnLand,
				"Drowned can roam on land instead of returning to water.",
				v -> config.drownedCanGoOnLand = v);

		addSectionKey("config.project-waterworld.section.turtles");
		addBoolOption("Turtle Ocean Spawns", config.turtleOceanSpawns,
				"Turtles spawn naturally in biomes tagged #project-waterworld:turtle_spawns.",
				v -> config.turtleOceanSpawns = v);
		addIntOption("Turtle Spawn Weight", config.turtleSpawnWeight,
				"Spawn weight for ocean turtles.",
				v -> config.turtleSpawnWeight = v);

		addSectionKey("config.project-waterworld.section.illagers");
		addBoolOption("Ocean Pillager Patrols", config.oceanPillagerPatrols,
				"Pillager patrols and raids spawn in boats on water.",
				v -> config.oceanPillagerPatrols = v);
		addIntOption("Patrol Min Days", config.patrolMinDays,
				"Days before pillager patrols begin.",
				v -> config.patrolMinDays = v);
		addIntOption("Patrol Full Strength Days", config.patrolFullStrengthDays,
				"Day patrol frequency reaches full rate.",
				v -> config.patrolFullStrengthDays = v);
		addBoolOption("Pillager Armor", config.pillagerArmor,
				"Illagers spawn with armor during patrols and raids.",
				v -> config.pillagerArmor = v);
		addDoubleOption("Pillager Armor Chance", config.pillagerArmorChance,
				"Base chance per armor piece for illagers (0.0-1.0).",
				v -> config.pillagerArmorChance = v);
		addBoolOption("Armor Scales With Difficulty", config.armorScalesWithDifficulty,
				"Whether armor tier scales with the world difficulty setting.",
				v -> config.armorScalesWithDifficulty = v);

		addSectionKey("config.project-waterworld.section.traders");
		addBoolOption("Wandering Trader Boats", config.wanderingTraderBoats,
				"Wandering traders spawn in boats at sea with one llama.",
				v -> config.wanderingTraderBoats = v);
		addIntOption("Wandering Trader Min Days", config.wanderingTraderMinDays,
				"Days before wandering traders appear.",
				v -> config.wanderingTraderMinDays = v);
		addIntOption("Trader Full Strength Days", config.wanderingTraderFullStrengthDays,
				"Day wandering trader frequency reaches full rate.",
				v -> config.wanderingTraderFullStrengthDays = v);

		addSectionKey("config.project-waterworld.section.boats");
		addBoolOption("Mobs Can Exit Boats", config.mobsCanExitBoats,
				"Intelligent mobs can exit boats when they reach land.",
				v -> config.mobsCanExitBoats = v);
		addBoolOption("Mobs Can Pilot Boats", config.mobsCanPilotBoats,
				"Illagers and wandering traders can steer boats.",
				v -> config.mobsCanPilotBoats = v);

		this.addWidget(optionList);

		int buttonY = this.height - FOOTER_HEIGHT + 8;
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> saveAndClose())
				.pos(this.width / 2 - 154, buttonY)
				.size(150, 20)
				.build());
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> cancelAndClose())
				.pos(this.width / 2 + 4, buttonY)
				.size(150, 20)
				.build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		if (optionList != null) {
			optionList.extractRenderState(graphics, mouseX, mouseY, delta);
		}
		graphics.centeredText(this.font, this.title, this.width / 2, 8, 0xFFFFFFFF);

		for (var entry : tooltips.entrySet()) {
			AbstractWidget widget = entry.getKey();
			if (widget.isHovered()) {
				List<FormattedCharSequence> lines = this.font.split(entry.getValue(), TOOLTIP_WIDTH);
				graphics.setTooltipForNextFrame(this.font, lines, mouseX, mouseY);
				break;
			}
		}
	}

	private void saveAndClose() {
		for (Runnable applier : appliers) {
			applier.run();
		}
		config.activationMode = WaterworldConfig.normalizeActivationMode(config.activationMode);
		config.save(configDir);
		WaterworldConfig.INSTANCE = config;
		this.minecraft.gui.setScreen(parent);
	}

	private void cancelAndClose() {
		WaterworldConfig.INSTANCE = WaterworldConfig.load(configDir);
		this.minecraft.gui.setScreen(parent);
	}

	private void addSectionKey(String translationKey) {
		optionList.add(optionList.createSection(Component.translatable(translationKey)).asEntry());
	}

	private void addActivationModeOption() {
		String initial = WaterworldConfig.normalizeActivationMode(config.activationMode);
		CycleButton<String> button = CycleButton.<String>builder(this::activationLabel, initial)
				.withValues(ACTIVATION_MODES)
				.create(0, 0, 120, 20, Component.translatable("config.project-waterworld.option.activation_mode"),
						(btn, val) -> {});
		tooltips.put(button, Component.literal(
				"Auto = only in Waterworld worlds. Always = all worlds. Never = disabled (worldgen only)."));
		appliers.add(() -> config.activationMode = button.getValue());
		optionList.add(optionList.createOption(
				Component.translatable("config.project-waterworld.option.activation_mode"), button).asEntry());
	}

	private Component activationLabel(String mode) {
		return switch (mode) {
			case "always" -> Component.translatable("config.project-waterworld.activation.always");
			case "never" -> Component.translatable("config.project-waterworld.activation.never");
			default -> Component.translatable("config.project-waterworld.activation.auto");
		};
	}

	private void addBoolOption(String label, boolean value, String tooltip, BoolConsumer setter) {
		CycleButton<Boolean> button = CycleButton.onOffBuilder(value)
				.create(0, 0, 120, 20, Component.empty(), (btn, val) -> {});
		tooltips.put(button, Component.literal(tooltip));
		appliers.add(() -> setter.accept(button.getValue()));
		optionList.add(optionList.createOption(Component.literal(label), button).asEntry());
	}

	private void addIntOption(String label, int value, String tooltip, IntConsumer setter) {
		EditBox box = new EditBox(this.font, 0, 0, 120, 20, Component.literal(label));
		box.setValue(String.valueOf(value));
		box.setMaxLength(10);
		tooltips.put(box, Component.literal(tooltip));
		appliers.add(() -> {
			try {
				setter.accept(Integer.parseInt(box.getValue().trim()));
			} catch (NumberFormatException ignored) {
			}
		});
		optionList.add(optionList.createOption(Component.literal(label), box).asEntry());
	}

	private void addDoubleOption(String label, double value, String tooltip, DoubleConsumer setter) {
		EditBox box = new EditBox(this.font, 0, 0, 120, 20, Component.literal(label));
		box.setValue(String.valueOf(value));
		box.setMaxLength(16);
		tooltips.put(box, Component.literal(tooltip));
		appliers.add(() -> {
			try {
				setter.accept(Double.parseDouble(box.getValue().trim()));
			} catch (NumberFormatException ignored) {
			}
		});
		optionList.add(optionList.createOption(Component.literal(label), box).asEntry());
	}

	private void addStringOption(String label, String value, String hint, String tooltip, StringConsumer setter) {
		EditBox box = new EditBox(this.font, 0, 0, 120, 20, Component.literal(label));
		box.setValue(value);
		box.setMaxLength(256);
		box.setHint(Component.literal(hint));
		tooltips.put(box, Component.literal(tooltip));
		appliers.add(() -> setter.accept(box.getValue().trim()));
		optionList.add(optionList.createOption(Component.literal(label), box).asEntry());
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(parent);
	}

	@FunctionalInterface
	private interface BoolConsumer {
		void accept(boolean value);
	}

	@FunctionalInterface
	private interface IntConsumer {
		void accept(int value);
	}

	@FunctionalInterface
	private interface DoubleConsumer {
		void accept(double value);
	}

	@FunctionalInterface
	private interface StringConsumer {
		void accept(String value);
	}

	private final class ConfigOptionsList extends ContainerObjectSelectionList<ConfigOptionsList.BaseEntry> {
		ConfigOptionsList(Minecraft minecraft, int width, int height, int top, int itemHeight) {
			super(minecraft, width, height, top, itemHeight);
		}

		void add(BaseEntry entry) {
			this.addEntry(entry);
		}

		@Override
		protected int scrollBarX() {
			return this.getX() + this.width - 6;
		}

		@Override
		public int getRowWidth() {
			return this.width - 10;
		}

		SectionEntry createSection(Component title) {
			return new SectionEntry(title);
		}

		OptionEntry createOption(Component label, AbstractWidget widget) {
			return new OptionEntry(label, widget);
		}

		final class BaseEntry extends ContainerObjectSelectionList.Entry<BaseEntry> {
			private final EntryRenderer renderer;

			BaseEntry(EntryRenderer renderer) {
				this.renderer = renderer;
			}

			@Override
			public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
					boolean hovered, float partialTick) {
				renderer.render(this, graphics, mouseX, mouseY, partialTick);
			}

			@Override
			public List<? extends GuiEventListener> children() {
				return renderer.children();
			}

			@Override
			public List<? extends NarratableEntry> narratables() {
				return renderer.narratables();
			}
		}

		final class SectionEntry {
			private final BaseEntry entry;

			SectionEntry(Component title) {
				this.entry = new BaseEntry(new EntryRenderer() {
					@Override
					public void render(BaseEntry entry, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
						int left = entry.getContentX();
						int top = entry.getContentY();
						graphics.text(WaterworldConfigScreen.this.font, title, left + 4,
								top + (ROW_HEIGHT - WaterworldConfigScreen.this.font.lineHeight) / 2, 0xFFFF55);
					}

					@Override
					public List<? extends GuiEventListener> children() {
						return List.of();
					}

					@Override
					public List<? extends NarratableEntry> narratables() {
						return List.of();
					}
				});
			}

			BaseEntry asEntry() {
				return entry;
			}
		}

		final class OptionEntry {
			private final BaseEntry entry;

			OptionEntry(Component label, AbstractWidget widget) {
				this.entry = new BaseEntry(new EntryRenderer() {
					@Override
					public void render(BaseEntry entry, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
						int left = entry.getContentX();
						int top = entry.getContentY();
						int rowWidth = ConfigOptionsList.this.getRowWidth();
						int labelWidth = rowWidth / 2 - 4;
						int controlX = left + labelWidth + 8;
						int controlWidth = rowWidth - labelWidth - 8;

						graphics.text(WaterworldConfigScreen.this.font, label, left + 4,
								top + (ROW_HEIGHT - WaterworldConfigScreen.this.font.lineHeight) / 2, 0xFFFFFFFF);

						widget.setPosition(controlX, top + 2);
						widget.setWidth(controlWidth);
						widget.setHeight(ROW_HEIGHT - 4);
						widget.extractRenderState(graphics, mouseX, mouseY, partialTick);
					}

					@Override
					public List<? extends GuiEventListener> children() {
						return List.of(widget);
					}

					@Override
					public List<? extends NarratableEntry> narratables() {
						return List.of(widget);
					}
				});
			}

			BaseEntry asEntry() {
				return entry;
			}
		}

		private interface EntryRenderer {
			void render(BaseEntry entry, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick);
			List<? extends GuiEventListener> children();
			List<? extends NarratableEntry> narratables();
		}
	}
}
