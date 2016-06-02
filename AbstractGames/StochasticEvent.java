package AbstractGames;

/**
 *
 */
public abstract class StochasticEvent {
  protected double eventProbability;

  public void setEventProbability(double ep) { eventProbability = ep; }

  public double getEventProbability() { return eventProbability; }
}
