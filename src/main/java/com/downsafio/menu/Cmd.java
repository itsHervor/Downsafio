package com.downsafio.menu;

import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Envio de comandos, textos JSON y tareas programadas (temporizadores del lado del cliente). */
public final class Cmd {
    public static final class Tarea {
        public int ticks;
        public final Runnable accion;
        public boolean cancelada;

        Tarea(int ticks, Runnable accion) {
            this.ticks = ticks;
            this.accion = accion;
        }
    }

    private static final List<Tarea> TAREAS = new ArrayList<>();
    /** Momentos activos: clave "color:id" -> tarea que los termina. */
    public static final Map<String, Tarea> ACTIVOS = new HashMap<>();

    private Cmd() {
    }

    public static void enviar(String comando) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.networkHandler.sendChatCommand(comando);
        }
    }

    public static void aviso(String texto) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(net.minecraft.text.Text.literal(texto), true);
        }
    }

    /** Ejecuta la accion dentro de N segundos (20 ticks por segundo). Devuelve null si se ejecuta al instante. */
    public static Tarea despues(double segundos, Runnable accion) {
        int ticks = (int) Math.round(segundos * 20.0);
        if (ticks <= 0) {
            accion.run();
            return null;
        }
        Tarea t = new Tarea(ticks, accion);
        TAREAS.add(t);
        return t;
    }

    public static void tick(MinecraftClient client) {
        if (client.player == null) {
            TAREAS.clear();
            ACTIVOS.clear();
            return;
        }
        for (Tarea t : new ArrayList<>(TAREAS)) {
            if (t.cancelada) {
                TAREAS.remove(t);
                continue;
            }
            t.ticks--;
            if (t.ticks <= 0) {
                TAREAS.remove(t);
                t.accion.run();
            }
        }
    }

    public static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static String json(String texto, String color, boolean negrita) {
        return "{\"text\":\"" + esc(texto) + "\",\"color\":\"" + color + "\",\"bold\":" + negrita + "}";
    }

    /** Acepta 30s, 5m, 1h, 1h30m, 90 (segundos) o mm:ss. Devuelve -1 si no se entiende. */
    public static int parseSegundos(String entrada) {
        try {
            String s = entrada.trim().toLowerCase();
            if (s.isEmpty()) {
                return -1;
            }
            if (s.contains(":")) {
                int total = 0;
                for (String p : s.split(":")) {
                    total = total * 60 + Integer.parseInt(p.trim());
                }
                return total;
            }
            int total = 0;
            StringBuilder num = new StringBuilder();
            for (char c : s.toCharArray()) {
                if (Character.isDigit(c)) {
                    num.append(c);
                } else if (c == 'h' || c == 'm' || c == 's') {
                    if (num.length() == 0) {
                        return -1;
                    }
                    int v = Integer.parseInt(num.toString());
                    num.setLength(0);
                    total += c == 'h' ? v * 3600 : c == 'm' ? v * 60 : v;
                } else if (c != ' ') {
                    return -1;
                }
            }
            if (num.length() > 0) {
                total += Integer.parseInt(num.toString());
            }
            return total;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
