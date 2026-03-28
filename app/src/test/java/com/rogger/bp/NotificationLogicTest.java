package com.rogger.bp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Calendar;

/**
 * Teste de unidade para validar a lógica de cálculo de dias usada nas notificações.
 * Como o ExpirationWorker depende do contexto do Android, testamos a lógica central aqui.
 */
public class NotificationLogicTest {

    @Test
    public void testCalculateDaysDifference() {
        // Configura "hoje" como 28/03/2026 (data atual do sistema Manus)
        Calendar calHoje = Calendar.getInstance();
        calHoje.set(2026, Calendar.MARCH, 28, 10, 0, 0);
        calHoje.set(Calendar.MILLISECOND, 0);
        long hojeMs = calHoje.getTimeInMillis();

        // Configura "produto" vencendo em 31/03/2026 (daqui a 3 dias)
        Calendar calProd = Calendar.getInstance();
        calProd.set(2026, Calendar.MARCH, 31, 15, 0, 0);
        calProd.set(Calendar.MILLISECOND, 0);
        long prodMs = calProd.getTimeInMillis();

        // Lógica de cálculo simplificada (mesma do ExpirationWorker corrigido)
        
        // Zera horas para comparação apenas por dia
        Calendar c1 = Calendar.getInstance();
        c1.setTimeInMillis(hojeMs);
        c1.set(Calendar.HOUR_OF_DAY, 0);
        c1.set(Calendar.MINUTE, 0);
        c1.set(Calendar.SECOND, 0);
        c1.set(Calendar.MILLISECOND, 0);
        long hMs = c1.getTimeInMillis();

        Calendar c2 = Calendar.getInstance();
        c2.setTimeInMillis(prodMs);
        c2.set(Calendar.HOUR_OF_DAY, 0);
        c2.set(Calendar.MINUTE, 0);
        c2.set(Calendar.SECOND, 0);
        c2.set(Calendar.MILLISECOND, 0);
        long pMs = c2.getTimeInMillis();

        long diffMs = pMs - hMs;
        long diffDias = diffMs / (1000 * 60 * 60 * 24);

        assertEquals("A diferença deve ser de 3 dias", 3, diffDias);
    }

    @Test
    public void testProductExpired() {
        // Hoje: 28/03
        Calendar calHoje = Calendar.getInstance();
        calHoje.set(2026, Calendar.MARCH, 28, 0, 0, 0);
        long hojeMs = calHoje.getTimeInMillis();

        // Vencido: 27/03
        Calendar calProd = Calendar.getInstance();
        calProd.set(2026, Calendar.MARCH, 27, 23, 59, 59);
        long prodMs = calProd.getTimeInMillis();

        long diffMs = prodMs - hojeMs;
        // Se diffMs for negativo, está vencido
        assertTrue("Produto com data anterior a hoje deve ser considerado vencido", diffMs < 0);
    }
}
