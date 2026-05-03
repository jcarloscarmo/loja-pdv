package br.com.churrasco.util;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

public final class InputMaskUtil {

    private InputMaskUtil() {
    }

    public static void aplicarMascaraMonetaria(TextField field) {
        if (field == null) return;
        field.setTextFormatter(new TextFormatter<>(change -> {
            if (!change.isContentChange()) {
                return change;
            }

            String digitos = extrairDigitos(change.getControlNewText());
            change.setRange(0, change.getControlText().length());
            change.setText(formatarMonetario(digitos));
            change.selectRange(change.getControlNewText().length(), change.getControlNewText().length());
            return change;
        }));
        field.setText(formatarMonetario(extrairDigitos(field.getText())));
    }

    public static void aplicarMascaraCnpj(TextField field) {
        if (field == null) return;
        field.setTextFormatter(new TextFormatter<>(change -> {
            if (!change.isContentChange()) {
                return change;
            }

            String digitos = limitar(extrairDigitos(change.getControlNewText()), 14);
            change.setRange(0, change.getControlText().length());
            change.setText(formatarCnpj(digitos));
            change.selectRange(change.getControlNewText().length(), change.getControlNewText().length());
            return change;
        }));
        field.setText(formatarCnpj(limitar(extrairDigitos(field.getText()), 14)));
    }

    public static void aplicarMascaraTelefone(TextField field) {
        if (field == null) return;
        field.setTextFormatter(new TextFormatter<>(change -> {
            if (!change.isContentChange()) {
                return change;
            }

            String digitos = limitar(extrairDigitos(change.getControlNewText()), 11);
            change.setRange(0, change.getControlText().length());
            change.setText(formatarTelefone(digitos));
            change.selectRange(change.getControlNewText().length(), change.getControlNewText().length());
            return change;
        }));
        field.setText(formatarTelefone(limitar(extrairDigitos(field.getText()), 11)));
    }

    public static String extrairDigitos(String texto) {
        return texto == null ? "" : texto.replaceAll("[^0-9]", "");
    }

    private static String formatarMonetario(String digitos) {
        if (digitos == null || digitos.isEmpty()) {
            return "";
        }
        String valor = digitos == null || digitos.isEmpty() ? "0" : digitos;
        long inteiro = Long.parseLong(valor);
        long reais = inteiro / 100;
        long centavos = inteiro % 100;
        return reais + "," + String.format("%02d", centavos);
    }

    private static String formatarCnpj(String digitos) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digitos.length(); i++) {
            if (i == 2 || i == 5) sb.append('.');
            if (i == 8) sb.append('/');
            if (i == 12) sb.append('-');
            sb.append(digitos.charAt(i));
        }
        return sb.toString();
    }

    private static String formatarTelefone(String digitos) {
        if (digitos.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append('(');
        sb.append(digitos, 0, Math.min(2, digitos.length()));

        if (digitos.length() >= 2) {
            sb.append(") ");
        }

        if (digitos.length() <= 2) {
            return sb.toString();
        }

        String restante = digitos.substring(2);
        int blocoInicial = restante.length() > 8 ? 5 : 4;
        int primeiraParte = Math.min(blocoInicial, restante.length());
        sb.append(restante, 0, primeiraParte);

        if (restante.length() > primeiraParte) {
            sb.append('-');
            sb.append(restante.substring(primeiraParte));
        }

        return sb.toString();
    }

    private static String limitar(String texto, int maximo) {
        return texto.length() <= maximo ? texto : texto.substring(0, maximo);
    }
}
