package uk.co.kennah.tkapi.model;

import java.io.Serializable;

/**
 * Represents a horse racing runner with its associated data.
 * This is a simple Plain Old Java Object (POJO) used to store details
 * about a runner, including its name, odds, and the event it's participating in.
 * It implements {@link Serializable} to allow for object serialization.
 */
public class MyRunner implements Serializable {
	private static final long serialVersionUID = 1L;
	public String name;
	public Double odds;
	public String event;

	/**
	 * Constructs a MyRunner instance with just a name.
	 *
	 * @param name The name of the runner.
	 */
	public MyRunner(String name) {
		this.name = name;
	}

	/**
	 * Constructs a MyRunner instance with a name, odds, and event details.
	 *
	 * @param name The name of the runner.
	 * @param odds The odds for the runner.
	 * @param event A string describing the event.
	 */
	public MyRunner(String name, Double odds, String event) {
		this.name = name;
		this.odds = odds;
		this.event = event;
	}

	/**
	 * Gets the name of the runner.
	 *
	 * @return The runner's name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name of the runner.
	 *
	 * @param name The new name for the runner.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gets the odds of the runner.
	 *
	 * @return The runner's odds.
	 */
	public Double getOdds() {
		return odds;
	}

	/**
	 * Sets the odds of the runner.
	 *
	 * @param odds The new odds for the runner.
	 */
	public void setOdds(Double odds) {
		this.odds = odds;
	}

	/**
	 * Gets the event description for the runner.
	 *
	 * @return The event description string.
	 */
	public String getEvent() {
		return event;
	}

	/**
	 * Sets the event description for the runner.
	 *
	 * @param event The new event description string.
	 */
	public void setEvent(String event) {
		this.event = event;
	}
}