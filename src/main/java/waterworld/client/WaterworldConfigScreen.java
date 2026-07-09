package waterworld.client;

import com.google.gson.annotations.SerializedName;
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
import waterworld.ProjectWaterworld;
import waterworld.WaterworldConfig;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WaterworldConfigScreen extends Screen {
	private static final int ROW_HEIGHT = 24;
	private static final int LIST_TOP = 32;
	private static final int FOOTER_HEIGHT = 32;

	private final Screen parent;
	private final Map<Field, Object> originalValues = new HashMap<>();
	private final List<ConfigFieldBinding> bindings = new ArrayList<>();

	private ConfigOptionsList optionList;

	public WaterworldConfigScreen(Screen parent) {
		super(Component.literal("Project Waterworld"));
		this.parent = parent;
		snapshotConfig();
	}

	private void snapshotConfig() {
		for (Field field : getEditableConfigFields()) {
			try {
				originalValues.put(field, field.get(WaterworldConfig.INSTANCE));
			} catch (IllegalAccessException e) {
				ProjectWaterworld.LOGGER.warn("Failed to read config field {}", field.getName(), e);
			}
		}
	}

	@Override
	protected void init() {
		this.bindings.clear();
		this.bindings.addAll(discoverBindings());

		int listHeight = this.height - LIST_TOP - FOOTER_HEIGHT;
		this.optionList = new ConfigOptionsList(this.minecraft, this.width - 40, listHeight, LIST_TOP, ROW_HEIGHT);
		this.optionList.setX(20);

		for (ConfigFieldBinding binding : this.bindings) {
			this.optionList.add(new OptionEntry(binding));
		}

		this.addWidget(this.optionList);

		int buttonY = this.height - FOOTER_HEIGHT + 6;
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.saveAndClose())
				.pos(this.width / 2 - 154, buttonY)
				.size(150, 20)
				.build());
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> this.cancelAndClose())
				.pos(this.width / 2 + 4, buttonY)
				.size(150, 20)
				.build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float delta) {
		super.extractRenderState(drawContext, mouseX, mouseY, delta);
		if (this.optionList != null) {
			this.optionList.extractRenderState(drawContext, mouseX, mouseY, delta);
		}
		drawContext.centeredText(this.font, this.title, this.width / 2, 8, 0xFFFFFFFF);
	}

	private void saveAndClose() {
		for (ConfigFieldBinding binding : this.bindings) {
			try {
				applyBinding(binding);
			} catch (IllegalAccessException | NumberFormatException e) {
				ProjectWaterworld.LOGGER.warn("Failed to apply config field {}", binding.field().getName(), e);
			}
		}

		WaterworldConfig.INSTANCE.saveConfigFile(
				FabricLoader.getInstance().getConfigDir().resolve("project-waterworld.json").toFile());
		this.minecraft.gui.setScreen(this.parent);
	}

	private void cancelAndClose() {
		for (Map.Entry<Field, Object> entry : this.originalValues.entrySet()) {
			try {
				entry.getKey().set(WaterworldConfig.INSTANCE, entry.getValue());
			} catch (IllegalAccessException e) {
				ProjectWaterworld.LOGGER.warn("Failed to restore config field {}", entry.getKey().getName(), e);
			}
		}
		this.minecraft.gui.setScreen(this.parent);
	}

	private static void applyBinding(ConfigFieldBinding binding) throws IllegalAccessException {
		Field field = binding.field();
		AbstractWidget widget = binding.widget();
		Class<?> type = field.getType();

		if (widget instanceof CycleButton<?> cycleButton && type == boolean.class) {
			field.setBoolean(WaterworldConfig.INSTANCE, (Boolean) cycleButton.getValue());
			return;
		}

		if (widget instanceof EditBox editBox) {
			String text = editBox.getValue().trim();
			if (type == int.class) {
				field.setInt(WaterworldConfig.INSTANCE, Integer.parseInt(text));
			} else if (type == double.class) {
				field.setDouble(WaterworldConfig.INSTANCE, Double.parseDouble(text));
			} else if (type == String.class) {
				field.set(WaterworldConfig.INSTANCE, text);
			}
		}
	}

	private static List<Field> getEditableConfigFields() {
		List<Field> fields = new ArrayList<>();

		for (Field field : WaterworldConfig.class.getDeclaredFields()) {
			int modifiers = field.getModifiers();
			if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
				continue;
			}
			if (field.getName().startsWith("_comment")) {
				continue;
			}

			SerializedName serializedName = field.getAnnotation(SerializedName.class);
			if (serializedName == null) {
				continue;
			}

			String key = serializedName.value();
			if (key.startsWith("//")) {
				continue;
			}

			field.setAccessible(true);
			fields.add(field);
		}

		return fields;
	}

	private List<ConfigFieldBinding> discoverBindings() {
		List<ConfigFieldBinding> discovered = new ArrayList<>();

		for (Field field : getEditableConfigFields()) {
			SerializedName serializedName = field.getAnnotation(SerializedName.class);
			String key = serializedName.value();
			AbstractWidget widget = createWidget(field, Component.literal(key));
			if (widget == null) {
				ProjectWaterworld.LOGGER.warn("Skipping unsupported config field type: {}", field.getName());
				continue;
			}

			discovered.add(new ConfigFieldBinding(field, Component.literal(key), widget));
		}

		return discovered;
	}

	private AbstractWidget createWidget(Field field, Component label) {
		Class<?> type = field.getType();
		try {
			if (type == boolean.class) {
				boolean value = field.getBoolean(WaterworldConfig.INSTANCE);
				return CycleButton.onOffBuilder(value)
						.create(0, 0, 120, 20, Component.empty(), (button, newValue) -> {});
			}
			if (type == int.class || type == double.class || type == String.class) {
				Object value = field.get(WaterworldConfig.INSTANCE);
				EditBox editBox = new EditBox(this.font, 0, 0, 120, 20, label);
				editBox.setValue(String.valueOf(value));
				editBox.setMaxLength(256);
				return editBox;
			}
		} catch (IllegalAccessException e) {
			ProjectWaterworld.LOGGER.warn("Failed to read config field {}", field.getName(), e);
		}
		return null;
	}

	private final class ConfigOptionsList extends ContainerObjectSelectionList<OptionEntry> {
		ConfigOptionsList(Minecraft minecraft, int width, int height, int top, int itemHeight) {
			super(minecraft, width, height, top, itemHeight);
		}

		void add(OptionEntry entry) {
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
	}

	private final class OptionEntry extends ContainerObjectSelectionList.Entry<OptionEntry> {
		private final ConfigFieldBinding binding;

		OptionEntry(ConfigFieldBinding binding) {
			this.binding = binding;
		}

		@Override
		public void extractContent(
				GuiGraphicsExtractor graphics,
				int mouseX,
				int mouseY,
				boolean hovered,
				float partialTick
		) {
			int left = this.getContentX();
			int top = this.getContentY();
			int rowWidth = WaterworldConfigScreen.this.optionList.getRowWidth();
			int labelWidth = rowWidth / 2 - 4;
			int controlX = left + labelWidth + 8;
			int controlWidth = rowWidth - labelWidth - 8;

			graphics.text(
					WaterworldConfigScreen.this.font,
					this.binding.label(),
					left + 4,
					top + (ROW_HEIGHT - WaterworldConfigScreen.this.font.lineHeight) / 2,
					0xFFFFFFFF
			);

			AbstractWidget widget = this.binding.widget();
			widget.setPosition(controlX, top + 2);
			widget.setWidth(controlWidth);
			widget.setHeight(ROW_HEIGHT - 4);
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return List.of(this.binding.widget());
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return List.of(this.binding.widget());
		}
	}

	private record ConfigFieldBinding(Field field, Component label, AbstractWidget widget) {
	}
}
