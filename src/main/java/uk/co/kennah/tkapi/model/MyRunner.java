package uk.co.kennah.tkapi.model;

import java.io.Serializable;

public class MyRunner implements Serializable {
	private static final long serialVersionUID = 1L;
	public String name;
	public Double odds;
	public String event;

	public MyRunner(String name) {
		this.name = name;
	}

	public MyRunner(String name, Double odds, String event) {
		this.name = name;
		this.odds = odds;
		this.event = event;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Double getOdds() {
		return odds;
	}

	public void setOdds(Double odds) {
		this.odds = odds;
	}

		public String getEvent() {
		return event;
	}

	public void setEvent(String event) {
		this.event = event;
	}
}