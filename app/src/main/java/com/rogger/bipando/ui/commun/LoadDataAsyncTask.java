package com.rogger.bipando.ui.commun;

import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ProgressBar;

import com.rogger.bipando.database.Registro;
import com.rogger.bipando.ui.home.AdapterHome;

import java.util.ArrayList;
import java.util.List;


public class LoadDataAsyncTask extends AsyncTask<Void, Void, List<Registro>> {
	private Context context;
	private AdapterHome adapter;
	private ProgressBar progressBar;

	public LoadDataAsyncTask(Context context, AdapterHome adapter, ProgressBar progressBar) {
		this.context = context;
		this.adapter = adapter;
		this.progressBar = progressBar;
	}

	@Override
	protected void onPreExecute() {
		// Antes de iniciar a carga dos dados
		progressBar.setVisibility(View.VISIBLE);
	}

	@Override
	protected List<Registro> doInBackground(Void[] arg0) {
		return carregarDadosDoBancoDeDados();
	}

	@Override
	protected void onPostExecute(List<Registro> result) {
		// Após buscar os dados
		progressBar.setVisibility(View.GONE);
		// Atualize o Adapter com os dados obtidos
		adapter.setDados(result);
		adapter.ordenarPorDiferencaDeDias();

	}

	private List<Registro> carregarDadosDoBancoDeDados() {
		// Lógica para recuperar dados do banco de dados, por exemplo, usando SQLiteOpenHelper ou Room
		// Retorne a lista de dados carregados
		List<Registro> dados = new ArrayList<>();

		return dados;

	}

}