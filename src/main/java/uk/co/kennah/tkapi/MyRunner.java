package uk.co.kennah.tkapi;

import java.io.Serializable;

public class MyRunner implements Serializable {
	private static final long serialVersionUID = 1L;
	public String name;
	public Double odds;

	public MyRunner(String name) {
		this.name = name;
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
}