package com.downsafio.menu;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/** Pantalla 2: menu del color elegido (titulo, mensaje, destinatarios, modo y LANZAR). */
public class ColorScreen extends Screen {
    private static final String[] MODOS = {"SOLO RULETA", "RULETA + TITULO", "RULETA + CHAT", "RULETA + AMBOS"};
    private static int modo = 1;
    private static boolean paraTodos = true;
    private static String ultimoTitulo = "";
    private static String ultimoMensaje = "";

    private final int idx;
    private TextFieldWidget titulo;
    private TextFieldWidget mensaje;
    private TextFieldWidget retraso;
    private ButtonWidget btnPara;
    private final ButtonWidget[] btnModo = new ButtonWidget[4];
    private int x0;
    private int top;

    public ColorScreen(int idx) {
        super(Text.literal(Cfg.get().colores.get(idx).nombreMenu));
        this.idx = idx;
    }

    @Override
    protected void init() {
        this.x0 = this.width / 2 - 140;
        this.top = Math.max(8, this.height / 2 - 118);
        int t = this.top;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> {
            guardarTextos();
            MinecraftClient.getInstance().setScreen(new RuletaMenuScreen());
        }).dimensions(this.x0 - 28, t, 22, 20).build());

        this.btnPara = ButtonWidget.builder(Text.literal(""), b -> {
            paraTodos = !paraTodos;
            refrescar();
        }).dimensions(this.x0 + 170, t, 110, 20).build();
        this.addDrawableChild(this.btnPara);

        this.titulo = new TextFieldWidget(this.textRenderer, this.x0, t + 44, 280, 20, Text.literal("Titulo"));
        this.titulo.setMaxLength(60);
        this.titulo.setText(ultimoTitulo);
        this.addDrawableChild(this.titulo);

        this.mensaje = new TextFieldWidget(this.textRenderer, this.x0, t + 84, 280, 20, Text.literal("Mensaje"));
        this.mensaje.setMaxLength(150);
        this.mensaje.setText(ultimoMensaje);
        this.addDrawableChild(this.mensaje);

        int n = Cfg.get().colores.get(this.idx).momentos.size();
        this.addDrawableChild(ButtonWidget.builder(Text.literal("MOMENTOS (" + n + ")"), b -> {
            guardarTextos();
            MinecraftClient.getInstance().setScreen(new MomentosScreen(this.idx));
        }).dimensions(this.x0 + 60, t + 112, 160, 20).build());

        for (int i = 0; i < 4; i++) {
            final int m = i;
            this.btnModo[i] = ButtonWidget.builder(Text.literal(""), b -> {
                modo = m;
                refrescar();
            }).dimensions(this.x0 + (i % 2) * 142, t + 138 + (i / 2) * 22, 138, 20).build();
            this.addDrawableChild(this.btnModo[i]);
        }

        this.retraso = new TextFieldWidget(this.textRenderer, this.x0 + 190, t + 186, 46, 20, Text.literal("Retraso"));
        this.retraso.setMaxLength(5);
        this.retraso.setText(String.valueOf(Cfg.get().retrasoAnuncio));
        this.addDrawableChild(this.retraso);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("LANZAR"), b -> lanzar())
                .dimensions(this.x0 + 70, t + 214, 140, 20).build());
        refrescar();
    }

    private void refrescar() {
        this.btnPara.setMessage(Text.literal("PARA: " + (paraTodos ? "TODOS" : "SOLO YO")));
        for (int i = 0; i < 4; i++) {
            this.btnModo[i].setMessage(Text.literal(i == modo ? "\u00bb " + MODOS[i] + " \u00ab" : MODOS[i]));
        }
    }

    private void guardarTextos() {
        ultimoTitulo = this.titulo.getText();
        ultimoMensaje = this.mensaje.getText();
    }

    private void lanzar() {
        guardarTextos();
        String t = ultimoTitulo.trim();
        String m = ultimoMensaje.trim();
        String sel = paraTodos ? "@a" : "@s";
        double seg = Cfg.get().retrasoAnuncio;
        try {
            seg = Math.max(0, Double.parseDouble(this.retraso.getText().trim().replace(',', '.')));
        } catch (NumberFormatException ignorada) {
            // se conserva el valor anterior
        }
        Cfg.get().retrasoAnuncio = seg;
        Cfg.get().guardar();

        Cmd.enviar("ruleta " + Cfg.CMDS[this.idx]);
        if (modo != 0 && (!t.isEmpty() || !m.isEmpty())) {
            final int modoFinal = modo;
            Cmd.despues(seg, () -> anunciar(sel, t, m, modoFinal));
        }
        this.close();
    }

    /** Se ejecuta DESPUES de la ruleta, para que la animacion no tape el anuncio. */
    static void anunciar(String sel, String t, String m, int modoFinal) {
        if (modoFinal == 1 || modoFinal == 3) {
            Cmd.enviar("title " + sel + " times 10 100 20");
            String grande = t.isEmpty() ? m : t;
            if (!t.isEmpty() && !m.isEmpty()) {
                Cmd.enviar("title " + sel + " subtitle " + Cmd.json(m, "white", false));
            }
            Cmd.enviar("title " + sel + " title " + Cmd.json(grande, "gold", true));
        }
        if (modoFinal == 2 || modoFinal == 3) {
            String texto = "[{\"text\":\"\u00bb \",\"color\":\"gold\"},{\"text\":\"" + Cmd.esc(t.isEmpty() ? "" : t + " ")
                    + "\",\"color\":\"gold\",\"bold\":true},{\"text\":\"" + Cmd.esc(m) + "\",\"color\":\"white\"}]";
            Cmd.enviar("tellraw " + sel + " " + texto);
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0xD0000000);
        int c = Cfg.COLORES[this.idx];
        int t = this.top;
        // icono redondo del color + nombre del menu
        for (int dy = -6; dy <= 6; dy++) {
            int w = (int) Math.sqrt(36 - dy * dy);
            ctx.fill(this.x0 + 8 - w, t + 10 + dy, this.x0 + 8 + w + 1, t + 11 + dy, c);
        }
        ctx.drawTextWithShadow(this.textRenderer, Text.literal(Cfg.get().colores.get(this.idx).nombreMenu), this.x0 + 22, t + 6, c);
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("T\u00cdTULO:"), this.x0, t + 32, 0xFFFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("MENSAJE:"), this.x0, t + 72, 0xFFFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("Mostrar el anuncio a los"), this.x0, t + 192, 0xFFBBBBBB);
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("seg."), this.x0 + 240, t + 192, 0xFFBBBBBB);
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("(15 = despu\u00e9s de que termine la ruleta)"), this.x0 + 30, t + 238, 0xFF777777);
        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
