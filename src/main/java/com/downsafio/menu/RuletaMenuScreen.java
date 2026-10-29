package com.downsafio.menu;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/** Pantalla: rueda de 8 colores clicable + titulo/mensaje + botones. Envia comandos /ruleta al servidor. */
public class RuletaMenuScreen extends Screen {
    private static final String[] NOMBRES = {"ROJO OSCURO", "NARANJA", "AMARILLO", "VERDE AQUA", "AZUL CIELO", "AZUL MARINO", "MORADO CLARO", "ROSA PASTEL"};
    private static final String[] CMDS = {"roja", "naranja", "amarilla", "verde", "celeste", "azul", "morada", "rosa"};
    private static final int[] COLORES = {0xFFC0283A, 0xFFFF8A1F, 0xFFFFD60A, 0xFF1FE0B0, 0xFF5BC8FF, 0xFF3F63D6, 0xFFB79CFF, 0xFFFFB7D5};
    private static final int R = 70;
    private static final int ORO = 0xFFE8B923;

    private int seleccion = -1;
    private boolean aviso = true;
    private TextFieldWidget titulo;
    private TextFieldWidget mensaje;
    private ButtonWidget btnAviso;
    private int cx;
    private int cy;

    public RuletaMenuScreen() {
        super(Text.literal("Panel de Ruleta"));
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 175;
        int top = this.height / 2 - 80;
        this.cx = this.width / 2 + 95;
        this.cy = this.height / 2 - 8;

        this.titulo = new TextFieldWidget(this.textRenderer, left, top + 30, 160, 20, Text.literal("Titulo"));
        this.titulo.setMaxLength(60);
        this.addDrawableChild(this.titulo);

        this.mensaje = new TextFieldWidget(this.textRenderer, left, top + 75, 160, 20, Text.literal("Mensaje"));
        this.mensaje.setMaxLength(100);
        this.addDrawableChild(this.mensaje);

        this.btnAviso = ButtonWidget.builder(Text.literal(""), b -> {
            this.aviso = !this.aviso;
            actualizarBoton();
        }).dimensions(left, top + 105, 160, 20).build();
        this.addDrawableChild(this.btnAviso);
        actualizarBoton();

        int by = this.height / 2 + 78;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("LANZAR"), b -> lanzar())
                .dimensions(this.width / 2 - 160, by, 100, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("ALEATORIA"), b -> {
            enviar("ruleta random");
            this.close();
        }).dimensions(this.width / 2 - 50, by, 100, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("DETENER"), b -> enviar("ruleta stop"))
                .dimensions(this.width / 2 + 60, by, 100, 20).build());
    }

    private void actualizarBoton() {
        this.btnAviso.setMessage(Text.literal("Mostrar aviso: " + (this.aviso ? "SI" : "NO")));
    }

    private static void enviar(String comando) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.networkHandler.sendChatCommand(comando);
        }
    }

    private static String json(String texto, String color, boolean negrita) {
        String t = texto.replace("\\", "\\\\").replace("\"", "\\\"");
        return "{\"text\":\"" + t + "\",\"color\":\"" + color + "\",\"bold\":" + negrita + "}";
    }

    private void lanzar() {
        if (this.seleccion < 0) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) {
                mc.player.sendMessage(Text.literal("Elige un color en la ruleta primero"), true);
            }
            return;
        }
        String t = this.titulo.getText().trim();
        String m = this.mensaje.getText().trim();
        if (this.aviso && !t.isEmpty()) {
            if (!m.isEmpty()) {
                enviar("title @a subtitle " + json(m, "white", false));
            }
            enviar("title @a title " + json(t, "gold", true));
        }
        enviar("ruleta " + CMDS[this.seleccion]);
        this.close();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0xB0000000);
        int px = this.width / 2 - 185;
        int py = this.height / 2 - 100;
        ctx.fill(px, py, px + 370, py + 200, 0xFF14141F);
        ctx.fill(px, py, px + 370, py + 1, ORO);
        ctx.fill(px, py + 199, px + 370, py + 200, ORO);
        ctx.fill(px, py, px + 1, py + 200, ORO);
        ctx.fill(px + 369, py, px + 370, py + 200, ORO);

        ctx.drawTextWithShadow(this.textRenderer, Text.literal("ELIGE UNA RULETA"), px + 10, py + 8, 0xFFFF5C40);
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("TITULO:"), px + 10, py + 30, 0xFFFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("MENSAJE:"), px + 10, py + 75, 0xFFFFFFFF);

        dibujarRueda(ctx);
        super.render(ctx, mouseX, mouseY, delta);

        String nombre = this.seleccion >= 0 ? NOMBRES[this.seleccion] : "(toca un color)";
        int col = this.seleccion >= 0 ? COLORES[this.seleccion] : 0xFFAAAAAA;
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(nombre), this.cx, this.cy + R + 12, col);
    }

    private void dibujarRueda(DrawContext ctx) {
        for (int dy = -(R + 6); dy <= R + 6; dy += 2) {
            for (int dx = -(R + 6); dx <= R + 6; dx += 2) {
                double x = dx + 1;
                double y = dy + 1;
                double d = Math.hypot(x, y);
                if (d > R + 6) {
                    continue;
                }
                int color;
                if (d > R) {
                    color = ORO;
                } else if (d < 6) {
                    color = 0xFFFFE27A;
                } else {
                    double ang = Math.toDegrees(Math.atan2(x, -y));
                    if (ang < 0) {
                        ang += 360;
                    }
                    int idx = ((int) (ang / 45)) % 8;
                    double dentro = ang - idx * 45;
                    double borde = Math.min(dentro, 45 - dentro) * Math.PI / 180.0 * d;
                    if (borde < 1.6) {
                        color = 0xFF1A0A06;
                    } else {
                        color = COLORES[idx];
                        if (this.seleccion >= 0 && this.seleccion != idx) {
                            color = atenuar(color);
                        }
                    }
                }
                ctx.fill(this.cx + dx, this.cy + dy, this.cx + dx + 2, this.cy + dy + 2, color);
            }
        }
        // flecha (pin) arriba
        for (int i = 0; i < 18; i++) {
            int mitad = (18 - i) * 7 / 18;
            ctx.fill(this.cx - mitad, this.cy - R - 18 + i, this.cx + mitad + 1, this.cy - R - 17 + i, ORO);
        }
        ctx.fill(this.cx - 3, this.cy - R - 17, this.cx + 4, this.cy - R - 10, 0xFFFF5CAA);
    }

    private static int atenuar(int argb) {
        int r = ((argb >> 16) & 0xFF) * 40 / 100;
        int g = ((argb >> 8) & 0xFF) * 40 / 100;
        int b = (argb & 0xFF) * 40 / 100;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double dx = mouseX - this.cx;
        double dy = mouseY - this.cy;
        double d = Math.hypot(dx, dy);
        if (button == 0 && d <= R && d > 6) {
            double ang = Math.toDegrees(Math.atan2(dx, -dy));
            if (ang < 0) {
                ang += 360;
            }
            this.seleccion = ((int) (ang / 45)) % 8;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
