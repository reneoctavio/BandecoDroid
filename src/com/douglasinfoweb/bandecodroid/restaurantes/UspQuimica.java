package com.douglasinfoweb.bandecodroid.restaurantes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.MutableDateTime;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import android.util.Log;

import com.douglasinfoweb.bandecodroid.Cardapio;
import com.douglasinfoweb.bandecodroid.Cardapio.Refeicao;
import com.douglasinfoweb.bandecodroid.Main;
import com.douglasinfoweb.bandecodroid.R;
import com.douglasinfoweb.bandecodroid.Restaurante;
import com.douglasinfoweb.bandecodroid.Util;

@SuppressWarnings("serial")
public class UspQuimica extends Restaurante {
	boolean proximo;
	@Override
	public boolean atualizarCardapios(Main main) {
		Log.v("bandeco","ATUALIZAR");
		ArrayList<Cardapio> cardapios=new ArrayList<Cardapio>();
		try {
			String URL = "http://www.usp.br/coseas/cardapioquimica.html";
			Document doc = Jsoup.connect(URL).userAgent("Mozilla").header("Accept", "text/html").get();
			//Pega semana
			int semana=0;
			DateTime ultimaData = new DateTime();
			for (Element pre : doc.select("pre")) {
				String text = pre.text();
				if (text.contains("Semana")) {
					String[] dataSplited = text.split(" ")[4].split("/");
					ultimaData = new DateTime(
							Integer.parseInt(dataSplited[2])+2000,
							Integer.parseInt(dataSplited[1]),
							Integer.parseInt(dataSplited[0]), 
							0, 0 ,0);
					Log.v("usp-quimicas", "ultimaData: "+ultimaData.toString());
					semana = ultimaData.getWeekOfWeekyear();
				}
			}
			//Pega infos
			int rowN=0;
			for (Element row : doc.select("tr")) {
				Log.v("usp-quimicas", "tr "+rowN+": "+row);
				rowN++;
				if (rowN == 1) continue;
				//TODO USP
				int tdN=0;
				tdCardapio:
				for (Element td : row.select("td")) {
					Log.v("usp-quimicas", "td "+tdN+": "+td);
					Cardapio cardapio = new Cardapio();
					if (tdN == 0)
						cardapio.setRefeicao(Refeicao.ALMOCO);
					else
						cardapio.setRefeicao(Refeicao.JANTA);
					List<Element> preList = td.select("pre");
					if (preList.size() == 0) {
						tdN++;
						continue tdCardapio;
					}
					String dia = Util.removerEspacosDuplicados(preList.get(0).text()).toLowerCase();
					Log.v("usp-quimicas", "dia "+dia);
					int diaDaSemana=0;
					if (dia.contains("segunda")) {
						diaDaSemana=1;
					} else if (dia.contains("ter�a")) {
						diaDaSemana=2;
					} else if (dia.contains("quarta")) {
						diaDaSemana=3;
					} else if (dia.contains("quinta")) {
						diaDaSemana=4;
					} else if (dia.contains("sexta")) {
						diaDaSemana=5;
					} else if (dia.contains("s�bado")) {
						diaDaSemana=6;
					} else if (dia.contains("domingo")) {
						diaDaSemana=7;
					}
					MutableDateTime data = new MutableDateTime();
					data.setDayOfWeek(diaDaSemana);
					data.setWeekOfWeekyear(semana);
					data.setYear(ultimaData.getYear());
					cardapio.setData(data.toDateTime());
					int spanID=0;
					for (Element span : td.select("span")) {
						String text = span.text().trim();
						Log.v("usp-quimicas", "span "+spanID+": "+text);
						switch (spanID) {
							case 1: cardapio.setPratoPrincipal(text); break;
							case 2: cardapio.setPratoPrincipal(cardapio.getPratoPrincipal() + "\n"+text); break;		 
							case 3: cardapio.setSalada(text); break;
							case 4: cardapio.setSobremesa(text); break;
							case 5: cardapio.setSuco(text); break;
						}
						spanID++;
					}
					tdN++;
					if (cardapio.getPratoPrincipal() != null 
							&& Util.removerEspacosDuplicados(cardapio.getPratoPrincipal().trim()).length() > 2
							&& !cardapio.getPratoPrincipal().toLowerCase().contains("fechado")) {
						cardapios.add(cardapio);
					}
				}
			}		

			setCardapios(cardapios);
			removeCardapiosAntigos();
			main.save();
			return true;
		} catch (IOException e1) {
			e1.printStackTrace();
			return false;
		}
	}
	
	
	@Override
	public Boolean temQueAtualizar() {
		DateTime now = new DateTime(new Date());
		ArrayList<Cardapio> cardapios = getCardapios();
		if (cardapios.size() >= 1) {
			Cardapio ultimoCardapio = cardapios.get(cardapios.size() -1);
			//Se o ultimo que esta na memoria ainda eh dessa semana, nao precisa atualizar.
			if (ultimoCardapio.getData().getWeekOfWeekyear() >= now.getWeekOfWeekyear()
					&& ultimoCardapio.getData().getYear() >= now.getYear()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void removeCardapiosAntigos() {
		DateTime now = DateTime.now();
		for (Cardapio c : new ArrayList<Cardapio>(getCardapios())) {
			//Pra remover, tem que ser no minimo do mesmo dia
			if (c.getData().getDayOfYear() <= now.getDayOfYear() 
					&& c.getData().getYear() <= now.getYear()) {
				//Se for de dias que ja passaram, remove
				if (c.getData().getDayOfYear() < now.getDayOfYear()) { 
					getCardapios().remove(c);
				} else { //Se eh de hoje, ver se ja passou a hora do almo�o/janta
					switch (c.getRefeicao()) {
						case ALMOCO: if (now.getHourOfDay() >= 14) getCardapios().remove(c);
						case JANTA: if (now.getHourOfDay() >= 20) getCardapios().remove(c);
					}
				}
			}
		}
	}

	@Override
	public int getImagem() {
		return R.drawable.logo_usp_quimica;
	}

	@Override
	public String getNome() {
		return "USP Qu�mica";
	}
	

}