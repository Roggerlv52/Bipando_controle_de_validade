package com.rogger.bipando.ui.base;

import android.graphics.Bitmap;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class SpinerData {
	private int day, month, year;
	private String barcode, dataSql;
	private Bitmap bitmap;

    public void setBitmap(Bitmap b){
		this.bitmap = b;
	}
	public  Bitmap getBitmap(){
		return this.bitmap;
	}
	public void setBarcode(String b) {
		this.barcode = b;
	}

	public String getBarcode() {
		return this.barcode;
	}

	public void setDataSql(String d) {
		this.dataSql = d;
	}

	public String getDataSql(String d) {
		String Agora = "";
		try {
			SimpleDateFormat dataFormatada = new SimpleDateFormat("yyyy-MM-dd", new Locale("pt", "BR"));
			Date datasalva = dataFormatada.parse(d);
			SimpleDateFormat minhadata = new SimpleDateFormat("yyyy-MM-dd", new Locale("pt", "BR"));
			Agora = minhadata.format(datasalva);

		} catch (Exception e) {
		}
		return Agora;
	}

	public int[] retornaVetor(String datas) {
		int year = 0;
		int month = 0;
		int day = 0;

		if (datas.isEmpty()) {
			try {
				Calendar calendar = Calendar.getInstance();
				day = calendar.get(Calendar.DAY_OF_MONTH);
				month = calendar.get(Calendar.MONTH); // O mês é baseado em zero, então adicionamos 1
				year = calendar.get(Calendar.YEAR);

			} catch (Exception e) {

			}
			int[] vetor = { day, month, year };
			return vetor;
		} else {
			SimpleDateFormat formatoDay = new SimpleDateFormat("dd");
			SimpleDateFormat formatoMes = new SimpleDateFormat("MM");
			SimpleDateFormat formatoAno = new SimpleDateFormat("yyyy");
			SimpleDateFormat formatoOriginal = new SimpleDateFormat("yyyy-MM-dd");
			try {
				Date data = formatoOriginal.parse(datas);
				day = Integer.parseInt(formatoDay.format(data));
				month = Integer.parseInt(formatoMes.format(data));
				year = Integer.parseInt(formatoAno.format(data));

			} catch (ParseException e) {

			}
			int[] vetor = { day, month, year };
			return vetor;
		}
	}

	public String makeDateString(int day, int month, int year) {
		return " " + day + " de " + getMonthFormat(month) + " de " + year;
	}

	private String getMonthFormat(int month) {
		if (month == 1)
			return "Jan";
		if (month == 2)
			return "Fev";
		if (month == 3)
			return "Mar";
		if (month == 4)
			return "Abr";
		if (month == 5)
			return "Mai";
		if (month == 6)
			return "Jun";
		if (month == 7)
			return "Jul";
		if (month == 8)
			return "Ago";
		if (month == 9)
			return "Set";
		if (month == 10)
			return "Out";
		if (month == 11)
			return "Nov";
		if (month == 12)
			return "Ded";
		return "Jan";
	}

}