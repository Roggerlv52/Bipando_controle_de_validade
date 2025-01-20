package com.rogger.bipando.database;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DatabaseTest  {
    public static List<Registro> gerarDadosFicticios(int quantidade) {
        List<Registro> dados = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < quantidade; i++) {
            Registro registro = new Registro();
            registro.id = i + 1;
            registro.type = "Tipo" + (random.nextInt(5) + 1); // Tipo aleatório
            registro.setname = "NomeSet" + (random.nextInt(1000) + 1); // Nome fictício
            registro.setnote = "Nota" + (random.nextInt(1000) + 1); // Nota fictícia
            registro.setbarcod = "123456789" + (random.nextInt(1000) + 1000); // Código de barras fictício
            registro.setdate = "2024-"+(random.nextInt(10) + 2)+"-" + (random.nextInt(30) + 1); // Data fictícia no formato YYYY-MM-DD
            registro.setUri = "https://res.cloudinary.com/fonte-online/image/upload/c_fill,h_600,q_auto,w_600/v1/PDO_PROD/943518_1";
            dados.add(registro);

        }
        return dados;
    }
}
